package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Test

internal class DobbelbehandlingIInfotrygdTest : AbstractEndToEndTest() {

    @Test
    internal fun `avdekker overlapp dobbelbehandlinger i Infotrygd`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterUtbetalingshistorikk(0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
            3.januar,
            26.januar,
            1000,
            100,
            ORGNUMMER
        ))

        håndterSykmelding(Triple(3.februar, 26.februar, 100))
        håndterUtbetalingshistorikk(0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
            26.februar,
            26.mars,
            1000,
            100,
            ORGNUMMER
        ))

        håndterSykmelding(Triple(1.mai, 30.mai, 100))
        håndterUtbetalingshistorikk(0, Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.april, 1.mai, 1000, 100, ORGNUMMER))

        assertTilstander(0, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(1, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
        assertTilstander(2, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
    }
}
