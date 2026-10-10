package com.campusswap.app.domain

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

    const val SEARCH_MAX = 60
    const val ADDRESS_MAX = 120
    const val MESSAGE_MAX = 500
    const val ADDRESS_MIN = 8

    /** Listings are second-hand items; nobody needs more than a few of the same one. */
    const val CART_QUANTITY_MAX = 5
}

object InputValidation {
    private val copFormat = NumberFormat.getNumberInstance(Locale("es", "CO"))
    private val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val institutionalDomain = Regex("\\.edu(\\.[a-z]{2})?$")

    /** Letters (any alphabet, accents included), single spaces, apostrophes and hyphens. */
    fun sanitizeName(input: String, max: Int = InputLimits.NAME_MAX): String =
        input.filter { it.isLetter() || it == ' ' || it == '\'' || it == '-' }
            .replace(Regex(" {2,}"), " ")
            .trimStart()
            .take(max)

    /** Emails never contain spaces and are case-insensitive. */
    fun sanitizeEmail(input: String): String =
        input.filterNot { it.isWhitespace() }.lowercase().take(InputLimits.EMAIL_MAX)

    fun sanitizePassword(input: String): String =
        input.filterNot { it.isWhitespace() }.take(InputLimits.PASSWORD_MAX)

    /** Digits only, no leading zeros, capped so the amount can't exceed [InputLimits.PRICE_MAX]'s magnitude. */
    fun sanitizePrice(input: String): String =
        input.filter { it.isDigit() }.trimStart('0').take(InputLimits.PRICE_MAX_DIGITS)

    /** Free text: no leading whitespace, no runs of blank lines, capped at [max]. */
    fun sanitizeText(input: String, max: Int): String =
        input.trimStart().replace(Regex("\n{3,}"), "\n\n").take(max)

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

    fun passwordError(password: String): String? = when {
        password.length < InputLimits.PASSWORD_MIN -> "Password must be at least ${InputLimits.PASSWORD_MIN} characters"
        password.none { it.isLetter() } || password.none { it.isDigit() } -> "Password must include letters and numbers"
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
            else -> null
        }
    }
}
