package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.assertForventetFeil
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
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
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NavUtbetalerAgpTest: AbstractEndToEndTest() {

    @Test
    fun `nav utbetaler en hullete arbeidsgiverperiode`() {
        val søknad = håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent))
        val im = håndterInntektsmelding(listOf(1.januar til 5.januar, 10.januar til 20.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeLoenn", refusjon = Inntektsmelding.Refusjon(INGEN, null))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertEquals(listOf(
            Dokumentsporing.søknad(søknad),
            Dokumentsporing.inntektsmeldingDager(im),
            Dokumentsporing.inntektsmeldingInntekt(im)
        ), inspektør.hendelser(1.vedtaksperiode))
        assertEquals(listOf(
            im to 1.vedtaksperiode.id(ORGNUMMER),
            im to 1.vedtaksperiode.id(ORGNUMMER)
        ), observatør.inntektsmeldingHåndtert)
        assertTrue(observatør.inntektsmeldingIkkeHåndtert.isEmpty())
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
        assertEquals(Dag.Sykedag::class, inspektør.sykdomshistorikk.sykdomstidslinje()[1.januar]::class)
        assertTrue(inspektør.sykdomshistorikk.sykdomstidslinje()[1.januar].kommerFra(Søknad::class))
    }
}