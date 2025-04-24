package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.utbetalingslinjer.Endringskode.ENDR
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class KorrigerendeInntektsmeldingTest : AbstractEndToEndTest() {

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i agp`() {
        nyttVedtak(januar)
        håndterInntektsmelding(listOf(2.januar til 17.januar))
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
    }

    @Test
    fun `Korrigerende inntektsmelding som strekker agp tilbake skal ha varsel`() {
        nyttVedtak(2.januar til 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        val utbetalingstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje
        assertTrue(utbetalingstidslinje.subset(1.januar til 16.januar).all {
            it.økonomi.inspektør.arbeidsgiverbeløp == INGEN && it is Utbetalingsdag.ArbeidsgiverperiodeDag
        })
        assertEquals(1431.daglig, utbetalingstidslinje[17.januar].økonomi.inspektør.arbeidsgiverbeløp)

        assertEquals(januar, inspektør.vedtaksperioder(1.vedtaksperiode).periode)
        assertEquals("USSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("USSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).toShortString())
    }

    @Test
    fun `Korrigerende inntektsmelding med lik agp skal ikke ha varsel`() {
        nyttVedtak(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        val utbetalingstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje
        assertTrue(utbetalingstidslinje.subset(1.januar til 16.januar).all {
            it.økonomi.inspektør.arbeidsgiverbeløp == INGEN && it is Utbetalingsdag.ArbeidsgiverperiodeDag
        })
        assertEquals(1431.daglig, utbetalingstidslinje[17.januar].økonomi.inspektør.arbeidsgiverbeløp)
    }

    @Test
    fun `Korrigerende inntektsmelding som strekker agp fremover`() {
        nyttVedtak(januar)
        håndterInntektsmelding(listOf(2.januar til 17.januar))
        assertEquals(listOf(2.januar til 17.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertVarsler(listOf(RV_UT_23, RV_IM_24), 1.vedtaksperiode.filter())
        val utbetalingstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje
        assertTrue(utbetalingstidslinje[1.januar] is Utbetalingsdag.Arbeidsdag)
        assertEquals(0.daglig, utbetalingstidslinje[1.januar].økonomi.inspektør.arbeidsgiverbeløp)
        assertTrue(utbetalingstidslinje.subset(2.januar til 17.januar).all {
            it.økonomi.inspektør.arbeidsgiverbeløp == INGEN && it is Utbetalingsdag.ArbeidsgiverperiodeDag
        })
        assertEquals(1431.daglig, utbetalingstidslinje[18.januar].økonomi.inspektør.arbeidsgiverbeløp)

        assertEquals(januar, inspektør.vedtaksperioder(1.vedtaksperiode).periode)
        assertEquals("ASSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("ASSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).toShortString())
    }

    @Test
    fun `Antall dager mellom opplyst agp og gammel agp er mer enn 10`() {
        nyttVedtak(januar)
        forlengVedtak(februar)
        forlengVedtak(mars)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars)
        )
        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        håndterYtelser(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertVarsler(listOf(RV_IM_24), 3.vedtaksperiode.filter())
    }

    @Test
    fun `Antall dager mellom opplyst agp og gammel agp er mindre enn 10`() {
        nyttVedtak(10.januar til 31.januar)
        forlengVedtak(februar)
        forlengVedtak(mars)
        håndterInntektsmelding(listOf(1.februar til 16.februar))

        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())
        assertEquals("AAARR AAAAARR AAAAARR AAASSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        val overstyringerIgangsatt = observatør.overstyringIgangsatt.map { it.årsak }
        assertEquals(listOf("ARBEIDSGIVERPERIODE"), overstyringerIgangsatt)
        assertVarsler(listOf(RV_IM_24, RV_UT_23), 1.vedtaksperiode.filter())
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
    }

    @Test
    fun `her er det et gap mellom første og andre vedtaksperiode og mindre enn 10 dager mellom agps`() {
        nyttVedtak(10.januar til 29.januar)
        nyttVedtak(februar, vedtaksperiodeIdInnhenter = 2.vedtaksperiode, arbeidsgiverperiode = emptyList())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        håndterInntektsmelding(
            listOf(1.februar til 16.februar)
        )

        assertVarsler(listOf(RV_IM_3), 2.vedtaksperiode.filter())
        assertEquals("SSSHH SSSSSHH SSSSSHH S??SSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `her er det et gap mellom første og andre vedtaksperiode og mer enn 10 dager mellom agps, men mindre enn 16 dager mellom periodene`() {
        nyttVedtak(10.januar til 29.januar)
        nyttVedtak(10.februar til 26.februar, arbeidsgiverperiode = emptyList(), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        val inntektsmeldingId = håndterInntektsmelding(
            listOf(10.februar til 25.februar)
        )

        assertEquals("SSSHH SSSSSHH SSSSSHH S?????? ?????HH SSSSSHH SSSSSHH S", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertVarsler(listOf(RV_IM_24), 2.vedtaksperiode.filter())
        assertTrue(inntektsmeldingId in observatør.inntektsmeldingHåndtert.map { it.first })
    }

    @Test
    fun `Antall dager mellom opplyst agp og gammel agp er mindre enn 10 - flere perioder før korrigerte dager`() {
        nyttVedtak(10.januar til 30.januar)
        forlengVedtak(31.januar til 31.januar)
        forlengVedtak(februar)
        håndterInntektsmelding(
            listOf(1.februar til 16.februar)
        )

        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())
        assertVarsel(RV_IM_24, 3.vedtaksperiode.filter())
        assertEquals("AAARR AAAAARR AAAAARR AAASSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(3.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        val overstyringerIgangsatt = observatør.overstyringIgangsatt.map { it.årsak }
        assertEquals(listOf("ARBEIDSGIVERPERIODE"), overstyringerIgangsatt)
        assertVarsler(listOf(RV_IM_24, RV_UT_23), 1.vedtaksperiode.filter())
        assertVarsel(Varselkode.RV_IV_7, 2.vedtaksperiode.filter())

        val revurdering1Vedtaksperiode = inspektør.utbetaling(3)
        revurdering1Vedtaksperiode.also { utbetalingInspektør ->
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertEquals(0, utbetalingInspektør.personOppdrag.size)
            utbetalingInspektør.arbeidsgiverOppdrag.inspektør.also { oppdragInspektør ->
                assertEquals(ENDR, oppdragInspektør.endringskode)
                assertEquals(1, oppdragInspektør.delytelseId(0))
                assertEquals(26.januar, oppdragInspektør.datoStatusFom(0))
            }
        }
        val revurdering2Vedtaksperiode = inspektør.utbetaling(4)
        revurdering2Vedtaksperiode.also { utbetalingInspektør ->
            assertEquals(ENDR, utbetalingInspektør.arbeidsgiverOppdrag.inspektør.endringskode)
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertEquals(0, utbetalingInspektør.personOppdrag.size)
        }
        val revurdering3Vedtaksperiode = inspektør.utbetaling(5)
        revurdering3Vedtaksperiode.also { utbetalingInspektør ->
            assertEquals(2, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertEquals(ENDR, utbetalingInspektør.arbeidsgiverOppdrag.inspektør.endringskode)
            utbetalingInspektør.arbeidsgiverOppdrag[0].inspektør.also { linjeInspektør ->
                assertEquals(1.februar til 28.februar, linjeInspektør.periode)
            }
            utbetalingInspektør.arbeidsgiverOppdrag[1].inspektør.also { linjeInspektør ->
                assertEquals(17.februar til 28.februar, linjeInspektør.periode)
            }
            assertEquals(0, utbetalingInspektør.personOppdrag.size)
        }
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())
        assertVarsel(RV_UT_23, 3.vedtaksperiode.filter())
    }

    @Test
    fun `Endring i både dager og inntekt`() {
        nyttVedtak(januar)
        håndterInntektsmelding(
            listOf(15.januar til 30.januar),
            beregnetInntekt = INNTEKT * 1.1
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        val overstyringerIgangsatt = observatør.overstyringIgangsatt.map { it.årsak }
        assertEquals(listOf("ARBEIDSGIVERPERIODE"), overstyringerIgangsatt)
        assertVarsler(listOf(RV_IM_24, RV_UT_23), 1.vedtaksperiode.filter())
        assertInntektsgrunnlag(15.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT * 1.1)
        }
    }

    @Test
    fun `Endring i bare inntekt`() {
        nyttVedtak(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT * 1.1
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val overstyringerIgangsatt = observatør.overstyringIgangsatt.map { it.årsak }
        assertEquals(listOf("KORRIGERT_INNTEKTSMELDING_INNTEKTSOPPLYSNINGER"), overstyringerIgangsatt)
        assertVarsel(RV_IM_4, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Endring i siste del av agp`() {
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        nyttVedtak(10.januar til 31.januar, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        val agpFør = inspektør.arbeidsgiverperiode(2.vedtaksperiode)
        håndterInntektsmelding(
            listOf(12.januar til 27.januar)
        )
        val agpEtter = inspektør.arbeidsgiverperiode(2.vedtaksperiode)

        assertEquals(listOf(1.januar til 5.januar, 10.januar til 20.januar), agpFør)
        assertEquals(listOf(1.januar til 5.januar, 12.januar til 22.januar), agpEtter)

        assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertVarsler(listOf(RV_IM_3, RV_UT_23, RV_IM_24), 2.vedtaksperiode.filter())
    }

    @Test
    fun `arbeidsgiver korrigerer AGP ved å stykke den opp`() {
        nyttVedtak(januar)
        håndterInntektsmelding(
            listOf(1.januar til 15.januar, 20.januar.somPeriode())
        )
        assertEquals("SSSSSHH SSSSSHH SAAAAHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `arbeidsgiver korrigerer AGP men første og siste dag er lik`() {
        nyttVedtak(januar, arbeidsgiverperiode = listOf(1.januar til 10.januar, 15.januar til 21.januar))
        håndterInntektsmelding(
            listOf(1.januar til 8.januar, 13.januar til 20.januar)
        )
        assertEquals("SSSSSHH SAAAAGG SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `korrigert agp i avventer_godkjenning`()  {
        tilGodkjenning(januar, a1, beregnetInntekt = INNTEKT)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)

        assertVarsler(1.vedtaksperiode.filter(), etter = listOf(RV_IM_24)) {
            håndterInntektsmelding(listOf(5.januar til 20.januar))
        }

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

        assertEquals("AAAASHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `forlengelse til godkjenning - korrigerende agp mindre enn ti dager fra forrige`() {
        nyttVedtak(10.januar til 31.januar)
        nyPeriode(februar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterInntektsmelding(listOf(1.februar til 16.februar))

        assertEquals("AAARR AAAAARR AAAAARR AAASSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `forlengelse til godkjenning - korrigerende agp mer enn ti dager fra forrige`() {
        nyttVedtak(januar)
        nyPeriode(februar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertVarsler(2.vedtaksperiode.filter(), etter = listOf(RV_IM_24)) {
            håndterInntektsmelding(
                listOf(1.februar til 16.februar)
            )
        }

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
    }
}
