package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.lagStandardSammenligningsgrunnlag
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurderKorrigertSoknad::class)
internal class RevurderKorrigertSøknadFlereArbeidsgivereTest : AbstractDslTest() {
    @Test
    fun `Korrigerende søknad hos en arbeidsgiver - setter i gang revurdering`() {
        listOf(a1, a2).nyeVedtak(1.januar til 31.januar)
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 50.prosent),
                Arbeid(30.januar, 31.januar)
            )
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            håndterYtelser(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertEquals(21, inspektør.sykdomstidslinje.inspektør.dagteller[Sykedag::class])
            assertEquals(2, inspektør.sykdomstidslinje.inspektør.dagteller[Arbeidsdag::class])
            assertEquals(8, inspektør.sykdomstidslinje.inspektør.dagteller[SykHelgedag::class])
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar] is NavDag)
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[18.januar] is NavDag)
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `Overlappende søknad som strekker seg forbi vedtaksperioden - setter ikke i gang revurdering`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(15.januar, 15.februar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(15.januar, 15.februar, 100.prosent))
            håndterInntektsmelding(listOf(15.januar til 30.januar), beregnetInntekt = 20000.månedlig)
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 20000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurdering = listOf(a1, a2).lagStandardSammenligningsgrunnlag(20000.månedlig, 1.januar)
            )
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
            håndterSykmelding(Sykmeldingsperiode(15.januar, 15.februar, 50.prosent))
            håndterSøknad(
                Sykdom(15.januar, 15.februar, 50.prosent)
            )
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            (17..31).forEach {
                assertEquals(100.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Vedtaksperiodene til a1 og a2 er kant-i-kant og det kommer en korrigerende søknad for a1 - setter i gang revurdering`() {
        val periodeAg1 = 1.januar til 31.januar
        val periodeAg2 = 1.februar til 28.februar
        a1 {
            håndterSykmelding(Sykmeldingsperiode(periodeAg1.start, periodeAg1.endInclusive, 100.prosent))
            håndterSøknad(Sykdom(periodeAg1.start, periodeAg1.endInclusive, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(periodeAg2.start, periodeAg2.endInclusive, 100.prosent))
            håndterSøknad(Sykdom(periodeAg2.start, periodeAg2.endInclusive, 100.prosent))
            håndterInntektsmelding(listOf(periodeAg2.start til periodeAg2.start.plusDays(15)), beregnetInntekt = 20000.månedlig)
        }
        a1 {
            håndterInntektsmelding(listOf(periodeAg1.start til periodeAg1.start.plusDays(15)), beregnetInntekt = 20000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurdering = listOf(a1, a2).lagStandardSammenligningsgrunnlag(20000.månedlig, periodeAg1.start),
                inntektsvurderingForSykepengegrunnlag = listOf(a1, a2).lagStandardSykepengegrunnlag(20000.månedlig, periodeAg1.start)
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
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
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(25.januar, 25.februar, 100.prosent))
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent))
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = TestPerson.INNTEKT)
            håndterSykmelding(Sykmeldingsperiode(24.februar, 24.mars, 100.prosent))
            håndterSøknad(Sykdom(24.februar, 24.mars, 100.prosent))
            håndterInntektsmelding(listOf(24.februar til 11.mars), beregnetInntekt = TestPerson.INNTEKT)
        }
        a2 {
            håndterInntektsmelding(listOf(25.januar til 9.februar), beregnetInntekt = TestPerson.INNTEKT)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(  1.vedtaksperiode,
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
                ),
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, 1.januar, TestPerson.INNTEKT.repeat(12)),
                        sammenligningsgrunnlag(a2, 1.januar, TestPerson.INNTEKT.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, TestPerson.INNTEKT.repeat(3)),
                        grunnlag(a2, 1.januar, TestPerson.INNTEKT.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
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
                inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[Sykedag::class]
            )
            assertEquals(
                2,
                inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[Dag.Feriedag::class]
            )
            assertEquals(
                8,
                inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[SykHelgedag::class]
            )
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `Korrigerende søknad for tidligere skjæringstidspunkt - setter ikke i gang en revurdering`() {
        listOf(a1, a2).nyeVedtak(1.januar til 31.januar)
        listOf(a1, a2).nyeVedtak(1.mars til 31.mars)

        a2 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(18.januar, 19.januar))
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }

        a1 {
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Korrigerende søknad for førstegangsbehandling med forlengelse - setter i gang en revurdering`() {
        listOf(a1, a2).nyeVedtak(1.januar til 31.januar)
        listOf(a1, a2).forlengVedtak(1.februar til 28.februar)

        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
            assertTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            (17..31).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
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
        listOf(a1, a2).nyeVedtak(1.januar til 31.januar)
        listOf(a1, a2).forlengVedtak(1.februar til 28.februar)

        a1 {
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Arbeid(27.februar, 28.februar))
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

            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            assertTrue(inspektør.utbetalingstidslinjer(2.vedtaksperiode)[27.februar] is Utbetalingsdag.Arbeidsdag)
            assertTrue(inspektør.utbetalingstidslinjer(2.vedtaksperiode)[28.februar] is Utbetalingsdag.Arbeidsdag)
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
        listOf(a1, a2).nyeVedtak(1.januar til 31.januar)
        listOf(a1, a2).nyeVedtak(1.mars til 31.mars)

        a1 {
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), Arbeid(29.mars, 31.mars))
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

            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            assertTrue(inspektør.utbetalingstidslinjer(2.vedtaksperiode)[29.mars] is Utbetalingsdag.Arbeidsdag)
            assertTrue(inspektør.utbetalingstidslinjer(2.vedtaksperiode)[30.mars] is Utbetalingsdag.Arbeidsdag)
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
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(25.januar, 25.februar, 100.prosent))
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent))
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = TestPerson.INNTEKT)
            håndterSykmelding(Sykmeldingsperiode(24.februar, 24.mars, 100.prosent))
            håndterSøknad(Sykdom(24.februar, 24.mars, 100.prosent))
            håndterInntektsmelding(listOf(24.februar til 11.mars), beregnetInntekt = TestPerson.INNTEKT)
        }
        a2 {
            håndterInntektsmelding(listOf(25.januar til 9.februar), beregnetInntekt = TestPerson.INNTEKT)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(  1.vedtaksperiode,
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
                ),
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, 1.januar, TestPerson.INNTEKT.repeat(12)),
                        sammenligningsgrunnlag(a2, 1.januar, TestPerson.INNTEKT.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, TestPerson.INNTEKT.repeat(3)),
                        grunnlag(a2, 1.januar, TestPerson.INNTEKT.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
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
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }

        a1 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            (10..25).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.februar].økonomi.inspektør.grad)
            }
        }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `To arbeidsgivere med ett sykefraværstilfelle og gap under 16 dager på den ene arbeidsgiveren - korrigerende søknader setter i gang revurdering`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(25.januar, 25.februar, 100.prosent))
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent))
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = TestPerson.INNTEKT)
            håndterSykmelding(Sykmeldingsperiode(10.februar, 10.mars, 100.prosent))
            håndterSøknad(Sykdom(10.februar, 10.mars, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = TestPerson.INNTEKT, 10.februar)
        }
        a2 {
            håndterInntektsmelding(listOf(25.januar til 9.februar), beregnetInntekt = TestPerson.INNTEKT)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(  1.vedtaksperiode,
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
                ),
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, 1.januar, TestPerson.INNTEKT.repeat(12)),
                        sammenligningsgrunnlag(a2, 1.januar, TestPerson.INNTEKT.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, TestPerson.INNTEKT.repeat(3)),
                        grunnlag(a2, 1.januar, TestPerson.INNTEKT.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
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
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }

        a1 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            (10..25).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.februar].økonomi.inspektør.grad)
            }
        }

        a1 {
            assertTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)

            håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
            assertTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
            (17..31).forEach {
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
            (10..28).forEach {
                assertEquals(100.prosent, inspektør.utbetalingstidslinjer(2.vedtaksperiode)[it.februar].økonomi.inspektør.grad)
            }
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    @Disabled("ikke implemnentert enda - avventer bug i håndtering av inntektsmelding")
    fun `To arbeidsgivere med ett sykefraværstilfelle og gap over 16 dager - korrigerende søknad setter i gang revurdering`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(25.januar, 25.februar, 100.prosent))
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent))
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 15000.00.månedlig)
            håndterSykmelding(Sykmeldingsperiode(24.februar, 24.mars, 100.prosent))
            håndterSøknad(Sykdom(24.februar, 24.mars, 100.prosent))
            håndterInntektsmelding(listOf(24.februar til 11.mars), beregnetInntekt = 17000.00.månedlig)
        }
        a2 {
            håndterInntektsmelding(listOf(25.januar til 9.februar), beregnetInntekt = 16000.00.månedlig)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(  1.vedtaksperiode,
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
                ),
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, 1.januar, 15000.00.månedlig.repeat(12)),
                        sammenligningsgrunnlag(a2, 1.januar, 16000.00.månedlig.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, 15000.00.månedlig.repeat(3)),
                        grunnlag(a2, 1.januar, 16000.00.månedlig.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
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
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent), Arbeid(20.februar, 25.februar))
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }

        a1 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            (20..25).forEach {
                assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.februar] is Utbetalingsdag.Arbeidsdag)
            }
        }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode,
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
                ),
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, 24.februar, 15000.00.månedlig.repeat(12)),
                        sammenligningsgrunnlag(a2, 24.februar, 16000.00.månedlig.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 24.februar, 15000.00.månedlig.repeat(3)),
                        grunnlag(a2, 24.februar, 16000.00.månedlig.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `To arbeidsgivere med ett sykefraværstilfelle og gap over 16 dager - korrigerende søknad revurderer uten vilkårsprøving`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(25.januar, 25.februar, 100.prosent))
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent))
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 15000.00.månedlig)
            håndterSykmelding(Sykmeldingsperiode(24.februar, 24.mars, 100.prosent))
            håndterSøknad(Sykdom(24.februar, 24.mars, 100.prosent))
            håndterInntektsmelding(listOf(24.februar til 11.mars), beregnetInntekt = 17000.00.månedlig)
        }
        a2 {
            håndterInntektsmelding(listOf(25.januar til 9.februar), beregnetInntekt = 16000.00.månedlig)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(  1.vedtaksperiode,
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
                ),
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, 1.januar, 15000.00.månedlig.repeat(12)),
                        sammenligningsgrunnlag(a2, 1.januar, 16000.00.månedlig.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, 15000.00.månedlig.repeat(3)),
                        grunnlag(a2, 1.januar, 16000.00.månedlig.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
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
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent), Arbeid(20.februar, 25.februar))
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }

        a1 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
    }
    @Test
    fun `Korrigerende søknad for periode i AvventerGodkjenningRevurdering - setter i gang en overstyring av revurderingen`() {
        listOf(a1, a2).nyeVedtak(1.januar til 31.januar)
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                Arbeid(30.januar, 31.januar)
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)

            håndterSøknad(
                Sykdom(1.januar, 31.januar, 50.prosent),
                Arbeid(30.januar, 31.januar)
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, AVSLUTTET)

            assertEquals(9, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navDagTeller)
            assertEquals(2, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.arbeidsdagTeller)

            (17..29).forEach{
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }
    @Test
    fun `Korrigerende søknad for periode i AvventerSimuleringRevurdering - setter i gang en overstyring av revurderingen`() {
        listOf(a1, a2).nyeVedtak(1.januar til 31.januar)
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                Arbeid(30.januar, 31.januar)
            )
            håndterYtelser(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)

            håndterSøknad(
                Sykdom(1.januar, 31.januar, 50.prosent),
                Arbeid(30.januar, 31.januar)
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, AVSLUTTET)

            assertEquals(9, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navDagTeller)
            assertEquals(2, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.arbeidsdagTeller)

            (17..29).forEach{
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }
    @Test
    fun `Korrigerende søknad for periode i AvventerHistorikkRevurdering - setter i gang en overstyring av revurderingen`() {
        listOf(a1, a2).nyeVedtak(1.januar til 31.januar)
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                Arbeid(30.januar, 31.januar)
            )
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            håndterSøknad(
                Sykdom(1.januar, 31.januar, 50.prosent),
                Arbeid(30.januar, 31.januar)
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, AVSLUTTET)

            assertEquals(9, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navDagTeller)
            assertEquals(2, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.arbeidsdagTeller)

            (17..29).forEach{
                assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
            }
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }
}
