package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyPeriode
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
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RevurderTidslinjeFlereArbeidsgivereTest : AbstractDslTest() {

    @Test
    fun `revurdering for periode som start samme dag som en førstegangsvurdering`() {
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(17.januar, 25.januar))
            håndterSøknad(Sykdom(17.januar, 25.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 20000.månedlig)
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
        }

        a1 {
            håndterSykmelding(Sykmeldingsperiode(10.januar, 16.januar))
            håndterSøknad(Sykdom(10.januar, 16.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(17.januar, 25.januar))
            håndterSøknad(Sykdom(17.januar, 25.januar, 100.prosent))
        }

        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
        }

        a1 {
            håndterInntektsmelding(listOf(2.januar til 17.januar), beregnetInntekt = 20000.månedlig)



            assertIngenFunksjonelleFeil()
            assertVarsler(listOf(RV_IM_4), 2.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertVarsler(listOf(Varselkode.RV_VV_2), 2.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `to AG - to perioder på hver - første periode blir revurdert på én AG og avventer godkjenning`() {
        listOf(a2, a1).nyeVedtak(januar)
        listOf(a2, a1).forlengVedtak(februar)
        nullstillTilstandsendringer()

        a2 {
            håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) })
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }

        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
            assertIngenFunksjonelleFeil()
            assertEquals(2, inspektør.avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(2, inspektør.avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
            assertIngenFunksjonelleFeil()
            assertEquals(2, inspektør.avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(2, inspektør.avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun `revurdere en AG når en annen AG er til godkjenning`() {
        a1 {
            nyPeriode(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a2 {
            nyPeriode(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        nullstillTilstandsendringer()

        a1 {
            håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) })
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `to AG - to perioder på hver - én blir revurdert på én AG`() {
        listOf(a2, a1).nyeVedtak(januar)
        listOf(a2, a1).forlengVedtak(februar)
        nullstillTilstandsendringer()

        a1 {
            håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) })
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
        }
        a1 {
            assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertIngenFunksjonelleFeil()
            assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertIngenFunksjonelleFeil()
            assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun `Revurdering til ferie på a1 skal ikke påvirke utbetalingen til a2`() {
        listOf(a1, a2).nyeVedtak(januar, inntekt = 32000.månedlig)
        assertPeriode(17.januar til 31.januar, a1, 1080.daglig)
        assertPeriode(17.januar til 31.januar, a2, 1080.daglig)

        a1 {
            håndterOverstyrTidslinje((17.januar til 21.januar).map { ManuellOverskrivingDag(it, Feriedag) })
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }

        assertPeriode(17.januar til 21.januar, a1, INGEN)
        assertPeriode(22.januar til 31.januar, a1, 1080.daglig)
        assertPeriode(17.januar til 31.januar, a2, 1080.daglig)
    }

    private fun assertDag(dato: LocalDate, orgnummer: String, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt) {
        val vedtaksperiodeId = orgnummer { 1.vedtaksperiode }
        inspektør(orgnummer).utbetalingstidslinjer(vedtaksperiodeId)[dato].let {
            if (it is NavHelgDag) return
            assertEquals(arbeidsgiverbeløp, it.økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(personbeløp, it.økonomi.inspektør.personbeløp)
        }
    }

    private fun assertPeriode(periode: Periode, orgnummer: String, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt = INGEN) =
        periode.forEach { assertDag(it, orgnummer, arbeidsgiverbeløp, personbeløp) }
}
