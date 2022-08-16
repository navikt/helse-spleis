package no.nav.helse.person

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FunksjonelleFeilTilVarslerTest {

    private lateinit var aktivitetslogg: IAktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `error blir error om det logges på kilden`() {
        val funksjonelleFeilTilVarsler = FunksjonelleFeilTilVarsler(aktivitetslogg)
        aktivitetslogg.funksjonellFeil("Det er en feil")
        assertTrue(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertTrue(funksjonelleFeilTilVarsler.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `error blir warning om det logges på wrapperen`() {
        val funksjonelleFeilTilVarsler = FunksjonelleFeilTilVarsler(aktivitetslogg)
        funksjonelleFeilTilVarsler.funksjonellFeil("Det er en feil med wrapperen")
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertTrue(funksjonelleFeilTilVarsler.harVarslerEllerVerre())
        assertFalse(funksjonelleFeilTilVarsler.harFunksjonelleFeilEllerVerre())
    }
}