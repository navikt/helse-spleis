package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import no.nav.helse.dsl.a1
import no.nav.helse.gjenopprettFraJSON
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.håndterPåminnelse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class FlerePerioderTilGodkjenningTest : AbstractEndToEndTest() {

    @Test
    fun `flere perioder til godkjenning samtidig`() {
        createTestPerson { jurist -> gjenopprettFraJSON("/personer/to_perioder_til_godkjenning_samtidig.json", jurist) }
        val m = assertThrows<IllegalStateException> {
            this@FlerePerioderTilGodkjenningTest.håndterPåminnelse(1.vedtaksperiode, påminnetTilstand = TilstandType.AVVENTER_GODKJENNING, orgnummer = a1)
        }
        assertTrue(m.message?.contains("Ugyldig situasjon! Flere perioder til godkjenning samtidig") == true)
    }
}
