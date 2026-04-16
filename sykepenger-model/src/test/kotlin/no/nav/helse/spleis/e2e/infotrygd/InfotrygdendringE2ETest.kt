package no.nav.helse.spleis.e2e.infotrygd

import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.harBehov
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.EventSubscription.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InfotrygdendringE2ETest : AbstractDslTest() {

    @Test
    fun `en auu vil omgjøres som følge av Infotrygdutbetaling, mangler inntektsmelding men kan ikke forkastes`() {
        a1 {
            håndterSøknad(16.desember(2024) til 19.desember(2024))
            håndterSøknad(2.januar(2025) til 15.januar(2025))
            håndterArbeidsgiveropplysninger(arbeidsgiverperioder = listOf(16.desember(2024) til 19.desember(2024), 2.januar(2025) til 13.januar(2025)), beregnetInntekt = INNTEKT, vedtaksperiodeId = 2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(2.januar(2025), inspektør.vedtaksperioder(2.vedtaksperiode).skjæringstidspunkt)
            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(
                ArbeidsgiverUtbetalingsperiode(
                    a1, 20.november(2024), 30.november(2024)
                )))

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `infotrygdendring gjør vi at trenger oppdatert historikk`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            håndterInfotrygdendring()
            assertTrue(testperson.personlogg.harBehov(Sykepengehistorikk))
            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(
                ArbeidsgiverUtbetalingsperiode(
                    a1, 1.januar(2016), 31.januar(2016)
                )))

            val infotrygdHistorikk = testperson.person.inspektør.utbetaltIInfotrygd
            assertEquals(1.januar(2016) til 31.januar(2016), infotrygdHistorikk.single())
            assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `Utgående event om overlappende perioder`() {
        a1 {
            nyPeriode(1.januar til 20.januar)
            nyPeriode(21.januar til 31.januar)
            håndterInfotrygdendring()
            val meldingsreferanseId = UUID.randomUUID()
            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(
                ArbeidsgiverUtbetalingsperiode(
                    a1, 17.januar, 31.januar
                )), id = meldingsreferanseId)

            assertEquals(2, observatør.overlappendeInfotrygdperioder.size)
            val event = observatør.overlappendeInfotrygdperioder.last()
            val vedtaksperiodeId = 1.vedtaksperiode
            val forventet = EventSubscription.OverlappendeInfotrygdperioder(
                listOf(
                    EventSubscription.OverlappendeInfotrygdperiodeEtterInfotrygdendring(
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

                    EventSubscription.OverlappendeInfotrygdperiodeEtterInfotrygdendring(
                        yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                        vedtaksperiodeId = 2.vedtaksperiode,
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
}
