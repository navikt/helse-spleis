package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Test

internal class ForlengelseFraInfotrygdTest : AbstractEndToEndTest() {

    @Test
    internal fun `forlenger vedtaksperiode som har gått til infotrygd`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterUtbetalingshistorikk(0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000, 100)) // <-- TIL_INFOTRYGD
        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(29.januar,  23.februar, 100))

        håndterYtelser(1, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000, 100))
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    internal fun `forlenger ikke vedtaksperiode som har gått til infotrygd, der utbetaling ikke er gjort`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterUtbetalingshistorikk(0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000, 100))  // <-- TIL_INFOTRYGD
        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(1, Sykdom(29.januar,  23.februar, 100))
        håndterYtelser(1, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(3.januar, 25.januar, 1000, 100))
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, TIL_INFOTRYGD)
    }

    @Test
    fun `forlengelsesperiode der refusjon opphører`() {
        håndterSykmelding(Triple(13.mars(2020), 29.mars(2020), 100))
        håndterInntektsmeldingMedValidering(0, listOf(Periode(13.mars(2020), 28.mars(2020))), førsteFraværsdag = 13.mars(2020), refusjon = Triple(31.mars(2020), INNTEKT, emptyList()))
        håndterSykmelding(Triple(30.mars(2020), 14.april(2020), 100))
        håndterSøknad(Sykdom(13.mars(2020), 29.mars(2020), 100))
        håndterSøknad(Sykdom(30.mars(2020), 14.april(2020), 100))
        håndterYtelser(1, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(13.mars(2020), 29.mars(2020), 1000, 100), inntektshistorikk = listOf(
            Utbetalingshistorikk.Inntektsopplysning(13.mars(2020), INNTEKT.toInt(), ORGNUMMER, true, 31.mars(2020))
        ))
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, TIL_INFOTRYGD)
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes ikke påfølgende, tilstøtende perioder som bare har mottatt sykmelding`() {
        håndterSykmelding(Triple(1.januar, 31.januar, 100))
        håndterSykmelding(Triple(1.februar, 28.februar, 100))
        håndterSykmelding(Triple(14.mars, 31.mars, 100))
        håndterUtbetalingshistorikk(0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 31.januar, 1000, 100))  // <-- TIL_INFOTRYGD
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(1, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertTilstander(2, START, MOTTATT_SYKMELDING_UFERDIG_GAP)
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes påfølgende, tilstøtende perioder også`() {
        håndterSykmelding(Triple(1.januar, 31.januar, 100))
        håndterSykmelding(Triple(1.februar, 28.februar, 100))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100))
        håndterSykmelding(Triple(14.mars, 31.mars, 100))
        håndterSøknad(Sykdom(14.mars, 31.mars, 100))
        håndterUtbetalingshistorikk(0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 31.januar, 1000, 100))  // <-- TIL_INFOTRYGD
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(1, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, TIL_INFOTRYGD)
        assertTilstander(2, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
    }

    @Test
    fun `når en periode går Til Infotrygd avsluttes påfølgende, tilstøtende perioder også (out of order)`() {
        håndterSykmelding(Triple(14.mars, 31.mars, 100))
        håndterSykmelding(Triple(1.februar, 28.februar, 100))
        håndterSøknad(Sykdom(14.mars, 31.mars, 100))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100))
        håndterSykmelding(Triple(1.januar, 31.januar, 100))
        håndterUtbetalingshistorikk(0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.januar, 31.januar, 1000, 100))  // <-- TIL_INFOTRYGD
        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, TIL_INFOTRYGD)
        assertTilstander(2, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP)
    }
}
