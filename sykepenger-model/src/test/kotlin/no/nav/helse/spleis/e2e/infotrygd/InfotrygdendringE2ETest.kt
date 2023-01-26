package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.assertForventetFeil
import no.nav.helse.harBehov
import no.nav.helse.inspectors.personLogg
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.håndterInfotrygdendring
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
