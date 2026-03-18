package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.person.tilstandsmaskin.TilstandType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class FlerePerioderTilGodkjenningTest : AbstractDslTest() {

    @Test
    fun `flere perioder til godkjenning samtidig`() {
        medJSONPerson("/personer/to_perioder_til_godkjenning_samtidig.json", 334)
        a1 {
            val m = assertThrows<IllegalStateException> {
                håndterPåminnelse(1.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING)
            }
            assertTrue(m.message?.contains("Ugyldig situasjon! Flere perioder til godkjenning samtidig") == true)

        }
    }
}
