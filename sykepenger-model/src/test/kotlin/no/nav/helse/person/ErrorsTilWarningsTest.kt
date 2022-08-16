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
        aktivitetslogg.funksjonellFeil("Det er en feil")
        assertTrue(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertTrue(errorsTilWarnings.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `error blir warning om det logges på wrapperen`() {
        val errorsTilWarnings = ErrorsTilWarnings(aktivitetslogg)
        errorsTilWarnings.funksjonellFeil("Det er en feil med wrapperen")
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertTrue(errorsTilWarnings.harVarslerEllerVerre())
        assertFalse(errorsTilWarnings.harFunksjonelleFeilEllerVerre())
    }
}