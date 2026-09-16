package com.example.poststudy.data.session

import com.example.poststudy.di.AppContainer

/** Starts and stops the LAN server that serves group sessions to students. */
object AppServer {
    fun start(): Boolean = AppContainer.networkRepository.startServer(
        getSession = { groupId ->
            // Runs on a server thread
            SessionBuilder.buildForGroup(groupId).map { it.toSessionData() }
        },
        onRecordReceived = { record ->
            val repo = AppContainer.localRepository
            val subjectId = record.subjectId
                ?: repo.getGroupOverviews().firstOrNull { it.id == record.groupId }?.subjectId
            repo.saveExamRecord(record.copy(id = 0, subjectId = subjectId))
        },
        // Students see this name in their list of active admins
        adminName = {
            val repo = AppContainer.localRepository
            repo.getAdminName()?.takeIf { it.isNotBlank() } ?: repo.getAdminLogin().orEmpty()
        }
    )

    fun stop() = AppContainer.networkRepository.stopServer()
}
