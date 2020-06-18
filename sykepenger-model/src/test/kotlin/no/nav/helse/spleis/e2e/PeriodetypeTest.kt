package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PeriodetypeTest : AbstractEndToEndTest() {

    @Test
    fun `periodetype settes til førstegangs hvis foregående ikke hadde utbetalingsdager`() {
        håndterSykmelding(Triple(28.januar(2020), 10.februar(2020), 100))
        håndterSykmelding(Triple(11.februar(2020), 21.februar(2020), 100))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Søknadsperiode(28.januar(2020), 10.februar(2020), 100))
        håndterSøknad(Sykdom(11.februar(2020), 21.februar(2020), 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(28.januar(2020), 12.februar(2020))),
            førsteFraværsdag = 28.januar(2020)
        )
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(1)   // No history
        håndterSimulering(1)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVSLUTTET_UTEN_UTBETALING,
            TilstandType.AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD,
            TilstandType.AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            1,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING
        )

        assertEquals(
            Periodetype.FØRSTEGANGSBEHANDLING.name,
            hendelselogg.behov().first().detaljer()["periodetype"]
        )
    }

    @Test
    fun `periodetype settes til førstegangs hvis foregående ikke hadde utbetalingsdager - ikke arbeidsgiversøknad`() {
        håndterSykmelding(Triple(28.januar(2020), 10.februar(2020), 100))
        håndterSykmelding(Triple(11.februar(2020), 21.februar(2020), 100))
        håndterSøknad(Sykdom(28.januar(2020), 10.februar(2020), 100))
        håndterSøknad(Sykdom(11.februar(2020), 21.februar(2020), 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(28.januar(2020), 12.februar(2020))),
            førsteFraværsdag = 28.januar(2020)
        )
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)
        håndterYtelser(1)
        håndterSimulering(1)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_GAP,
            TilstandType.AVVENTER_VILKÅRSPRØVING_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING
        )
        assertTilstander(
            1,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING
        )

        assertEquals(
            Periodetype.FØRSTEGANGSBEHANDLING.name,
            hendelselogg.behov().first().detaljer()["periodetype"]
        )
    }

    @Test
    fun `periodetype settes til infotrygdforlengelse hvis foregående ble behandlet i Infotrygd`() {
        val historikk = Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(3.januar, 26.januar, 1000, 100, ORGNUMMER)

        håndterSykmelding(Triple(29.januar, 23.februar, 100))
        håndterSøknadMedValidering(0, Sykdom(29.januar, 23.februar, 100))
        håndterUtbetalingshistorikk(0, historikk)
        håndterYtelser(0, historikk)
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0, historikk)
        håndterSimulering(0)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_GAP,
            TilstandType.AVVENTER_VILKÅRSPRØVING_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING
        )
        assertEquals(
            Periodetype.INFOTRYGDFORLENGELSE.name,
            hendelselogg.behov().first().detaljer()["periodetype"]
        )
    }

    @Test
    fun `periodetype settes til førstegangs hvis foregående ikke hadde utbetalingsdager pga lav sykdomsgrad`() {
        håndterSykmelding(Triple(20.januar(2020), 10.februar(2020), 15))
        håndterSykmelding(Triple(11.februar(2020), 21.februar(2020), 100))
        håndterSøknad(Sykdom(20.januar(2020), 10.februar(2020), 15))
        håndterSøknad(Sykdom(11.februar(2020), 21.februar(2020), 100))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(20.januar(2020), 4.februar(2020))),
            førsteFraværsdag = 20.januar(2020)
        )
        håndterVilkårsgrunnlag(0, INNTEKT)
        håndterYtelser(0)   // No history
        håndterUtbetalingsgodkjenning(0, true)
        håndterYtelser(1)   // No history
        håndterSimulering(1)

        assertTilstander(
            0,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_GAP,
            TilstandType.AVVENTER_VILKÅRSPRØVING_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.AVSLUTTET
        )
        assertTilstander(
            1,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING
        )

        assertEquals(
            Periodetype.FØRSTEGANGSBEHANDLING.name,
            hendelselogg.behov().first().detaljer()["periodetype"]
        )
    }

}
