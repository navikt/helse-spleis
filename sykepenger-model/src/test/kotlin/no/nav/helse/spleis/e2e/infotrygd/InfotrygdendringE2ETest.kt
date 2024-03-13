package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.harBehov
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode
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

    @Test
    fun `Utgående event om overlappende perioder`() {
        nyPeriode(1.januar til 20.januar)
        nyPeriode(21.januar til 31.januar)
        håndterInfotrygdendring()
        val meldingsreferanseId = håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT))
        assertEquals(2, observatør.overlappendeInfotrygdperioder.size)
        val event = observatør.overlappendeInfotrygdperioder.last()
        val vedtaksperiodeId = inspektør.vedtaksperiodeId(1.vedtaksperiode)
        val forventet = PersonObserver.OverlappendeInfotrygdperioder(listOf(
            PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring(
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = vedtaksperiodeId,
                vedtaksperiodeFom = 1.januar,
                vedtaksperiodeTom = 20.januar,
                vedtaksperiodetilstand = "AVVENTER_INNTEKTSMELDING",
                infotrygdhistorikkHendelseId = null,
                infotrygdperioder = listOf(Infotrygdperiode(
                    fom = 17.januar,
                    tom = 31.januar,
                    type = "ARBEIDSGIVERUTBETALING",
                    orgnummer = ORGNUMMER
                ))
            ),

            PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring(
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = inspektør.vedtaksperiodeId(2.vedtaksperiode),
                vedtaksperiodeFom = 21.januar,
                vedtaksperiodeTom = 31.januar,
                vedtaksperiodetilstand = "AVVENTER_INNTEKTSMELDING",
                infotrygdhistorikkHendelseId = null,
                infotrygdperioder = listOf(Infotrygdperiode(
                    fom = 17.januar,
                    tom = 31.januar,
                    type = "ARBEIDSGIVERUTBETALING",
                    orgnummer = ORGNUMMER
                ))
            )

        ), meldingsreferanseId.toString())
        assertEquals(forventet, event)
    }
}
