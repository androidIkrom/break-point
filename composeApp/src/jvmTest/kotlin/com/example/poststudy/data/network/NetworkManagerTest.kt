package com.example.poststudy.data.network

import com.example.poststudy.TestFixtures
import com.example.poststudy.data.session.SessionBuilder
import com.example.poststudy.data.session.toPreparedSession
import com.example.poststudy.data.session.toSessionData
import com.example.poststudy.di.AppContainer
import com.example.poststudy.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import kotlin.test.*

class NetworkManagerTest {

    @Test
    fun parseAddressAcceptsCommonInput() {
        assertEquals(ServerAddress("192.168.1.5", 8080), NetworkManager.parseAddress("192.168.1.5"))
        assertEquals(ServerAddress("192.168.1.5", 8080), NetworkManager.parseAddress("  192.168.1.5  "))
        assertEquals(ServerAddress("192.168.1.5", 9000), NetworkManager.parseAddress("192.168.1.5:9000"))
        assertEquals(ServerAddress("192.168.1.5", 8080), NetworkManager.parseAddress("http://192.168.1.5:8080/"))
        assertEquals(ServerAddress("192.168.1.5", 8080), NetworkManager.parseAddress("192,168,1,5"))
        assertEquals(ServerAddress("teacher-pc", 8080), NetworkManager.parseAddress("teacher-pc"))
    }

    @Test
    fun parseAddressRejectsBadInput() {
        assertNull(NetworkManager.parseAddress(""))
        assertNull(NetworkManager.parseAddress("192.168.1"))
        assertNull(NetworkManager.parseAddress("192.168.1.256"))
        assertNull(NetworkManager.parseAddress("192.168.1.5:abc"))
        assertNull(NetworkManager.parseAddress("192.168.1.5:70000"))
        assertNull(NetworkManager.parseAddress("192.168 .1.5"))
    }

    @Test
    fun localAddressesAreIpv4() {
        val addresses = NetworkManager.getLocalIpAddresses()
        println("Detected addresses: $addresses")
        addresses.forEach {
            assertNotNull(NetworkManager.parseAddress(it), "Not a valid address: $it")
        }
    }

    @Test
    fun unreachableServerGivesReadableError() {
        val result = NetworkManager.connect("127.0.0.1:${freePort()}")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("ulanib bo'lmadi"))
    }

    @Test
    fun fullStudentFlowOverHttp() {
        TestFixtures.initDatabase()
        val repo = AppContainer.localRepository
        val subjectId = runBlocking { repo.addSubject("Tarix").first() }
        val password = "a&b= ü#1"
        val assignedGroup = runBlocking { repo.addGroup("101-guruh", subjectId, password).first() }
        val otherLockedGroup = runBlocking { repo.addGroup("103-guruh", subjectId, "boshqa").first() }
        val emptyGroup = runBlocking { repo.addGroup("102-guruh", subjectId).first() }

        val dir = TestFixtures.tempDir()
        repo.addLesson(
            Lesson(
                title = "Rim tarixi",
                presentationPath = TestFixtures.pptx(dir, 2),
                testPath = TestFixtures.docx(dir, 3),
                slideTimerSeconds = 300,
                testTimerSeconds = 600,
                mode = LessonMode.ReAppropriation,
                subjectId = subjectId
            )
        )
        val lesson = runBlocking { repo.getAllLessons(subjectId).first().single() }
        repo.setGroupAssignment(GroupAssignment(assignedGroup, AssignmentKind.Lesson, lesson.id, LessonMode.ReAppropriation))

        val received = mutableListOf<ExamRecord>()
        val port = freePort()
        assertTrue(
            NetworkManager.startServer(
                port,
                { groupId -> SessionBuilder.buildForGroup(groupId).map { it.toSessionData() } },
                { received += it }
            )
        )
        try {
            val address = NetworkManager.connect("127.0.0.1:$port").getOrThrow()

            val groups = NetworkManager.fetchGroups(address).getOrThrow().filter { it.subjectId == subjectId }
            assertEquals(listOf("101-guruh", "102-guruh", "103-guruh"), groups.map { it.name })
            val overview = groups.first { it.id == assignedGroup }
            assertEquals("Tarix", overview.subjectName)
            assertEquals("Rim tarixi", overview.assignmentTitle)
            assertEquals(LessonMode.ReAppropriation, overview.mode)
            assertTrue(overview.isAssigned)
            assertFalse(groups.first { it.id == emptyGroup }.isAssigned)
            // Students learn only that a password exists, never the password itself
            assertTrue(overview.isLocked)
            assertNull(overview.password)
            assertFalse(groups.first { it.id == emptyGroup }.isLocked)

            // Wrong password is rejected everywhere
            val wrong = "noto'g'ri"
            assertEquals("Guruh paroli noto'g'ri.", NetworkManager.verifyGroup(address, assignedGroup, wrong).exceptionOrNull()?.message)
            assertEquals("Guruh paroli noto'g'ri.", NetworkManager.fetchSession(address, assignedGroup, wrong).exceptionOrNull()?.message)
            assertTrue(NetworkManager.fetchStudents(address, assignedGroup, wrong).isFailure)
            assertTrue(NetworkManager.createStudentRemote(address, "Begona", assignedGroup, wrong).isFailure)
            assertTrue(NetworkManager.verifyGroup(address, 999_999, "").isFailure)
            NetworkManager.verifyGroup(address, assignedGroup, password).getOrThrow()
            NetworkManager.verifyGroup(address, emptyGroup, "").getOrThrow()

            // The assigned group gets slides and questions over the wire
            val session = NetworkManager.fetchSession(address, assignedGroup, password).getOrThrow().toPreparedSession()
            assertEquals("Rim tarixi", session.title)
            assertEquals(AssignmentKind.Lesson, session.kind)
            assertEquals(2, session.slides.size)
            assertEquals(3, session.questions.size)

            // A group without an assignment gets the explanation, not a crash
            val error = NetworkManager.fetchSession(address, emptyGroup, "").exceptionOrNull()
            assertNotNull(error)
            assertTrue(error.message!!.contains("biriktirilmagan"))

            // Cyrillic/Uzbek names must survive the round trip, and a retry must not duplicate
            val id1 = NetworkManager.createStudentRemote(address, "Ғулом O'ktamov", assignedGroup, password).getOrThrow()
            val id2 = NetworkManager.createStudentRemote(address, "ғулом o'ktamov", assignedGroup, password).getOrThrow()
            assertEquals(id1, id2)
            val students = NetworkManager.fetchStudents(address, assignedGroup, password).getOrThrow()
            assertEquals(listOf("Ғулом O'ktamov"), students.map { it.name })

            val record = ExamRecord(
                studentName = "Ғулом O'ktamov",
                lessonTitle = "Rim tarixi",
                totalQuestions = 3,
                correctAnswers = 2,
                wrongAnswers = 1,
                wrongDetails = "",
                timeSpentSeconds = 42,
                timestamp = 1L,
                groupId = assignedGroup,
                studentId = id1,
                subjectId = subjectId
            )
            assertTrue(NetworkManager.submitResult(address, record, wrong).isFailure)
            assertTrue(received.isEmpty())
            // A valid password for another group does not open this one
            assertTrue(NetworkManager.submitResult(address, record.copy(groupId = otherLockedGroup), password).isFailure)
            NetworkManager.submitResult(address, record, password).getOrThrow()
            assertEquals(listOf(record), received)

            assertTrue(NetworkManager.createStudentRemote(address, "   ", assignedGroup, password).isFailure)
        } finally {
            NetworkManager.stopServer()
        }
    }

    @Test
    fun serverFallsBackWhenDefaultPortIsBusyAndClientFindsIt() {
        // Occupy 8080 with a foreign web server that answers 404 (like Apache), unless something already does
        val foreign = try {
            com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("0.0.0.0", NetworkManager.DEFAULT_PORT), 0)
                .apply {
                    createContext("/") { it.sendResponseHeaders(404, -1); it.close() }
                    start()
                }
        } catch (e: java.net.BindException) {
            null
        }
        try {
            assertTrue(NetworkManager.startServer(getSession = { Result.failure(Exception("-")) }, onRecordReceived = {}))
            val port = NetworkManager.serverPort!!
            assertNotEquals(NetworkManager.DEFAULT_PORT, port)
            assertTrue(port in NetworkManager.PORT_RANGE)
            assertNull(NetworkManager.serverError)

            // The student types only the IP; another BreakPoint already running here may be found first
            val found = NetworkManager.connect("127.0.0.1").getOrThrow().port
            assertTrue(found in NetworkManager.DEFAULT_PORT + 1..port, "found $found, ours $port")

            // An explicit wrong port is not silently replaced
            assertTrue(NetworkManager.connect("127.0.0.1:${NetworkManager.DEFAULT_PORT}").isFailure)
        } finally {
            NetworkManager.stopServer()
            foreign?.stop(0)
        }
    }

    @Test
    fun studentsFindRunningAdminsByName() {
        val oldPort = LanDiscovery.port
        LanDiscovery.port = java.net.DatagramSocket(0).use { it.localPort }
        val httpPort = freePort()
        try {
            assertTrue(
                NetworkManager.startServer(
                    httpPort,
                    { Result.failure(Exception("-")) },
                    {},
                    adminName = { "Karimov Aziz" }
                )
            )
            val admins = LanDiscovery.discover()
            val me = admins.singleOrNull { it.address.port == httpPort }
            assertNotNull(me, "found: $admins")
            assertEquals("Karimov Aziz", me.name)
            // The found address is usable for connecting
            assertEquals(httpPort, NetworkManager.connect(me.address.toString()).getOrThrow().port)

            NetworkManager.stopServer()
            assertTrue(LanDiscovery.discover(timeoutMs = 600).none { it.address.port == httpPort })
        } finally {
            NetworkManager.stopServer()
            LanDiscovery.port = oldPort
        }
    }

    @Test
    fun olderAdminAppGivesVersionMessage() {
        // Builds before versioning answered /ping with a bare "BreakPoint"
        val old = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/ping") {
                val bytes = "BreakPoint".toByteArray()
                it.sendResponseHeaders(200, bytes.size.toLong())
                it.responseBody.use { body -> body.write(bytes) }
            }
            start()
        }
        try {
            val error = NetworkManager.connect("127.0.0.1:${old.address.port}").exceptionOrNull()
            assertNotNull(error)
            assertTrue(error.message!!.contains("eskiroq versiya"), error.message)
        } finally {
            old.stop(0)
        }
    }

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }
}
