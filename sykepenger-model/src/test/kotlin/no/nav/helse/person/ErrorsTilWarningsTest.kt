package no.nav.helse.person

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ErrorsTilWarningsTest {

    private lateinit var aktivitetslogg: IAktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `error blir error om det logges på kilden`() {
        val errorsTilWarnings = ErrorsTilWarnings(aktivitetslogg)
        aktivitetslogg.error("Det er en feil")
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
        assertTrue(errorsTilWarnings.hasErrorsOrWorse())
    }

    @Test
    fun `error blir warning om det logges på wrapperen`() {
        val errorsTilWarnings = ErrorsTilWarnings(aktivitetslogg)
        errorsTilWarnings.error("Det er en feil med wrapperen")
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
        assertTrue(errorsTilWarnings.hasWarningsOrWorse())
        assertFalse(errorsTilWarnings.hasErrorsOrWorse())
    }
}