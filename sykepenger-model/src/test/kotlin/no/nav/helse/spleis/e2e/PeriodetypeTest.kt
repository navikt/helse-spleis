package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PeriodetypeTest : AbstractEndToEndTest() {

    @Test
    fun `periodetype settes til førstegangs hvis foregående ikke hadde utbetalingsdager`() {
        håndterSykmelding(Sykmeldingsperiode(28.januar(2020), 10.februar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(28.januar(2020), 10.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(28.januar(2020), 12.februar(2020))),
            førsteFraværsdag = 28.januar(2020)
        )

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode)

        håndterSimulering(2.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
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
        håndterSykmelding(Sykmeldingsperiode(28.januar(2020), 10.februar(2020), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(28.januar(2020), 10.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(28.januar(2020), 12.februar(2020))),
            førsteFraværsdag = 28.januar(2020)
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
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
    fun `periodetype er overgang fra Infotrygd hvis foregående ble behandlet i Infotrygd`() {
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar,  26.januar, 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 3.januar(2018), 1000.daglig, true))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING
        )
        assertEquals(
            Periodetype.OVERGANG_FRA_IT.name,
            hendelselogg.behov().first().detaljer()["periodetype"]
        )
    }

    @Test
    fun `periodetype er forlengelse fra Infotrygd hvis førstegangsbehandlingen skjedde i Infotrygd`() {
        val historikk = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 3.januar,  26.januar, 100.prosent, 1000.daglig)
        val inntekter = listOf(Inntektsopplysning(ORGNUMMER, 3.januar(2018), 1000.daglig, true))
        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar, 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, historikk, inntektshistorikk = inntekter)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(
            Periodetype.OVERGANG_FRA_IT.name,
            hendelselogg.behov().first().detaljer()["periodetype"]
        )

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(26.februar, 15.april, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(26.februar, 15.april, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertEquals(
            Periodetype.INFOTRYGDFORLENGELSE.name,
            hendelselogg.behov().first().detaljer()["periodetype"]
        )
    }

    @Test
    fun `periodetype settes til førstegangs hvis foregående ikke hadde utbetalingsdager pga lav sykdomsgrad`() {
        håndterSykmelding(Sykmeldingsperiode(20.januar(2020), 10.februar(2020), 15.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterSøknad(Sykdom(20.januar(2020), 10.februar(2020), 15.prosent))
        håndterSøknad(Sykdom(11.februar(2020), 21.februar(2020), 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(20.januar(2020), 4.februar(2020))),
            førsteFraværsdag = 20.januar(2020)
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2019) til 1.desember(2019) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
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
