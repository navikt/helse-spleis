package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
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
        assertTrue(
            Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja)
                .valider(aktivitetslogg)
                .hasOnlyInfoAndNeeds()
        )
    }

    @Test
    fun `bruker er kanskje medlem`() {
        Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.VetIkke)
            .valider(aktivitetslogg).also {
                assertTrue(it.hasWarnings())
                assertFalse(aktivitetslogg.hasErrors())
            }
    }

    @Test
    fun `bruker er ikke medlem`() {
        assertTrue(
            Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Nei)
                .valider(aktivitetslogg)
                .hasErrors()
        )
    }
}
