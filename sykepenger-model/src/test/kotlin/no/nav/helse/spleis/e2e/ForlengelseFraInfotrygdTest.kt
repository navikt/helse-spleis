package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class ForlengelseFraInfotrygdTest : AbstractEndToEndTest() {

    @Test
    internal fun `forlenger vedtaksperiode som har gått til infotrygd`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterUtbetalingshistorikk(
            0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                3.januar,
                26.januar,
                1000,
                100,
                ORGNUMMER
            )
        ) // <-- TIL_INFOTRYGD
        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(0, Sykdom(29.januar, 23.februar, 100))

        håndterUtbetalingshistorikk(
            0,
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000, 100, ORGNUMMER)
        )

        håndterYtelser(
            0,
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000, 100, ORGNUMMER)
        )
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP)
        assertEquals(3.januar, inspektør.førsteFraværsdag(0)) {
            "Første fraværsdag settes til den første utbetalte dagen fordi " +
                "vi ikke er i stand til å regne den ut selv ennå. " +
                "Bør regnes ut riktig når vi har én sykdomstidslinje på arbeidsgiver-nivå"
        }
    }

    @Test
    internal fun `forlenger ikke vedtaksperiode som har gått til infotrygd, der utbetaling ikke er gjort`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterUtbetalingshistorikk(
            0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                3.januar,
                26.januar,
                1000,
                100,
                ORGNUMMER
            )
        )  // <-- TIL_INFOTRYGD
        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(0, Sykdom(29.januar, 23.februar, 100))
        håndterYtelser(
            0,
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(3.januar, 25.januar, 1000, 100, ORGNUMMER)
        )
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_INNTEKTSMELDING_FERDIG_GAP)
    }

    @Test
    fun `forlengelsesperiode der refusjon opphører`() {
        håndterSykmelding(Triple(13.mars(2020), 29.mars(2020), 100))
        håndterInntektsmeldingMedValidering(
            0,
            listOf(Periode(13.mars(2020), 28.mars(2020))),
            førsteFraværsdag = 13.mars(2020),
            refusjon = Triple(31.mars(2020), INNTEKT, emptyList())
        )
        håndterSykmelding(Triple(30.mars(2020), 14.april(2020), 100))
        håndterSøknad(Sykdom(13.mars(2020), 29.mars(2020), 100))
        håndterSøknad(Sykdom(30.mars(2020), 14.april(2020), 100))
        håndterYtelser(
            0,
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(13.mars(2020), 29.mars(2020), 1000, 100, ORGNUMMER),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(13.mars(2020), INNTEKT.toInt(), ORGNUMMER, true, 31.mars(2020))
            )
        )
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, TIL_INFOTRYGD)
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes ikke påfølgende, tilstøtende perioder som bare har mottatt sykmelding`() {
        håndterSykmelding(Triple(1.januar, 31.januar, 100))
        håndterSykmelding(Triple(1.februar, 28.februar, 100))
        håndterSykmelding(Triple(14.mars, 31.mars, 100))
        håndterUtbetalingshistorikk(
            0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                1.januar,
                31.januar,
                1000,
                100,
                ORGNUMMER
            )
        )  // <-- TIL_INFOTRYGD
        assertTilstander(2, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(0, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(1, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `avdekker tilstøtende periode i Infotrygd`() {
        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100))
        håndterYtelser(
            0,
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000, 100, ORGNUMMER)
        )
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, AVVENTER_VILKÅRSPRØVING_GAP)
        assertEquals(3.januar, inspektør.førsteFraværsdag(0)) {
            "Første fraværsdag settes til den første utbetalte dagen fordi " +
                "vi ikke er i stand til å regne den ut selv ennå. " +
                "Bør regnes ut riktig når vi har én sykdomstidslinje på arbeidsgiver-nivå"
        }
    }

    @Test
    fun `avdekker tilstøtende periode i Infotrygd uten at vi har Inntektsopplysninger`() {
        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknad(Sykdom(29.januar, 23.februar, 100))
        håndterYtelser(
            0,
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000, 100, ORGNUMMER),
            inntektshistorikk = emptyList()
        )
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, TIL_INFOTRYGD)
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes påfølgende, tilstøtende perioder også`() {
        håndterSykmelding(Triple(1.januar, 31.januar, 100))
        håndterSykmelding(Triple(1.februar, 28.februar, 100))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100))
        håndterSykmelding(Triple(14.mars, 31.mars, 100))
        håndterSøknad(Sykdom(14.mars, 31.mars, 100))
        håndterUtbetalingshistorikk(
            0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                1.januar,
                31.januar,
                1000,
                100,
                ORGNUMMER
            )
        )  // <-- TIL_INFOTRYGD
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(
            2,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
        assertTilstander(0, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes påfølgende, tilstøtende perioder også (out of order)`() {
        håndterSykmelding(Triple(14.mars, 31.mars, 100))
        håndterSykmelding(Triple(1.februar, 28.februar, 100))
        håndterSøknad(Sykdom(14.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100))
        håndterSykmelding(Triple(1.januar, 31.januar, 100))
        håndterUtbetalingshistorikk(
            0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                1.januar,
                31.januar,
                1000,
                100,
                ORGNUMMER
            )
        )  // <-- TIL_INFOTRYGD
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(2, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, TIL_INFOTRYGD)
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP)
    }

    @Test
    fun `n`() {
        håndterSykmelding(Triple(1.januar, 31.januar, 100))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(1.januar, 16.januar)), 1.januar)
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)
        håndterSimulering(0)
        håndterUtbetalingsgodkjenning(0, true)
        håndterUtbetalt(0, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)


        håndterSykmelding(Triple(1.februar, 28.februar, 100))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100), Permisjon(10.februar, 20.februar))    // <-- TIL_INFOTRYGD


        håndterSykmelding(Triple(1.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100))
        håndterYtelser(0, Utbetalingshistorikk.Periode.Utbetaling(1.februar, 28.februar, 1000, 100, ORGNUMMER))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0, Utbetalingshistorikk.Periode.Utbetaling(1.februar, 28.februar, 1000, 100, ORGNUMMER))
        håndterSimulering(0)
        håndterUtbetalingsgodkjenning(0, true)
        håndterUtbetalt(0, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        assertForkastetPeriodeTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertForkastetPeriodeTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, TIL_INFOTRYGD)

        assertGyldigPeriodeTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_GAP,
            AVVENTER_VILKÅRSPRØVING_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertEquals(inspektør.maksdato(2), inspektør.maksdato(0))
    }

    @Test
    fun `lager ny arbeidsgiverperiode selv om det er tilstøtende historikk`() {
        håndterSykmelding(Triple(18.februar(2020), 3.mars(2020), 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(18.februar(2020), 3.mars(2020), 100))
        håndterSykmelding(Triple(4.mars(2020), 17.mars(2020), 100))
        håndterSøknad(Sykdom(4.mars(2020), 17.mars(2020), 100))
        håndterSykmelding(Triple(18.mars(2020), 15.april(2020), 70))
        håndterSøknad(Sykdom(18.mars(2020), 15.april(2020), 70))
        håndterInntektsmelding(listOf(Periode(18.februar(2020), 4.mars(2020))), 18.februar(2020))
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(1)
        håndterSimulering(1)
        håndterPåminnelse(1, AVVENTER_GODKJENNING, LocalDateTime.now().minusDays(3)) // <-- TIL_INFOTRYGD
        håndterSykmelding(Triple(16.april(2020), 7.mai(2020), 50))
        håndterSøknad(Sykdom(16.april(2020), 7.mai(2020), 50))
        håndterYtelser(3,
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(5.mars(2020), 17.mars(2020), 1000, 100),
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(18.mars(2020), 15.april(2020), 1000, 100),
            inntektshistorikk = listOf(
                Utbetalingshistorikk.Inntektsopplysning(5.mars(2020), INNTEKT.toInt(), ORGNUMMER, true)
            )
        )
        håndterSimulering(3)

        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD, AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING)
        assertTilstander(1, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, AVVENTER_UFERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_INFOTRYGD)
        assertTilstander(2, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, TIL_INFOTRYGD)
        assertTilstander(3, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
        inspektør.arbeidsgiverutbetalinger(0).also {
            assertEquals(2, it.size)
            UtbetalingstidslinjeInspektør(it.last().utbetalingstidslinje().gjøreKortere(16.april(2020))).result().also {
                assertEquals(0, it.arbeidsgiverperiodeDagTeller)
                assertEquals(16, it.navDagTeller)
            }
        }
    }
}
