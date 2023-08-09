package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.august
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_23
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.spleis.e2e.assertSisteForkastetPeriodeTilstand
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertUtbetalingsdag
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NavUtbetalerAgpTest: AbstractEndToEndTest() {

    @Test
    fun `skal aldri foreslå sykedag NAV ved en hullete arbedisgiverperiode og begrunnelse for reduksjon satt`() {
        håndterSøknad(Sykdom(18.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.mai, 14.mai, 100.prosent))
        håndterInntektsmelding(listOf(14.april til 14.april, 18.april til 2.mai))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterSøknad(Sykdom(22.mai, 26.mai, 100.prosent))

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        assertEquals("GR AASSSHH SSSSSHH SSSSSHH SSSSSHH S?????? ?SSSSH", inspektør.sykdomstidslinje.toShortString())

        val innteksmeldingId = håndterInntektsmelding(listOf(14.april til 14.april, 18.april til 2.mai), begrunnelseForReduksjonEllerIkkeUtbetalt = "EnBegrunnelse")

        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 3.vedtaksperiode, TIL_INFOTRYGD)
        assertFunksjonellFeil(RV_IM_23, 1.vedtaksperiode.filter())

        assertEquals("GR AASSSHH SSSSSHH SSSSSHH SSSSSHH S", inspektør.sykdomstidslinje.toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

        assertTrue(observatør.inntektsmeldingIkkeHåndtert.contains(innteksmeldingId))
    }

    @Test
    fun `begrunnelse satt med første fraværsdag og hullete arbeidsgiverperiode kan behandles`() {
        håndterSøknad(Sykdom(18.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.mai, 14.mai, 100.prosent))
        håndterInntektsmelding(listOf(14.april til 14.april, 18.april til 2.mai))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterSøknad(Sykdom(22.mai, 26.mai, 100.prosent))

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(14.april til 14.april, 18.april til 2.mai), førsteFraværsdag = 22.mai, begrunnelseForReduksjonEllerIkkeUtbetalt = "EnBegrunnelse")

        assertEquals("GR AASSSHH SSSSSHH SSSSSHH SSSSSHH S?????? ?NSSSH", inspektør.sykdomstidslinje.toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `kort periode så gap til neste - korrigert inntektsmelding opplyser om ikke-utbetalt AGP`() {
        nyPeriode(1.januar til 15.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyPeriode(20.januar til 30.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 20.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFullStillingsandel")
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

        assertEquals("SSSSSHH SSSSSHH SU???HH NSSSSHH SS", inspektør.sykdomstidslinje.toShortString())
        assertIngenVarsler(1.vedtaksperiode.filter())
        assertVarsel(RV_IM_8, 2.vedtaksperiode.filter())
    }

    @Test
    fun `begrunnelse for reduksjon påvirker ikke tidligere arbeidsgiverperiode når første fraværsdag er opplyst`() {
        nyPeriode(1.januar til 4.januar)
        nyPeriode(5.januar til 10.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFullStillingsandel", førsteFraværsdag = 1.mars)

        assertEquals("SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `hullete AGP sammen med begrunnelse for reduksjon blir kastet ut foreløpig`() {
        val søknad = håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent))
        val im = håndterInntektsmelding(listOf(1.januar til 5.januar, 10.januar til 20.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "NoeSomUmuligKanVæreIListaViIkkeTillater", refusjon = Inntektsmelding.Refusjon(INGEN, null))
        assertSisteForkastetPeriodeTilstand(a1, 1.vedtaksperiode, TIL_INFOTRYGD)
        assertFunksjonellFeil(RV_IM_23)
        assertEquals(Dokumentsporing.søknad(søknad), inspektør.hendelser(1.vedtaksperiode).single())
        assertTrue(observatør.inntektsmeldingHåndtert.isEmpty())
        assertEquals(im, observatør.inntektsmeldingIkkeHåndtert.single())
    }

    @Test
    fun `ingen oppgitt agp og første fraværsdag i helg`() {
        nyPeriode(6.januar til 20.januar)
        håndterInntektsmelding(emptyList(), førsteFraværsdag = 6.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "foo") // 6.januar lørdag
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `ingen oppgitt agp og første fraværsdag i helg -- første fraværsdag er siste dag i perioden`() {
        nyPeriode(1.januar til 6.januar)
        håndterInntektsmelding(emptyList(), førsteFraværsdag = 6.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "foo") // 6.januar lørdag
        assertForventetFeil(
            forklaring = "NAV skal utbetale",
            nå = {
                assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            },
            ønsket = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            }
        )
    }

    @Test
    fun `ingen oppgitt agp og første fraværsdag i helg -- første fraværsdag er første og siste dag i perioden`() {
        nyPeriode(6.januar til 6.januar)
        håndterInntektsmelding(emptyList(), førsteFraværsdag = 6.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "foo") // 6.januar lørdag
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `Overstyrer agp til sykedagNav - ingen refusjon`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        assertEquals(2, utbetalinger.size)
        utbetalinger.last().inspektør.also { overstyringen ->
            assertEquals(1, overstyringen.personOppdrag.size)
            assertEquals(0, overstyringen.arbeidsgiverOppdrag.size)
            overstyringen.personOppdrag[0].inspektør.also { linje ->
                assertEquals(1.januar til 31.januar, linje.fom til linje.tom)
                assertEquals(1431, linje.beløp)
            }
        }
    }

    @Test
    fun `im medfører at nav skal utbetale arbeidsgiverperioden`() {
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()), begrunnelseForReduksjonEllerIkkeUtbetalt = "ArbeidOpphoert")
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        assertVarsel(RV_IM_8, 2.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `Overstyrer sykedagNav tilbake til vanlig agp`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()), begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        val dagerFør = inspektør.sykdomstidslinje.inspektør.dager

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.Sykedag, 100) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val dagerEtter = inspektør.sykdomstidslinje.inspektør.dager
        (1.januar til 16.januar).forEach {
            if (!it.erHelg()) assertTrue(dagerFør.getValue(it).kommerFra(Inntektsmelding::class)) { "$it kommer ikke fra Inntektsmelding" }
            assertTrue(dagerEtter.getValue(it).kommerFra(OverstyrTidslinje::class)) { "$it kommer ikke fra OverstyrTidslinje" }
        }

        val utbetalinger = inspektør.utbetalinger
        assertEquals(2, utbetalinger.size)
        utbetalinger.last().inspektør.also { overstyringen ->
            assertEquals(1, overstyringen.personOppdrag.size)
            assertEquals(0, overstyringen.arbeidsgiverOppdrag.size)
            overstyringen.personOppdrag[0].inspektør.also { linje ->
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                assertEquals(1431, linje.beløp)
            }
        }
    }

    @Test
    fun `Overstyrer egenmeldingsdager til SykedagNav`() {
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        assertEquals(2, utbetalinger.size)
        utbetalinger.last().inspektør.also { overstyringen ->
            assertEquals(1, overstyringen.personOppdrag.size)
            assertEquals(0, overstyringen.arbeidsgiverOppdrag.size)
            overstyringen.personOppdrag[0].inspektør.also { linje ->
                assertEquals(1.januar til 31.januar, linje.fom til linje.tom)
                assertEquals(1431, linje.beløp)
            }
        }
    }

    @Test
    fun `Overstyrer agp til sykedagNav - refusjon`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val utbetalinger = inspektør.utbetalinger
        assertEquals(2, utbetalinger.size)
        utbetalinger.last().inspektør.also { overstyringen ->
            assertEquals(0, overstyringen.personOppdrag.size)
            assertEquals(1, overstyringen.arbeidsgiverOppdrag.size)
            overstyringen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(1.januar til 31.januar, linje.fom til linje.tom)
                assertEquals(1431, linje.beløp)
            }
        }
    }

    @Test
    fun `arbeidsgiver ikke utbetalt i arbeidsgiverperiode på grunn av manglende opptjening`() {
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(listOf(), førsteFraværsdag = 1.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening", refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()))
        assertEquals(Dag.SykedagNav::class, inspektør.sykdomshistorikk.sykdomstidslinje()[1.januar]::class)
        assertTrue(inspektør.sykdomshistorikk.sykdomstidslinje()[1.januar].kommerFra(Inntektsmelding::class))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val utbetaling = inspektør.utbetalinger.single().inspektør
        assertTrue(utbetaling.arbeidsgiverOppdrag.isEmpty())
        assertEquals(1, utbetaling.personOppdrag.size)
        assertEquals(1.januar til 1.januar, utbetaling.personOppdrag[0].periode)
    }

    @Test
    fun `arbeidsgiver ikke utbetalt i arbeidsgiverperiode på grunn av ferie eller avspasering`() {
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(listOf(), førsteFraværsdag = 1.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering")
        assertEquals(Dag.SykedagNav::class, inspektør.sykdomshistorikk.sykdomstidslinje()[1.januar]::class)
        assertTrue(inspektør.sykdomshistorikk.sykdomstidslinje()[1.januar].kommerFra(Inntektsmelding::class))
    }

    @Test
    fun `kort periode etter ferie uten sykdom`() {
        nyttVedtak(1.juni, 30.juni)
        håndterSøknad(Sykdom(1.august, 10.august, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.juni til 16.juni), førsteFraværsdag = 1.august, begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering")
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertIngenVarsler(1.vedtaksperiode.filter())
        assertVarsel(RV_IM_3, 2.vedtaksperiode.filter())
        assertVarsel(RV_IM_8, 2.vedtaksperiode.filter())
    }

    @Test
    fun `kort periode etter ferie uten sykdom med arbeidsgiverperioden spredt litt utover`() {
        nyttVedtak(1.juni, 30.juni, arbeidsgiverperiode = listOf(
            1.juni til 5.juni,
            8.juni til 18.juni
        ))
        håndterSøknad(Sykdom(1.august, 10.august, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.juni til 5.juni, 8.juni til 18.juni), førsteFraværsdag = 1.august, begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering")
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertIngenVarsler(1.vedtaksperiode.filter())
        assertVarsel(RV_IM_3, 2.vedtaksperiode.filter())
        assertVarsel(RV_IM_8, 2.vedtaksperiode.filter())
    }

    @Test
    fun `Inntektsmelding med begrunnelseForReduksjonEllerIkkeUtbetalt må forkaste alle perioder med samme arbeidsgiverperiode`() {
        håndterSøknad(Sykdom(14.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(8.februar, 11.februar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        håndterInntektsmelding(listOf(2.januar til 4.januar, 14.januar til 26.januar))
        assertEquals(2.januar til 20.januar, inspektør.periode(1.vedtaksperiode))
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertEquals("UUUARR AAAAARH SSSSSHH SSSSS?? ??????? ???SSHH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())

        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(2.januar til 4.januar, 14.januar til 26.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFullStillingsandel")
        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)

        assertEquals("Tom tidslinje", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `avviser dager nav skal utbetale i arbeidsgiverperioden om sykdomsgrad er for lav`() {
        nyPeriode(2.januar til 17.januar, orgnummer = a1)
        håndterInntektsmelding(listOf(2.januar til 17.januar),  beregnetInntekt = 4000.månedlig, orgnummer = a1, begrunnelseForReduksjonEllerIkkeUtbetalt = "mjau")
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 4000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 4000.månedlig.repeat(3)),
                    grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertUtbetalingsdag(inspektør.sisteUtbetalingUtbetalingstidslinje()[2.januar], expectedDagtype = Utbetalingsdag.AvvistDag::class, 11)
    }
}