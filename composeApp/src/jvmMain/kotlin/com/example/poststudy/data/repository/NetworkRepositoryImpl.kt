package com.example.poststudy.data.repository

import com.example.poststudy.data.network.NetworkManager
import com.example.poststudy.data.network.ServerAddress
import com.example.poststudy.data.network.SessionData
import com.example.poststudy.domain.model.*
import com.example.poststudy.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NetworkRepositoryImpl : NetworkRepository {
    override fun getLocalIpAddress(): Flow<String> = flow {
        emit(NetworkManager.getLocalIpAddress())
    }

    override fun getLocalIpAddresses(): List<String> = NetworkManager.getLocalIpAddresses()

    override fun parseAddress(input: String): ServerAddress? = NetworkManager.parseAddress(input)

    override fun startServer(
        port: Int?,
        getSession: (groupId: Int) -> Result<SessionData>,
        onRecordReceived: (ExamRecord) -> Unit,
        adminName: () -> String
    ): Boolean = NetworkManager.startServer(port, getSession, onRecordReceived, adminName)

    override fun discoverAdmins(): List<com.example.poststudy.data.network.ActiveAdmin> =
        com.example.poststudy.data.network.LanDiscovery.discover()

    override fun stopServer() {
        NetworkManager.stopServer()
    }

    override fun serverError(): String? = NetworkManager.serverError

    override fun serverPort(): Int? = NetworkManager.serverPort

    override val serverRunning: kotlinx.coroutines.flow.StateFlow<Boolean> = NetworkManager.running

    override fun connect(input: String): Result<ServerAddress> =
        NetworkManager.connect(input)

    override fun verifyGroup(address: ServerAddress, groupId: Int, password: String): Result<Unit> =
        NetworkManager.verifyGroup(address, groupId, password)

    override fun fetchSession(address: ServerAddress, groupId: Int, password: String): Result<SessionData> =
        NetworkManager.fetchSession(address, groupId, password)

    override fun fetchGroups(address: ServerAddress): Result<List<GroupOverview>> =
        NetworkManager.fetchGroups(address)

    override fun fetchStudents(address: ServerAddress, groupId: Int, password: String): Result<List<Student>> =
        NetworkManager.fetchStudents(address, groupId, password)

    override fun createStudentRemote(address: ServerAddress, name: String, groupId: Int, password: String): Result<Int> =
        NetworkManager.createStudentRemote(address, name, groupId, password)

    override fun submitResult(address: ServerAddress, record: ExamRecord, password: String): Result<Unit> =
        NetworkManager.submitResult(address, record, password)
}
