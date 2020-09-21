package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Periodetype
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MedlemskapsvurderingTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `bruker er medlem`() {
        assertFalse(
            Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja)
                .valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING)
                .hasWarnings()
        )
    }

    @Test
    fun `bruker er kanskje medlem`() {
        Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.VetIkke)
            .valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING).also {
                assertTrue(it.hasWarnings())
                assertFalse(aktivitetslogg.hasErrors())
            }
    }

    @Test
    fun `bruker er ikke medlem`() {
        assertTrue(
            Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei)
                .valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING)
                .hasErrors()
        )
    }
}
