package no.nav.helse.person

import no.nav.helse.Aktivitetskode.A_INGEN
import no.nav.helse.hentWarnings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class KreverHandlingTest {
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun beforeEach() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `kode blir til funksjonell feil`() {
        aktivitetslogg.handlingStrategi(DefaultStrategi())
        aktivitetslogg.kreverHandling(A_INGEN)
        assertEquals(0, aktivitetslogg.hentWarnings().size)
        assertTrue(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `kode blir til varsel`() {
        aktivitetslogg.handlingStrategi(RevurderingStrategi())
        aktivitetslogg.kreverHandling(A_INGEN)
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `forelder arver strategi`() {
        val child = Aktivitetslogg(aktivitetslogg)
        child.handlingStrategi(RevurderingStrategi())
        aktivitetslogg.kreverHandling(A_INGEN)
        assertTrue(aktivitetslogg.harVarslerEllerVerre())
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `kan ikke bruke aktivitetslogg uten å ha satt strategi`() {
        assertThrows<UninitializedPropertyAccessException> {
            aktivitetslogg.kreverHandling(A_INGEN)
        }
    }
}