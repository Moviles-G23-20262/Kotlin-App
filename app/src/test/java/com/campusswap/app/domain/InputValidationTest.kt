package com.campusswap.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InputValidationTest {

    @Test fun namesKeepOnlyLettersAndSingleSpaces() {
        assertEquals("María José O'Neil-Ruiz", InputValidation.sanitizeName("  María  José 3 O'Neil-Ruiz!"))
        assertEquals(InputLimits.NAME_MAX, InputValidation.sanitizeName("a".repeat(100)).length)
    }

    @Test fun nameNeedsFirstAndLastName() {
        assertNotNull(InputValidation.nameError(""))
        assertNotNull(InputValidation.nameError("Ana"))
        assertNotNull(InputValidation.nameError("Ana -"))
        assertNull(InputValidation.nameError("Ana Ruiz"))
    }

    @Test fun registrationRequiresInstitutionalEmail() {
        assertNotNull(InputValidation.emailError("not-an-email"))
        assertNull(InputValidation.emailError("ana@gmail.com"))
        assertNotNull(InputValidation.emailError("ana@gmail.com", requireInstitutional = true))
        assertNull(InputValidation.emailError("ana@uniandes.edu.co", requireInstitutional = true))
        assertNull(InputValidation.emailError("ana@mit.edu", requireInstitutional = true))
    }

    @Test fun emailsAreLowercasedWithoutSpaces() {
        assertEquals("ana@uniandes.edu.co", InputValidation.sanitizeEmail(" Ana @Uniandes.edu.co "))
    }

    @Test fun passwordNeedsLengthLettersAndNumbers() {
        assertNotNull(InputValidation.passwordError("abc12"))
        assertNotNull(InputValidation.passwordError("abcdefgh"))
        assertNotNull(InputValidation.passwordError("12345678"))
        assertNull(InputValidation.passwordError("abcd1234"))
        assertEquals(InputLimits.PASSWORD_MAX, InputValidation.sanitizePassword("a".repeat(100)).length)
    }

    @Test fun priceKeepsDigitsWithoutLeadingZeros() {
        assertEquals("15000", InputValidation.sanitizePrice("0015.000"))
        assertEquals("", InputValidation.sanitizePrice("000"))
        assertEquals(InputLimits.PRICE_MAX_DIGITS, InputValidation.sanitizePrice("123456789").length)
    }

    @Test fun priceMustBeWithinRange() {
        assertNotNull(InputValidation.priceError(""))
        assertNotNull(InputValidation.priceError("999"))
        assertNotNull(InputValidation.priceError("5000001"))
        assertNull(InputValidation.priceError("1000"))
        assertNull(InputValidation.priceError("5000000"))
    }

    @Test fun listingTextHasMinimums() {
        assertNotNull(InputValidation.titleError("ab"))
        assertNotNull(InputValidation.titleError("12345"))
        assertNull(InputValidation.titleError("Calculus book"))
        assertNotNull(InputValidation.descriptionError("short"))
        assertNull(InputValidation.descriptionError("Barely used, no notes inside."))
    }

    @Test fun freeTextIsCappedAndTrimmedAtStart() {
        assertEquals("hi\n\nthere", InputValidation.sanitizeText("  hi\n\n\n\nthere", 100))
        assertEquals(5, InputValidation.sanitizeText("abcdefgh", 5).length)
    }
}
