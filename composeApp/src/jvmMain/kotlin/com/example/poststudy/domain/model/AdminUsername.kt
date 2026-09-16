package com.example.poststudy.domain.model

/**
 * The admin's public name: students see it in the list of active admins. One word, at most
 * [MAX_LENGTH] characters (Uzbek apostrophes allowed, e.g. O'ktam).
 */
object AdminUsername {
    const val MAX_LENGTH = 10

    private val allowed = Regex("[\\p{L}\\p{N}'‘’`ʻʼ_-]+")

    /** Error text for the form, or null when [input] is a valid username. */
    fun validate(input: String): String? {
        val value = input.trim()
        return when {
            value.isEmpty() -> "Username kiriting"
            value.any { it.isWhitespace() } -> "Username bitta so'zdan iborat bo'lsin (probelsiz)"
            value.length > MAX_LENGTH -> "Username $MAX_LENGTH ta belgidan oshmasin"
            !allowed.matches(value) -> "Faqat harf va raqamlardan foydalaning"
            else -> null
        }
    }

    fun isValid(input: String?): Boolean = input != null && validate(input) == null

    /** What a text field accepts while typing: no spaces, no more than [MAX_LENGTH] characters. */
    fun filterTyping(input: String): String = input.filterNot { it.isWhitespace() }.take(MAX_LENGTH)

    /** A starting value for the edit dialog made from an older full name like "Karimov Aziz". */
    fun suggestFrom(oldName: String?): String =
        oldName.orEmpty().trim().split(Regex("\\s+")).firstOrNull().orEmpty().take(MAX_LENGTH)
}
