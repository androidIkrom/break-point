package com.example.poststudy.data.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.*
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.BindException
import java.net.ConnectException
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

data class SessionData(
    val title: String,
    val questions: List<Question>,
    val slideTimerSeconds: Int,
    val testTimerSeconds: Int,
    val mode: LessonMode,
    val subjectId: Int,
    val encodedSlides: List<String> = emptyList(),
    // Nullable: Gson leaves absent fields null despite Kotlin defaults
    val kind: AssignmentKind? = AssignmentKind.Lesson
)

/** Answer of /session: either the group's session or a message to show the student. */
data class SessionResponse(val session: SessionData?, val error: String?)

private const val PING_ANSWER = "BreakPoint"

/** The other side runs an app whose network protocol differs from ours. */
private class VersionMismatchException(message: String) : Exception(message)

internal fun versionMismatchMessage(otherProtocol: Int, otherVersion: String?): String {
    val other = otherVersion?.let { " ($it)" }.orEmpty()
    return if (otherProtocol < com.example.poststudy.AppInfo.PROTOCOL_VERSION) {
        "Admin kompyuterida BreakPoint'ning eskiroq versiyasi$other o'rnatilgan. Adminda ham yangi versiyani o'rnating."
    } else {
        "Admin kompyuterida BreakPoint'ning yangiroq versiyasi$other o'rnatilgan. Bu kompyuterda ham dasturni yangilang."
    }
}
private const val WRONG_PASSWORD_MESSAGE = "Guruh paroli noto'g'ri."
/** Slows down password guessing from the LAN. */
private const val WRONG_PASSWORD_DELAY_MS = 700L

/** Host and port the student typed on the connect screen. */
data class ServerAddress(val host: String, val port: Int) {
    override fun toString(): String = if (port == NetworkManager.DEFAULT_PORT) host else "$host:$port"
}

/** Non-200 answer: something else (e.g. another web server) is listening on that port. */
private class UnexpectedResponseException(message: String) : Exception(message)

object NetworkManager {
    const val DEFAULT_PORT = 8080

    /** Ports tried in order when 8080 is taken by another program (Apache, Tomcat, ...). */
    val PORT_RANGE = DEFAULT_PORT..DEFAULT_PORT + 9

    private var server: HttpServer? = null
    private val gson = Gson()

    /** Why the server could not start, or null if it is running (or was never started). */
    @Volatile
    var serverError: String? = null
        private set

    val isServerRunning: Boolean get() = server != null

    private val _running = kotlinx.coroutines.flow.MutableStateFlow(false)
    /** Observable server state for the UI and the tray icon. */
    val running: kotlinx.coroutines.flow.StateFlow<Boolean> = _running

    /** Port the server actually listens on, or null if it is not running. */
    val serverPort: Int? get() = server?.address?.port

    // Adapter names that usually belong to VMs, VPNs and similar, not the real LAN card.
    private val virtualAdapterHints = listOf(
        "virtual", "vmware", "vbox", "hyper-v", "vethernet", "docker", "wsl",
        "tap-", "tun", "wireguard", "zerotier", "tailscale", "hamachi", "radmin",
        "bluetooth", "pseudo", "loopback"
    )

    fun getLocalIpAddress(): String = getLocalIpAddresses().firstOrNull() ?: "127.0.0.1"

    /** All usable IPv4 addresses of this machine, most likely LAN address first. */
    fun getLocalIpAddresses(): List<String> {
        val routedIp = routedIpv4()
        val candidates = mutableListOf<Pair<Int, String>>()
        try {
            for (iface in NetworkInterface.getNetworkInterfaces()) {
                if (iface.isLoopback || !iface.isUp || iface.isVirtual) continue
                val name = "${iface.name} ${iface.displayName}".lowercase()
                val isVirtualAdapter = virtualAdapterHints.any { name.contains(it) }

                for (addr in iface.inetAddresses) {
                    if (addr !is Inet4Address || addr.isLoopbackAddress || addr.isLinkLocalAddress) continue
                    val ip = addr.hostAddress
                    var score = when {
                        ip.startsWith("192.168.") -> 30
                        ip.startsWith("10.") -> 20
                        isPrivate172(ip) -> 20
                        else -> 10
                    }
                    if (isVirtualAdapter) score -= 25
                    // The address the OS would use for outgoing traffic is almost always the real LAN one
                    if (ip == routedIp && !isVirtualAdapter) score += 100
                    candidates += score to ip
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return candidates.sortedByDescending { it.first }.map { it.second }.distinct()
    }

    /** IPv4 of the interface that holds the default route. UDP connect sends no packets. */
    private fun routedIpv4(): String? = try {
        DatagramSocket().use { socket ->
            socket.connect(InetAddress.getByName("8.8.8.8"), 53)
            (socket.localAddress as? Inet4Address)
                ?.takeUnless { it.isAnyLocalAddress || it.isLoopbackAddress }
                ?.hostAddress
        }
    } catch (e: Exception) {
        null
    }

    private fun isPrivate172(ip: String): Boolean {
        if (!ip.startsWith("172.")) return false
        val second = ip.split(".").getOrNull(1)?.toIntOrNull() ?: return false
        return second in 16..31
    }

    /**
     * Parses what the student typed: "192.168.1.5", " 192.168.1.5:8080 ", "http://192.168.1.5/".
     * Returns null if the input is not a usable address.
     */
    fun parseAddress(input: String): ServerAddress? {
        var s = input.trim()
            .replace(',', '.')
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore('/')
        if (s.isEmpty() || s.any { it.isWhitespace() }) return null

        var port = DEFAULT_PORT
        if (s.count { it == ':' } == 1) {
            port = s.substringAfter(':').toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            s = s.substringBefore(':')
        } else if (s.contains(':')) {
            return null
        }

        val looksNumeric = s.all { it.isDigit() || it == '.' }
        if (looksNumeric) {
            val parts = s.split(".")
            if (parts.size != 4 || parts.any { it.isEmpty() || it.length > 3 || it.toInt() > 255 }) return null
        } else if (!s.all { it.isLetterOrDigit() || it == '-' || it == '.' }) {
            return null
        }
        return ServerAddress(s, port)
    }

    // --- Server (admin side) ---

    /**
     * Starts the session server on [port], or on the first free port of [PORT_RANGE] if [port] is null.
     * Returns false and sets [serverError] if it could not start.
     */
    fun startServer(
        port: Int? = null,
        getSession: (groupId: Int) -> Result<SessionData>,
        onRecordReceived: (ExamRecord) -> Unit,
        adminName: () -> String = { "" }
    ): Boolean {
        if (server != null) return true

        val ports = if (port != null) listOf(port) else PORT_RANGE.toList()
        for (candidate in ports) {
            try {
                server = createServer(candidate, getSession, onRecordReceived)
                LanDiscovery.startResponder(candidate, adminName)
                serverError = null
                _running.value = true
                println("Server started on 0.0.0.0:$candidate")
                return true
            } catch (e: BindException) {
                println("Port $candidate is busy, trying next")
            } catch (e: Exception) {
                serverError = "Serverni ishga tushirib bo'lmadi: ${e.message}"
                e.printStackTrace()
                return false
            }
        }
        serverError = if (ports.size == 1) {
            "${ports.single()}-port band."
        } else {
            "${ports.first()}–${ports.last()} portlarning barchasi band. Ularni ishlatayotgan dasturlarni yoping."
        }
        return false
    }

    private fun createServer(
        port: Int,
        getSession: (groupId: Int) -> Result<SessionData>,
        onRecordReceived: (ExamRecord) -> Unit
    ): HttpServer =
        // Bind to 0.0.0.0 to listen on all available network interfaces (LAN, WiFi, etc.)
        HttpServer.create(InetSocketAddress("0.0.0.0", port), 0).apply {
            createContext("/ping") { exchange ->
                handle(exchange, "GET") {
                    exchange.sendText(
                        200,
                        "$PING_ANSWER;protocol=${com.example.poststudy.AppInfo.PROTOCOL_VERSION};version=${com.example.poststudy.AppInfo.version}"
                    )
                }
            }

            createContext("/session") { exchange ->
                handle(exchange, "GET") {
                    val groupId = exchange.authorizedGroupId()
                    val response = if (groupId == null) {
                        SessionResponse(null, WRONG_PASSWORD_MESSAGE)
                    } else {
                        getSession(groupId).fold(
                            onSuccess = { SessionResponse(it, null) },
                            onFailure = { SessionResponse(null, it.message ?: "Sessiyani tayyorlab bo'lmadi.") }
                        )
                    }
                    exchange.sendJson(200, gson.toJson(response))
                }
            }

            createContext("/groups") { exchange ->
                handle(exchange, "GET") {
                    val subjectId = exchange.queryParam("subjectId")?.toIntOrNull()
                    val groups = AppContainer.localRepository.getGroupOverviews(subjectId).map { it.forStudents() }
                    exchange.sendJson(200, gson.toJson(groups))
                }
            }

            createContext("/verify-group") { exchange ->
                handle(exchange, "GET") {
                    if (exchange.authorizedGroupId() == null) {
                        exchange.sendText(403, WRONG_PASSWORD_MESSAGE)
                    } else {
                        exchange.sendText(200, "OK")
                    }
                }
            }

            createContext("/students") { exchange ->
                handle(exchange, "GET") {
                    val groupId = exchange.authorizedGroupId()
                    if (groupId == null) {
                        exchange.sendText(403, WRONG_PASSWORD_MESSAGE)
                    } else {
                        val students = runBlocking { AppContainer.localRepository.getStudentsByGroup(groupId).first() }
                        exchange.sendJson(200, gson.toJson(students))
                    }
                }
            }

            createContext("/create-student") { exchange ->
                handle(exchange, "POST") {
                    val body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).readText()
                    val request = gson.fromJson(body, Student::class.java)
                    val name = request?.name?.trim().orEmpty()
                    if (exchange.authorizedGroupId().let { it == null || it != request?.groupId }) {
                        exchange.sendText(403, WRONG_PASSWORD_MESSAGE)
                    } else if (name.isEmpty()) {
                        exchange.sendText(400, "name required")
                    } else {
                        val repo = AppContainer.localRepository
                        // A retried request must not create the same student twice
                        val existing = runBlocking { repo.getStudentsByGroup(request.groupId).first() }
                            .firstOrNull { it.name.equals(name, ignoreCase = true) }
                        val id = existing?.id ?: runBlocking { repo.addStudent(name, request.groupId).first() }
                        exchange.sendText(200, id.toString())
                    }
                }
            }

            createContext("/submit") { exchange ->
                handle(exchange, "POST") {
                    val body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).readText()
                    val record = gson.fromJson(body, ExamRecord::class.java)
                    if (exchange.authorizedGroupId().let { it == null || it != record?.groupId }) {
                        exchange.sendText(403, WRONG_PASSWORD_MESSAGE)
                    } else if (record == null || record.totalQuestions <= 0) {
                        exchange.sendText(400, "invalid record")
                    } else {
                        onRecordReceived(record)
                        exchange.sendText(200, "OK")
                    }
                }
            }
            executor = Executors.newCachedThreadPool()
            start()
        }

    fun stopServer() {
        LanDiscovery.stopResponder()
        server?.stop(0)
        server = null
        serverError = null
        _running.value = false
    }

    private inline fun handle(exchange: HttpExchange, method: String, block: () -> Unit) {
        try {
            if (exchange.requestMethod != method) {
                exchange.sendText(405, "Method not allowed")
            } else {
                block()
            }
        } catch (e: Exception) {
            println("Error serving ${exchange.requestURI}: ${e.message}")
            e.printStackTrace()
            try {
                exchange.sendText(500, "Server error")
            } catch (_: Exception) {
                // Headers were already sent
            }
        } finally {
            exchange.close()
        }
    }

    private fun HttpExchange.sendJson(code: Int, json: String) =
        send(code, json, "application/json; charset=utf-8")

    private fun HttpExchange.sendText(code: Int, text: String) =
        send(code, text, "text/plain; charset=utf-8")

    private fun HttpExchange.send(code: Int, body: String, contentType: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        responseHeaders.set("Content-Type", contentType)
        sendResponseHeaders(code, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    /** Group id from the query if its password matches, otherwise null (after a short delay). */
    private fun HttpExchange.authorizedGroupId(): Int? {
        val groupId = queryParam("groupId")?.toIntOrNull()
        val password = queryParam("password").orEmpty()
        if (groupId != null && AppContainer.localRepository.checkGroupPassword(groupId, password)) return groupId
        Thread.sleep(WRONG_PASSWORD_DELAY_MS)
        return null
    }

    private fun HttpExchange.queryParam(name: String): String? =
        requestURI.rawQuery
            ?.split("&")
            ?.map { it.split("=", limit = 2) }
            ?.firstOrNull { it[0] == name }
            ?.getOrNull(1)
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }

    // --- Client (student side) ---

    private fun hasExplicitPort(input: String): Boolean =
        input.trim().removePrefix("http://").removePrefix("https://").substringBefore('/').contains(':')

    /**
     * Finds the admin server from what the student typed. Without an explicit port every port of
     * [PORT_RANGE] is tried, because the admin falls back to them when 8080 is busy.
     */
    fun connect(input: String): Result<ServerAddress> {
        val parsed = parseAddress(input)
            ?: return Result.failure(Exception("IP manzil noto'g'ri. Masalan: 192.168.1.5"))
        val ports = if (hasExplicitPort(input)) listOf(parsed.port) else PORT_RANGE.toList()

        var firstError: Throwable? = null
        for (port in ports) {
            val address = parsed.copy(port = port)
            val result = ping(address)
            result.onSuccess { return Result.success(address) }
            val error = result.exceptionOrNull()!!
            if (firstError == null) firstError = error
            // A refused connection or a foreign web server means "wrong port"; anything else is a host problem
            val wrongPort = error is UnexpectedResponseException ||
                error.cause is ConnectException || error.cause is JsonParseException
            if (!wrongPort) return Result.failure(error)
        }
        if (ports.size == 1) return Result.failure(firstError!!)
        return Result.failure(
            Exception(
                "${parsed.host} da BreakPoint serveri topilmadi (${ports.first()}–${ports.last()} portlar tekshirildi). " +
                    "Admin dasturga kirganini va Windows Firewall Java'ga ruxsat berganini tekshiring."
            )
        )
    }

    private fun ping(address: ServerAddress): Result<Unit> =
        request(address, "/ping") { text ->
            val parts = text.trim().split(";")
            if (parts.first() != PING_ANSWER) {
                throw UnexpectedResponseException("$address da BreakPoint emas, boshqa dastur ishlayapti.")
            }
            val fields = parts.drop(1).mapNotNull { it.split("=", limit = 2).takeIf { kv -> kv.size == 2 } }
                .associate { (k, v) -> k to v }
            // Builds before versioning answered a bare "BreakPoint"
            val protocol = fields["protocol"]?.toIntOrNull() ?: 1
            if (protocol != com.example.poststudy.AppInfo.PROTOCOL_VERSION) {
                throw VersionMismatchException(versionMismatchMessage(protocol, fields["version"]))
            }
        }

    private fun groupQuery(groupId: Int, password: String): String =
        "groupId=$groupId&password=${URLEncoder.encode(password, StandardCharsets.UTF_8)}"

    fun verifyGroup(address: ServerAddress, groupId: Int, password: String): Result<Unit> =
        request(address, "/verify-group", query = groupQuery(groupId, password)) { }

    fun fetchSession(address: ServerAddress, groupId: Int, password: String): Result<SessionData> =
        request(address, "/session", query = groupQuery(groupId, password), readTimeoutMs = 60_000) { text ->
            gson.fromJson(text, SessionResponse::class.java)
        }.mapCatching { response ->
            response.session ?: throw Exception(response.error ?: "Sessiyani yuklab bo'lmadi.")
        }

    /** All groups of the admin computer with their subject and current assignment. */
    fun fetchGroups(address: ServerAddress): Result<List<GroupOverview>> =
        request(address, "/groups") { text ->
            gson.fromJson<List<GroupOverview>>(text, object : TypeToken<List<GroupOverview>>() {}.type).orEmpty()
        }

    fun fetchStudents(address: ServerAddress, groupId: Int, password: String): Result<List<Student>> =
        request(address, "/students", query = groupQuery(groupId, password)) { text ->
            gson.fromJson<List<Student>>(text, object : TypeToken<List<Student>>() {}.type).orEmpty()
        }

    fun createStudentRemote(address: ServerAddress, name: String, groupId: Int, password: String): Result<Int> =
        request(
            address, "/create-student",
            query = groupQuery(groupId, password),
            body = gson.toJson(Student(0, name, groupId))
        ) { text ->
            text.trim().toIntOrNull() ?: throw JsonParseException("Bad id: $text")
        }

    fun submitResult(address: ServerAddress, record: ExamRecord, password: String): Result<Unit> =
        request(address, "/submit", query = groupQuery(record.groupId ?: -1, password), body = gson.toJson(record)) { }

    private fun <T> request(
        address: ServerAddress,
        path: String,
        query: String? = null,
        body: String? = null,
        readTimeoutMs: Int = 15_000,
        parse: (String) -> T
    ): Result<T> {
        var connection: HttpURLConnection? = null
        return try {
            // The query is already URL-encoded, so build the URI from a string
            val url = URI("http://${address.host}:${address.port}$path" + (query?.let { "?$it" } ?: "")).toURL()
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = readTimeoutMs
                useCaches = false
                if (body != null) {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                } else {
                    requestMethod = "GET"
                }
            }
            val code = connection.responseCode
            if (code == 403) {
                return Result.failure(Exception(WRONG_PASSWORD_MESSAGE))
            }
            if (code != 200) {
                return Result.failure(
                    UnexpectedResponseException("$address BreakPoint serveri emas yoki xatolik qaytardi (kod $code).")
                )
            }
            val text = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            Result.success(parse(text))
        } catch (e: UnexpectedResponseException) {
            Result.failure(e)
        } catch (e: VersionMismatchException) {
            Result.failure(e)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(describeError(e, address), e))
        } finally {
            connection?.disconnect()
        }
    }

    private fun describeError(e: Exception, address: ServerAddress): String = when (e) {
        is SocketTimeoutException ->
            "$address javob bermadi (vaqt tugadi). IP manzilni va admin kompyuteridagi Windows Firewall sozlamalarini tekshiring."
        is ConnectException, is NoRouteToHostException ->
            "$address manziliga ulanib bo'lmadi. Admin dasturga kirganini, ikkala kompyuter bitta tarmoqda ekanini va Firewall Java'ga ruxsat berganini tekshiring."
        is UnknownHostException ->
            "Manzil topilmadi: ${address.host}"
        is JsonParseException ->
            "Admin kompyuteri javobini o'qib bo'lmadi. Ikkala kompyuterda dastur versiyasi bir xil ekanini tekshiring."
        else ->
            "Tarmoq xatoligi: ${e.message ?: e.javaClass.simpleName}"
    }
}
