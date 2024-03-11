package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.harBehov
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.aktivitetslogg.Varselkode.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
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
    fun `utgående event om overlappende infotrygdperiode`() {
        nyPeriode(1.januar til 31.januar)
        håndterInfotrygdendring()
        val meldingsreferanseId = håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT))
        val infotrygdHistorikk = person.inspektør.utbetaltIInfotrygd
        assertEquals(17.januar til 31.januar, infotrygdHistorikk.single())
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertVarsel(RV_IT_3)

        val event = observatør.overlappendeInfotrygdperiodeEtterInfotrygdendring.single()
        val forventet =
            PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring(
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = observatør.sisteVedtaksperiodeId(ORGNUMMER),
                vedtaksperiodeFom = 1.januar,
                vedtaksperiodeTom = 31.januar,
                vedtaksperiodetilstand = "AVVENTER_INNTEKTSMELDING",
                infotrygdhistorikkHendelseId = meldingsreferanseId.toString(),
                medførteEndringerIHistorikken = true,
                infotrygdperioder = listOf(
                    Infotrygdperiode(
                        fom = 17.januar,
                        tom = 31.januar,
                        type = "ARBEIDSGIVERUTBETALING",
                        orgnummer = ORGNUMMER,
                    )
                )
            )
        assertEquals(forventet, event)
    }

    @Test
    fun `utgående event om overlappende infotrygdperiode, men medførte ingen endringer i historikken`() {
        nyPeriode(1.januar til 31.januar)
        håndterInfotrygdendring()
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT))
        var sisteEvent = observatør.overlappendeInfotrygdperiodeEtterInfotrygdendring.last()
        assertEquals(true, sisteEvent.medførteEndringerIHistorikken)
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT))
        sisteEvent = observatør.overlappendeInfotrygdperiodeEtterInfotrygdendring.last()
        assertEquals(false, sisteEvent.medførteEndringerIHistorikken)
    }

    @Test
    fun `ingen overlappende infotrygdperioder`() {
        nyPeriode(1.januar til 31.januar)
        håndterInfotrygdendring()
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar(2016), 31.januar(2016), 100.prosent, INNTEKT))
        val infotrygdHistorikk = person.inspektør.utbetaltIInfotrygd
        assertEquals(17.januar(2016) til 31.januar(2016), infotrygdHistorikk.single())
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        val event = observatør.overlappendeInfotrygdperiodeEtterInfotrygdendring.singleOrNull()
        assertEquals(null, event)
    }
}
