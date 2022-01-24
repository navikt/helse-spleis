package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetskontekst
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.person.TilstandType.*
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.utbetalingslinjer.Utbetaling.GodkjentUtenUtbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Sendt
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SamletSykdomsgradE2ETest: AbstractEndToEndTest() {

    @Test
    fun `avviser dager under 20 prosent`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 19.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 19.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(0))
        val utbetalingstidslinje = inspektør.utbetalingUtbetalingstidslinje(0)
        assertTrue(utbetalingstidslinje[17.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        assertTrue(utbetalingstidslinje[18.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        assertTrue(utbetalingstidslinje[19.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        assertEquals(3, utbetalingstidslinje.inspektør.avvistDagTeller)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_GODKJENNING,
            AVSLUTTET
        )
    }

    @Test
    fun `avviser dager under 20 prosent på forlengelser`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 19.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 19.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        val utbetalingstidslinje = inspektør.utbetalingUtbetalingstidslinje(1)
        assertTrue(utbetalingstidslinje[17.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        assertTrue(utbetalingstidslinje[18.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        assertTrue(utbetalingstidslinje[19.januar] is Utbetalingstidslinje.Utbetalingsdag.AvvistDag)
        assertEquals(3, utbetalingstidslinje.inspektør.avvistDagTeller)
        assertEquals(Sendt, inspektør.utbetalingtilstand(1))
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
    }

    @ForventetFeil("når vi mottar korrigert søknad ligger det igjen warnings fra før som ikke lengre gjelder")
    @Test
    fun `opprinnelig søknad med 100 prosent arbeidshelse blir korrigert slik at sykdomsgraden blir 100 prosent `() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent, 100.prosent)) // 100 prosent arbeidshelse => 0 prosent syk
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent)) // korrigert søknad med 0 prosent arbeidshelse => 100 prosent syk
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
            AVVENTER_GODKJENNING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertNoWarnings(inspektør)
    }

    @Test
    fun `ny periode med egen arbeidsgiverperiode skal ikke ha warning pga sykdomsgrad som gjelder forrige periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 19.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 19.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 20.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 20.mars, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.mars, 16.mars)))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        assertTrue(inspektør.vedtaksperiodeLogg(1.vedtaksperiode).hasWarningsOrWorse())
        assertFalse(inspektør.vedtaksperiodeLogg(2.vedtaksperiode).hasWarningsOrWorse())
    }

    private fun TestArbeidsgiverInspektør.vedtaksperiodeLogg(vedtaksperiodeIdInnhenter: IdInnhenter) = personLogg.logg(object : Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst("Vedtaksperiode", mapOf(
            "vedtaksperiodeId" to "${this@vedtaksperiodeLogg.vedtaksperiodeId(vedtaksperiodeIdInnhenter)}"
        ))
    })
}
