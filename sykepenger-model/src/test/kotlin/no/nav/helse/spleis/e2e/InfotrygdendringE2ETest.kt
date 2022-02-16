package no.nav.helse.spleis.e2e

import no.nav.helse.assertForventetFeil
import no.nav.helse.harBehov
import no.nav.helse.inspectors.personLogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InfotrygdendringE2ETest : AbstractEndToEndTest() {

    @Test
    fun `infotrygdendring gjør vi at trenger oppdatert historikk`() {
        håndterInfotrygdendring()
        assertForventetFeil(
            forklaring = "Ikke implementert enda",
            nå = { assertFalse(person.personLogg.harBehov(Sykepengehistorikk)) },
            ønsket = { assertTrue(person.personLogg.harBehov(Sykepengehistorikk)) }
        )
    }
}
