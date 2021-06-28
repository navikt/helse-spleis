package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.time.LocalDate

@TestInstance(Lifecycle.PER_CLASS)
internal class OverstyrerUtbetaltTidslinjeFlereAGTest : AbstractEndToEndTest() {

    private companion object {
        private const val AG1 = "123456789"
        private const val AG2 = "987612345"
    }

    @BeforeAll
    fun beforeAll() {
        Toggles.RevurderTidligerePeriode.enable()
    }

    @AfterAll
    fun afterAll() {
        Toggles.RevurderTidligerePeriode.disable()
    }

    @Test
    fun `to ag - to perioder - én blir revurdert`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = AG1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = AG2
        )

        håndterUtbetalingsgrunnlag(1.vedtaksperiode(AG1), orgnummer = AG1)
        håndterYtelser(vedtaksperiodeId = 1.vedtaksperiode(AG1), orgnummer = AG1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeId = 1.vedtaksperiode(AG1),
            orgnummer = AG1,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    AG1 inntekt 20000.månedlig
                    AG2 inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode(AG1), orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode(AG1), orgnummer = AG1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(AG1), orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode(AG1), orgnummer = AG1)

        håndterUtbetalingsgrunnlag(1.vedtaksperiode(AG2), orgnummer = AG2)
        håndterYtelser(1.vedtaksperiode(AG2), orgnummer = AG2)
        håndterSimulering(1.vedtaksperiode(AG2), orgnummer = AG2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(AG2), orgnummer = AG2)
        håndterUtbetalt(1.vedtaksperiode(AG2), orgnummer = AG2)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(1.vedtaksperiode(AG1), orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode(AG1), orgnummer = AG1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(AG1), orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode(AG1), orgnummer = AG1)

        håndterYtelser(1.vedtaksperiode(AG2), orgnummer = AG2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(AG2), orgnummer = AG2)

        assertNoErrors(inspektør)

        assertTilstander(
            1.vedtaksperiode(AG1),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_ARBEIDSGIVERE,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
//        assertTilstander(
//            1.vedtaksperiode(AG2),
//            START,
//            MOTTATT_SYKMELDING_FERDIG_GAP,
//            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
//            AVVENTER_ARBEIDSGIVERE,
//            AVVENTER_UTBETALINGSGRUNNLAG,
//            AVVENTER_HISTORIKK,
//            AVVENTER_SIMULERING,
//            AVVENTER_GODKJENNING,
//            TIL_UTBETALING,
//            AVSLUTTET
//        )
    }

    private fun manuellPermisjonsdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Permisjonsdag)
    private fun manuellFeriedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Feriedag)
    private fun manuellSykedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, 100)
}
