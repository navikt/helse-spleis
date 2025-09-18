package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RevurderTidslinjeFlereArbeidsgivereTest : AbstractEndToEndTest() {

    @Test
    fun `revurdering for periode som start samme dag som en førstegangsvurdering`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 25.januar), orgnummer = a2)
        håndterSøknad(Sykdom(17.januar, 25.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 20000.månedlig,
            orgnummer = a2
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, a1, a2, orgnummer = a2)
        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(10.januar, 16.januar), orgnummer = a1)
        håndterSøknad(Sykdom(10.januar, 16.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 25.januar), orgnummer = a1)
        håndterSøknad(Sykdom(17.januar, 25.januar, 100.prosent), orgnummer = a1)

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)

        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            beregnetInntekt = 20000.månedlig,
            orgnummer = a1
        )

        assertIngenFunksjonelleFeil()

        assertVarsler(listOf(RV_IM_4), 2.vedtaksperiode.filter(orgnummer = a1))
        assertVarsler(listOf(Varselkode.RV_VV_2), 2.vedtaksperiode.filter(orgnummer = a2))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
    }

    @Test
    fun `to AG - to perioder på hver - første periode blir revurdert på én AG og avventer godkjenning`() {
        nyeVedtak(januar, a2, a1)
        forlengVedtak(februar, a2, a1)
        nullstillTilstandsendringer()

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = a2)
        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter(orgnummer = a2))
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)

        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
            assertIngenFunksjonelleFeil()
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
            assertIngenFunksjonelleFeil()
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun `revurdere en AG når en annen AG er til godkjenning`() {
        tilGodkjenning(januar, a1, a2)
        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        nullstillTilstandsendringer()

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `to AG - to perioder på hver - én blir revurdert på én AG`() {
        nyeVedtak(januar, a2, a1)
        forlengVedtak(februar, a2, a1)
        nullstillTilstandsendringer()

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        assertVarsel(RV_UT_23, 1.vedtaksperiode.filter(orgnummer = a1))
        inspektør(a2) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertIngenFunksjonelleFeil()
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(0, ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        inspektør(a1) {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertIngenFunksjonelleFeil()
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(0, ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun `Revurdering til ferie på a1 skal ikke påvirke utbetalingen til a2`() {
        nyeVedtak(januar, a1, a2, inntekt = 32000.månedlig)
        assertPeriode(17.januar til 31.januar, a1, 1080.daglig)
        assertPeriode(17.januar til 31.januar, a2, 1080.daglig)

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterOverstyrTidslinje((17.januar til 21.januar).map { ManuellOverskrivingDag(it, Feriedag) }, orgnummer = a1)

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter(orgnummer = a1))
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        this@RevurderTidslinjeFlereArbeidsgivereTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)

        assertPeriode(17.januar til 21.januar, a1, INGEN)
        assertPeriode(22.januar til 31.januar, a1, 1080.daglig)

        assertPeriode(17.januar til 31.januar, a2, 1080.daglig)
    }

    private fun assertDag(dato: LocalDate, orgnummer: String, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt) {
        inspektør(orgnummer).utbetalingstidslinjer(1.vedtaksperiode)[dato].let {
            if (it is NavHelgDag) return
            assertEquals(arbeidsgiverbeløp, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(personbeløp, it.økonomi.inspektør.personbeløp)
        }
    }

    private fun assertPeriode(periode: Periode, orgnummer: String, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt = INGEN) =
        periode.forEach { assertDag(it, orgnummer, arbeidsgiverbeløp, personbeløp) }
}
