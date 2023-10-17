package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_2
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
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
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class KorrigerendeInntektsmeldingTest: AbstractEndToEndTest() {

    @Test
    fun `Kaster ut periode til godkjenning som får korrigerende IM med avvik`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 2)
        assertForventetFeil(
            forklaring = "Kastes ikke ut",
            nå = {
                assertVarsel(RV_IV_2, 1.vedtaksperiode.filter())
                assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
            },
            ønsket = {
                assertFunksjonellFeil(RV_IV_2, 1.vedtaksperiode.filter())
                assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, TIL_INFOTRYGD)
            }
        )
    }

    @Test
    fun `Feil periode får melding om avvik, og burde kastes ut fremfor å bli varsel`() {
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 2)

        assertForventetFeil(
            forklaring = "Feil periode får varsel, og ikke bare det, den burde jo vært error og alt blitt forkastet 😪",
            nå = {
                assertVarsel(RV_IV_2, 3.vedtaksperiode.filter())
                assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
                assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
                assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            },
            ønsket = {
                assertFunksjonellFeil(RV_IV_2, 2.vedtaksperiode.filter())
                assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
                assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
                assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
            }
        )
    }

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved helt lik korrigerende inntektsmelding`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar),)

        assertIngenVarsel(RV_IM_4)
    }

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i agp`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(2.januar til 17.januar),)

        assertVarsel(RV_IM_24)
    }

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i refusjonsbeløp`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(2000.månedlig, null, emptyList()),
        )

        assertVarsel(RV_IM_4)
    }

    @Test
    fun `bare varsel på første periode`() {
        nyPeriode(1.januar til 16.januar)
        nyttVedtak(17.januar, 31.januar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterInntektsmelding(listOf(1.januar til 16.januar),)

        assertIngenVarsel(RV_IM_4, 1.vedtaksperiode.filter())
        assertVarsel(RV_IM_4, 2.vedtaksperiode.filter())
        assertIngenVarsel(RV_IM_4, 3.vedtaksperiode.filter())
        assertIngenVarsel(RV_IM_4, 4.vedtaksperiode.filter())
    }

    @Test
    fun `Korrigerende inntektsmelding som strekker agp tilbake skal ha varsel`() {
        nyttVedtak(2.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar),)

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
    fun `Korrigerende inntektsmelding med lik agp skal ikke ha varsel`() {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar),)
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
    fun `Korrigerende inntektsmelding som strekker agp fremover`() {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(listOf(2.januar til 17.januar),)
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
    fun `Antall dager mellom opplyst agp og gammel agp er mer enn 10`()  {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterInntektsmelding(listOf(1.mars til 16.mars),)
        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        håndterYtelser(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()
        assertVarsel(RV_IM_24, 3.vedtaksperiode.filter())
    }

    @Test
    fun `Antall dager mellom opplyst agp og gammel agp er mindre enn 10`()  {
        nyttVedtak(10.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterInntektsmelding(listOf(1.februar til 16.februar),)
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
        assertEquals(listOf("KORRIGERT_INNTEKTSMELDING_ARBEIDSGIVERPERIODE", "KORRIGERT_INNTEKTSMELDING_ARBEIDSGIVERPERIODE"), overstyringerIgangsatt)
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `her er det et gap mellom første og andre vedtaksperiode og mindre enn 10 dager mellom agps`() {
        nyttVedtak(10.januar, 29.januar)
        nyttVedtak(1.februar, 28.februar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        håndterInntektsmelding(listOf(1.februar til 16.februar),)
        assertEquals("SSSHH SSSSSHH SSSSSHH S??SSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `Antall dager mellom opplyst agp og gammel agp er mindre enn 10 - flere perioder før korrigerte dager`() {
        nyttVedtak(10.januar, 30.januar)
        forlengVedtak(31.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterInntektsmelding(listOf(1.februar til 16.februar),)
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
        assertEquals(listOf("KORRIGERT_INNTEKTSMELDING_ARBEIDSGIVERPERIODE", "KORRIGERT_INNTEKTSMELDING_ARBEIDSGIVERPERIODE", "KORRIGERT_INNTEKTSMELDING_ARBEIDSGIVERPERIODE"), overstyringerIgangsatt)
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Endring i både dager og inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(listOf(15.januar til 31.januar), beregnetInntekt = INNTEKT * 1.1,)
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
    fun `Endring i bare inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 1.1,)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val overstyringerIgangsatt = observatør.overstyringIgangsatt.map { it.årsak }
        assertEquals(listOf("KORRIGERT_INNTEKTSMELDING_ARBEIDSGIVERPERIODE", "KORRIGERT_INNTEKTSMELDING_INNTEKTSOPPLYSNINGER"), overstyringerIgangsatt)
        assertVarsel(RV_IM_4, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Endring i siste del av agp`() {
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        nyttVedtak(10.januar, 31.januar)
        val agpFør = inspektør.arbeidsgiver.arbeidsgiverperiode(10.januar til 31.januar)
        håndterInntektsmelding(listOf(12.januar til 28.januar), avsendersystem = Inntektsmelding.Avsendersystem.ALTINN,)
        val agpEtter = inspektør.arbeidsgiver.arbeidsgiverperiode(10.januar til 31.januar)

        assertEquals(Arbeidsgiverperiode(listOf(1.januar til 5.januar, 10.januar til 26.januar)), agpFør)
        assertEquals(Arbeidsgiverperiode(listOf(1.januar til 5.januar, 12.januar til 28.januar)), agpEtter)

        assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertVarsel(RV_IM_3, 2.vedtaksperiode.filter())
    }

    @Test
    fun `arbeidsgiver korrigerer AGP ved å stykke den opp`() {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(listOf(1.januar til 15.januar, 20.januar.somPeriode()),)
        assertEquals("SSSSSHH SSSSSHH SAAAAHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `arbeidsgiver korrigerer AGP men første og siste dag er lik`() {
        nyttVedtak(1.januar, 31.januar, arbeidsgiverperiode = listOf(1.januar til 10.januar, 15.januar til 21.januar))
        håndterInntektsmelding(listOf(1.januar til 8.januar, 13.januar til 21.januar),)
        assertEquals("SSSSSHH SAAAAGG SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `korrigert agp i avventer_godkjenning`() {
        tilGodkjenning(1.januar, 31.januar, a1, beregnetInntekt = INNTEKT)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        håndterInntektsmelding(listOf(5.januar til 20.januar),)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
        assertEquals("AAAASHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `forlengelse til godkjenning - korrigerende agp mindre enn ti dager fra forrige`() {
        nyttVedtak(10.januar, 31.januar)
        nyPeriode(1.februar til 28.februar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterInntektsmelding(listOf(1.februar til 16.februar),)

        assertEquals("AAARR AAAAARR AAAAARR AAASSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertVarsel(RV_IM_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `forlengelse til godkjenning - korrigerende agp mer enn ti dager fra forrige`() {
        nyttVedtak(1.januar, 31.januar)
        nyPeriode(1.februar til 28.februar)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterInntektsmelding(listOf(1.februar til 16.februar),)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        assertVarsel(RV_IM_24, 2.vedtaksperiode.filter())
    }
}