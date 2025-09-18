package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.desember
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.harBehov
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InfotrygdendringE2ETest : AbstractEndToEndTest() {

    @Test
    fun `en auu vil omgjøres som følge av Infotrygdutbetaling, mangler inntektsmelding men kan ikke forkastes`() {
        håndterSøknad(16.desember(2024) til 19.desember(2024))
        håndterSøknad(2.januar(2025) til 15.januar(2025))
        håndterArbeidsgiveropplysninger(arbeidsgiverperioder = listOf(16.desember(2024) til 19.desember(2024), 2.januar(2025) til 13.januar(2025)), beregnetInntekt = INNTEKT, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@InfotrygdendringE2ETest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@InfotrygdendringE2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertEquals(2.januar(2025), inspektør.vedtaksperioder(2.vedtaksperiode).skjæringstidspunkt)
        this@InfotrygdendringE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(
                a1, 20.november(2024), 30.november(2024)
        ))

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
    }

    @Test
    fun `infotrygdendring gjør vi at trenger oppdatert historikk`() {
        nyPeriode(1.januar til 16.januar)
        håndterInfotrygdendring()
        assertTrue(personlogg.harBehov(Sykepengehistorikk))
        this@InfotrygdendringE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(
                a1, 1.januar(2016), 31.januar(2016)
        ))

        val infotrygdHistorikk = person.inspektør.utbetaltIInfotrygd
        assertEquals(1.januar(2016) til 31.januar(2016), infotrygdHistorikk.single())
        assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `Utgående event om overlappende perioder`() {
        nyPeriode(1.januar til 20.januar)
        nyPeriode(21.januar til 31.januar)
        håndterInfotrygdendring()
        val meldingsreferanseId = this@InfotrygdendringE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(
            ArbeidsgiverUtbetalingsperiode(
                a1, 17.januar, 31.januar
        ))

        assertEquals(2, observatør.overlappendeInfotrygdperioder.size)
        val event = observatør.overlappendeInfotrygdperioder.last()
        val vedtaksperiodeId = inspektør.vedtaksperiodeId(1.vedtaksperiode)
        val forventet = PersonObserver.OverlappendeInfotrygdperioder(
            listOf(
                PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring(
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = vedtaksperiodeId,
                    vedtaksperiodeFom = 1.januar,
                    vedtaksperiodeTom = 20.januar,
                    vedtaksperiodetilstand = "AVVENTER_INNTEKTSMELDING",
                    kanForkastes = true,
                    infotrygdperioder = listOf(
                        Infotrygdperiode(
                            fom = 17.januar,
                            tom = 31.januar,
                            type = "ARBEIDSGIVERUTBETALING",
                            orgnummer = a1
                        )
                    )
                ),

                PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring(
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = inspektør.vedtaksperiodeId(2.vedtaksperiode),
                    vedtaksperiodeFom = 21.januar,
                    vedtaksperiodeTom = 31.januar,
                    vedtaksperiodetilstand = "AVVENTER_INNTEKTSMELDING",
                    kanForkastes = true,
                    infotrygdperioder = listOf(
                        Infotrygdperiode(
                            fom = 17.januar,
                            tom = 31.januar,
                            type = "ARBEIDSGIVERUTBETALING",
                            orgnummer = a1
                        )
                    )
                )

            ), meldingsreferanseId
        )
        assertEquals(forventet, event)
    }
}
