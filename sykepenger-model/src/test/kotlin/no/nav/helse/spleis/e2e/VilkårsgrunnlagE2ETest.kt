package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagE2ETest : AbstractEndToEndTest() {

    @Test
    fun `skjæringstidspunkt måneden før inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(26.januar, 8.februar), orgnummer = a1)
        håndterSøknad(Sykdom(26.januar, 8.februar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(6.februar, 28.februar), orgnummer = a2)
        håndterSøknad(Sykdom(6.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(21.januar til 21.januar, 6.februar til 20.februar), orgnummer = a2)
        håndterVilkårsgrunnlag(1.vedtaksperiode,
            orgnummer = a2,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt INNTEKT
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    ArbeidsgiverInntekt(a1, listOf(
                        desember(2017).lønnsinntekt(),
                        november(2017).lønnsinntekt(),
                        oktober(2017).lønnsinntekt()
                    )),
                    ArbeidsgiverInntekt(a2, listOf(
                        desember(2017).lønnsinntekt(),
                        november(2017).lønnsinntekt(),
                        oktober(2017).lønnsinntekt(),
                    )),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, 1.januar(2017), null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.januar(2017), null)
            )
        )
        assertForventetFeil(
            forklaring = "vi må avklare hva vi ønsker å gjøre med sykepengegrunnlag hvor grunnlaget for begge arbeidsgiverne kommer fra skatt",
            nå = {
                assertFunksjonellFeil("Bruker mangler nødvendig inntekt ved validering av Vilkårsgrunnlag", 1.vedtaksperiode.filter(a2))
                assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
            },
            ønsket = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
            }
        )
    }

    @Test
    fun `negativt omregnet årsinntekt for ghost-arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    ArbeidsgiverInntekt(a1, listOf(
                        desember(2017).lønnsinntekt(INNTEKT),
                        november(2017).lønnsinntekt(INNTEKT),
                        oktober(2017).lønnsinntekt(INNTEKT)
                    )),
                    ArbeidsgiverInntekt(a2, listOf(
                        desember(2017).lønnsinntekt(2500.månedlig),
                        november(2017).lønnsinntekt((-3000).månedlig),
                    )),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, 1.januar(2017), null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.januar(2017), null)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING, orgnummer = a1)
    }

    @Test
    fun `mer enn 25% avvik lager kun én errormelding i aktivitetsloggen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT/2
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)

        assertFunksjonellFeil("Har mer enn 25 % avvik", 1.vedtaksperiode.filter())
    }

    @Test
    fun `ingen sammenligningsgrunlag fører til error om 25% avvik`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(emptyList()))
        håndterYtelser(1.vedtaksperiode)

        assertFunksjonellFeil("Har mer enn 25 % avvik", 1.vedtaksperiode.filter())
    }

    @Test
    fun `Forkaster etterfølgende perioder dersom vilkårsprøving feilet pga avvik i inntekt på første periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.januar, 20.januar))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))

        val arbeidsgiverperioder = listOf(
            1.januar til 16.januar
        )
        håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag = 1.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT * 2
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )

        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Forkaster ikke etterfølgende perioder dersom vilkårsprøving feiler pga minimum inntekt på første periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))

        val arbeidsgiverperioder = listOf(
            1.januar til 16.januar
        )

        håndterInntektsmelding(
            arbeidsgiverperioder = arbeidsgiverperioder,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 1000.månedlig,
            refusjon = Inntektsmelding.Refusjon(1000.månedlig, null, emptyList())
        )

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 1000
                }
            }
        ))

        håndterSykmelding(Sykmeldingsperiode(18.januar, 20.januar))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
           AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE
        )
    }

    @Test
    fun `25 % avvik i inntekt lager error`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT * 2 // 25 % avvik vs inntekt i inntektsmeldingen
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)

        assertFunksjonellFeil("Har mer enn 25 % avvik")
    }

    @Test
    fun `gjenbruker ikke vilkårsprøving når førstegangsbehandlingen kastes ut - med kort agp-periode som påvirker skjæringstidspunktet`() {
        val inntektFraIT = INNTEKT/2
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT/2
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode)
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, inntektFraIT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, inntektFraIT, true)
        ))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        assertEquals(1.januar, inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.skjæringstidspunkt)
        val vilkårsgrunnlag = inspektør.vilkårsgrunnlag(3.vedtaksperiode)
        assertNotNull(vilkårsgrunnlag)
        val grunnlagsdataInspektør = vilkårsgrunnlag.inspektør
        assertForventetFeil(
            forklaring = "vi plukker opp vilkårsgrunnlaget som ble lagret ved vurdering av 2.vedtaksperiode",
            nå = {
                assertEquals(INNTEKT, grunnlagsdataInspektør.sykepengegrunnlag.inspektør.sykepengegrunnlag)
            },
            ønsket = {
                assertEquals(inntektFraIT, grunnlagsdataInspektør.sykepengegrunnlag.inspektør.sykepengegrunnlag)
            }
        )
    }
    
    @Test
    fun `Forkaster ikke vilkårsgrunnlag om det er en periode i AUU med samme skjæringstidspunkt som den som blir annullert`() {
        nyPeriode(1.januar til 16.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        nyPeriode(17.januar til 31.januar)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertVilkårsgrunnlagFraSpleisFor(1.januar)
        håndterAnnullerUtbetaling()
        assertEquals(1.januar til 16.januar, inspektør.sykdomstidslinje.periode())
        assertForventetFeil(
            forklaring = "Sletter ikke vilkårsgrunnlaget lagret 1.januar pga. AUU-perioden",
            ønsket = { assertIngenVilkårsgrunnlagFraSpleis() },
            nå = { assertVilkårsgrunnlagFraSpleisFor(1.januar) }
        )
    }

    @Test
    fun `Forkaster vilkårsgrunnlag når periode annulleres`() {
        nyttVedtak(1.januar, 31.januar)
        assertVilkårsgrunnlagFraSpleisFor(1.januar)
        håndterAnnullerUtbetaling()
        assertIngenVilkårsgrunnlagFraSpleis()
    }

    private fun assertVilkårsgrunnlagFraSpleisFor(vararg skjæringstidspunkt: LocalDate) {
        assertEquals(skjæringstidspunkt.toSet(), person.inspektør.vilkårsgrunnlagHistorikk.inspektør.aktiveSpleisSkjæringstidspunkt)
    }
    private fun assertIngenVilkårsgrunnlagFraSpleis() = assertVilkårsgrunnlagFraSpleisFor()
}
