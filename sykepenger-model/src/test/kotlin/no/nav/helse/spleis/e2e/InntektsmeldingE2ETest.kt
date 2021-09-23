package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class InntektsmeldingE2ETest : AbstractEndToEndTest() {

    @Disabled("WIP Test for inntektsmelding med refusjonsopphold")
    @Test
    fun `inntektsmelding med refusjonsopphold`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 30.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterSøknad(Sykdom(1.januar, 30.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        // -> TODO refusjon IM kommer her

        håndterSykmelding(Sykmeldingsperiode(31.januar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(31.januar, 28.februar, 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.januar, refusjon = Refusjon(6.februar, INNTEKT, emptyList())
        )

        inspektør.also {
            assertEquals(Periode(1.januar, 30.januar), it.vedtaksperioder(1.vedtaksperiode).periode())
        }
    }

    @Test
    fun `Opphør i refusjon som overlapper med senere periode fører til at perioden forkastes`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.november(2020), 16.november(2020))), førsteFraværsdag = 1.november(2020))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(6.desember(2020), INNTEKT, emptyList())
        )

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Opphør i refusjon som ikke overlapper med senere periode fører ikke til at perioden forkastes`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.november(2020), 16.november(2020))), førsteFraværsdag = 1.november(2020))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(6.desember(2020), INNTEKT, emptyList())
        )

        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 10.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(25.november(2020), 10.desember(2020), 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
    }

    @Test
    fun `Opphør i refusjon som kommer mens forlengelse er i play kaster forlengelsen`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.november(2020), 16.november(2020))), førsteFraværsdag = 1.november(2020))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(6.desember(2020), INNTEKT, emptyList())
        )
        håndterSøknad(Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Opphør i refusjon som kommer mens førstegangssak er i play kaster perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.november(2020), 16.november(2020))), førsteFraværsdag = 1.november(2020))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(6.november(2020), INNTEKT, emptyList())
        )

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            TIL_INFOTRYGD
        )
    }


    @Test
    fun `Opphør i refusjon i første periode som kommer mens forlengelse er i play kaster forlengelsen`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.november(2020), 16.november(2020))), førsteFraværsdag = 1.november(2020))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(1.november(2020), INNTEKT, emptyList())
        )
        håndterSøknad(Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Opphør i refusjon kommer før overgang fra infotrygd vet den er overgang`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.oktober(2020), 16.oktober(2020))),
            førsteFraværsdag = 1.oktober(2020), refusjon = Refusjon(30.oktober(2020), INNTEKT, emptyList())
        )

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Inntektsmelding med opphør i refusjon`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(18.november(2020), INNTEKT, emptyList())
        )

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Refusjonsbeløp lik 0 i første periode som kommer mens forlengelse er i play kaster forlengelsen`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.november(2020), 16.november(2020))), førsteFraværsdag = 1.november(2020))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020),
            refusjon = Refusjon(null, INGEN, emptyList()),
            beregnetInntekt = INNTEKT
        )
        håndterSøknad(Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Refusjonsbeløp mindre enn inntekt i første periode som kommer mens forlengelse er i play kaster forlengelsen`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.november(2020), 16.november(2020))), førsteFraværsdag = 1.november(2020))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020),
            refusjon = Refusjon(null, 1000.månedlig, emptyList()),
            beregnetInntekt = INNTEKT
        )
        håndterSøknad(Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er en gap periode uten utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.desember(2020), 7.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.desember(2020), 14.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(8.desember(2020), 14.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(15.desember(2020), 3.januar(2021), 100.prosent))
        håndterSøknad(Sykdom(15.desember(2020), 3.januar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(25.november(2020), 27.november(2020)),
                Periode(1.desember(2020), 7.desember(2020)),
                Periode(10.desember(2020), 10.desember(2020)),
                Periode(15.desember(2020), 19.desember(2020))
            ),
            førsteFraværsdag = 15.desember(2020),
            refusjon = Refusjon(null, 30000.månedlig, emptyList()),
            beregnetInntekt = 30000.månedlig
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVSLUTTET_UTEN_UTBETALING
        )
        håndterYtelser(4.vedtaksperiode)
        håndterVilkårsgrunnlag(4.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.desember(2019) til 1.november(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(4.vedtaksperiode)
        assertTilstander(
            4.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er fortsatt en en forlengelse uten utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.desember(2020), 7.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.desember(2020), 14.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(8.desember(2020), 14.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(15.desember(2020), 3.januar(2021), 100.prosent))
        håndterSøknad(Sykdom(15.desember(2020), 3.januar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(25.november(2020), 27.november(2020)),
                Periode(1.desember(2020), 7.desember(2020)),
                Periode(8.desember(2020), 10.desember(2020)),
                Periode(15.desember(2020), 17.desember(2020))
            ),
            førsteFraværsdag = 15.desember(2020),
            refusjon = Refusjon(null, 30000.månedlig, emptyList()),
            beregnetInntekt = 30000.månedlig
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVSLUTTET_UTEN_UTBETALING
        )
        håndterYtelser(4.vedtaksperiode)
        håndterVilkårsgrunnlag(4.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.desember(2019) til 1.november(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(4.vedtaksperiode)
        assertTilstander(
            4.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er en gap periode med utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.desember(2020), 7.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.desember(2020), 3.januar(2021), 100.prosent))
        håndterSøknad(Sykdom(8.desember(2020), 3.januar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(25.november(2020), 27.november(2020)),
                Periode(1.desember(2020), 7.desember(2020)),
                Periode(10.desember(2020), 10.desember(2020)),
                Periode(15.desember(2020), 19.desember(2020))
            ),
            førsteFraværsdag = 15.desember(2020),
            refusjon = Refusjon(null, 30000.månedlig, emptyList()),
            beregnetInntekt = 30000.månedlig
        )
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.desember(2019) til 1.november(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(3.vedtaksperiode)
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `Replayede inntektsmeldinger påvirker ikke tidligere vedtaksperioder enn den som trigget replay`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        val inntektsmeldingId1 = håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        val inntektsmeldingId2 = håndterInntektsmelding(listOf(Periode(1.mars, 16.mars)), 1.mars)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        håndterInntektsmeldingReplay(inntektsmeldingId1, 2.vedtaksperiode(ORGNUMMER))
        håndterInntektsmeldingReplay(inntektsmeldingId2, 2.vedtaksperiode(ORGNUMMER))
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
        assertFalse(person.aktivitetslogg.logg(inspektør.vedtaksperioder(1.vedtaksperiode)).hasWarningsOrWorse())
    }

    @Test
    fun `Replay av inntektsmelding skal håndteres av periode som trigget replay og etterfølgende perioder`() {

        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(1.januar, 10.januar), Periode(21.januar, 26.januar)), førsteFraværsdag = 21.januar)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))

        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode(ORGNUMMER))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Inntektsmelding utvider ikke vedtaksperiode bakover over tidligere forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 1.februar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )

        håndterSykmelding(Sykmeldingsperiode(3.februar, 18.februar, 100.prosent))
        håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)

        inspektør.also {
            assertEquals(3.februar, it.vedtaksperioder(3.vedtaksperiode).periode().start)
        }
    }

    @Test
    fun `Inntektsmelding utvider ikke vedtaksperiode bakover over tidligere utbetalt periode i IT - IT-historikk kommer først`() {
        håndterSykmelding(Sykmeldingsperiode(3.februar, 18.februar, 100.prosent))
        håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
        val utbetalinger = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 21.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        inspektør.also {
            assertEquals(3.februar, it.vedtaksperioder(1.vedtaksperiode).periode().start)
        }
    }

    @Test
    fun `Inntektsmelding utvider ikke vedtaksperiode bakover over tidligere utbetalt periode i IT - IM kommer før søknad`() {
        håndterSykmelding(Sykmeldingsperiode(3.februar, 18.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
        val utbetalinger = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 21.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        håndterYtelser(1.vedtaksperiode, utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        inspektør.also {
            assertEquals(3.februar, it.vedtaksperioder(1.vedtaksperiode).periode().start)
        }
    }

    @Test
    fun `Inntektsmelding utvider ikke vedtaksperiode bakover over tidligere utbetalt periode i IT - IM kommer etter søknad, men før historikk`() {
        //Inntektsmelding kan komme før historikk fra IT hvis replikering er nede eller sirup
        håndterSykmelding(Sykmeldingsperiode(3.februar, 25.februar, 100.prosent))
        håndterSøknad(Sykdom(3.februar, 25.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        val utbetalinger = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 21.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        håndterYtelser(1.vedtaksperiode, utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        inspektør.also {
            assertEquals(3.februar, it.vedtaksperioder(1.vedtaksperiode).periode().start)
        }
    }

    @Test
    fun `Inntektsmelding utvider ikke vedtaksperiode bakover over tidligere utbetalt periode i IT - IM kommer før sykmelding`() {
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        håndterSykmelding(Sykmeldingsperiode(3.februar, 25.februar, 100.prosent))
        håndterSøknad(Sykdom(3.februar, 25.februar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode(ORGNUMMER))
        val utbetalinger = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 21.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        håndterYtelser(1.vedtaksperiode, utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        inspektør.also {
            assertEquals(3.februar, it.vedtaksperioder(1.vedtaksperiode).periode().start)
        }
    }

    @Test
    fun `Inntektsmelding utvider ikke vedtaksperiode bakover over tidligere utbetalt periode i IT`() {
        håndterSykmelding(Sykmeldingsperiode(3.februar, 25.februar, 100.prosent))
        håndterSøknad(Sykdom(3.februar, 25.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        val utbetalinger = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 21.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        håndterYtelser(1.vedtaksperiode, utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        inspektør.also {
            assertEquals(3.februar, it.vedtaksperioder(1.vedtaksperiode).periode().start)
        }
    }

    @Test
    fun `trimmer inntektsmelding etter tom`() {
        håndterSykmelding(Sykmeldingsperiode(6.januar, 14.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(6.januar, 14.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(15.januar, 21.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(15.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 7.februar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 7.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.februar, 21.februar, 100.prosent))
        håndterSøknad(Sykdom(8.februar, 21.februar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(6.januar til 21.januar),
            førsteFraværsdag = 22.januar,
            refusjon = Refusjon(null, 25000.månedlig, emptyList()),
            beregnetInntekt = 30000.månedlig
        )
        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(3.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(4.vedtaksperiode))
    }

    @Test
    fun `replay strekker periode tilbake og lager overlapp`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 19.januar, 100.prosent, null))
        val inntektsmelding1 = håndterInntektsmelding(listOf(3.januar til 18.januar), 3.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(20.januar, 3.februar, 100.prosent))
        håndterSøknad(Sykdom(20.januar, 3.februar, 100.prosent, null))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(7.februar, 7.februar, 100.prosent))
        håndterSøknad(Sykdom(7.februar, 7.februar, 100.prosent, null))
        val inntektsmelding2 = håndterInntektsmelding(listOf(3.januar til 18.januar), 7.februar)
        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)

        val inntektsmelding3 = håndterInntektsmelding(listOf(3.januar til 18.januar), 23.februar)
        håndterSykmelding(Sykmeldingsperiode(23.februar, 25.februar, 100.prosent))
        håndterSøknad(Sykdom(23.februar, 25.februar, 100.prosent, null))
        håndterInntektsmeldingReplay(inntektsmelding1, 4.vedtaksperiode(ORGNUMMER))
        håndterInntektsmeldingReplay(inntektsmelding2, 4.vedtaksperiode(ORGNUMMER))
        håndterInntektsmeldingReplay(inntektsmelding3, 4.vedtaksperiode(ORGNUMMER))
        håndterYtelser(4.vedtaksperiode)
        håndterVilkårsgrunnlag(4.vedtaksperiode)
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)

        assertEquals(3.januar til 19.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(20.januar til 3.februar, inspektør.periode(2.vedtaksperiode))
        assertEquals(7.februar til 7.februar, inspektør.periode(3.vedtaksperiode))
        assertEquals(23.februar til 25.februar, inspektør.periode(4.vedtaksperiode))
    }

    @Test
    fun `Inntektsmelding med error som treffer flere perioder`() {
        håndterSykmelding(Sykmeldingsperiode(29.mars(2021), 31.mars(2021), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(6.april(2021), 17.april(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(29.mars(2021), 31.mars(2021), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.april(2021), 2.mai(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(6.april(2021), 17.april(2021), 100.prosent))
        håndterSøknad(Sykdom(18.april(2021), 2.mai(2021), 100.prosent))

        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(29.mars(2021), 31.mars(2021)), Periode(6.april(2021), 18.april(2021))),
            refusjon = Refusjon(null, INNTEKT, emptyList()),
            beregnetInntekt = INGEN
        )

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
        )

        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Inntektsmelding med error som treffer flere perioder uten gap`() {
        håndterSykmelding(Sykmeldingsperiode(29.mars(2021), 31.mars(2021), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.april(2021), 17.april(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(29.mars(2021), 31.mars(2021), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.april(2021), 2.mai(2021), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.april(2021), 17.april(2021), 100.prosent))
        håndterSøknad(Sykdom(18.april(2021), 2.mai(2021), 100.prosent))

        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(29.mars(2021), 31.mars(2021)), Periode(1.april(2021), 12.april(2021))),
            refusjon = Refusjon(null, INNTEKT, emptyList()),
            beregnetInntekt = INGEN
        )

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
    }
}
