package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.harBehov
import no.nav.helse.inspectors.personLogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InfotrygdendringE2ETest : AbstractEndToEndTest() {

    @ForventetFeil("Ikke implementert enda")
    @Test
    fun `infotrygdendring gjør vi at trenger oppdatert historikk`() {
        håndterInfotrygdendring()
        assertTrue(person.personLogg.harBehov(Sykepengehistorikk))
    }
}
