package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.spleis.e2e.AbstractEndToEndTest.Companion.INNTEKT
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.reflect.KClass

internal class GjenbrukeTidsnæreOpplysningerTest: AbstractDslTest() {

    @Test
    fun `vedtaksperiode strekker seg tilbake og endrer skjæringstidspunktet`() {
        a1 {
            tilGodkjenning(10.januar, 31.januar, beregnetInntekt = 20000.månedlig)
            val vilkårsgrunnlagFørEndring = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!
            nullstillTilstandsendringer()
            håndterOverstyrTidslinje(listOf(
                ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)
            ))

            assertEquals(9.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

            val dagen = inspektør.sykdomstidslinje[9.januar]
            assertEquals(Dag.Sykedag::class, dagen::class)
            assertTrue(dagen.kommerFra(OverstyrTidslinje::class))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt = 20000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            val vilkårsgrunnlagEtterEndring = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!

            assertTidsnærInntektsopplysning(a1, vilkårsgrunnlagFørEndring.inspektør.sykepengegrunnlag, vilkårsgrunnlagEtterEndring.inspektør.sykepengegrunnlag)
            assertTilstander(1.vedtaksperiode,
                AVVENTER_GODKJENNING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING
            )
        }

    }

    @Test
    fun `endrer skjæringstidspunkt på en førstegangsbehandling ved å omgjøre en arbeidsdag til sykedag`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 9.januar, 100.prosent))
            tilGodkjenning(10.januar, 31.januar, beregnetInntekt = 20000.månedlig, arbeidsgiverperiode = listOf(10.januar til 26.januar)) // 1. jan - 9. jan blir omgjort til arbeidsdager ved innsending av IM her
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            nullstillTilstandsendringer()
            // Saksbehandler korrigerer; 9.januar var vedkommende syk likevel
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)))

            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(2.vedtaksperiode, inntekt = 20000.månedlig)
            håndterYtelser(2.vedtaksperiode)

            assertSykdomstidslinjedag(9.januar, Dag.Sykedag::class, OverstyrTidslinje::class)
            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
            assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `endrer skjæringstidspunkt på sykefraværstilfelle etter - endrer ikke dato for inntekter`() {
        val inntektsvurdering = Inntektsvurdering(
            inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            })

        a1 {
            håndterSøknad(Sykdom(1.januar, 9.januar, 100.prosent), Arbeid(1.januar, 9.januar))
            håndterSøknad(Sykdom(15.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(15.januar til 29.januar))
        }
        a2 {
            håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(10.januar til 25.januar))

            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = inntektsvurdering)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            nullstillTilstandsendringer()
        }
        val sykepengegrunnlagFør = a2 {
            inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        }

        a1 {
            // Saksbehandler korrigerer; 9.januar var vedkommende syk likevel
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)))
            assertSykdomstidslinjedag(9.januar, Dag.Sykedag::class, OverstyrTidslinje::class)
        }
        a2 {

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = inntektsvurdering)
            håndterYtelser(1.vedtaksperiode)

        }
        val sykepengegrunnlagEtter = a2 {
            inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        }

        a2 {
            assertTidsnærInntektsopplysning(a2, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertEquals(10.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
        a1 {
            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)
            assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
            assertEquals(15.januar til 31.januar, inspektør.periode(2.vedtaksperiode))
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `vedtaksperiode flytter skjæringstidspunktet frem`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje(listOf(
                ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag, 100),
                ManuellOverskrivingDag(4.januar, Dagtype.Arbeidsdag, 100),
            ))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

            assertUtbetalingsdag(18.januar, Utbetalingsdag.ArbeidsgiverperiodeDag::class, Inntekt.INGEN, Inntekt.INGEN)
            assertUtbetalingsdag(19.januar, Utbetalingsdag.NavDag::class, 1431.daglig, Inntekt.INGEN)

            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `vedtaksperiode flytter skjæringstidspunktet frem etter utbetalt`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje(listOf(
                ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag, 100),
                ManuellOverskrivingDag(4.januar, Dagtype.Arbeidsdag, 100),
            ))
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

            assertUtbetalingsdag(18.januar, Utbetalingsdag.ArbeidsgiverperiodeDag::class, Inntekt.INGEN, Inntekt.INGEN)
            assertUtbetalingsdag(19.januar, Utbetalingsdag.NavDag::class, 1431.daglig, Inntekt.INGEN)

            val førsteUtbetaling = inspektør.utbetaling(0).inspektør
            val revurdering = inspektør.utbetaling(1).inspektør
            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        }
    }

    @Test
    fun `flytter arbeidsgiverperioden frem 16 dager etter utbetalt`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje((1.januar til 16.januar).map { dag ->
                ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
            })
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterYtelser(2.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertEquals(17.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

            val førsteUtbetaling = inspektør.utbetaling(0).inspektør
            val revurdering = inspektør.utbetaling(1).inspektør
            val februarutbetaling = inspektør.utbetaling(2).inspektør

            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
            assertEquals(1.januar til 31.januar, revurdering.periode)

            assertEquals(førsteUtbetaling.korrelasjonsId, februarutbetaling.korrelasjonsId)
            assertEquals(1.januar til 28.februar, februarutbetaling.periode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET
            )
        }
    }

    @Test
    fun `flytter arbeidsgiverperioden frem 10 dager etter utbetalt`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje((1.januar til 10.januar).map { dag ->
                ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
            })
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertEquals(11.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

            val førsteUtbetaling = inspektør.utbetaling(0).inspektør
            val revurdering = inspektør.utbetaling(1).inspektør
            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
            assertEquals(1.januar til 31.januar, revurdering.periode)
            revurdering.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(2, oppdrag.size)
                oppdrag[0].inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                    assertEquals(17.januar, linje.datoStatusFom)
                    assertEquals(Endringskode.ENDR, linje.endringskode)
                    assertEquals("OPPH", linje.statuskode)
                }
                oppdrag[1].inspektør.also { linje ->
                    assertEquals(27.januar til 31.januar, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
            assertEquals(0, revurdering.personOppdrag.size)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        }
    }

    @Test
    fun `flytter arbeidsgiverperioden frem 10 dager etter utbetalt - med tidligere utbetalt vedtak`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)

            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje((1.mars til 10.mars).map { dag ->
                ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
            })
            håndterYtelser(2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertEquals(11.mars, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(1.mars til 31.mars, inspektør.periode(2.vedtaksperiode))

            val førsteUtbetaling = inspektør.utbetaling(1).inspektør
            val revurdering = inspektør.utbetaling(2).inspektør
            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
            assertEquals(1.mars til 31.mars, revurdering.periode)
            assertEquals(1.januar til 31.mars, revurdering.utbetalingstidslinje.periode())
            revurdering.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(2, oppdrag.size)
                oppdrag[0].inspektør.also { linje ->
                    assertEquals(17.mars til 30.mars, linje.fom til linje.tom)
                    assertEquals(17.mars, linje.datoStatusFom)
                    assertEquals(Endringskode.ENDR, linje.endringskode)
                    assertEquals("OPPH", linje.statuskode)
                }
                oppdrag[1].inspektør.also { linje ->
                    assertEquals(27.mars til 30.mars, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
            assertEquals(0, revurdering.personOppdrag.size)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        }
    }

    @Test
    fun `innfører ny arbeidsgiverperiode på en førstegangsbehandling etter tidligere utbetalt`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            nyttVedtak(14.februar, 10.mars, arbeidsgiverperiode = listOf(1.januar til 16.januar))
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje((14.februar til 16.februar).map { dag ->
                ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
            })

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertEquals(17.februar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(14.februar til 10.mars, inspektør.periode(2.vedtaksperiode))

            val januarutbetaling = inspektør.utbetaling(0).inspektør
            val februarutbetaling = inspektør.utbetaling(1).inspektør
            val revurderingJanuar = inspektør.utbetaling(2).inspektør
            val revurderingFebruar = inspektør.utbetaling(3).inspektør

            assertEquals(januarutbetaling.korrelasjonsId, februarutbetaling.korrelasjonsId)
            assertEquals(1.januar til 31.januar, januarutbetaling.periode)
            assertEquals(1.januar til 10.mars, februarutbetaling.periode)
            assertEquals(1.januar til 10.mars, februarutbetaling.utbetalingstidslinje.periode())

            februarutbetaling.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(2, oppdrag.size)
                oppdrag[0].inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                    assertEquals(Endringskode.UEND, linje.endringskode)
                }
                oppdrag[1].inspektør.also { linje ->
                    assertEquals(14.februar til 9.mars, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
            assertEquals(0, februarutbetaling.personOppdrag.size)

            assertEquals(januarutbetaling.korrelasjonsId, revurderingJanuar.korrelasjonsId)
            assertEquals(1.januar til 31.januar, revurderingJanuar.periode)
            revurderingJanuar.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(1, oppdrag.size)
                oppdrag[0].inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
            assertEquals(0, revurderingJanuar.personOppdrag.size)

            assertNotEquals(revurderingJanuar.korrelasjonsId, revurderingFebruar.korrelasjonsId)
            assertEquals(14.februar til 10.mars, revurderingFebruar.periode)
            assertEquals(0, revurderingFebruar.personOppdrag.size)
            assertEquals(1, revurderingFebruar.arbeidsgiverOppdrag.size)
            assertEquals(Endringskode.NY, revurderingFebruar.arbeidsgiverOppdrag.inspektør.endringskode)
            revurderingFebruar.arbeidsgiverOppdrag.single().inspektør.also { linje ->
                assertEquals(5.mars til 9.mars, linje.fom til linje.tom)
                assertEquals(Endringskode.NY, linje.endringskode)
            }
        }
    }

    @Test
    fun `innfører ny arbeidsgiverperiode ved å lage feriedager på en førstegangsbehandling etter tidligere utbetalt`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            nyttVedtak(14.februar, 10.mars, arbeidsgiverperiode = listOf(1.januar til 16.januar))
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje((14.februar til 16.februar).map { dag ->
                ManuellOverskrivingDag(dag, Dagtype.Feriedag, 100)
            })

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertEquals(17.februar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(14.februar til 10.mars, inspektør.periode(2.vedtaksperiode))

            val januarutbetaling = inspektør.utbetaling(0).inspektør
            val februarutbetaling = inspektør.utbetaling(1).inspektør
            val revurderingJanuar = inspektør.utbetaling(2).inspektør
            val revurderingFebruar = inspektør.utbetaling(3).inspektør

            assertEquals(januarutbetaling.korrelasjonsId, februarutbetaling.korrelasjonsId)
            assertEquals(1.januar til 31.januar, januarutbetaling.periode)
            assertEquals(1.januar til 10.mars, februarutbetaling.periode)
            assertEquals(1.januar til 10.mars, februarutbetaling.utbetalingstidslinje.periode())

            februarutbetaling.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(2, oppdrag.size)
                oppdrag[0].inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                    assertEquals(Endringskode.UEND, linje.endringskode)
                }
                oppdrag[1].inspektør.also { linje ->
                    assertEquals(14.februar til 9.mars, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
            assertEquals(0, februarutbetaling.personOppdrag.size)

            assertEquals(januarutbetaling.korrelasjonsId, revurderingJanuar.korrelasjonsId)
            assertEquals(1.januar til 31.januar, revurderingJanuar.periode)
            revurderingJanuar.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(1, oppdrag.size)
                oppdrag[0].inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
            assertEquals(0, revurderingJanuar.personOppdrag.size)

            assertNotEquals(revurderingJanuar.korrelasjonsId, revurderingFebruar.korrelasjonsId)
            assertEquals(14.februar til 10.mars, revurderingFebruar.periode)
            assertEquals(0, revurderingFebruar.personOppdrag.size)
            assertEquals(1, revurderingFebruar.arbeidsgiverOppdrag.size)
            assertEquals(Endringskode.NY, revurderingFebruar.arbeidsgiverOppdrag.inspektør.endringskode)
            revurderingFebruar.arbeidsgiverOppdrag.single().inspektør.also { linje ->
                assertEquals(5.mars til 9.mars, linje.fom til linje.tom)
                assertEquals(Endringskode.NY, linje.endringskode)
            }
        }
    }

    @Test
    fun `innfører arbeidsdager på halen av første vedtaksperiode`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.sykepengegrunnlag

            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Arbeidsdag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inspektør.sykepengegrunnlag

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)
        }
    }

    private fun assertTidsnærInntektsopplysning(orgnummer: String, sykepengegrunnlagFør: Sykepengegrunnlag, sykepengegrunnlagEtter: Sykepengegrunnlag) {
        val inntektsopplysningerFørEndring = sykepengegrunnlagFør.inspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(orgnummer)
        val inntektsopplysningerEtterEndring = sykepengegrunnlagEtter.inspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(orgnummer)

        assertEquals(
            inntektsopplysningerEtterEndring.inspektør.inntektsopplysning.inspektør.hendelseId,
            inntektsopplysningerFørEndring.inspektør.inntektsopplysning.inspektør.hendelseId
        )
        assertEquals(
            inntektsopplysningerEtterEndring.inspektør.inntektsopplysning.inspektør.beløp,
            inntektsopplysningerFørEndring.inspektør.inntektsopplysning.inspektør.beløp
        )
        assertEquals(
            inntektsopplysningerEtterEndring.inspektør.inntektsopplysning.inspektør.tidsstempel,
            inntektsopplysningerFørEndring.inspektør.inntektsopplysning.inspektør.tidsstempel
        )
        assertEquals(
            Inntektsmelding::class,
            inntektsopplysningerEtterEndring.inspektør.inntektsopplysning::class
        )
    }

    private fun TestPerson.TestArbeidsgiver.assertSykdomstidslinjedag(dato: LocalDate, dagtype: KClass<out Dag>, kommerFra: KClass<out SykdomstidslinjeHendelse>) {
        val dagen = inspektør.sykdomstidslinje[dato]
        assertEquals(dagtype, dagen::class)
        assertTrue(dagen.kommerFra(kommerFra))
    }

    private fun TestPerson.TestArbeidsgiver.assertUtbetalingsdag(dato: LocalDate, dagtype: KClass<out Utbetalingsdag>, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt) {
        val sisteUtbetaling = inspektør.utbetalinger.last().inspektør
        val utbetalingstidslinje = sisteUtbetaling.utbetalingstidslinje
        val dagen = utbetalingstidslinje[dato]

        assertEquals(dagtype, dagen::class)
        assertEquals(arbeidsgiverbeløp, dagen.økonomi.inspektør.arbeidsgiverbeløp)
        assertEquals(personbeløp, dagen.økonomi.inspektør.personbeløp)
    }
}