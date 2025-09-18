package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import kotlin.reflect.KClass
import no.nav.helse.august
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Melding
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.AndreYtelser
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.ArbeidsgiverperiodeDag
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.NavDag
import no.nav.helse.person.PersonObserver.Utbetalingsdag.Dagtype.NavHelgDag
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterOverstyringSykedag
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellArbeidsgiverdag
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.manuellForeldrepengedag
import no.nav.helse.spleis.e2e.manuellPermisjonsdag
import no.nav.helse.spleis.e2e.manuellSykedag
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyrTidslinjeTest : AbstractEndToEndTest() {

    @Test
    fun `Overstyring av hele perioden til andre ytelser`() {
        tilGodkjenning(januar, a1)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(januar.map { ManuellOverskrivingDag(it, Dagtype.Pleiepengerdag) })
        // Vet ikke om det er ønskelig, men er sånn det er da
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `sendes ikke ut overstyring igangsatt når det kommer inntektsmelding i avventer inntektsmelding`() {
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        assertEquals(0, observatør.overstyringIgangsatt.size)
    }

    @Test
    fun `Senere perioder inngår ikke i overstyring igangsatt selv om det er en endring fra saksbehandler`() {
        tilGodkjenning(januar, a1)
        håndterSøknad(mars)
        this@OverstyrTidslinjeTest.håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT / 2)))
        val overstyringIgangsatt = observatør.overstyringIgangsatt.single()
        assertEquals(listOf(1.vedtaksperiode.id(a1)), overstyringIgangsatt.berørtePerioder.map { it.vedtaksperiodeId })
    }

    @Test
    fun `overstyring av tidslinje i avventer inntektsmelding`() {
        håndterSøknad(Sykdom(3.februar, 26.februar, 100.prosent))
        håndterSøknad(Sykdom(27.februar, 12.mars, 100.prosent))
        håndterSøknad(Sykdom(13.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(
            listOf(6.mars til 21.mars)
        )
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(2.februar til 17.februar)
        )
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE)
        assertTilstander(3.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)

        nullstillTilstandsendringer()

        assertEquals("UGG UUUUUGG UUUUUGR AAAAARR AAAAARR ASSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomstidslinje.toShortString())
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje((20.februar til 24.februar).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, 100) })
        assertEquals("UGG UUUUUGG UUUUUGR ASSSSHR AAAAARR ASSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomstidslinje.toShortString())
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje((18.februar til 19.februar).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, 100) })
        assertEquals("UGG UUUUUGG UUUUUGH SSSSSHR AAAAARR ASSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomstidslinje.toShortString())

        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `overstyring av tidslinje som flytter skjæringstidspunkt blant annet - da gjenbruker vi tidsnære opplysninger`() {
        nyPeriode(1.januar til 10.januar)
        nyPeriode(15.januar til 20.januar)
        håndterInntektsmelding(
            listOf(15.januar til 30.januar)
        )

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        nyPeriode(21.januar til 10.mars)
        assertVarsel(RV_IM_3, 3.vedtaksperiode.filter())
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag),
                ManuellOverskrivingDag(2.januar, Dagtype.Arbeidsdag)
            )
        )
        this@OverstyrTidslinjeTest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar)
        )
        assertVarsel(RV_IM_3, 2.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)

        nyPeriode(11.mars til 20.mars)
        assertSisteTilstand(4.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        this@OverstyrTidslinjeTest.håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        this@OverstyrTidslinjeTest.håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterUtbetalingsgodkjenning(4.vedtaksperiode)
        håndterUtbetalt()

        assertVarsler(listOf(RV_IM_3), 3.vedtaksperiode.filter())
    }

    @Test
    fun `arbeidsgiver endrer arbeidsgiverperioden tilbake - må overstyre tidslinje for å fikse`() {
        håndterSøknad(Sykdom(1.februar, 10.februar, 100.prosent))
        nyttVedtak(11.februar til 28.februar, arbeidsgiverperiode = listOf(1.februar til 4.februar, 7.februar til 18.februar), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        assertEquals(Dag.Arbeidsdag::class, inspektør.sykdomstidslinje[5.februar]::class)
        assertEquals(Dag.Arbeidsdag::class, inspektør.sykdomstidslinje[6.februar]::class)
        nullstillTilstandsendringer()

        observatør.vedtaksperiodeVenter.clear()
        håndterInntektsmelding(listOf(16.januar til 31.januar))

        assertEquals(listOf(7.februar, 16.januar), inspektør.skjæringstidspunkter(1.vedtaksperiode))
        assertVarsel(Varselkode.RV_IV_11, 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)

        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(5.februar, Dagtype.Sykedag, 100),
                ManuellOverskrivingDag(6.februar, Dagtype.Sykedag, 100),
            )
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(Dag.Sykedag::class, inspektør.sykdomstidslinje[5.februar]::class)
        assertEquals(Dag.Sykedag::class, inspektør.sykdomstidslinje[6.februar]::class)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
    }

    @Test
    fun `overstyre ferie til sykdom`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(17.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(
            (17.januar til 31.januar).map { dagen -> ManuellOverskrivingDag(dagen, Dagtype.Sykedag, 100) }
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
            assertInntektsgrunnlag(a1, INNTEKT)
        }
    }

    @Test
    fun `saksbehandler endrer dager nav overtar ansvar for`() {
        nyttVedtak(januar)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje((1.januar til 16.januar).map { ManuellOverskrivingDag(it, Dagtype.SykedagNav, 100) })
        assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
    }

    @Test
    fun `ferie uten sykmelding mellom to perioder`() {
        nyttVedtak(juni)

        håndterSøknad(Sykdom(1.august, 31.august, 100.prosent))
        håndterInntektsmelding(
            listOf(1.juni til 16.juni),
            førsteFraværsdag = 1.august,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "FerieEllerAvspasering"
        )
        assertVarsler(listOf(RV_IM_3, Varselkode.RV_IM_25), 2.vedtaksperiode.filter())
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        nullstillTilstandsendringer()

        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje((juli).map { ManuellOverskrivingDag(it, Dagtype.ArbeidIkkeGjenopptattDag) })
        this@OverstyrTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        assertEquals(listOf(1.august.somPeriode()), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
        assertEquals("SHH SSSSSHH SSSSSHH SSSSSHH SSSSSHJ JJJJJJJ JJJJJJJ JJJJJJJ JJJJJJJ JJSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSS", inspektør.sykdomstidslinje.toShortString())

        assertEquals(1.august, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(1.juli til 31.august, inspektør.periode(2.vedtaksperiode))

        val juniutbetaling = inspektør.utbetaling(0)
        val augustutbetalingFør = inspektør.utbetaling(1)
        val augustutbetalingEtter = inspektør.utbetaling(2)

        assertNotEquals(juniutbetaling.korrelasjonsId, augustutbetalingFør.korrelasjonsId)
        assertNotEquals(juniutbetaling.korrelasjonsId, augustutbetalingEtter.korrelasjonsId)

        augustutbetalingEtter.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(1, oppdrag.size)
            oppdrag[0].also { linje ->
                assertEquals(august, linje.fom til linje.tom)
            }
        }
    }

    @Test
    fun `vedtaksperiode strekker seg tilbake og endrer ikke skjæringstidspunktet`() {
        tilGodkjenning(10.januar til 31.januar, a1)
        nullstillTilstandsendringer()
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(9.januar, Dagtype.Arbeidsdag)
            ), orgnummer = a1
        )

        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Arbeidsdag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))

        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertEquals(9.januar til 31.januar, inspektør.periode(1.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `strekker ikke inn i forrige periode`() {
        nyPeriode(1.januar til 9.januar, a1)
        tilGodkjenning(10.januar til 31.januar, a1, vedtaksperiodeIdInnhenter = 2.vedtaksperiode) // 1. jan - 9. jan blir omgjort til arbeidsdager ved innsending av IM her
        nullstillTilstandsendringer()
        // Saksbehandler korrigerer; 9.januar var vedkommende syk likevel
        assertEquals(4, inspektør.sykdomshistorikk.elementer())
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(9.januar, Dagtype.Arbeidsdag)
            ), orgnummer = a1
        )
        assertEquals(5, inspektør.sykdomshistorikk.elementer())
        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Arbeidsdag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))

        this@OverstyrTidslinjeTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `vedtaksperiode strekker seg ikke tilbake hvis det er en periode foran`() {
        nyPeriode(1.januar til 9.januar, a1)
        nyPeriode(10.januar til 31.januar, a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        nullstillTilstandsendringer()
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)
            ), orgnummer = a1
        )
        this@OverstyrTidslinjeTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        val dagen = inspektør.sykdomstidslinje[9.januar]
        assertEquals(Dag.Sykedag::class, dagen::class)
        assertTrue(dagen.kommerFra(OverstyrTidslinje::class))

        assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `overstyr tidslinje endrer to perioder samtidig`() {
        nyPeriode(1.januar til 9.januar, a1)
        nyPeriode(10.januar til 31.januar, a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        nullstillTilstandsendringer()
        assertEquals(4, inspektør.sykdomshistorikk.elementer())
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(
            listOf(
                ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100),
                ManuellOverskrivingDag(10.januar, Dagtype.Feriedag)
            ), orgnummer = a1
        )
        assertEquals(6, inspektør.sykdomshistorikk.elementer())
        this@OverstyrTidslinjeTest.håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertSykdomstidslinjedag(9.januar, Dag.Sykedag::class, OverstyrTidslinje::class)
        assertSykdomstidslinjedag(10.januar, Dag.Feriedag::class, OverstyrTidslinje::class)

        assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `kan ikke utbetale overstyrt utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterInntektsmelding(
            listOf(Periode(2.januar, 17.januar)),
            førsteFraværsdag = 2.januar
        )
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(listOf(manuellSykedag(2.januar), manuellArbeidsgiverdag(24.januar), manuellFeriedag(25.januar)))
        this@OverstyrTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        assertEquals(Utbetalingstatus.FORKASTET, inspektør.utbetalingtilstand(0))
    }

    @Test
    fun `grad over grensen overstyres på enkeltdag`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(
            listOf(Periode(2.januar, 17.januar)),
            førsteFraværsdag = 2.januar
        )
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(listOf(manuellSykedag(22.januar, 30)))

        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(3, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.size)
        assertEquals(21.januar, inspektør.sisteUtbetaling().arbeidsgiverOppdrag[0].tom)
        assertEquals(30, inspektør.sisteUtbetaling().arbeidsgiverOppdrag[1].grad)
        assertEquals(23.januar, inspektør.sisteUtbetaling().arbeidsgiverOppdrag[2].fom)
    }

    @Test
    fun `grad under grensen blir ikke utbetalt etter overstyring av grad`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(
            listOf(Periode(2.januar, 17.januar)),
            førsteFraværsdag = 2.januar
        )
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(listOf(manuellSykedag(22.januar, 0)))

        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
        assertEquals(2, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.size)
        assertEquals(21.januar, inspektør.sisteUtbetaling().arbeidsgiverOppdrag[0].tom)
        assertEquals(23.januar, inspektør.sisteUtbetaling().arbeidsgiverOppdrag[1].fom)
    }

    @Test
    fun `overstyrt til fridager i midten av en periode blir ikke utbetalt`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 25.januar))
        håndterInntektsmelding(
            listOf(Periode(2.januar, 17.januar)),
            førsteFraværsdag = 2.januar
        )
        håndterSøknad(Sykdom(2.januar, 25.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje(listOf(manuellFeriedag(22.januar), manuellPermisjonsdag(23.januar)))

        assertNotEquals(AVVENTER_GODKJENNING, inspektør.sisteTilstand(1.vedtaksperiode))

        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(2, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.size)
        assertEquals(21.januar, inspektør.sisteUtbetaling().arbeidsgiverOppdrag[0].tom)
        assertEquals(24.januar, inspektør.sisteUtbetaling().arbeidsgiverOppdrag[1].fom)
    }

    @Test
    fun `Overstyring av sykHelgDag`() {
        håndterSykmelding(januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar)
        )
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(20.januar, 21.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyringSykedag(20.januar til 21.januar)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNNNHH NNN", inspektør.utbetalingstidslinjer(1.vedtaksperiode).toString())
    }

    @Test
    fun `overstyrer fra SykedagNav til Sykedag`() {
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "Saerregler",
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))

        val førsteUtbetalingsdag = inspektør.utbetaling(0).arbeidsgiverOppdrag[0].fom
        assertEquals(1.januar, førsteUtbetalingsdag)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje((1.januar til 16.januar).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, 100) })

        assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
        assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))

        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        val førsteUtbetalingsdagEtterOverstyring = inspektør.utbetaling(1).arbeidsgiverOppdrag[0].fom
        assertEquals(17.januar, førsteUtbetalingsdagEtterOverstyring)
        assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
    }

    @Test
    fun `overstyring av andre ytelser i halen`() {
        nyttVedtak(januar)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje((20.januar til 31.januar).map { manuellForeldrepengedag(it) })
        assertEquals("SSSSSHH SSSSSHH SSSSSYY YYYYYYY YYY", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør
        assertEquals(12, utbetalingstidslinje.avvistDagTeller)
    }

    @Test
    fun `overstyring av egenmeldingsdager til arbeidsdager`()  {
        nyttVedtak(januar)
        håndterSøknad(Sykdom(10.februar, 28.februar, 100.prosent), Arbeid(20.februar, 28.februar))
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(10.februar til 26.februar),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertEquals("HH SSSSSHH SUUUUGG UAA", inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).toShortString())
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje((20..26).map { ManuellOverskrivingDag(it.februar, Dagtype.Arbeidsdag) })
        this@OverstyrTidslinjeTest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertEquals("HH SSSSSHH SAAAARR AAA", inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).toShortString())
        this@OverstyrTidslinjeTest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
    }

    @Test
    fun `overstyring av andre ytelser i snuten`() {
        nyttVedtak(januar)
        this@OverstyrTidslinjeTest.håndterOverstyrTidslinje((1.januar til 10.januar).map { manuellForeldrepengedag(it) })
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterYtelser(1.vedtaksperiode)
        assertVarsler(listOf(Varselkode.RV_IV_7, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@OverstyrTidslinjeTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val forventetUtbetaling =
            (1.januar til 16.januar).associateWith { ArbeidsgiverperiodeDag } +
                (17.januar til 19.januar).associateWith { NavDag } +
                (20.januar til 21.januar).associateWith { NavHelgDag } +
                (22.januar til 26.januar).associateWith { NavDag } +
                (27.januar til 28.januar).associateWith { NavHelgDag } +
                (29.januar til 31.januar).associateWith { NavDag }

        assertEquals(forventetUtbetaling, observatør.utbetalingMedUtbetalingEventer.first().dager)

        val forventetRevurdering =
            (1.januar til 10.januar).associateWith { AndreYtelser } +
                (11.januar til 26.januar).associateWith { ArbeidsgiverperiodeDag } +
                (27.januar til 28.januar).associateWith { NavHelgDag } +
                (29.januar til 31.januar).associateWith { NavDag }

        assertEquals(forventetRevurdering, observatør.utbetalingMedUtbetalingEventer.last().dager)
    }

    private fun assertSykdomstidslinjedag(dato: LocalDate, dagtype: KClass<out Dag>, kommerFra: Melding) {
        assertSykdomstidslinjedag(dato, dagtype, kommerFra.simpleName!!)
    }

    private fun assertSykdomstidslinjedag(dato: LocalDate, dagtype: KClass<out Dag>, kommerFra: String) {
        val dagen = inspektør.sykdomstidslinje[dato]
        assertEquals(dagtype, dagen::class)
        assertTrue(dagen.kommerFra(kommerFra))
    }

    private val PersonObserver.UtbetalingUtbetaltEvent.dager get() = utbetalingsdager.associateBy { it.dato }.mapValues { it.value.type }
}
