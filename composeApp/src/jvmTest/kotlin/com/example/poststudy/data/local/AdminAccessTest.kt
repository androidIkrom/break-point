package com.example.poststudy.data.local

import com.example.poststudy.TestFixtures
import com.example.poststudy.data.network.NetworkManager
import com.example.poststudy.data.session.AppServer
import com.example.poststudy.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class AdminAccessTest {
    private val repo get() = AppContainer.localRepository

    @Test
    fun secretKeyUnlocksAndLogoutLocksAgain() {
        TestFixtures.initDatabase()
        repo.lockAdmin()
        assertFalse(repo.isAdminUnlocked())

        assertFalse(repo.unlockAdmin("Lesson"))
        assertFalse(repo.unlockAdmin(""))
        assertFalse(repo.isAdminUnlocked())

        assertTrue(repo.isSecretKey(" lesson "))
        assertTrue(repo.unlockAdmin("lesson"))
        assertTrue(repo.isAdminUnlocked())

        repo.lockAdmin()
        assertFalse(repo.isAdminUnlocked())
    }

    @Test
    fun backgroundModeIsRemembered() {
        TestFixtures.initDatabase()
        repo.setBackgroundModeEnabled(true)
        assertTrue(repo.isBackgroundModeEnabled())
        repo.setBackgroundModeEnabled(false)
        assertFalse(repo.isBackgroundModeEnabled())
    }

    @Test
    fun logoutKeepsDataButDeleteAccountWipesEverything() = runBlocking {
        TestFixtures.initDatabase()
        repo.registerUser("admin", "parol123")
        val subjectId = repo.addSubject("Saqlanadigan fan").first()
        repo.addGroup("G-1", subjectId, "123").first()
        assertTrue(repo.unlockAdmin("lesson"))

        // Logout: only the lock changes
        repo.lockAdmin()
        assertTrue(repo.isUserRegistered().first())
        assertTrue(repo.getAllSubjects().first().any { it.name == "Saqlanadigan fan" })

        repo.deleteAccount()
        assertFalse(repo.isUserRegistered().first())
        assertFalse(repo.isAdminUnlocked())
        val subjects = repo.getAllSubjects().first()
        assertEquals(listOf("Asosiy fan"), subjects.map { it.name })
        assertTrue(repo.getGroupOverviews().none { it.name == "G-1" })
    }

    @Test
    fun adminNameIsSavedAndEditable() = runBlocking {
        TestFixtures.initDatabase()
        repo.deleteAccount()
        assertNull(repo.getAdminName())
        repo.registerUser("admin", "parol", "Karimov Aziz")
        assertEquals("Karimov Aziz", repo.getAdminName())
        repo.setAdminName("Aziz Karimov")
        assertEquals("Aziz Karimov", repo.getAdminName())
        assertTrue(repo.validateUser("admin", "parol").first())
    }

    @Test
    fun serverStateIsObservable() {
        TestFixtures.initDatabase()
        assertFalse(NetworkManager.running.value)
        try {
            assertTrue(AppServer.start())
            assertTrue(NetworkManager.running.value)
        } finally {
            AppServer.stop()
        }
        assertFalse(NetworkManager.running.value)
        assertNull(NetworkManager.serverPort)
    }
}
