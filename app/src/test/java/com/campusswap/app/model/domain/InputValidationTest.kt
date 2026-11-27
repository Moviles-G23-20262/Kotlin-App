package com.campusswap.app.model.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InputValidationTest {

    @Test fun namesKeepOnlyLettersSingleSpacesPeriodsAndHyphens() {
        // The backend refuses apostrophes, digits and emojis in names
        assertEquals("María José ONeil-Ruiz Jr.", InputValidation.sanitizeName("  María  José 3 O'Neil-Ruiz Jr.!😀"))
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

    @Test fun emailsDropEmojisAndAccents() {
        assertEquals("anamaria@uniandes.edu.co", InputValidation.sanitizeEmail("ana😀maria@uniandes.edu.co"))
        assertEquals("jos@uniandes.edu.co", InputValidation.sanitizeEmail("josé@uniandes.edu.co"))
    }

    @Test fun newPasswordsFollowTheBackendRules() {
        assertNotNull(InputValidation.passwordError("Ab1!"))
        assertNotNull(InputValidation.passwordError("abcd1234!"))   // no uppercase
        assertNotNull(InputValidation.passwordError("ABCD1234!"))   // no lowercase
        assertNotNull(InputValidation.passwordError("Abcdefgh!"))   // no number
        assertNotNull(InputValidation.passwordError("Abcd1234"))    // no symbol
        assertNotNull(InputValidation.passwordError("Ábcd1234!"))   // accent: not ASCII
        assertNull(InputValidation.passwordError("Abcd1234!"))
    }

    @Test fun passwordsKeepOnlyPrintableAsciiWithoutSpaces() {
        assertEquals("Abc1!", InputValidation.sanitizePassword("A b c😀1ñ!"))
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

    @Test fun priceMustBeWholeHundreds() {
        assertNotNull(InputValidation.priceError("154541"))
        assertNotNull(InputValidation.priceError("1050"))
        assertNull(InputValidation.priceError("154500"))
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

    @Test fun freeTextDropsEmojis() {
        assertEquals("Calculadora CASIO", InputValidation.sanitizeText("Calculadora 😀CASIO🫠", 100))
        assertEquals("jjv", InputValidation.sanitizeText("😀🫠😝🤪jjv", 100))
        // Sequences: skin tone, family (zero-width joiners), flag, keycap, heart with variation selector
        assertEquals("ok", InputValidation.sanitizeText("👍🏽o👨‍👩‍👧🇨🇴k1️⃣❤️".replace("1", ""), 100))
        assertEquals("Lo vendo ya", InputValidation.sanitizeText("Lo vendo ✅ ya⭐", 100).replace("  ", " "))
    }

    @Test fun freeTextKeepsLettersAccentsPunctuationAndMoney() {
        val text = "Cálculo de Stewart (8.ª ed.) — ñandú, $50.000 €, 90°, ¿sí? ¡ya! 2+2=4 #1 @ana"
        assertEquals(text, InputValidation.sanitizeText(text, 200))
    }

    @Test fun freeTextDropsInvisibleAndControlCharacters() {
        assertEquals("hola mundo", InputValidation.sanitizeText("hola\u200B \u202Emundo\u0007\uFEFF", 100))
        assertEquals("a b", InputValidation.sanitizeText("a\tb", 100))
    }

    @Test fun anEmojiCannotBeCutInHalfByTheLengthCap() {
        val capped = InputValidation.sanitizeText("abcd😀", 5)
        assertEquals("abcd", capped)
        assertEquals(capped, InputValidation.stripEmojis(capped))
    }

    @Test fun typingAndAutocompleteGoThroughButAPasteDoesNot() {
        assertEquals("hola", InputValidation.withoutPaste("hol", "hola"))
        assertEquals("me interesa ", InputValidation.withoutPaste("me ", "me interesa "))
        val pasted = "a".repeat(InputLimits.MAX_TYPED_BURST + 1)
        assertEquals("", InputValidation.withoutPaste("", pasted))
        // Deleting a whole selection is fine
        assertEquals("", InputValidation.withoutPaste(pasted, ""))
    }

    @Test fun aPastedParagraphNeverReachesTheFieldEvenIfItHasEmojis() {
        val paste = "😀".repeat(40) + "x".repeat(600)
        val shown = InputValidation.withoutPaste("", InputValidation.sanitizeText(paste, InputLimits.DESCRIPTION_MAX))
        assertEquals("", shown)
    }
}
