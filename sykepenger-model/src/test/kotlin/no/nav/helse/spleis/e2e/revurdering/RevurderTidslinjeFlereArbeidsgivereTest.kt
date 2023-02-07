package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
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
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.AUUSomFørstegangsbehandling::class)
internal class RevurderTidslinjeFlereArbeidsgivereTest : AbstractEndToEndTest() {

    private companion object {
        private val aadvokatene = "123456789"
        private val haandtverkerne = "987612345"
    }

    @Test
    fun `revurdering for periode som start samme dag som en førstegangsvurdering`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = haandtverkerne)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 25.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(17.januar, 25.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 20000.månedlig, orgnummer = haandtverkerne)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode,
            orgnummer = haandtverkerne,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    aadvokatene inntekt 20000.månedlig
                    haandtverkerne inntekt 20000.månedlig
                }
            }),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    aadvokatene inntekt 20000.månedlig
                    haandtverkerne inntekt 20000.månedlig
                }
            }, arbeidsforhold = listOf()),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(haandtverkerne, 1.januar(2017)),
                Vilkårsgrunnlag.Arbeidsforhold(aadvokatene, 1.januar(2017))
            )
        )
        håndterYtelser(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(2.vedtaksperiode, orgnummer = haandtverkerne)

        håndterSykmelding(Sykmeldingsperiode(10.januar, 16.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(10.januar, 16.januar, 100.prosent), orgnummer = aadvokatene)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 25.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(17.januar, 25.januar, 100.prosent), orgnummer = aadvokatene)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(2.vedtaksperiode, orgnummer = haandtverkerne)

        håndterInntektsmelding(listOf(2.januar til 17.januar), beregnetInntekt = 20000.månedlig, orgnummer = aadvokatene)

        assertIngenFunksjonelleFeil()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = haandtverkerne)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = haandtverkerne)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = aadvokatene)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = aadvokatene)
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
            assertIngenFunksjonelleFeil()
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
            assertIngenFunksjonelleFeil()
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
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertIngenFunksjonelleFeil()
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
            assertIngenFunksjonelleFeil()
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
                AVVENTER_REVURDERING,
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
            assertIngenFunksjonelleFeil()
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
            assertIngenFunksjonelleFeil()
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
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertIngenFunksjonelleFeil()
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
            assertIngenFunksjonelleFeil()
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
            AVVENTER_REVURDERING,
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
        håndterYtelser(2.vedtaksperiode, orgnummer = haandtverkerne)

        inspektør(haandtverkerne) {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING
            )
            assertTilstander(
                2.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING
            )
            assertIngenFunksjonelleFeil()
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        inspektør(aadvokatene) {
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
            assertIngenFunksjonelleFeil()
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
