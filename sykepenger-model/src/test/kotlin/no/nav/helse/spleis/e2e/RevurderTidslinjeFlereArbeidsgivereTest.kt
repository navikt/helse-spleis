package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RevurderTidslinjeFlereArbeidsgivereTest : AbstractEndToEndTest() {

    private companion object {
        private val aadvokatene = "123456789"
        private val haandtverkerne = "987612345"
    }

    @Test
    fun `forlengelse av revurderte flere arbeidsgivere bør da virke`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = aadvokatene
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = haandtverkerne
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = aadvokatene)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = aadvokatene,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    aadvokatene inntekt 20000.månedlig
                    haandtverkerne inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(orgnummer = haandtverkerne)

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)

        // og så forlenger vi.
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = aadvokatene)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)

        håndterYtelser(2.vedtaksperiode, orgnummer = aadvokatene)

        håndterSimulering(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(orgnummer = aadvokatene)

        håndterYtelser(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(orgnummer = haandtverkerne)


        inspektør(aadvokatene) {
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertNoErrors()
            assertEquals(3, utbetalinger.filter { it.erAvsluttet() }.size)
        }

        inspektør(haandtverkerne) {
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertNoErrors()
            assertEquals(3, utbetalinger.filter { it.erAvsluttet() }.size)
        }
    }

    @Test
    fun `to AG - én periode på hver - én blir revurdert`() {
        nyeVedtak(1.januar, 31.januar, haandtverkerne, aadvokatene)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = haandtverkerne)
        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(orgnummer = haandtverkerne)

        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)

        inspektør(haandtverkerne) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertNoErrors()
            assertEquals(2, utbetalinger.filter { it.erAvsluttet() }.size)
        }

        inspektør(aadvokatene) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
            assertNoErrors()
            assertEquals(2, utbetalinger.filter { it.erAvsluttet() }.size)
        }
    }

    @Test
    fun `to AG - to perioder på den ene der den siste er ufullstendig, én periode på den andre - én blir revurdert`() {
        nyeVedtak(1.januar, 31.januar, haandtverkerne, aadvokatene)
        nullstillTilstandsendringer()

        håndterSykmelding(Sykmeldingsperiode(10.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(10.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = haandtverkerne)
        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(orgnummer = haandtverkerne)

        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)

        inspektør(haandtverkerne) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
            )
            assertNoErrors()
            assertEquals(2, utbetalinger.filter { it.erAvsluttet() }.size)
        }

        inspektør(aadvokatene) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
            assertNoErrors()
            assertEquals(2, utbetalinger.filter { it.erAvsluttet() }.size)
        }
    }

    @Test
    fun `to AG - to perioder på hver - første periode blir revurdert på én AG og avventer godkjenning`() {
        nyeVedtak(1.januar, 31.januar, haandtverkerne, aadvokatene)
        forlengVedtak(1.februar, 28.februar, haandtverkerne, aadvokatene)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = haandtverkerne)
        håndterYtelser(2.vedtaksperiode, orgnummer = haandtverkerne)

        håndterSimulering(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(orgnummer = haandtverkerne)

        håndterYtelser(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = aadvokatene)

        inspektør(haandtverkerne) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertNoErrors()
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        inspektør(aadvokatene) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
            assertNoErrors()
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun `revurdere en AG når en annen AG er til godkjenning`() {
        tilGodkjenning(1.januar, 31.januar, aadvokatene, haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)

        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            orgnummer = aadvokatene
        )
        assertTilstander(
            1.vedtaksperiode,
            AVVENTER_GODKJENNING,
            AVVENTER_BLOKKERENDE_PERIODE,
            orgnummer = haandtverkerne
        )
    }

    @Test
    fun `to AG - to perioder på hver - én blir revurdert på én AG`() {
        nyeVedtak(1.januar, 31.januar, haandtverkerne, aadvokatene)
        forlengVedtak(1.februar, 28.februar, haandtverkerne, aadvokatene)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)
        håndterYtelser(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(2.vedtaksperiode, orgnummer = aadvokatene)

        inspektør(aadvokatene) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING
            )
            assertNoErrors()
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        inspektør(haandtverkerne) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING
            )
            assertNoErrors()
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun `Revurdering til ferie på a1 skal ikke påvirke utbetalingen til a2`() {
        nyeVedtak(1.januar, 31.januar, a1, a2, inntekt = 32000.månedlig)
        assertPeriode(17.januar til 31.januar, a1, 1081.daglig)
        assertPeriode(17.januar til 31.januar, a2, 1080.daglig)

        håndterOverstyrTidslinje((17.januar til 21.januar).map { ManuellOverskrivingDag(it, Feriedag) }, orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)

        assertPeriode(17.januar til 21.januar, a1, INGEN)
        assertPeriode(22.januar til 31.januar, a1, 1081.daglig)

        assertPeriode(17.januar til 31.januar, a2, 1080.daglig)
    }

    private fun assertDag(dato: LocalDate, orgnummer: String, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt) {
        inspektør(orgnummer).sisteUtbetalingUtbetalingstidslinje()[dato].let {
            if (it is NavHelgDag) return
            assertEquals(arbeidsgiverbeløp, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(personbeløp, it.økonomi.inspektør.personbeløp)
        }
    }
    private fun assertPeriode(periode: Periode, orgnummer: String, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt = INGEN) =
        periode.forEach { assertDag(it, orgnummer, arbeidsgiverbeløp, personbeløp) }

}
