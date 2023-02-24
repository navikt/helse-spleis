package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.harBehov
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.håndterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InfotrygdendringE2ETest : AbstractEndToEndTest() {

    @Test
    fun `infotrygdendring gjør vi at trenger oppdatert historikk`() {
        nyPeriode(1.januar til 16.januar)
        håndterInfotrygdendring()
        assertTrue(person.personLogg.harBehov(Sykepengehistorikk))
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar(2016), 31.januar(2016), 100.prosent, INNTEKT))

        val infotrygdHistorikk = person.inspektør.utbetaltIInfotrygd
        assertEquals(1.januar(2016) til 31.januar(2016), infotrygdHistorikk.single())
        assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
    }
}
