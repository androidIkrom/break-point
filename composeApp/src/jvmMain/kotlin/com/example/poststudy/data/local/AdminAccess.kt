package com.example.poststudy.data.local

import java.security.MessageDigest

/**
 * The admin part of the app is locked on every computer until the secret key is entered.
 * Only the key's SHA-256 is kept in the code.
 */
object AdminAccess {
    private const val SECRET_KEY_SHA256 = "b78e58750d4f05fb33fffc966d320bc268c69ae923a676f8248ab750b84f5c79"
    private const val UNLOCKED_KEY = "admin_unlocked"

    fun isUnlocked(): Boolean = DatabaseHelper.getMeta(UNLOCKED_KEY) == "1"

    fun isSecretKey(input: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.trim().toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(hex.toByteArray(), SECRET_KEY_SHA256.toByteArray())
    }

    /** Unlocks admin mode if [input] is the secret key. */
    fun unlock(input: String): Boolean {
        if (!isSecretKey(input)) return false
        DatabaseHelper.setMeta(UNLOCKED_KEY, "1")
        return true
    }

    fun lock() = DatabaseHelper.setMeta(UNLOCKED_KEY, "0")
}
