package com.example.poststudy.domain.repository

import com.example.poststudy.data.network.ServerAddress
import com.example.poststudy.data.network.SessionData
import com.example.poststudy.domain.model.*
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun getLocalIpAddress(): Flow<String>
    fun getLocalIpAddresses(): List<String>
    fun parseAddress(input: String): ServerAddress?

    /** Null port: 8080, or the next free port if 8080 is taken. */
    fun startServer(
        port: Int? = null,
        getSession: (groupId: Int) -> Result<SessionData>,
        onRecordReceived: (ExamRecord) -> Unit,
        adminName: () -> String = { "" }
    ): Boolean
    fun stopServer()
    fun serverError(): String?
    fun serverPort(): Int?
    val serverRunning: kotlinx.coroutines.flow.StateFlow<Boolean>

    // Blocking calls: run them on Dispatchers.IO
    /** Running admins on the local network, found by UDP broadcast. */
    fun discoverAdmins(): List<com.example.poststudy.data.network.ActiveAdmin>
    /** Parses the typed address and finds the admin server, trying fallback ports if none was typed. */
    fun connect(input: String): Result<ServerAddress>
    fun verifyGroup(address: ServerAddress, groupId: Int, password: String): Result<Unit>
    fun fetchSession(address: ServerAddress, groupId: Int, password: String): Result<SessionData>
    fun fetchGroups(address: ServerAddress): Result<List<GroupOverview>>
    fun fetchStudents(address: ServerAddress, groupId: Int, password: String): Result<List<Student>>
    fun createStudentRemote(address: ServerAddress, name: String, groupId: Int, password: String): Result<Int>
    fun submitResult(address: ServerAddress, record: ExamRecord, password: String): Result<Unit>
}
