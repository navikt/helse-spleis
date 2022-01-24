package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.TilstandType.*
import no.nav.helse.januar
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class IngentingÅSimulereE2ETest : AbstractEndToEndTest() {

    @Test
    fun `forlenger et vedtak med bare helg`() {
        nyttVedtak(1.januar, 19.januar)
        håndterSykmelding(Sykmeldingsperiode(20.januar, 21.januar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(20.januar, 21.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVSLUTTET)
        assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(1))
    }

    @Test
    fun `førstegangsbehandling på eksisterende utbetaling med bare helg`() {
        nyttVedtak(1.januar, 18.januar)
        håndterSykmelding(Sykmeldingsperiode(20.januar, 21.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 20.januar)
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(20.januar, 21.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVSLUTTET)
        assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(1))
    }

    @Test
    fun `forlenger et vedtak med bare helg og litt ferie`() {
        nyttVedtak(1.januar, 19.januar)
        håndterSykmelding(Sykmeldingsperiode(20.januar, 23.januar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(20.januar, 21.januar, 100.prosent), Ferie(22.januar, 23.januar))
        håndterYtelser(2.vedtaksperiode)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, AVVENTER_HISTORIKK, AVSLUTTET)
        assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(1))
    }

    @Test
    fun `tomt simuleringsresultat`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 21.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode, simuleringsresultat = null)
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_SØKNAD_FERDIG_GAP, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
    }
}
