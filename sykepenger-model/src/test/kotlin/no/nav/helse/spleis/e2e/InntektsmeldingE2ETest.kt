package no.nav.helse.spleis.e2e

import no.nav.helse.*
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsmelding.Companion.WARN_UENIGHET_ARBEIDSGIVERPERIODE
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

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
            førsteFraværsdag = 1.januar,
            refusjon = Refusjon(INNTEKT, 6.februar, emptyList())
        )

        inspektør.also {
            assertEquals(Periode(1.januar, 30.januar), it.vedtaksperioder(1.vedtaksperiode).periode())
        }
    }

    @Test
    fun `arbeidsgiverperiode fra inntektsmelding trumfer ferieopplysninger`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 5.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        val utbetaling = inspektør.utbetaling(0).inspektør
        assertTrue((1.januar til 16.januar).all { utbetaling.utbetalingstidslinje[it] is Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag })
    }

    @Test
    fun `dersom spleis regner arbeidsgiverperioden ulik fra arbeidsgiver lages warning - im først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 15.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        assertWarning(1.vedtaksperiode, WARN_UENIGHET_ARBEIDSGIVERPERIODE)
    }

    @Test
    fun `dersom spleis regner arbeidsgiverperioden ulik fra arbeidsgiver lages warning - søknad først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 15.januar)))
        assertWarning(1.vedtaksperiode, WARN_UENIGHET_ARBEIDSGIVERPERIODE)
    }

    @Test
    fun `vi sammenligner ikke arbeidsgiverperiodeinformasjon dersom inntektsmelding har oppgitt første fraværsdag`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.februar, 28.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.februar)
        assertNoWarnings(1.vedtaksperiode)
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

        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(INNTEKT, 6.desember(2020), emptyList())
        )

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        håndterYtelser(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
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
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(INNTEKT, 6.desember(2020), emptyList())
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

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(INNTEKT, 6.desember(2020), emptyList())
        )
        håndterSøknad(Sykdom(21.november(2020), 10.desember(2020), 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
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
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(INNTEKT, 6.november(2020), emptyList())
        )
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
        assertWarningTekst(person, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
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

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(INNTEKT, 1.november(2020), emptyList())
        )
        håndterSøknad(Sykdom(21.november(2020), 10.desember(2020), 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
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

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020),
            beregnetInntekt = INNTEKT,
            refusjon = Refusjon(INGEN, null, emptyList())
        )
        håndterSøknad(Sykdom(21.november(2020), 10.desember(2020), 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
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
            beregnetInntekt = INNTEKT,
            refusjon = Refusjon(1000.månedlig, null, emptyList())
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
        håndterYtelser(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er en gap periode uten utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020), 100.prosent))
        håndterSøknad(Sykdom(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(1.desember(2020), 7.desember(2020), 100.prosent))

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
            beregnetInntekt = 30000.månedlig,
            refusjon = Refusjon(30000.månedlig, null, emptyList())
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er fortsatt en en forlengelse uten utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020), 100.prosent))
        håndterSøknad(Sykdom(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(1.desember(2020), 7.desember(2020), 100.prosent))

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
            beregnetInntekt = 30000.månedlig,
            refusjon = Refusjon(30000.månedlig, null, emptyList())
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er en gap periode med utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020), 100.prosent))
        håndterSøknad(Sykdom(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020), 100.prosent))
        håndterSøknad(Sykdom(1.desember(2020), 7.desember(2020), 100.prosent))

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
            beregnetInntekt = 30000.månedlig,
            refusjon = Refusjon(30000.månedlig, null, emptyList())
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

        håndterInntektsmeldingReplay(inntektsmeldingId1, 2.vedtaksperiode.id(ORGNUMMER))
        håndterInntektsmeldingReplay(inntektsmeldingId2, 2.vedtaksperiode.id(ORGNUMMER))
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
    fun `Replay av inntektsmelding skal håndteres av periode som trigget replay og etterfølgende perioder 1`() {
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(1.januar, 10.januar), Periode(21.januar, 26.januar)), førsteFraværsdag = 21.januar)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode.id(ORGNUMMER))
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
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Replay av inntektsmelding skal håndteres av periode som trigget replay og etterfølgende perioder 2`() {
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(1.januar, 10.januar), Periode(21.januar, 26.januar)), førsteFraværsdag = 21.januar)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))


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
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Replay av inntekstmelding med forkasting`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 16.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        val im = håndterInntektsmeldingMedValidering(3.vedtaksperiode, listOf(1.januar til 16.januar))
        person.søppelbøtte(hendelselogg, 17.januar til 31.januar) // simulerer forkastelse av januar-perioden
        håndterSykmelding(Sykmeldingsperiode(1.februar, 10.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 10.februar, 100.prosent))
        person.søppelbøtte(hendelselogg, 1.februar til 10.februar) // simulerer feil etter at perioden har bedt om replay; f.eks. ved at Utbetalingshistorikk inneholder feil, etc.
        håndterInntektsmeldingReplay(im, 4.vedtaksperiode.id(ORGNUMMER))
        assertNoWarnings(1.vedtaksperiode)
        assertNoWarnings(2.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertForkastetPeriodeTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, TIL_INFOTRYGD)
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
        val utbetalinger = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 21.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
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
        val utbetalinger = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 21.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
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
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
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
        håndterSøknad(Sykdom(6.januar, 14.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(15.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(15.januar, 21.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(22.januar, 7.februar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 7.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.februar, 21.februar, 100.prosent))
        håndterSøknad(Sykdom(8.februar, 21.februar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(6.januar til 21.januar),
            førsteFraværsdag = 22.januar,
            beregnetInntekt = 30000.månedlig,
            refusjon = Refusjon(25000.månedlig, null, emptyList())
        )

        håndterYtelser(3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)

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
        håndterInntektsmeldingReplay(inntektsmelding1, 4.vedtaksperiode.id(ORGNUMMER))
        håndterInntektsmeldingReplay(inntektsmelding2, 4.vedtaksperiode.id(ORGNUMMER))
        håndterInntektsmeldingReplay(inntektsmelding3, 4.vedtaksperiode.id(ORGNUMMER))
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
        håndterSøknad(Sykdom(29.mars(2021), 31.mars(2021), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.april(2021), 2.mai(2021), 100.prosent))
        håndterSøknad(Sykdom(6.april(2021), 17.april(2021), 100.prosent))
        håndterSøknad(Sykdom(18.april(2021), 2.mai(2021), 100.prosent))

        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(29.mars(2021), 31.mars(2021)), Periode(6.april(2021), 18.april(2021))),
            beregnetInntekt = INGEN,
            refusjon = Refusjon(INNTEKT, null, emptyList())
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
        håndterSøknad(Sykdom(29.mars(2021), 31.mars(2021), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.april(2021), 2.mai(2021), 100.prosent))
        håndterSøknad(Sykdom(1.april(2021), 17.april(2021), 100.prosent))
        håndterSøknad(Sykdom(18.april(2021), 2.mai(2021), 100.prosent))

        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            arbeidsgiverperioder = listOf(29.mars(2021) til 31.mars(2021), 1.april(2021) til 12.april(2021)),
            beregnetInntekt = INGEN,
            refusjon = Refusjon(INNTEKT, null, emptyList())
        )

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING,
        )

        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )

        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `om arbeidsgiverperioden fra IM treffer ikke første vedtaksperiodens burde den fortsatt bli håndtert dersom forlengelsen treffes`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 7.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 7.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(8.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(listOf(8.januar til 23.januar), førsteFraværsdag = 8.januar)
        assertNoErrors(2.vedtaksperiode)
        val tidslinje = inspektør.sykdomstidslinje
        assertTrue((1.januar til 7.januar).all { tidslinje[it] is Dag.Arbeidsdag || tidslinje[it] is Dag.FriskHelgedag })
        assertTrue((8.januar til 23.januar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag || tidslinje[it] is Dag.Arbeidsgiverdag || tidslinje[it] is Dag.ArbeidsgiverHelgedag })
        assertNoWarning(1.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertWarning(2.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(8.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }

    @Test
    fun `Håndterer ikke inntektsmelding to ganger ved replay - hvor vi har en tidligere periode og gap`() {
        // Ved en tidligere periode resettes trimming av inntektsmelding og vi ender med å håndtere samme inntektsmelding flere ganger i en vedtaksperiode
        nyttVedtak(1.januar(2017), 31.januar(2017))

        val inntektsmeldingId = håndterInntektsmelding(listOf(10.januar til 25.januar), førsteFraværsdag = 10.januar)
        håndterSykmelding(Sykmeldingsperiode(10.januar, 31.januar, 100.prosent))
        assertNoWarnings(2.vedtaksperiode)
        håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent))
        assertNoWarnings(2.vedtaksperiode)
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode.id(ORGNUMMER))
        assertNoWarnings(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertNoWarnings(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        assertNoWarnings(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertNoWarnings(2.vedtaksperiode)
    }

    @Test
    fun `Håndterer ikke inntektsmelding to ganger ved replay`() {
        // Happy case av testen med navn: Håndterer ikke inntektsmelding to ganger ved replay - hvor vi har en tidligere periode og gap
        val inntektsmeldingId = håndterInntektsmelding(listOf(10.januar til 25.januar), førsteFraværsdag = 10.januar)
        håndterSykmelding(Sykmeldingsperiode(10.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertNoWarnings(1.vedtaksperiode)
    }

    @Test
    fun `Inntekstmelding kommer i feil rekkefølge - riktig inntektsmelding skal bli valgt i vilkårgrunnlaget`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(5.februar, 28.februar, 100.prosent))

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 5.februar, beregnetInntekt = 42000.månedlig)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = INNTEKT)

        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)

        val inntektsopplysning = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.sykepengegrunnlag()?.inntektsopplysningPerArbeidsgiver()?.get(ORGNUMMER)
        assertEquals(INNTEKT, inntektsopplysning?.grunnlagForSykepengegrunnlag())
        assertInstanceOf(Inntektshistorikk.Inntektsmelding::class.java, inntektsopplysning)
    }

    @Test
    fun `Ikke klipp inntektsmelding dersom vi overlapper med forkastet vedtaksperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 11.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(11.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(
                1.januar til 16.januar
            ), førsteFraværsdag = 1.januar
        )
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)

        assertEquals(1.januar til 31.januar, inspektør.vedtaksperioder(2.vedtaksperiode).periode())
    }

    @Test
    fun `sender med arbeidsforholdId på godkjenningsbehov`() {
        val arbeidsforholdId = UUID.randomUUID().toString()

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)), arbeidsforholdId = arbeidsforholdId)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val godkjenningsbehov = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning)
        assertEquals(arbeidsforholdId, godkjenningsbehov.detaljer()["arbeidsforholdId"])
    }

    @Test
    fun `sender ikke med arbeidsforholdId på godkjenningsbehov når det mangler`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val godkjenningsbehov = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning)
        assertNull(godkjenningsbehov.detaljer()["arbeidsforholdId"])
    }

    @Test
    fun `Ber ikke om ny IM hvis det bare er helg mellom to perioder`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 26.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 31.januar, 100.prosent))

        assertFalse(2.vedtaksperiode.id(ORGNUMMER) in observatør.manglendeInntektsmeldingVedtaksperioder)
    }

    @Test
    fun `legger ved inntektsmeldingId på vedtaksperiode_endret-event for forlengende vedtaksperioder`() {
        val inntektsmeldingId = UUID.randomUUID()
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), id = inntektsmeldingId)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).contains(inntektsmeldingId)
        observatør.hendelseider(2.vedtaksperiode.id(ORGNUMMER)).contains(inntektsmeldingId)
    }

    @Test
    fun `legger ved inntektsmeldingId på vedtaksperiode_endret-event for første etterfølgende av en kort periode`() {
        val inntektsmeldingId = UUID.randomUUID()
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), id = inntektsmeldingId)
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))

        observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).contains(inntektsmeldingId)
        observatør.hendelseider(2.vedtaksperiode.id(ORGNUMMER)).contains(inntektsmeldingId)
    }

    @Test
    fun `Avventer inntektsmelding venter faktisk på inntektsmelding, går ikke videre selv om senere periode avsluttes`() {
        håndterSykmelding(Sykmeldingsperiode(28.oktober, 8.november, 100.prosent))
        håndterSøknad(Sykdom(28.oktober, 8.november, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.november, 22.november, 100.prosent))
        håndterSøknad(Sykdom(9.november, 22.november, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.desember, 14.desember, 100.prosent))
        håndterSøknad(Sykdom(10.desember, 14.desember, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `Inntektsmelding treffer periode som dekker hele arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(28.oktober, 8.november, 100.prosent))
        håndterSøknad(Sykdom(28.oktober, 8.november, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.november, 22.november, 100.prosent))
        håndterSøknad(Sykdom(9.november, 22.november, 100.prosent))
        håndterInntektsmelding(listOf(Periode(27.oktober, 8.november)), 27.oktober)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK)
        assertEquals(27.oktober til 8.november, inspektør.periode(1.vedtaksperiode))
        assertTrue(inspektør.sykdomstidslinje[27.oktober] is Dag.ArbeidsgiverHelgedag)
    }

    @ForventetFeil("Vilkårsgrunnlag for arbeidsgiver 1 gjør at vedtaksperiode hos arbeidsgiver 2 ikke venter på inntektsmelding")
    @Test
    fun `vilkårsvurdering med flere arbeidsgivere skal ikke medføre at vi går til avventer historikk fra mottatt sykmelding ferdig forlengelse uten IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        val skjæringstidspunkt = inspektør(a1).skjæringstidspunkt(1.vedtaksperiode)
        val inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
            skjæringstidspunkt.minusMonths(12L).withDayOfMonth(1) til skjæringstidspunkt.minusMonths(1L).withDayOfMonth(1) inntekter {
                a1 inntekt INNTEKT
                a2 inntekt INNTEKT
            }
        })
        val inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = listOf(a1, a2).map { arbeidsgiver ->
            ArbeidsgiverInntekt(arbeidsgiver.toString(), (0..2).map {
                val yearMonth = YearMonth.from(skjæringstidspunkt).minusMonths(3L - it)
                ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                    yearMonth = yearMonth,
                    type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                    inntekt = INNTEKT,
                    fordel = "fordel",
                    beskrivelse = "beskrivelse"
                )
            })
        }, arbeidsforhold = emptyList())
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = inntektsvurdering, inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, orgnummer = a2)
    }

    @Test
    fun `inntektsmelding uten relevant inntekt (fordi perioden er i agp) flytter perioden til ferdig-tilstand`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent), orgnummer = ORGNUMMER)

        håndterSykmelding(Sykmeldingsperiode(9.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 10.januar, 100.prosent), orgnummer = ORGNUMMER)

        håndterSykmelding(Sykmeldingsperiode(12.januar, 24.januar, 100.prosent))
        håndterSøknad(Sykdom(12.januar, 24.januar, 100.prosent))

        håndterInntektsmelding(listOf(Periode(1.januar, 5.januar), Periode(9.januar, 10.januar), Periode(12.januar, 20.januar)), 12.januar)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `Opphør av naturalytelser kaster periode til infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar, harOpphørAvNaturalytelser = true)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Når inntektsmeldingens første fraværsdag er midt i en vedtaksperiode lagres inntekten på vedtaksperiodens skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(30.januar, 12.februar, 100.prosent))
        håndterSøknad(Sykdom(30.januar, 12.februar, 100.prosent))

        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.februar)
        assertTrue(inspektør.sykdomstidslinje[30.januar] is Dag.Arbeidsdag)
        assertTrue(inspektør.sykdomstidslinje[31.januar] is Dag.Arbeidsdag)
        assertInntektForDato(INNTEKT, 1.februar, inspektør=inspektør)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    //@ForventetFeil("1. februar til 11. februar blir ikke mappet som arbeidsdager fordi perioden er i Avventer Uferdig, som ikke håndterer Inntektsmelding. Det lages istedenfor en warning")
    @Test
    fun `inntektsmelding oppgir første fraværsdag i en periode med ferie etter sykdom`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 11.februar))
        // med denne inntektsmeldingen forteller arbeidsgiver én av to ting:
        // det er arbeidsdager mellom 1. februar og 11. februar (siden det kun er februar-perioden som overlapper med inntektsmelding)
        // ELLER at det skal være arbeidsdager i januar også. Arbeidsgiver har sendt inntektsmelding for januar, men arbeidsgiver har ingen mulighet til
        // å fortelle oss om arbeidsdager etter gjennomført arbeidsgiverperiode: det er kun bruker som kan opplyse om det i søknaden.
        // Dersom bruker egentlig har gjenopptatt arbeid 25. januar (f.eks.) vil ikke vi få vite om dette før inntektsmeldingen som blir sendt under.
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 12.februar)
        val tidslinje = inspektør.sykdomstidslinje
        assertTrue((17.januar til 31.januar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag })
        assertFalse((1.februar til 11.februar).all { tidslinje[it] is Dag.Arbeidsdag || tidslinje[it] is Dag.FriskHelgedag })
        assertTrue((12.februar til 28.februar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag  })
        assertNoWarning(1.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertWarning(2.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_UFERDIG)
    }

    @Test
    fun `inntektsmelding oppgir første fraværsdag i en periode med ferie etter sykdom med kort periode først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 11.februar))
        // med denne inntektsmeldingen forteller arbeidsgiver én av to ting:
        // det er arbeidsdager mellom 1. februar og 11. februar (siden det kun er februar-perioden som overlapper med inntektsmelding)
        // ELLER at det skal være arbeidsdager i januar også. Arbeidsgiver har sendt inntektsmelding for januar, men arbeidsgiver har ingen mulighet til
        // å fortelle oss om arbeidsdager etter gjennomført arbeidsgiverperiode: det er kun bruker som kan opplyse om det i søknaden.
        // Dersom bruker egentlig har gjenopptatt arbeid 25. januar (f.eks.) vil ikke vi få vite om dette før inntektsmeldingen som blir sendt under.
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 12.februar)

        val tidslinje = inspektør.sykdomstidslinje
        assertTrue((17.januar til 31.januar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag })
        assertFalse((1.februar til 11.februar).all { tidslinje[it] is Dag.Arbeidsdag || tidslinje[it] is Dag.FriskHelgedag })
        assertTrue((12.februar til 28.februar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag  })
        assertNoWarning(1.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertNoWarning(2.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertWarning(3.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_UFERDIG)
    }

    @Test
    fun `overlappende inntektsmelding på grunn av ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))

        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, LocalDateTime.now().minusYears(1))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 16.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 16.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
    }

    @Test
    fun `Går ikke videre fra AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE hvis forrige periode ikke er ferdig behandlet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(20.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE
        )
    }

    @Test
    fun `Går videre fra AVVENTER_UFERDIG hvis en gammel periode er i AVSLUTTET_UTEN_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(20.november(2017), 12.desember(2017), 100.prosent))
        håndterSøknad(Sykdom(20.november(2017), 12.desember(2017), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 12.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(20.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(20.februar, 8.mars)), 20.februar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(20.november(2017), 12.desember(2017), 100.prosent))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(4.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_UFERDIG, AVVENTER_HISTORIKK)
    }

    @Test
    fun `første fraværsdato fra inntektsmelding er ulik utregnet første fraværsdato`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 4.januar)
        assertWarning(
            1.vedtaksperiode,
            "Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode."
        )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
    }

    @Test
    fun `første fraværsdato fra inntektsmelding er ulik utregnet første fraværsdato for påfølgende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(27.januar, 7.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(3.januar, 18.januar)),
            3.januar
        )
        assertFalse(person.personLogg.hasWarningsOrWorse())
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE
        )
    }

    @Test
    fun `første fraværsdato i inntektsmelding er utenfor perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 27.januar)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
    }

    @Test
    fun `første fraværsdato i inntektsmelding, før søknad, er utenfor perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 27.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
    }

    @Test
    fun `To tilstøtende perioder inntektsmelding først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(8.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 7.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertNoErrors(person)
        assertActivities(person)
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_UFERDIG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `To tilstøtende perioder, inntektsmelding 2 med arbeidsdager i starten`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(Periode(3.januar, 7.januar), Periode(15.januar, 20.januar), Periode(23.januar, 28.januar))
        )
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 7.januar, 100.prosent))

        assertNoErrors(person)
        assertActivities(person)
        assertTilstander(
            1.vedtaksperiode,
            START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP
        )
    }

    @Test
    fun `ignorer inntektsmeldinger på påfølgende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        assertNoErrors(person)
        assertActivities(person)
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
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
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Inntektsmelding vil ikke utvide vedtaksperiode til tidligere vedtaksperiode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(3.januar til 18.januar), 3.januar)
        assertNoErrors(person)
        assertNoWarnings(person)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        forventetEndringTeller++
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            2.vedtaksperiode,
            listOf(3.januar til 18.januar),
            førsteFraværsdag = 1.februar
        ) // Touches prior periode
        assertNoErrors(person)

        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 23.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode, Oppdragstatus.AKSEPTERT)
        assertNoErrors(person)

        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
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
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `IM som treffer periode i AVSLUTTET_UTEN_UTBETALING etter en utbetalt periode skal ikke fylle perioden med arbeidsdager fra fom til skjæringstidspunktet`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(27.februar til 14.mars), førsteFraværsdag = 27.februar)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        // Siden vi tidligere fylte ut 2. vedtaksperiode med arbeidsdager ville vi regne ut et ekstra skjæringstidspunkt i den sammenhengende perioden
        assertEquals(listOf(1.januar), person.skjæringstidspunkter())
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)

        assertNoWarning(1.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertWarning(2.vedtaksperiode, "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        assertEquals(17.januar til 31.mars, inspektør.utbetalinger.last().periode)
    }

    @Test
    fun `vedtaksperiode i AVSLUTTET_UTEN_UTBETALING burde utvides ved replay av inntektsmelding`() {
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)
        håndterSykmelding(Sykmeldingsperiode(4.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode.id(ORGNUMMER))

        assertEquals(1.januar til 10.januar, inspektør.vedtaksperioder(1.vedtaksperiode).periode())

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        assertNoErrors(2.vedtaksperiode, orgnummer = ORGNUMMER)
    }

    @Test
    fun `kaste ut vedtaksperiode hvis arbeidsgiver ikke utbetaler arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "begrunnelse")
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        assertError(1.vedtaksperiode, "Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: begrunnelse")
    }
}
