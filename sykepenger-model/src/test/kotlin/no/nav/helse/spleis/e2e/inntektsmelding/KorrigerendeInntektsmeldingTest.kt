package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class KorrigerendeInntektsmeldingTest: AbstractEndToEndTest() {

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved helt lik korrigerende inntektsmelding`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertIngenVarsel(RV_IM_4)
    }

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i agp med toggle på`() = Toggle.RevurdereAgpFraIm.enable {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(2.januar til 17.januar))

        assertVarsel(RV_IM_24)
    }

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i agp`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(2.januar til 17.januar))

        assertVarsel(RV_IM_4)
    }

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i refusjonsbeløp`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(2000.månedlig, null, emptyList()))

        assertVarsel(RV_IM_4)
    }

    @Test
    fun `Korrigerende inntektsmelding som strekker agp tilbake skal ha varsel`() = Toggle.RevurdereAgpFraIm.enable {
        nyttVedtak(2.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertEquals(1.januar til 16.januar, inspektør.arbeidsgiverperiode(1.vedtaksperiode))
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

        assertEquals(1.januar til 31.januar, inspektør.vedtaksperioder(1.vedtaksperiode).periode())
        assertEquals("USSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("USSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).toShortString())
    }

    @Test
    fun `Korrigerende inntektsmelding med lik agp skal ikke ha varsel`() = Toggle.RevurdereAgpFraIm.enable {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertEquals(1.januar til 16.januar, inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertIngenVarsel(RV_IM_24, 1.vedtaksperiode.filter())
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
    fun `Korrigerende inntektsmelding som strekker agp fremover`() = Toggle.RevurdereAgpFraIm.enable {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(2.januar til 17.januar))

        assertEquals(2.januar til 17.januar, inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        val utbetalingstidslinje = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje

        assertTrue(utbetalingstidslinje[1.januar] is Utbetalingsdag.Arbeidsdag)
        assertEquals(0.daglig, utbetalingstidslinje[1.januar].økonomi.inspektør.arbeidsgiverbeløp)
        assertTrue(utbetalingstidslinje.subset(2.januar til 17.januar).all {
            it.økonomi.inspektør.arbeidsgiverbeløp == INGEN && it is Utbetalingsdag.ArbeidsgiverperiodeDag
        })
        assertEquals(1431.daglig, utbetalingstidslinje[18.januar].økonomi.inspektør.arbeidsgiverbeløp)

        assertEquals(1.januar til 31.januar, inspektør.vedtaksperioder(1.vedtaksperiode).periode())
        assertEquals("ASSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("ASSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).toShortString())
    }

    @Test
    fun `Antall dager mellom opplyst agp og gammel agp er mer enn 20`()  = Toggle.RevurdereAgpFraIm.enable  {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterInntektsmelding(listOf(1.mars til 16.mars))
        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())

        håndterYtelser(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())
    }

    @Test
    fun `Antall dager mellom opplyst agp og gammel agp er mindre enn 20`()  = Toggle.RevurdereAgpFraIm.enable  {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterInntektsmelding(listOf(1.februar til 16.februar))
        assertEquals("AAAAARR AAAAARR AAAAARR AAAAARR AAASSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(3.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        val overstyringerIgangsatt = observatør.overstyringIgangsatt.map { it.årsak }
        assertEquals(listOf("KORRIGERT_INNTEKTSMELDING_ARBEIDSGIVERPERIODE"), overstyringerIgangsatt)
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Endring i både dager og inntekt`()  = Toggle.RevurdereAgpFraIm.enable  {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(listOf(15.januar til 31.januar), beregnetInntekt = INNTEKT * 1.1)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val overstyringerIgangsatt = observatør.overstyringIgangsatt.map { it.årsak }
        assertEquals(listOf("KORRIGERT_INNTEKTSMELDING_ARBEIDSGIVERPERIODE"), overstyringerIgangsatt)
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertEquals(INNTEKT * 1.1, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.sykepengegrunnlag.inspektør.omregnetÅrsinntekt)
    }

    @Test
    fun `Endring i bare inntekt`()  = Toggle.RevurdereAgpFraIm.enable  {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 1.1)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val overstyringerIgangsatt = observatør.overstyringIgangsatt.map { it.årsak }
        assertEquals(listOf("KORRIGERT_INNTEKTSMELDING_ARBEIDSGIVERPERIODE", "KORRIGERT_INNTEKTSMELDING_INNTEKTSOPPLYSNINGER"), overstyringerIgangsatt)
        assertVarsel(RV_IM_4, 1.vedtaksperiode.filter())
    }
}