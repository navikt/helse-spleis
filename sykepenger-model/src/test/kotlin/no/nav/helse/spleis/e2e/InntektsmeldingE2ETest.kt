package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class InntektsmeldingE2ETest : AbstractEndToEndTest() {

    @Disabled("WIP Test for inntektsmelding med refusjonsopphold")
    @Test
    fun `inntektsmelding med refusjonsopphold`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 30.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 30.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        // -> TODO refusjon IM kommer her

        håndterSykmelding(Sykmeldingsperiode(31.januar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(31.januar, 28.februar, 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.januar, refusjon = Triple(6.februar, INNTEKT, emptyList())
        )

        inspektør.also {
            assertEquals(Periode(1.januar, 30.januar), it.vedtaksperioder(1.vedtaksperiode).periode())
        }
    }

    @Test
    fun `Opphør i refusjon som overlapper med senere periode fører til at perioden forkastes`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.november(2020), 16.november(2020))), førsteFraværsdag = 1.november(2020))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Triple(6.desember(2020), INNTEKT, emptyList())
        )

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Triple(6.desember(2020), INNTEKT, emptyList())
        )

        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 10.desember(2020), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(25.november(2020), 10.desember(2020), 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Triple(6.desember(2020), INNTEKT, emptyList())
        )
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Triple(6.november(2020), INNTEKT, emptyList())
        )

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            TIL_INFOTRYGD
        )
    }


    @Test
    fun `Opphør i refusjon i første periode som kommer mens forlengelse er i play kaster forlengelsen`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.november(2020), 16.november(2020))), førsteFraværsdag = 1.november(2020))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Triple(1.november(2020), INNTEKT, emptyList())
        )
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.oktober(2020), 16.oktober(2020))),
            førsteFraværsdag = 1.oktober(2020), refusjon = Triple(30.oktober(2020), INNTEKT, emptyList())
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Triple(18.november(2020), INNTEKT, emptyList())
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020),
            refusjon = Triple(null, Inntekt.INGEN, emptyList()),
            beregnetInntekt = INNTEKT
        )
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020),
            refusjon = Triple(null, 1000.månedlig, emptyList()),
            beregnetInntekt = INNTEKT
        )
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
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
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.desember(2020), 7.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.desember(2020), 14.desember(2020), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(8.desember(2020), 14.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(15.desember(2020), 3.januar(2021), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(15.desember(2020), 3.januar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(25.november(2020), 27.november(2020)),
                Periode(1.desember(2020), 7.desember(2020)),
                Periode(10.desember(2020), 10.desember(2020)),
                Periode(15.desember(2020), 19.desember(2020))
            ),
            førsteFraværsdag = 15.desember(2020),
            refusjon = Triple(null, 30000.månedlig, emptyList()),
            beregnetInntekt = 30000.månedlig
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        håndterVilkårsgrunnlag(4.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
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
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er fortsatt en en forlengelse uten utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.desember(2020), 7.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.desember(2020), 14.desember(2020), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(8.desember(2020), 14.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(15.desember(2020), 3.januar(2021), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(15.desember(2020), 3.januar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(25.november(2020), 27.november(2020)),
                Periode(1.desember(2020), 7.desember(2020)),
                Periode(8.desember(2020), 10.desember(2020)),
                Periode(15.desember(2020), 17.desember(2020))
            ),
            førsteFraværsdag = 15.desember(2020),
            refusjon = Triple(null, 30000.månedlig, emptyList()),
            beregnetInntekt = 30000.månedlig
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        håndterVilkårsgrunnlag(4.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
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
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er en gap periode med utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(1.desember(2020), 7.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.desember(2020), 3.januar(2021), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(8.desember(2020), 3.januar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(25.november(2020), 27.november(2020)),
                Periode(1.desember(2020), 7.desember(2020)),
                Periode(10.desember(2020), 10.desember(2020)),
                Periode(15.desember(2020), 19.desember(2020))
            ),
            førsteFraværsdag = 15.desember(2020),
            refusjon = Triple(null, 30000.månedlig, emptyList()),
            beregnetInntekt = 30000.månedlig
        )
        håndterVilkårsgrunnlag(3.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.desember(2019) til 1.november(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(3.vedtaksperiode)
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `Replayede inntektsmeldinger påvirker ikke tidligere vedtaksperioder enn den som trigget replay`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))

        val inntektsmeldingId1 = håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

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
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        val inntektsmeldingId2 = håndterInntektsmelding(listOf(Periode(1.mars, 16.mars)), 1.mars)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterInntektsmeldingReplay(inntektsmelding(inntektsmeldingId1, arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar))), 2.vedtaksperiode)
        håndterInntektsmeldingReplay(inntektsmelding(inntektsmeldingId2, arbeidsgiverperioder = listOf(Periode(1.mars, 16.mars)), førsteFraværsdag = 1.mars), 2.vedtaksperiode)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP
        )
        assertFalse(person.aktivitetslogg.logg(inspektør.vedtaksperioder(1.vedtaksperiode)).hasWarningsOrWorse())
    }

    @Test
    fun `Replay av inntektsmelding skal håndteres av periode som trigget replay og etterfølgende perioder`() {

        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(1.januar, 10.januar), Periode(21.januar, 26.januar)), førsteFraværsdag = 21.januar)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 20.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(21.januar, 31.januar, 100.prosent))

        håndterInntektsmeldingReplay(
            inntektsmelding(
                inntektsmeldingId,
                arbeidsgiverperioder = listOf(Periode(1.januar, 10.januar), Periode(21.januar, 26.januar)),
                førsteFraværsdag = 21.januar
            ), 1.vedtaksperiode
        )

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_VILKÅRSPRØVING_GAP
        )
    }

    @Test
    fun `Inntektsmelding utvider ikke vedtaksperiode bakover over tidligere forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 1.februar, 100.prosent))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.februar, 18.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.februar, 18.februar, 100.prosent))
        val utbetalinger = Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver(17.januar, 21.januar, 1000.daglig, 100.prosent, ORGNUMMER)
        val inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(17.januar, INNTEKT, ORGNUMMER, true))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            utbetalinger,
            inntektshistorikk = inntektshistorikk
        )
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(
            1.vedtaksperiode,
            utbetalinger,
            inntektshistorikk = inntektshistorikk
        )
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.februar, 18.februar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        val utbetalinger = Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver(17.januar, 21.januar, 1000.daglig, 100.prosent, ORGNUMMER)
        val inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(17.januar, INNTEKT, ORGNUMMER, true))
        håndterYtelser(
            1.vedtaksperiode,
            utbetalinger,
            inntektshistorikk = inntektshistorikk
        )
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.februar, 25.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        val utbetalinger = Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver(17.januar, 21.januar, 1000.daglig, 100.prosent, ORGNUMMER)
        val inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(17.januar, INNTEKT, ORGNUMMER, true))
        håndterYtelser(
            1.vedtaksperiode,
            utbetalinger,
            inntektshistorikk = inntektshistorikk
        )
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
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.februar, 25.februar, 100.prosent))
        håndterInntektsmeldingReplay(
            inntektsmelding(
                inntektsmeldingId,
                arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
                førsteFraværsdag = 3.februar
            ), 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        val utbetalinger = Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver(17.januar, 21.januar, 1000.daglig, 100.prosent, ORGNUMMER)
        val inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(17.januar, INNTEKT, ORGNUMMER, true))
        håndterYtelser(
            1.vedtaksperiode,
            utbetalinger,
            inntektshistorikk = inntektshistorikk
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        inspektør.also {
            assertEquals(3.februar, it.vedtaksperioder(1.vedtaksperiode).periode().start)
        }
    }

    @Test
    fun `Inntektsmelding utvider ikke vedtaksperiode bakover over tidligere utbetalt periode i IT - ferie i IM etter historikk beholdes`() {
        håndterSykmelding(Sykmeldingsperiode(3.februar, 25.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.februar, 25.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar, ferieperioder = listOf(28.januar til 2.februar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        val utbetalinger = Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver(17.januar, 21.januar, 1000.daglig, 100.prosent, ORGNUMMER)
        val inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(17.januar, INNTEKT, ORGNUMMER, true))
        håndterYtelser(
            1.vedtaksperiode,
            utbetalinger,
            inntektshistorikk = inntektshistorikk
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        inspektør.also {
            assertEquals(28.januar, it.vedtaksperioder(1.vedtaksperiode).periode().start)
        }
    }

    @Test
    fun `Inntektsmelding utvider ikke vedtaksperiode bakover over tidligere utbetalt periode i IT - ferie i IM før historikk fjernes`() {
        håndterSykmelding(Sykmeldingsperiode(3.februar, 25.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.februar, 25.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar, ferieperioder = listOf(18.januar til 22.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        val utbetalinger = Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver(24.januar, 28.januar, 1000.daglig, 100.prosent, ORGNUMMER)
        val inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(24.januar, INNTEKT, ORGNUMMER, true))
        håndterYtelser(
            1.vedtaksperiode,
            utbetalinger,
            inntektshistorikk = inntektshistorikk
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        inspektør.also {
            assertEquals(3.februar, it.vedtaksperioder(1.vedtaksperiode).periode().start)
        }
    }
}
