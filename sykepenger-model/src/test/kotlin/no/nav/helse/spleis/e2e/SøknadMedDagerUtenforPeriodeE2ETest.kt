package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SøknadMedDagerUtenforPeriodeE2ETest: AbstractEndToEndTest() {

    @Test
    fun `søppelbøtter dersom ny søknad inneholder dager som overlapper med tidligere sykmelding`(){
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(20.januar, 31.januar))
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
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
    fun `periode med ferie utenfor søknadsperiode aksepteres men får warning`(){
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(20.januar, 31.januar))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
        assertWarningTekst(inspektør, "Søknaden inneholder Feriedager utenfor perioden søknaden gjelder for")
    }

    @Test
    fun `eldgammel ferieperiode før sykdomsperioden klippes bort`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 28.mars, 100.prosent))
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.mars, 28.mars, 100.prosent),
            Søknad.Søknadsperiode.Ferie(LocalDate.of(2014, 7, 1), LocalDate.of(2015, 7, 10))
        )
        assertWarningTekst(inspektør, "Søknaden inneholder Feriedager utenfor perioden søknaden gjelder for")
        assertEquals(null, inspektør.vedtaksperiodeDagTeller[1.vedtaksperiode]?.get(Feriedag::class))
    }

    @Test
    fun `ferieperiode som begynte for mindre enn 40 dager før sykdomsperioden blir tatt hensyn til`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 28.mars, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 28.mars, 100.prosent), Søknad.Søknadsperiode.Ferie(21.januar, 30.januar))
        assertWarningTekst(inspektør, "Søknaden inneholder Feriedager utenfor perioden søknaden gjelder for")
        assertEquals(10, inspektør.vedtaksperiodeDagTeller[1.vedtaksperiode]?.get(Feriedag::class))
    }

    @Test
    fun `periode med ferie før sykemeldingsperiode finnes på sykdomstidslinjen`(){
        nyttVedtak(1.februar, 28.februar)
        håndterSykmelding(Sykmeldingsperiode(10.mars, 25.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(10.mars, 31.mars)))
        håndterSøknad(Søknad.Søknadsperiode.Ferie(1.mars, 9.mars), Søknad.Søknadsperiode.Sykdom(10.mars, 25.mars, 100.prosent))

        assertWarningTekst(inspektør, "Søknaden inneholder Feriedager utenfor perioden søknaden gjelder for")
        assertEquals(9, inspektør.vedtaksperiodeDagTeller[2.vedtaksperiode]?.get(Feriedag::class))
    }

    @Test
    fun `søknad med ferie mellom to perioder bridger de to periodene`(){
        nyttVedtak(1.januar, 19.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(20.januar, 31.januar))
        assertWarningTekst(inspektør, "Søknaden inneholder Feriedager utenfor perioden søknaden gjelder for")
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
        )
    }

    @Test
    fun `søknad med arbeidsdager mellom to perioder bridger ikke de to periodene`(){
        nyttVedtak(1.januar, 19.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Arbeid(20.januar, 31.januar))
        assertWarningTekst(inspektør, "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu")
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
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
    fun `søknad med permisjon mellom to perioder bridger ikke de to periodene`() {
        nyttVedtak(1.januar, 19.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Permisjon(20.januar, 31.januar))
        assertWarningTekst(
            inspektør,
            "Søknaden inneholder Permisjonsdager utenfor sykdomsvindu",
            "Permisjon oppgitt i perioden i søknaden. Vurder rett til sykepenger og korriger sykmeldingsperioden"
        )
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
        )
    }
}
