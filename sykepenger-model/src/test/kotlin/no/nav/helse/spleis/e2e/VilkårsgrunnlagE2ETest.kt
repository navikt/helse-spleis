package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.YearMonth
import no.nav.helse.Toggle
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
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
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
        håndterSykmelding(Sykmeldingsperiode(26.januar, 8.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(26.januar, 8.februar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(6.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(6.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)
        håndterInntektsmelding(listOf(21.januar til 21.januar, 6.februar til 20.februar), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
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
                assertError("Bruker mangler nødvendig inntekt ved validering av Vilkårsgrunnlag", 1.vedtaksperiode.filter(a2))
                assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
            },
            ønsket = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
            }
        )
    }

    @Test
    fun `negativt omregnet årsinntekt for ghost-arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 31.januar))

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT/2
                }
            }
        ))

        assertError("Har mer enn 25 % avvik", 1.vedtaksperiode.filter())
    }

    @Test
    fun `ingen sammenligningsgrunlag fører til error om 25% avvik`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 31.januar))

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(emptyList()))

        assertError("Har mer enn 25 % avvik", 1.vedtaksperiode.filter())
    }

    @Test
    fun `Forkaster etterfølgende perioder dersom vilkårsprøving feilet pga avvik i inntekt på første periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))

        val arbeidsgiverperioder = listOf(
            1.januar til 16.januar
        )
        håndterInntektsmelding(arbeidsgiverperioder, førsteFraværsdag = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT * 2
                }
            }
        ))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            TIL_INFOTRYGD
        )

        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Forkaster ikke etterfølgende perioder dersom vilkårsprøving feiler pga minimum inntekt på første periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
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

        håndterYtelser(1.vedtaksperiode)

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 1000
                }
            }
        ))

        håndterSykmelding(Sykmeldingsperiode(18.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)

        håndterYtelser(1.vedtaksperiode)

        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT * 2 // 25 % avvik vs inntekt i inntektsmeldingen
                }
            }
        ))

        assertError("Har mer enn 25 % avvik")
    }

    @Test
    fun `gjenbruker ikke vilkårsprøving når førstegangsbehandlingen kastes ut - med kort agp-periode som påvirker skjæringstidspunktet`() {
        val inntektFraIT = INNTEKT/2
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT/2
                }
            }
        ))
        assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, inntektFraIT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, inntektFraIT, true)
        ))
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
    fun `skal ikke gjenbruke et vilkårsgrunnlag som feiler pga frilanser arbeidsforhold`() = Toggle.IkkeForlengInfotrygdperioder.disable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterYtelser(1.vedtaksperiode)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            INNTEKT,
            inntektsvurdering = Inntektsvurdering(
                inntekter = listOf(sammenligningsgrunnlag(a1, 1.januar, (INNTEKT*1.3).repeat(12))),
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar, INNTEKT.repeat(1))
                ),
                arbeidsforhold = listOf(
                    InntektForSykepengegrunnlag.Arbeidsforhold(
                        orgnummer = a2,
                        månedligeArbeidsforhold = listOf(
                            InntektForSykepengegrunnlag.MånedligArbeidsforhold(
                                yearMonth = YearMonth.of(2017, 12),
                                erFrilanser = true
                            )
                        ),
                    )
                )
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, 1.desember(2017)),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.desember(2017))
            )
        )
        assertError("Fant frilanserinntekt på en arbeidsgiver de siste 3 månedene", 1.vedtaksperiode.filter())

        håndterSykmelding(Sykmeldingsperiode(18.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 18.januar))

        håndterYtelser(2.vedtaksperiode)

        assertForventetFeil(
            forklaring = "https://trello.com/c/edYRnoPm",
            nå = { assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING) },
            ønsket = { assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD) }
        )
    }

    @Test
    fun `flere kall til håndterYtelser burde ikke duplisere vilkårsgrunnlag fra infotrygd`() {
        val gammelSykdom = arrayOf(
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar(2016), 31.januar(2016), 100.prosent, INNTEKT),
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mars(2016), 31.mars(2016), 100.prosent, INNTEKT)
        )
        val gammelInntekt = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar(2016), INNTEKT, false),
            Inntektsopplysning(ORGNUMMER, 1.mars(2016), INNTEKT, false)
        )
        /*
            Vi må ha gap for å lagre nye vilkårsgrunnlag. håndterYtelser lagrer kun vilkårsgrunnlag fra IT dersom
            vi ikke har en utbetalt periode som gjelder skjæringstidspunktet
         */
        repeat(3) {
            val fom = LocalDate.of(2018, it + 1, 1)
            val tom = fom.plusDays(17)
            håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
            håndterSøknad(Sykdom(fom, tom, 100.prosent))
            håndterUtbetalingshistorikk(
                (it+1).vedtaksperiode,
                utbetalinger = gammelSykdom,
                inntektshistorikk = gammelInntekt,
                besvart = LocalDate.EPOCH.atStartOfDay()
            )
            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = fom)

            håndterYtelser(
                (it+1).vedtaksperiode,
                utbetalinger = gammelSykdom,
                inntektshistorikk = gammelInntekt,
                besvart = LocalDate.EPOCH.atStartOfDay()
            )
            håndterVilkårsgrunnlag((it+1).vedtaksperiode)
            håndterYtelser(
                (it+1).vedtaksperiode,
                utbetalinger = gammelSykdom,
                inntektshistorikk = gammelInntekt,
                besvart = LocalDate.EPOCH.atStartOfDay()
            )
            håndterSimulering((it+1).vedtaksperiode)
            håndterUtbetalingsgodkjenning((it+1).vedtaksperiode)
            håndterUtbetalt()
        }

        assertEquals(4, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
    }
}
