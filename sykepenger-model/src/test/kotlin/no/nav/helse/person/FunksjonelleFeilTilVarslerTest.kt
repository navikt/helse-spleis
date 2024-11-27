package no.nav.helse.person

import no.nav.helse.hendelser.FunksjonelleFeilTilVarsler
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_1
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
        aktivitetslogg.funksjonellFeil(RV_VT_1)
        assertTrue(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertTrue(funksjonelleFeilTilVarsler.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `error blir warning om det logges på wrapperen`() {
        val funksjonelleFeilTilVarsler = FunksjonelleFeilTilVarsler(aktivitetslogg)
        funksjonelleFeilTilVarsler.funksjonellFeil(RV_VT_1)
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertTrue(funksjonelleFeilTilVarsler.harVarslerEllerVerre())
        assertFalse(funksjonelleFeilTilVarsler.harFunksjonelleFeilEllerVerre())
    }
}
