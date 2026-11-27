package com.campusswap.app.model.domain

import java.text.NumberFormat
import java.util.Locale

/**
 * Limits and checks shared by every form in the app. The `sanitize*` functions run on each
 * keystroke and drop input that can never be valid; the `*Error` functions run on submit
 * and return the message to show, or null when the value is fine.
 */
object InputLimits {
    const val NAME_MIN = 2
    const val NAME_MAX = 60
    const val MAJOR_MAX = 50
    const val EMAIL_MAX = 80
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX = 72

    const val TITLE_MIN = 3
    const val TITLE_MAX = 60
    const val DESCRIPTION_MIN = 10
    const val DESCRIPTION_MAX = 500

    const val PRICE_MIN = 1_000L
    const val PRICE_MAX = 5_000_000L
    const val PRICE_MAX_DIGITS = 7
    /** Cash changes hands on campus, so prices go in whole hundreds (no $154.541 COP). */
    const val PRICE_STEP = 100L

    const val SEARCH_MAX = 60
    const val MESSAGE_MAX = 500
    const val REVIEW_MAX = 200

    /**
     * The most characters one change may add to a text field. Typing, autocorrect and swipe typing add
     * a word at a time; a paste from the keyboard's clipboard chip adds the whole clip, and is refused.
     */
    const val MAX_TYPED_BURST = 24
}

object InputValidation {
    private val copFormat = NumberFormat.getNumberInstance(Locale("es", "CO"))
    private val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val institutionalDomain = Regex("\\.edu(\\.[a-z]{2})?$")

    // Compiled once. The sanitizers run on every keystroke of the name, search, title, description, chat,
    // alert and address fields; building the Regex inside them compiled a new java.util.regex.Pattern per key.
    private val repeatedSpaces = Regex(" {2,}")
    private val blankLineRuns = Regex("\n{3,}")

    /** Letters (any alphabet, accents included), single spaces, periods and hyphens: what the backend accepts. */
    fun sanitizeName(input: String, max: Int = InputLimits.NAME_MAX): String =
        input.filter { it.isLetter() || it == ' ' || it == '.' || it == '-' }
            .replace(repeatedSpaces, " ")
            .trimStart()
            .take(max)

    /** Emails are plain ASCII letters, digits and . _ % + - @, without spaces, and case-insensitive. */
    fun sanitizeEmail(input: String): String =
        input.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in "._%+-@" }
            .lowercase()
            .take(InputLimits.EMAIL_MAX)

    /** Printable ASCII without spaces: one character is one byte, which is what bcrypt reads on the server. */
    fun sanitizePassword(input: String): String =
        input.filter { it in '!'..'~' }.take(InputLimits.PASSWORD_MAX)

    /** Digits only, no leading zeros, capped so the amount can't exceed [InputLimits.PRICE_MAX]'s magnitude. */
    fun sanitizePrice(input: String): String =
        input.filter { it.isDigit() }.trimStart('0').take(InputLimits.PRICE_MAX_DIGITS)

    /** Free text: no emojis or invisible characters, no leading whitespace, no runs of blank lines, capped at [max]. */
    fun sanitizeText(input: String, max: Int): String =
        stripEmojis(input).trimStart().replace(blankLineRuns, "\n\n").take(max)

    /**
     * Drops emojis (and the joiners and variation selectors that build them), symbols drawn as pictures,
     * control characters other than a line break, and invisible characters. Letters of any alphabet,
     * digits, punctuation, math signs and currency symbols stay, and so does the degree sign.
     */
    fun stripEmojis(input: String): String {
        val out = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val codePoint = input.codePointAt(i)
            if (isAllowedInText(codePoint)) out.appendCodePoint(if (codePoint == '\t'.code) ' '.code else codePoint)
            i += Character.charCount(codePoint)
        }
        return out.toString()
    }

    private fun isAllowedInText(codePoint: Int): Boolean = when {
        codePoint == '\n'.code || codePoint == '\t'.code -> true
        codePoint == '°'.code -> true
        // Every emoji outside the basic plane (faces, hands, flags, food...), plus private-use glyphs
        codePoint > 0xFFFF -> false
        codePoint in 0xE000..0xF8FF -> false
        // Zero-width characters, bidi marks, BOM, joiners and variation selectors (emoji sequences)
        codePoint in 0x200B..0x200F || codePoint in 0x202A..0x202E || codePoint in 0x2060..0x2064 -> false
        codePoint == 0xFEFF || codePoint in 0xFE00..0xFE0F || codePoint == 0x20E3 -> false
        // Arrows, misc technical, dingbats and misc symbols drawn as emojis (⌚ ☀ ✅ ❤ ⭐)
        codePoint in 0x2190..0x21FF || codePoint in 0x2300..0x23FF || codePoint in 0x2600..0x27BF -> false
        codePoint in 0x2B00..0x2BFF || codePoint == 0x203C || codePoint == 0x2049 -> false
        else -> when (Character.getType(codePoint).toByte()) {
            Character.CONTROL, Character.FORMAT, Character.SURROGATE, Character.UNASSIGNED,
            Character.PRIVATE_USE, Character.OTHER_SYMBOL -> false
            else -> true
        }
    }

    /**
     * Refuses a change that adds more than [InputLimits.MAX_TYPED_BURST] characters at once, i.e. a paste
     * from the keyboard's clipboard: the field keeps [old]. Deleting or replacing text is always fine.
     */
    fun withoutPaste(old: String, new: String): String =
        if (new.length - old.length > InputLimits.MAX_TYPED_BURST) old else new

    fun nameError(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Enter your full name"
            trimmed.length < InputLimits.NAME_MIN -> "Name is too short"
            trimmed.split(' ').count { it.isNotBlank() } < 2 -> "Enter your first and last name"
            trimmed.split(' ').any { part -> part.isNotEmpty() && part.none { it.isLetter() } } -> "Name must contain letters"
            else -> null
        }
    }

    fun majorError(major: String): String? {
        val trimmed = major.trim()
        return when {
            trimmed.isEmpty() -> "Enter your major"
            trimmed.length < 3 -> "Major is too short"
            else -> null
        }
    }

    fun emailError(email: String, requireInstitutional: Boolean = false): String? = when {
        email.isBlank() -> "Enter your email"
        !emailPattern.matches(email) -> "Enter a valid email address"
        requireInstitutional && !institutionalDomain.containsMatchIn(email.substringAfter('@')) ->
            "Use your institutional email (e.g. name@uniandes.edu.co)"
        else -> null
    }

    /** A new password (sign-up, reset), with the same rules as the backend. Login doesn't use it: older accounts have simpler passwords. */
    fun passwordError(password: String): String? = when {
        password.length < InputLimits.PASSWORD_MIN -> "Password must be at least ${InputLimits.PASSWORD_MIN} characters"
        password.any { it !in '!'..'~' } -> "Use only letters, numbers and symbols, without spaces or accents"
        password.none { it.isUpperCase() } -> "Password must include an uppercase letter"
        password.none { it.isLowerCase() } -> "Password must include a lowercase letter"
        password.none { it.isDigit() } -> "Password must include a number"
        password.none { !it.isLetterOrDigit() } -> "Password must include a symbol, like ! or #"
        else -> null
    }

    fun confirmPasswordError(password: String, confirmation: String): String? =
        if (password != confirmation) "Passwords don't match" else null

    fun titleError(title: String): String? = when {
        title.trim().length < InputLimits.TITLE_MIN -> "Title must be at least ${InputLimits.TITLE_MIN} characters"
        title.none { it.isLetter() } -> "Title must contain letters"
        else -> null
    }

    fun descriptionError(description: String): String? =
        if (description.trim().length < InputLimits.DESCRIPTION_MIN) {
            "Description must be at least ${InputLimits.DESCRIPTION_MIN} characters"
        } else {
            null
        }

    fun priceError(price: String): String? {
        val amount = price.toLongOrNull() ?: return "Enter a price"
        return when {
            amount < InputLimits.PRICE_MIN -> "Minimum price is $${copFormat.format(InputLimits.PRICE_MIN)} COP"
            amount > InputLimits.PRICE_MAX -> "Maximum price is $${copFormat.format(InputLimits.PRICE_MAX)} COP"
            amount % InputLimits.PRICE_STEP != 0L ->
                "Use a multiple of $${copFormat.format(InputLimits.PRICE_STEP)} COP"
            else -> null
        }
    }
}
