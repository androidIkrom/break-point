package com.example.poststudy.data.network

import com.google.gson.Gson
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.concurrent.thread

/** An admin computer that answered the LAN search. [problem] is set when it cannot be used. */
data class ActiveAdmin(val name: String, val address: ServerAddress, val problem: String? = null)

private data class DiscoveryReply(
    val app: String?,
    val id: String?,
    val name: String?,
    val port: Int?,
    val protocol: Int? = null,
    val version: String? = null
)

/**
 * Lets students find running admins without typing an IP: students broadcast a UDP question,
 * every admin with the server on answers with its name and HTTP port.
 */
object LanDiscovery {
    private const val QUESTION = "BREAKPOINT_DISCOVER_V1"
    private const val APP = "BreakPoint"

    /** Fixed so students know where to ask; tests move it to avoid clashing with a running app. */
    @Volatile
    var port: Int = 8099

    private val gson = Gson()

    @Volatile
    private var socket: DatagramSocket? = null

    // --- Admin side ---

    /** Starts answering searches. Failure only disables discovery; connecting by IP still works. */
    fun startResponder(httpPort: Int, adminName: () -> String) {
        stopResponder()
        val instanceId = UUID.randomUUID().toString()
        val listenPort = port
        val responder = try {
            DatagramSocket(null).apply {
                reuseAddress = true
                // Not `port` here: inside apply that is the socket's own (unset) remote port
                bind(InetSocketAddress(listenPort))
            }
        } catch (e: Exception) {
            println("LAN discovery is off: ${e.message}")
            return
        }
        socket = responder
        thread(isDaemon = true, name = "breakpoint-discovery") {
            val buffer = ByteArray(512)
            while (!responder.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    responder.receive(packet)
                    val text = String(packet.data, packet.offset, packet.length, StandardCharsets.UTF_8)
                    if (text.trim() != QUESTION) continue
                    val name = runCatching { adminName() }
                        .onFailure { println("Discovery: admin name unavailable: ${it.message}") }
                        .getOrNull()?.takeIf { it.isNotBlank() }
                        ?: "Admin (${runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("?")})"
                    val reply = gson.toJson(
                        DiscoveryReply(APP, instanceId, name, httpPort, com.example.poststudy.AppInfo.PROTOCOL_VERSION, com.example.poststudy.AppInfo.version)
                    ).toByteArray(StandardCharsets.UTF_8)
                    responder.send(DatagramPacket(reply, reply.size, packet.socketAddress))
                } catch (e: Exception) {
                    if (!responder.isClosed) println("Discovery error: ${e.message}")
                }
            }
        }
    }

    fun stopResponder() {
        socket?.close()
        socket = null
    }

    // --- Student side ---

    /** Asks the local network for running admins and collects answers for [timeoutMs]. */
    fun discover(timeoutMs: Long = 1500): List<ActiveAdmin> {
        val found = LinkedHashMap<String, ActiveAdmin>()
        val localIps = NetworkManager.getLocalIpAddresses()
        try {
            DatagramSocket().use { s ->
                s.broadcast = true
                s.soTimeout = 200
                val question = QUESTION.toByteArray(StandardCharsets.UTF_8)
                for (target in targets()) {
                    runCatching { s.send(DatagramPacket(question, question.size, target, port)) }
                }
                val deadline = System.currentTimeMillis() + timeoutMs
                val buffer = ByteArray(1024)
                while (System.currentTimeMillis() < deadline) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        s.receive(packet)
                    } catch (e: SocketTimeoutException) {
                        continue
                    }
                    val reply = runCatching {
                        gson.fromJson(String(packet.data, packet.offset, packet.length, StandardCharsets.UTF_8), DiscoveryReply::class.java)
                    }.getOrNull() ?: continue
                    if (reply.app != APP || reply.id == null || reply.name == null || reply.port == null) continue
                    val protocol = reply.protocol ?: 1
                    val problem = if (protocol != com.example.poststudy.AppInfo.PROTOCOL_VERSION) {
                        versionMismatchMessage(protocol, reply.version)
                    } else null
                    val admin = ActiveAdmin(reply.name, ServerAddress(packet.address.hostAddress, reply.port), problem)
                    val existing = found[reply.id]
                    // One admin may answer from several of its addresses; keep the most useful one
                    if (existing == null || addressRank(admin.address.host, localIps) < addressRank(existing.address.host, localIps)) {
                        found[reply.id] = admin
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return found.values.sortedBy { it.name.lowercase() }
    }

    /**
     * Lower is better. A remote admin answers from its real LAN address; an admin on this very
     * computer may answer from loopback or a virtual adapter, so prefer our best LAN address then.
     */
    private fun addressRank(host: String, localIps: List<String>): Int = when {
        host.startsWith("127.") -> 1000
        host in localIps -> 10 + localIps.indexOf(host)
        else -> 0
    }

    private fun targets(): List<InetAddress> {
        val result = mutableListOf<InetAddress>(InetAddress.getByName("255.255.255.255"), InetAddress.getLoopbackAddress())
        runCatching {
            for (iface in NetworkInterface.getNetworkInterfaces()) {
                if (!iface.isUp || iface.isLoopback) continue
                iface.interfaceAddresses.mapNotNullTo(result) { it.broadcast }
            }
        }
        return result.distinct()
    }
}
