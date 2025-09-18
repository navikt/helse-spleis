package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.Grunnbeløp
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.den
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.lørdag
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.BehandlingView.TilstandView.UBEREGNET_OMGJØRING
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_23
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_25
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertSisteForkastetPeriodeTilstand
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertUtbetalingsdag
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.testhelpers.assertInstanceOf
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.til
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NavUtbetalerAgpTest : AbstractEndToEndTest() {

    @Test
    fun `skal aldri foreslå sykedag NAV ved en hullete arbedisgiverperiode og begrunnelse for reduksjon satt`() {
        håndterSøknad(Sykdom(18.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.mai, 14.mai, 100.prosent))
        håndterInntektsmelding(
            listOf(14.april til 14.april, 18.april til 2.mai)
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterSøknad(Sykdom(22.mai, 26.mai, 100.prosent))

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        assertEquals("GR AASSSHH SSSSSHH SSSSSHH SSSSSHH S?????? ?SSSSH", inspektør.sykdomstidslinje.toShortString())

        nullstillTilstandsendringer()
        val inntektsmeldingId = håndterInntektsmelding(
            listOf(14.april til 14.april, 18.april til 2.mai),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "EnBegrunnelse",
        )

        assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        assertVarsler(listOf(RV_IM_8, RV_IM_23, RV_IM_24), 2.vedtaksperiode.filter())
        assertFunksjonellFeil(RV_IM_23, 1.vedtaksperiode.filter())

        assertEquals("GR AASSSHH SSSSSHH SSSSSHH SSSSSHH S?????? ?SSSSH", inspektør.sykdomstidslinje.toShortString())
        assertEquals(listOf(14.april.somPeriode(), 18.april til 30.april), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        assertTrue(observatør.inntektsmeldingHåndtert.any { it.first == inntektsmeldingId })
        assertFalse(observatør.inntektsmeldingIkkeHåndtert.contains(inntektsmeldingId))
    }

    @Test
    fun `begrunnelse satt med første fraværsdag og hullete arbeidsgiverperiode kan behandles`() {
        håndterSøknad(Sykdom(18.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.mai, 14.mai, 100.prosent))
        håndterInntektsmelding(
            listOf(14.april til 14.april, 18.april til 2.mai)
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterSøknad(Sykdom(22.mai, 26.mai, 100.prosent))

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(14.april til 14.april, 18.april til 2.mai),
            førsteFraværsdag = 22.mai,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "EnBegrunnelse"
        )

        assertEquals(listOf(14.april til 14.april, 18.april til 30.april), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertEquals(listOf(1.mai til 2.mai), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
        assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(3.vedtaksperiode).dagerNavOvertarAnsvar)
        assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        assertVarsler(listOf(RV_IM_8, RV_IM_24), 2.vedtaksperiode.filter())
        assertEquals("GR AASSSHH SSSSSHH SSSSSHH SSSSSHH S?????? ?SSSSH", inspektør.sykdomstidslinje.toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `kort periode så gap til neste - korrigert inntektsmelding opplyser om ikke-utbetalt AGP`() {
        nyPeriode(1.januar til 15.januar)
        val im1 = håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyPeriode(20.januar til 30.januar)
        assertEquals(0, observatør.inntektsmeldingHåndtert.size)
        assertEquals(listOf(im1, im1), observatør.inntektsmeldingIkkeHåndtert)
        assertEquals(20.januar til 30.januar, inspektør.periode(2.vedtaksperiode))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = lørdag den 20.januar,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFullStillingsandel"
        )
        assertEquals(listOf(1.januar til 15.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertEquals(listOf(20.januar.somPeriode()), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        assertEquals("SSSSSHH SSSSSHH SU???HH SSSSSHH SS", inspektør.sykdomstidslinje.toShortString())
        assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        assertVarsler(listOf(RV_IM_8), 2.vedtaksperiode.filter())
    }

    @Test
    fun `begrunnelse for reduksjon påvirker ikke tidligere arbeidsgiverperiode når første fraværsdag er opplyst`() {
        nyPeriode(1.januar til 4.januar)
        nyPeriode(5.januar til 10.januar)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.mars,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFullStillingsandel"
        )

        assertEquals("SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `hullete AGP sammen med begrunnelse for reduksjon blir kastet ut foreløpig`() {
        val søknad = MeldingsreferanseId(håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent)))
        val im = MeldingsreferanseId(håndterInntektsmelding(
            listOf(1.januar til 5.januar, 10.januar til 20.januar),
            refusjon = Refusjon(INGEN, null),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "NoeSomUmuligKanVæreIListaViIkkeTillater",
        ))
        assertSisteForkastetPeriodeTilstand(a1, 1.vedtaksperiode, TIL_INFOTRYGD)
        assertEquals(listOf(1.januar til 5.januar, 10.januar til 20.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        assertFunksjonellFeil(RV_IM_23)
        assertEquals(setOf(Dokumentsporing.søknad(søknad), Dokumentsporing.inntektsmeldingDager(im)), inspektør.hendelser(1.vedtaksperiode).toSet())
        assertTrue(observatør.inntektsmeldingHåndtert.isEmpty())
        assertEquals(im.id, observatør.inntektsmeldingIkkeHåndtert.single())
    }

    @Test
    fun `ingen oppgitt agp og første fraværsdag i helg`() {
        nyPeriode(lørdag den 6.januar til 20.januar)
        nullstillTilstandsendringer()
        håndterInntektsmelding(
            emptyList(),
            førsteFraværsdag = lørdag den 6.januar,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "foo"
        )
        assertEquals("HH SSSSSHH SSSSSH", inspektør.sykdomstidslinje.toShortString())
        assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        assertEquals(listOf(6.januar.somPeriode()), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `ingen oppgitt agp og første fraværsdag i helg -- første fraværsdag er siste dag i perioden`() {
        nyPeriode(1.januar til lørdag den 6.januar)
        nullstillTilstandsendringer()
        håndterInntektsmelding(
            emptyList(),
            førsteFraværsdag = lørdag den 6.januar,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "foo"
        )
        assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        assertEquals(listOf(6.januar.somPeriode()), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertEquals(UBEREGNET_OMGJØRING, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().tilstand)
    }

    @Test
    fun `Overstyrer agp til sykedagNav - ingen refusjon`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Refusjon(INGEN, null, emptyList())
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.antallUtbetalinger)
        inspektør.sisteUtbetaling().also { overstyringen ->
            assertEquals(1, overstyringen.personOppdrag.size)
            assertEquals(0, overstyringen.arbeidsgiverOppdrag.size)
            overstyringen.personOppdrag[0].inspektør.also { linje ->
                assertEquals(januar, linje.fom til linje.tom)
                assertEquals(1431, linje.beløp)
            }
        }
    }

    @Test
    fun `im medfører at nav skal utbetale arbeidsgiverperioden`() {
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Refusjon(INGEN, null, emptyList()),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "ArbeidOpphoert",
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        assertVarsel(RV_IM_8, 2.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `Overstyrer sykedagNav tilbake til vanlig agp`() {
        håndterSøknad(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Refusjon(INGEN, null, emptyList()),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening",
        )
        assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        val dagerFør = inspektør.sykdomstidslinje.inspektør.dager
        assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.Sykedag, 100) })
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        val dagerEtter = inspektør.sykdomstidslinje.inspektør.dager
        (1.januar til 16.januar).forEach {
            if (!it.erHelg()) assertTrue(dagerFør.getValue(it).kommerFra("Søknad")) { "$it kommer ikke fra Søknad" }
            assertTrue(dagerEtter.getValue(it).kommerFra(OverstyrTidslinje::class)) { "$it kommer ikke fra OverstyrTidslinje" }
        }

        assertEquals(2, inspektør.antallUtbetalinger)
        inspektør.sisteUtbetaling().also { overstyringen ->
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
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Refusjon(INGEN, null, emptyList())
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.antallUtbetalinger)
        inspektør.sisteUtbetaling().also { overstyringen ->
            assertEquals(1, overstyringen.personOppdrag.size)
            assertEquals(0, overstyringen.arbeidsgiverOppdrag.size)
            overstyringen.personOppdrag[0].inspektør.also { linje ->
                assertEquals(januar, linje.fom til linje.tom)
                assertEquals(1431, linje.beløp)
            }
        }
    }

    @Test
    fun `Overstyrer agp til sykedagNav - refusjon`() {
        håndterSykmelding(januar)
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterOverstyrTidslinje((1.januar til 16.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.SykedagNav, 100) })
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.antallUtbetalinger)
        inspektør.sisteUtbetaling().also { overstyringen ->
            assertEquals(0, overstyringen.personOppdrag.size)
            assertEquals(1, overstyringen.arbeidsgiverOppdrag.size)
            overstyringen.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(januar, linje.fom til linje.tom)
                assertEquals(1431, linje.beløp)
            }
        }
    }

    @Test
    fun `arbeidsgiver ikke utbetalt i arbeidsgiverperiode på grunn av manglende opptjening`() {
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(),
            førsteFraværsdag = 1.januar,
            refusjon = Refusjon(INGEN, null, emptyList()),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening",
        )
        assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        assertEquals(listOf(1.januar.somPeriode()), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertInstanceOf<Sykedag>(inspektør.sykdomshistorikk.sykdomstidslinje()[1.januar])
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val utbetaling = inspektør.utbetaling(0)
        assertTrue(utbetaling.arbeidsgiverOppdrag.isEmpty())
        assertEquals(1, utbetaling.personOppdrag.size)
        assertEquals(1.januar til 1.januar, utbetaling.personOppdrag[0].periode)
    }

    @Test
    fun `arbeidsgiver ikke utbetalt i arbeidsgiverperiode på grunn av ferie eller avspasering`() {
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(
            listOf(),
            førsteFraværsdag = 1.januar,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering",
        )
        assertVarsel(RV_IM_25, 1.vedtaksperiode.filter())
        assertEquals(listOf(1.januar.somPeriode()), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertInstanceOf<Sykedag>(inspektør.sykdomshistorikk.sykdomstidslinje()[1.januar])
    }

    @Test
    fun `kort periode etter ferie uten sykdom`() {
        nyttVedtak(juni)
        håndterSøknad(Sykdom(1.august, 10.august, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(1.juni til 16.juni),
            førsteFraværsdag = 1.august,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering"
        )
        assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertEquals(listOf(1.august.somPeriode()), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertVarsler(listOf(RV_IM_3, RV_IM_25), 2.vedtaksperiode.filter())
    }

    @Test
    fun `kort periode etter ferie uten sykdom med arbeidsgiverperioden spredt litt utover`() {
        nyttVedtak(
            juni, arbeidsgiverperiode = listOf(
            1.juni til 5.juni,
            8.juni til 18.juni
        )
        )
        håndterSøknad(Sykdom(1.august, 10.august, 100.prosent))
        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(1.juni til 5.juni, 8.juni til 18.juni),
            førsteFraværsdag = 1.august,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering"
        )
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertVarsler(listOf(RV_IM_3, RV_IM_25), 2.vedtaksperiode.filter())
    }

    @Test
    fun `Inntektsmelding med begrunnelseForReduksjonEllerIkkeUtbetalt må forkaste alle perioder med samme arbeidsgiverperiode`() {
        håndterSøknad(Sykdom(14.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(8.februar, 11.februar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        håndterInntektsmelding(
            listOf(2.januar til 4.januar, 14.januar til 26.januar)
        )
        assertEquals(2.januar til 20.januar, inspektør.periode(1.vedtaksperiode))
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertEquals("UUUARR AAAAARH SSSSSHH SSSSS?? ??????? ???SSHH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())

        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(2.januar til 4.januar, 14.januar til 26.januar),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFullStillingsandel"
        )
        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, TIL_INFOTRYGD)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)

        assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        assertEquals("Tom tidslinje", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `avviser dager nav skal utbetale i arbeidsgiverperioden om sykdomsgrad er for lav`() {
        nyPeriode(2.januar til 17.januar, orgnummer = a1)
        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            beregnetInntekt = 4000.månedlig,
            orgnummer = a1,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "mjau",
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        assertVarsler(listOf(Varselkode.RV_VV_4, RV_IM_8, RV_VV_2), 1.vedtaksperiode.filter())
        assertEquals(listOf(2.januar til 17.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertUtbetalingsdag(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[2.januar], expectedDagtype = Utbetalingsdag.AvvistDag::class, 11)
    }

    @Test
    fun `eget varsel ved oppgitt begrunnelse FerieEllerAvspasering`() {
        nyttVedtak(juni)

        håndterSøknad(Sykdom(1.august, 31.august, 50.prosent))
        håndterInntektsmelding(
            listOf(1.juni til 16.juni),
            førsteFraværsdag = 1.august,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering"
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertVarsler(listOf(RV_IM_3, RV_IM_25), 2.vedtaksperiode.filter())

        this@NavUtbetalerAgpTest.håndterOverstyrTidslinje((1..31).map {
            ManuellOverskrivingDag(
                it.juli,
                Dagtype.ArbeidIkkeGjenopptattDag
            )
        } + listOf(
            ManuellOverskrivingDag(1.august, Dagtype.Sykedag, 50)
        ))
        this@NavUtbetalerAgpTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertEquals("SHH SSSSSHH SSSSSHH SSSSSHH SSSSSHJ JJJJJJJ JJJJJJJ JJJJJJJ JJJJJJJ JJSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `avviser dager nav skal utbetale i arbeidsgiverperioden om kravet til minsteinntekt ikke er innfridd`() {
        val underHalvG = Grunnbeløp.halvG.beløp(1.januar) - 1000.årlig
        håndterSøknad(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = underHalvG,
            refusjon = Refusjon(INGEN, null),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "HvaSomHelst",
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@NavUtbetalerAgpTest.håndterYtelser(1.vedtaksperiode)
        assertVarsler(listOf(RV_IM_8, RV_SV_1), 1.vedtaksperiode.filter())
        assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        (1.januar til 31.januar).filterNot { it.erHelg() }.forEach {
            assertTrue(inspektør.sykdomstidslinje[it] is Sykedag)
        }
        (januar).filterNot { it.erHelg() }.forEach {
            assertNotNull(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it].erAvvistMed(Begrunnelse.MinimumInntekt))
        }
    }
}
