package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.arbeidsgiveropplysninger.TrengerArbeidsgiveropplysningerTest.Companion.assertEtterspurt
import no.nav.helse.sykdomstidslinje.Dag.Feriedag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import no.nav.helse.økonomi.inspectors.inspektør

internal class RevurderKorrigertSøknadFlereArbeidsgivereTest : AbstractDslTest() {

    @Test
    fun `Korrigerende søknad hos en arbeidsgiver - setter i gang revurdering`() {
        listOf(a1, a2).nyeVedtak(januar)
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 50.prosent),
                Ferie(30.januar, 31.januar)
            )
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertEquals(21, inspektør.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(2, inspektør.sykdomstidslinje.inspektør.dagteller[Feriedag::class])
            assertEquals(8, inspektør.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar] is NavDag)
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[18.januar] is NavDag)
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `Overlappende søknad som strekker seg forbi vedtaksperioden`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
        }
        a2 {
            håndterSøknad(Sykdom(15.januar, 15.februar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(15.januar, 15.februar))
            håndterInntektsmelding(listOf(15.januar til 30.januar), beregnetInntekt = 20000.månedlig)
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 20000.månedlig)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        nullstillTilstandsendringer()
        a1 {
            håndterSykmelding(Sykmeldingsperiode(15.januar, 15.februar))
            håndterSøknad(Sykdom(15.januar, 15.februar, 50.prosent))
            håndterYtelser(1.vedtaksperiode)

            assertVarsler(listOf(Varselkode.RV_SØ_13, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode))
            assertEquals(15.januar til 15.februar, inspektør.periode(2.vedtaksperiode))
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            (17.januar til 31.januar).forEach {
                assertEquals(50.prosent, utbetalingstidslinje[it].økonomi.inspektør.grad)
            }
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `Vedtaksperiodene til a1 og a2 er kant-i-kant og det kommer en korrigerende søknad for a1 - setter i gang revurdering`() {
        val periodeAg1 = januar
        val periodeAg2 = februar
        a1 {
            håndterSykmelding(Sykmeldingsperiode(periodeAg1.start, periodeAg1.endInclusive))
            håndterSøknad(Sykdom(periodeAg1.start, periodeAg1.endInclusive, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(periodeAg2.start, periodeAg2.endInclusive))
            håndterSøknad(Sykdom(periodeAg2.start, periodeAg2.endInclusive, 100.prosent))
            håndterInntektsmelding(listOf(periodeAg2.start til periodeAg2.start.plusDays(15)))
        }
        a1 {
            håndterInntektsmelding(listOf(periodeAg1.start til periodeAg1.start.plusDays(15)))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertVarsler(listOf(Varselkode.RV_VV_2), 1.vedtaksperiode.filter())
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            (17..31).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }
        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVSLUTTET)

        }
    }

    @Test
    fun `To arbeidsgivere med ett sykefraværstilfelle og gap over 16 dager på den ene arbeidsgiveren - korrigerende søknad på periode før gap setter i gang revurdering`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(25.januar, 25.februar))
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent))
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterSykmelding(Sykmeldingsperiode(24.februar, 24.mars))
            håndterSøknad(Sykdom(24.februar, 24.mars, 100.prosent))
            observatør.assertEtterspurt(2.vedtaksperiode, PersonObserver.Refusjon::class, PersonObserver.Arbeidsgiverperiode::class)
            håndterInntektsmelding(listOf(24.februar til 11.mars), beregnetInntekt = INNTEKT)
        }
        a2 {
            håndterInntektsmelding(listOf(25.januar til 9.februar), beregnetInntekt = INNTEKT)
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
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(18.januar, 19.januar))
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertEquals(
                21,
                inspektør.sykdomstidslinje.subset(januar).inspektør.dagteller[Sykedag::class]
            )
            assertEquals(
                2,
                inspektør.sykdomstidslinje.subset(januar).inspektør.dagteller[Feriedag::class]
            )
            assertEquals(
                8,
                inspektør.sykdomstidslinje.subset(januar).inspektør.dagteller[SykHelgedag::class]
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a1 {
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }

        a1 {
            assertEquals(4, inspektør.antallUtbetalinger)

            inspektør.utbetaling(0).also { januarutbetaling ->
                val revurdering = inspektør.utbetaling(2)
                assertEquals(januarutbetaling.korrelasjonsId, revurdering.korrelasjonsId)
                assertEquals(januar, januarutbetaling.periode)
                assertEquals(januar, revurdering.periode)
                assertEquals(0, revurdering.personOppdrag.size)
                revurdering.arbeidsgiverOppdrag.inspektør.also { arbeidsgiveroppdrag ->
                    assertEquals(2, arbeidsgiveroppdrag.antallLinjer())
                    assertEquals(17.januar, arbeidsgiveroppdrag.fom(0))
                    assertEquals(17.januar, arbeidsgiveroppdrag.tom(0))
                    assertEquals(Endringskode.ENDR, arbeidsgiveroppdrag.endringskode(0))

                    assertEquals(20.januar, arbeidsgiveroppdrag.fom(1))
                    assertEquals(31.januar, arbeidsgiveroppdrag.tom(1))
                    assertEquals(Endringskode.NY, arbeidsgiveroppdrag.endringskode(1))
                }
            }

            inspektør.utbetaling(1).also { marsutbetaling ->
                val revurdering = inspektør.utbetaling(3)
                assertEquals(marsutbetaling.korrelasjonsId, revurdering.korrelasjonsId)
                assertEquals(24.februar til 24.mars, marsutbetaling.periode)
                assertEquals(24.februar til 24.mars, revurdering.periode)
                assertEquals(0, revurdering.personOppdrag.size)
                assertEquals(1, revurdering.arbeidsgiverOppdrag.size)
                assertEquals(Endringskode.UEND, revurdering.arbeidsgiverOppdrag.inspektør.endringskode)
            }
        }
        a2 {
            assertEquals(2, inspektør.antallUtbetalinger)
            inspektør.utbetaling(0).also { februarutbetaling ->
                val revurdering = inspektør.utbetaling(1)
                assertEquals(februarutbetaling.korrelasjonsId, revurdering.korrelasjonsId)
                assertEquals(25.januar til 25.februar, februarutbetaling.periode)
                assertEquals(25.januar til 25.februar, revurdering.periode)
                assertEquals(0, revurdering.personOppdrag.size)
                assertEquals(1, revurdering.arbeidsgiverOppdrag.size)
                assertEquals(Endringskode.UEND, revurdering.arbeidsgiverOppdrag.inspektør.endringskode)
                revurdering.arbeidsgiverOppdrag.inspektør.also { arbeidsgiveroppdrag ->
                    assertEquals(10.februar, arbeidsgiveroppdrag.fom(0))
                    assertEquals(23.februar, arbeidsgiveroppdrag.tom(0))
                    assertEquals(Endringskode.UEND, arbeidsgiveroppdrag.endringskode(0))
                }
            }
        }
    }

    @Test
    fun `Korrigerende søknad for tidligere skjæringstidspunkt`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).nyeVedtak(mars)

        a2 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(18.januar, 19.januar))
        }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            assertEquals(Feriedag::class, inspektør.sykdomstidslinje[18.januar]::class)
            assertEquals(Feriedag::class, inspektør.sykdomstidslinje[19.januar]::class)
            val arbeidsgiverOppdrag = inspektør.sisteUtbetaling().arbeidsgiverOppdrag
            assertEquals(2, arbeidsgiverOppdrag.size)
            arbeidsgiverOppdrag[0].inspektør.let { utbetalingslinjeInspektør ->
                assertEquals(17.januar, utbetalingslinjeInspektør.fom)
                assertEquals(17.januar, utbetalingslinjeInspektør.tom)
                assertEquals(Endringskode.ENDR, utbetalingslinjeInspektør.endringskode)
                assertNull(utbetalingslinjeInspektør.datoStatusFom)
            }
            arbeidsgiverOppdrag[1].inspektør.let { utbetalingslinjeInspektør ->
                assertEquals(20.januar, utbetalingslinjeInspektør.fom)
                assertEquals(31.januar, utbetalingslinjeInspektør.tom)
                assertEquals(Endringskode.NY, utbetalingslinjeInspektør.endringskode)
                assertNull(utbetalingslinjeInspektør.datoStatusFom)
            }
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a1 {
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `Korrigerende søknad for førstegangsbehandling med forlengelse - setter i gang en revurdering`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)

        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            (17..31).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            (17..31).forEach {
                assertEquals(100.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }
    }

    @Test
    fun `Korrigerende søknad for forlengelse - setter i gang en revurdering`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)

        a1 {
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(27.februar, 28.februar))
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            assertTrue(inspektør.utbetalingstidslinjer(2.vedtaksperiode)[27.februar] is Utbetalingsdag.Fridag)
            assertTrue(inspektør.utbetalingstidslinjer(2.vedtaksperiode)[28.februar] is Utbetalingsdag.Fridag)
            assertEquals(18, inspektør.utbetalingstidslinjer(2.vedtaksperiode).inspektør.navDagTeller)
        }

        a2 {
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Korrigerende søknad for nytt sykefraværstilfelle - setter i gang en revurdering`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).nyeVedtak(mars)

        a1 {
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), Ferie(29.mars, 31.mars))
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            assertTrue(inspektør.utbetalingstidslinjer(2.vedtaksperiode)[29.mars] is Utbetalingsdag.Fridag)
            assertTrue(inspektør.utbetalingstidslinjer(2.vedtaksperiode)[30.mars] is Utbetalingsdag.Fridag)
            assertEquals(8, inspektør.utbetalingstidslinjer(2.vedtaksperiode).inspektør.navDagTeller)
        }

        a2 {
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `To arbeidsgivere med ett sykefraværstilfelle og gap over 16 dager på den ene arbeidsgiveren - korrigerende søknad på arbeidsgiver uten gap setter i gang revurdering`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(25.januar, 25.februar))
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent))
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterSykmelding(Sykmeldingsperiode(24.februar, 24.mars))
            håndterSøknad(Sykdom(24.februar, 24.mars, 100.prosent))
            observatør.assertEtterspurt(2.vedtaksperiode, PersonObserver.Refusjon::class, PersonObserver.Arbeidsgiverperiode::class)
            håndterInntektsmelding(listOf(24.februar til 11.mars), beregnetInntekt = INNTEKT)
        }
        a2 {
            håndterInntektsmelding(listOf(25.januar til 9.februar), beregnetInntekt = INNTEKT)
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
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            håndterSøknad(Sykdom(25.januar, 25.februar, 50.prosent))
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a1 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            (10..25).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.februar].økonomi.inspektør.grad)
            }
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }

        a1 {
            assertEquals(4, inspektør.antallUtbetalinger)

            inspektør.utbetaling(0).also { januarutbetaling ->
                val revurdering = inspektør.utbetaling(2)
                assertEquals(januarutbetaling.korrelasjonsId, revurdering.korrelasjonsId)
                assertEquals(januar, januarutbetaling.periode)
                assertEquals(januar, revurdering.periode)
                assertEquals(0, revurdering.personOppdrag.size)
                assertEquals(1, revurdering.arbeidsgiverOppdrag.size)
                assertEquals(Endringskode.UEND, revurdering.arbeidsgiverOppdrag.inspektør.endringskode)
            }

            inspektør.utbetaling(1).also { marsutbetaling ->
                val revurdering = inspektør.utbetaling(3)
                assertEquals(marsutbetaling.korrelasjonsId, revurdering.korrelasjonsId)
                assertEquals(24.februar til 24.mars, marsutbetaling.periode)
                assertEquals(24.februar til 24.mars, revurdering.periode)
                assertEquals(0, revurdering.personOppdrag.size)
                assertEquals(1, revurdering.arbeidsgiverOppdrag.size)
                assertEquals(Endringskode.UEND, revurdering.arbeidsgiverOppdrag.inspektør.endringskode)
            }
        }

        a2 {
            assertEquals(2, inspektør.antallUtbetalinger)

            inspektør.utbetaling(0).also { februarutbetaling ->
                val revurdering = inspektør.utbetaling(1)
                assertEquals(februarutbetaling.korrelasjonsId, revurdering.korrelasjonsId)
                assertEquals(25.januar til 25.februar, februarutbetaling.periode)
                assertEquals(25.januar til 25.februar, revurdering.periode)
                assertEquals(0, revurdering.personOppdrag.size)
                assertEquals(1, revurdering.arbeidsgiverOppdrag.size)
                assertEquals(Endringskode.ENDR, revurdering.arbeidsgiverOppdrag.inspektør.endringskode)
                revurdering.arbeidsgiverOppdrag.inspektør.also { arbeidsgiveroppdrag ->
                    assertEquals(10.februar, arbeidsgiveroppdrag.fom(0))
                    assertEquals(23.februar, arbeidsgiveroppdrag.tom(0))
                    assertEquals(50, arbeidsgiveroppdrag.grad(0))
                    assertEquals(Endringskode.NY, arbeidsgiveroppdrag.endringskode(0))
                }
            }
        }
    }

    @Test
    fun `To arbeidsgivere med ett sykefraværstilfelle og gap under 16 dager på den ene arbeidsgiveren - korrigerende søknader setter i gang revurdering`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(25.januar, 25.februar))
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent))
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterSykmelding(Sykmeldingsperiode(10.februar, 10.mars))
            håndterSøknad(Sykdom(10.februar, 10.mars, 100.prosent))
            observatør.assertEtterspurt(2.vedtaksperiode, PersonObserver.Refusjon::class)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT, 10.februar)
        }
        a2 {
            håndterInntektsmelding(listOf(25.januar til 9.februar), beregnetInntekt = INNTEKT)
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
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            håndterSøknad(Sykdom(25.januar, 25.februar, 50.prosent))
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a1 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            (10..25).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.februar].økonomi.inspektør.grad)
            }
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }

        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            (17..31).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
            (10..28).forEach {
                assertEquals(100.prosent, inspektør.utbetalingstidslinjer(2.vedtaksperiode)[it.februar].økonomi.inspektør.grad)
            }
        }
    }

    @Test
    fun `Korrigerende søknad for periode i AvventerGodkjenningRevurdering - setter i gang en overstyring av revurderingen`() {
        listOf(a1, a2).nyeVedtak(januar)
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                Ferie(30.januar, 31.januar)
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())

            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent, 50.prosent),
                Ferie(30.januar, 31.januar)
            )
            inspektør.utbetalinger(1.vedtaksperiode).also { utbetalinger ->
                assertEquals(2, utbetalinger.size)
                assertEquals(Utbetalingstatus.FORKASTET, utbetalinger.last().inspektør.tilstand)
            }
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, AVSLUTTET)

            assertEquals(9, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navDagTeller)
            assertEquals(2, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.fridagTeller)

            (17..29).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `Korrigerende søknad for periode i AvventerSimuleringRevurdering - setter i gang en overstyring av revurderingen`() {
        listOf(a1, a2).nyeVedtak(januar)
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                Ferie(30.januar, 31.januar)
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)

            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent, 50.prosent),
                Ferie(30.januar, 31.januar)
            )
            inspektør.utbetalinger(1.vedtaksperiode).also { utbetalinger ->
                assertEquals(2, utbetalinger.size)
                assertEquals(Utbetalingstatus.FORKASTET, utbetalinger.last().inspektør.tilstand)
            }
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, AVSLUTTET)

            assertEquals(9, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navDagTeller)
            assertEquals(2, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.fridagTeller)

            (17..29).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `Korrigerende søknad for periode i AvventerHistorikkRevurdering - setter i gang en overstyring av revurderingen`() {
        listOf(a1, a2).nyeVedtak(januar)
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                Ferie(30.januar, 31.januar)
            )
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent, 50.prosent),
                Ferie(30.januar, 31.januar)
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertTilstand(1.vedtaksperiode, AVSLUTTET)

            assertEquals(9, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navDagTeller)
            assertEquals(2, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.fridagTeller)

            (17..29).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `Korrigerende søknad for periode i AvventerRevurdering - setter i gang en overstyring av revurderingen`() {
        listOf(a1, a2).nyeVedtak(januar)
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
        }
        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            (17..31).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            (17..31).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }
    }
}
