package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.forlengelseTilGodkjenning
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.Dagtype.Sykedag
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Overlapper med foreldrepenger`
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_13
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING_TIL_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.arbeidsgiveropplysninger.TrengerArbeidsgiveropplysningerTest.Companion.assertEtterspurt
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.Endringskode.ENDR
import no.nav.helse.utbetalingslinjer.Endringskode.NY
import no.nav.helse.utbetalingslinjer.Endringskode.UEND
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RevurderingOutOfOrderGapTest : AbstractDslTest() {

    @Test
    fun `Arbeidsgiver med kort gap mellom sykefravær blir forsøkt sklitaklet av annen arbeidsgiver som tetter gapet og flytter skjæringstidspunktet`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(emptyList(), vedtaksperiodeId = 2.vedtaksperiode)
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 2.vedtaksperiode.filter())

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }
    }

    @Test
    fun `out of order med utbetaling i arbeidsgiverperioden og overlapp med andre ytelser`() {
        a1 {
            nyttVedtak(1.januar til 25.januar)
            nyttVedtak(februar)

            håndterSøknad(Sykdom(26.januar, 31.januar, 100.prosent))
            håndterYtelser(3.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(26.januar til 31.januar, 100)))

            assertVarsel(`Overlapper med foreldrepenger`, 3.vedtaksperiode.filter())
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `out of order med nyere periode til godkjenning revurdering`() {
        a1 {
            nyttVedtak(mars)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(30.mars, Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            nullstillTilstandsendringer()
            nyPeriode(januar)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `out of order periode i helg mellom to andre perioder`() {
        a1 {
            nyttVedtak(1.januar til 26.januar)
            forlengVedtak(29.januar til 11.februar)
            nullstillTilstandsendringer()
            nyPeriode(27.januar til 28.januar)
            håndterYtelser(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)

            val førsteUtbetaling = inspektør.utbetaling(0)
            val andreUtbetaling = inspektør.utbetaling(1)
            val outOfOrderUtbetaling = inspektør.utbetaling(2)
            val revurderingutbetaling = inspektør.utbetaling(3)

            førsteUtbetaling.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(NY, linje.endringskode)
                assertEquals(17.januar, linje.fom)
                assertEquals(26.januar, linje.tom)
            }

            assertEquals(1, andreUtbetaling.arbeidsgiverOppdrag.size)
            assertEquals(0, andreUtbetaling.personOppdrag.size)
            andreUtbetaling.arbeidsgiverOppdrag.single().inspektør.also { linje ->
                assertEquals(NY, linje.endringskode)
                assertEquals(29.januar, linje.fom)
                assertEquals(11.februar, linje.tom)
            }

            val arbeidsgiverOppdrag = outOfOrderUtbetaling.arbeidsgiverOppdrag
            assertTrue(arbeidsgiverOppdrag.isEmpty())

            assertEquals(1, revurderingutbetaling.arbeidsgiverOppdrag.size)
            assertEquals(0, revurderingutbetaling.personOppdrag.size)
            revurderingutbetaling.arbeidsgiverOppdrag.single().inspektør.also { linje ->
                assertEquals(UEND, linje.endringskode)
                assertEquals(29.januar, linje.fom)
                assertEquals(11.februar, linje.tom)
            }

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET)
        }
    }

    @Test
    fun `hører til samme arbeidsgiverperiode som forrige - har en fremtidig utbetaling`() {
        a1 {
            nyttVedtak(januar)
            nyttVedtak(april)

            nyPeriode(10.februar til 28.februar)
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 10.februar
            )
            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_GODKJENNING)

            inspektør.utbetaling(0).also { inspektør ->
                assertEquals(1, inspektør.arbeidsgiverOppdrag.size)
                assertEquals(NY, inspektør.arbeidsgiverOppdrag[0].inspektør.endringskode)
                assertEquals(17.januar, inspektør.arbeidsgiverOppdrag[0].inspektør.fom)
                assertEquals(31.januar, inspektør.arbeidsgiverOppdrag[0].inspektør.tom)
            }

            inspektør.utbetaling(1).also { inspektør ->
                assertEquals(1, inspektør.arbeidsgiverOppdrag.size)
                assertEquals(NY, inspektør.arbeidsgiverOppdrag[0].inspektør.endringskode)
                assertEquals(17.april, inspektør.arbeidsgiverOppdrag[0].inspektør.fom)
                assertEquals(30.april, inspektør.arbeidsgiverOppdrag[0].inspektør.tom)
            }

            inspektør.utbetaling(2).also { inspektør ->
                assertEquals(1, inspektør.arbeidsgiverOppdrag.size)
                assertEquals(NY, inspektør.arbeidsgiverOppdrag[0].inspektør.endringskode)
                assertEquals(10.februar, inspektør.arbeidsgiverOppdrag[0].inspektør.fom)
                assertEquals(28.februar, inspektør.arbeidsgiverOppdrag[0].inspektør.tom)
            }
        }
    }

    @Test
    fun `out-of-order søknad medfører revurdering -- AvventerVilkårsprøving`() {
        a1 {
            nyPeriode(1.mars til 10.mars)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(20.februar til 7.mars)
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            nyPeriode(1.januar til 18.januar)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertUtbetalingsbeløp(
                1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1431,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 5.februar til 10.februar
            )
        }
    }

    @Test
    fun `out of order periode med kort gap - utbetalingen på revurderingen får korrekt beløp`() {
        a1 {
            nyttVedtak(februar)
            nyttVedtak(1.januar til 25.januar)
            håndterYtelser(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

            assertEquals(3, inspektør.antallUtbetalinger)
            val nettoBeløpForFebruarMedStandardInntektOgAgp = inspektør.utbetaling(0).nettobeløp
            val antallSykedagerIFebruar2018 = 20
            val nettoBeløpForFebruarMedStandardInntektUtenAgp = 1431 * antallSykedagerIFebruar2018
            val revurdering = inspektør.sisteUtbetaling()
            assertEquals(nettoBeløpForFebruarMedStandardInntektUtenAgp - nettoBeløpForFebruarMedStandardInntektOgAgp, revurdering.nettobeløp)
        }
    }

    @Test
    fun `out of order periode med langt gap - utbetalingen på revurderingen får korrekt beløp`() {
        a1 {
            nyttVedtak(mars)
            nyttVedtak(1.januar til 25.januar)
            håndterYtelser(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

            assertEquals(3, inspektør.antallUtbetalinger)
            val revurdering = inspektør.sisteUtbetaling()
            assertEquals(0, revurdering.nettobeløp)
        }
    }

    @Test
    fun `out of order periode med 18 dagers gap - revurderingen er uten endringer`() {
        a1 {
            nyttVedtak(19.februar til 15.mars)
            nyttVedtak(januar)
            håndterYtelser(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

            assertEquals(3, inspektør.antallUtbetalinger)
            val førsteUtbetaling = inspektør.utbetaling(0)
            val andreUtbetaling = inspektør.utbetaling(1)
            val revurdering = inspektør.utbetaling(2)

            assertNotEquals(førsteUtbetaling.korrelasjonsId, andreUtbetaling.korrelasjonsId)
            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
            assertEquals(0, revurdering.nettobeløp)
            assertEquals(UEND, revurdering.arbeidsgiverOppdrag.inspektør.endringskode)
        }
    }

    @Test
    fun `out of order periode med 15 dagers gap - mellom to perioder`() {
        a1 {
            nyPeriode(1.januar til 15.januar)
            nyttVedtak(29.januar til 15.februar)

            nyPeriode(17.januar til 25.januar)

            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 15.januar, 17.januar til 17.januar),
                vedtaksperiodeId = 3.vedtaksperiode
            )

            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)

            inspektør.utbetaling(0).also { førsteVedtak ->
                førsteVedtak.arbeidsgiverOppdrag.also { oppdraget ->
                    assertEquals(1, oppdraget.size)
                    assertEquals(NY, oppdraget.inspektør.endringskode)
                    oppdraget.single().inspektør.also { linje1 ->
                        assertEquals(30.januar, linje1.fom)
                        assertEquals(15.februar, linje1.tom)
                        assertEquals(1431, linje1.beløp)
                        assertEquals(NY, linje1.endringskode)
                        assertEquals(1, linje1.delytelseId)
                    }
                }
            }
            inspektør.utbetaling(1).also { outOfOrderUtbetalingen ->
                assertEquals(Utbetalingtype.UTBETALING, outOfOrderUtbetalingen.type)
                outOfOrderUtbetalingen.arbeidsgiverOppdrag.also { oppdraget ->
                    assertEquals(NY, oppdraget.inspektør.endringskode)
                    assertEquals(1, oppdraget.size)
                    oppdraget[0].inspektør.also { linje1 ->
                        assertEquals(18.januar, linje1.fom)
                        assertEquals(25.januar, linje1.tom)
                        assertEquals(1431, linje1.beløp)
                        assertEquals(1, linje1.delytelseId)
                        assertEquals(NY, linje1.endringskode)
                    }
                }
            }
            inspektør.utbetaling(2).also { revurderingen ->
                assertEquals(Utbetalingtype.REVURDERING, revurderingen.type)
                revurderingen.arbeidsgiverOppdrag.also { oppdraget ->
                    assertEquals(ENDR, oppdraget.inspektør.endringskode)
                    assertEquals(1, oppdraget.size)
                    oppdraget[0].inspektør.also { linje1 ->
                        assertEquals(29.januar, linje1.fom)
                        assertEquals(15.februar, linje1.tom)
                        assertEquals(1431, linje1.beløp)
                        assertEquals(2, linje1.delytelseId)
                        assertEquals(1, linje1.refDelytelseId)
                        assertEquals(NY, linje1.endringskode)
                    }
                }
            }

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `out of order periode rett før - mellom to perioder - arbeidsgiverperioden slutter tidligere`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar))
            håndterSøknad(1.januar til 15.januar)

            håndterSykmelding(Sykmeldingsperiode(29.januar, 15.februar))
            håndterSøknad(Sykdom(29.januar, 15.februar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 15.januar, 29.januar til 29.januar),
                vedtaksperiodeId = 2.vedtaksperiode
            )

            assertEquals(1.januar til 15.januar, inspektør.periode(1.vedtaksperiode))
            assertEquals(16.januar til 15.februar, inspektør.periode(2.vedtaksperiode))

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSykmelding(Sykmeldingsperiode(17.januar, 28.januar))
            håndterSøknad(17.januar til 28.januar)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `out of order periode rett før - mellom to perioder - arbeidsgiverperioden var ferdig`() {
        a1 {
            nyPeriode(1.januar til 16.januar)

            nyPeriode(29.januar til 15.februar)
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 29.januar
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            nullstillTilstandsendringer()
            nyPeriode(17.januar til 28.januar)

            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()
            håndterYtelser(2.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertTilstander(
                3.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )

            assertEquals(3, inspektør.antallUtbetalinger)
            val februarUtbetaling = inspektør.utbetaling(0)
            val januarUtbetaling = inspektør.utbetaling(1)
            val februarRevurderingUtbetaling = inspektør.utbetaling(2)

            assertEquals(1, februarUtbetaling.arbeidsgiverOppdrag.size)
            februarUtbetaling.arbeidsgiverOppdrag[0].also { linje ->
                assertEquals(29.januar til 15.februar, linje.fom til linje.tom)
                assertEquals(1431, linje.beløp)
                assertEquals(NY, linje.endringskode)
            }
            assertEquals(1, januarUtbetaling.arbeidsgiverOppdrag.size)
            januarUtbetaling.arbeidsgiverOppdrag[0].also { linje ->
                assertEquals(17.januar til 28.januar, linje.fom til linje.tom)
                assertEquals(1431, linje.beløp)
            }
            assertEquals(1, februarRevurderingUtbetaling.arbeidsgiverOppdrag.size)
            februarRevurderingUtbetaling.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                assertEquals(29.januar til 15.februar, linje.fom til linje.tom)
                assertEquals(1431, linje.beløp)
                assertEquals(UEND, linje.endringskode)
            }
        }
    }

    @Test
    fun `out of order periode trigger revurdering`() {
        a1 {
            nyttVedtak(mai)
            forlengVedtak(juni)
            nullstillTilstandsendringer()
            nyttVedtak(januar)

            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `kort out of order-periode medfører at senere kort periode skal omgjøres`() {
        a1 {
            nyPeriode(16.januar til 30.januar)
            nullstillTilstandsendringer()
            nyPeriode(1.januar til 15.januar)
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `out of order periode uten utbetaling trigger revurdering`() {
        a1 {
            nyttVedtak(mai)
            forlengVedtak(juni)
            nullstillTilstandsendringer()
            nyPeriode(1.januar til 15.januar)

            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `out of order periode uten utbetaling trigger revurdering -- flere ag`() {
        (a1 og a2).nyeVedtak(mai)
        nullstillTilstandsendringer()
        nyPeriode(1.januar til 15.januar, a1)

        a1 {
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
        a2 { assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING) }
    }

    @Test
    fun `Burde revurdere utbetalt periode dersom det kommer en eldre periode fra en annen AG`() {
        a2 { nyttVedtak(mars) }
        a1 { nyPeriode(januar) }

        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
    }

    @Test
    fun `out of order som overlapper med eksisterende -- flere ag`() {
        a2 { nyttVedtak(mars) }
        a1 {
            nyPeriode(20.februar til 15.mars)
            håndterArbeidsgiveropplysninger(
                listOf(20.februar til 7.mars),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        assertEquals(1, inspektør(a1).antallUtbetalinger)
        assertEquals(2, inspektør(a2).antallUtbetalinger)
        val a1Utbetaling = inspektør(a1).utbetaling(0)
        val a2FørsteUtbetaling = inspektør(a2).utbetaling(0)
        val a2AndreUtbetaling = inspektør(a2).utbetaling(1)

        assertEquals(a1Utbetaling.tilstand, Utbetalingstatus.UTBETALT)
        assertEquals(a2FørsteUtbetaling.tilstand, Utbetalingstatus.UTBETALT)
        assertEquals(a2AndreUtbetaling.tilstand, Utbetalingstatus.UTBETALT)
        assertEquals(a2FørsteUtbetaling.korrelasjonsId, a2AndreUtbetaling.korrelasjonsId)
        a2FørsteUtbetaling.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(1, oppdrag.size)
            val linje = oppdrag.single().inspektør
            assertEquals(17.mars, linje.fom)
            assertEquals(31.mars, linje.tom)
            assertEquals(1431, linje.beløp)
        }
        a2AndreUtbetaling.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(1, oppdrag.size)
            val linje = oppdrag.single().inspektør
            assertEquals(17.mars, linje.fom)
            assertEquals(31.mars, linje.tom)
            assertEquals(1080, linje.beløp)
        }
        a1Utbetaling.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(1, oppdrag.size)
            val linje = oppdrag.single().inspektør
            assertEquals(8.mars, linje.fom)
            assertEquals(15.mars, linje.tom)
            assertEquals(1080, linje.beløp)
        }

        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
    }

    @Test
    fun `out of order som overlapper med eksisterende`() {
        a1 {
            nyttVedtak(mars)
            nullstillTilstandsendringer()
            nyPeriode(20.februar til 15.mars)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertEquals(20.februar til 31.mars, inspektør.periode(1.vedtaksperiode))
            assertVarsler(listOf(Varselkode.RV_IV_7, RV_SØ_13), 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `revurdering av senere frittstående periode hos ag3 mens overlappende out of order hos ag1 og ag2 utbetales`() {
        val inntekt = 20000.månedlig
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.april, 30.april))
            håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.april til 16.april),
                beregnetInntekt = inntekt,
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        nyPeriode(februar, a2)
        nyPeriode(februar, a3)
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a3 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }

        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                beregnetInntekt = inntekt,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a3 {
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                beregnetInntekt = inntekt,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING) }
        a3 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }

        a2 {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a3 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK) }
    }

    @Test
    fun `revurdering av senere frittstående periode hos ag3 mens overlappende out of order hos ag1 og ag2 utbetales -- forlengelse`() {
        val inntekt = 20000.månedlig
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.april, 30.april))
            håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.april til 16.april),
                beregnetInntekt = inntekt,
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        nyPeriode(februar, a2)
        nyPeriode(februar, a3)

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a3 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }

        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                beregnetInntekt = inntekt,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a3 {
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                beregnetInntekt = inntekt,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a3 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a3 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }

        // forlengelseTilGodkjenning for a2 and a3:
        a2 { nyPeriode(1.mars til 15.mars) }
        a3 { nyPeriode(1.mars til 15.mars) }
        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, TIL_UTBETALING)
        }
        a3 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `out of order periode mens senere periode revurderes til utbetaling`() {
        a1 {
            nyttVedtak(mai)
            forlengelseTilGodkjenning(juni)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            nullstillTilstandsendringer()
            nyPeriode(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 3.vedtaksperiode
            )

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)

            nullstillTilstandsendringer()
            håndterUtbetalt()

            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
            assertNotNull(observatør.avsluttetMedVedtakEvent[2.vedtaksperiode])
        }
    }

    @Test
    fun `første periode i til utbetaling når det dukker opp en out of order-periode`() {
        a1 {
            val id = tilGodkjenning(mars)
            håndterUtbetalingsgodkjenning(id)
            nullstillTilstandsendringer()
            nyPeriode(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 2.vedtaksperiode
            )

            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING)
            håndterUtbetalt()

            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertNotNull(observatør.avsluttetMedVedtakEvent[1.vedtaksperiode])

            nullstillTilstandsendringer()

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertTilstander(
                2.vedtaksperiode,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVSLUTTET)
        }
    }

    @Test
    fun `første periode i til utbetaling når det dukker opp en out of order-periode - utbetalingen feiler`() {
        a1 {
            val id = tilGodkjenning(mars)
            håndterUtbetalingsgodkjenning(id)
            nyPeriode(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            nullstillTilstandsendringer()
            håndterUtbetalt(Oppdragstatus.AVVIST)

            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING_TIL_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertNull(observatør.avsluttetMedVedtakEvent[1.vedtaksperiode])
        }
    }

    @Test
    fun `kort periode, lang periode kommer out of order - kort periode trenger ikke å sendes til saksbehandler`() {
        a1 {
            nyPeriode(1.mars til 16.mars)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            nyPeriode(januar)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)

            assertIngenInfo("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding", 1.vedtaksperiode.filter())

            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `kort periode, lang periode kommer out of order og fører til utbetaling på kort periode som nå trenger IM`() {
        a1 {
            håndterSykmelding(1.mars til 16.mars)
            håndterSøknad(1.mars til 16.mars)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            håndterSykmelding(1.februar til 25.februar)
            håndterSøknad(1.februar til 25.februar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterArbeidsgiveropplysninger(
                listOf(1.februar til 16.februar),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterInntektsmelding(
                listOf(1.februar til 16.februar),
                førsteFraværsdag = 1.mars
            )

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `out-of-order med error skal ikke medføre revurdering`() {
        a1 {
            nyttVedtak(mars)
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = true)

            assertFunksjonellFeil(RV_SØ_10, 2.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `out-of-order som fører til nådd maksdato skal avslå riktige dager`() {
        medMaksSykedager(16, fødselsdato = 1.januar(1992))

        a1 {
            nyttVedtak(1.januar til 30.januar)
            assertEquals(6, inspektør.sisteMaksdato(1.vedtaksperiode).gjenståendeDager)

            nyttVedtak(1.mai til 24.mai)
            assertEquals(0, inspektør.sisteMaksdato(2.vedtaksperiode).gjenståendeDager)

            nyttVedtak(1.mars til 26.mars)
            håndterYtelser(2.vedtaksperiode)

            // Når out-of-order perioden for mars kommer inn, så er det dager i mai som skal bli avvist pga maksdato
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            assertEquals(0, inspektør.sisteMaksdato(3.vedtaksperiode).gjenståendeDager)
            assertEquals(0, inspektør.utbetalingstidslinjer(3.vedtaksperiode).inspektør.avvistDagTeller)
            assertEquals(6, inspektør.utbetalingstidslinjer(2.vedtaksperiode).inspektør.avvistDagTeller)
        }
    }

    @Test
    fun `Warning ved out-of-order - én warning for perioden som trigger out-of-order, én warning for de som blir påvirket av out-of-order`() {
        a1 {
            nyttVedtak(mars)
            forlengVedtak(april)
            forlengVedtak(mai)

            nyttVedtak(januar)
        }
    }

    @Test
    fun `Warning ved out-of-order - dukker ikke opp i revurderinger som ikke er out-of-order`() {
        a1 {
            nyttVedtak(mars)
            forlengVedtak(april)
            forlengVedtak(mai)

            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.mars, Sykedag, 50)))

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)

            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
            assertVarsler(emptyList(), 3.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Out of order kastes ut når det finnes en forkastet periode senere i tid`() {
        a1 {
            tilGodkjenning(1.februar til 25.februar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, false)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

            nyPeriode(1.januar til 25.januar)
            assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Korte perioder skal ikke revurderes dersom de forblir innenfor AGP`() {
        a1 {
            nyPeriode(1.mars til 10.mars)
            nyPeriode(11.mars til 16.mars)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            nyttVedtak(januar)

            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Out of order gjør at AUU revurderes fordi de ikke lenger er innen AGP - ber om inntektsmelding`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 10.mars))
            håndterSøknad(1.mars til 10.mars)

            håndterSykmelding(Sykmeldingsperiode(11.mars, 16.mars))
            håndterSøknad(11.mars til 16.mars)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            nyttVedtak(1.februar til 25.februar, arbeidsgiverperiode = listOf(1.februar til 16.februar))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterInntektsmelding(
                listOf(1.februar til 16.februar),
                førsteFraværsdag = 1.mars
            )
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

            håndterYtelser(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Out of order som overlapper med annen AG i trippeloverlapp og flytter skjæringstidspunktet - nærmere enn 18 dager fra neste - da gjenbruker vi tidsnære opplysninger`() {
        nyPeriode(mars, a1)
        nyPeriode(20.mars til 20.april, a2)

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(20.mars til 4.april),
                vedtaksperiodeId = 1.vedtaksperiode
            )
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

        // siden perioden slutter på en fredag starter ikke oppholdstelling i arbeidsgiverperioden før mandagen.
        // 10.februar-2.mars hører derfor til samme arbeidsgiverperioden som 20.mars-4.april, ettersom avstanden mellom
        // 5.mars (påfølgende mandag)-20.mars er akkurat 16 dager
        
        nyPeriode(10.februar til 2.mars, a2)
        observatør.assertEtterspurt(2.vedtaksperiode(a2), EventSubscription.Refusjon::class, EventSubscription.Arbeidsgiverperiode::class)

        a2 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }

        a2 {
            håndterInntektsmelding(listOf(10.februar til 25.februar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertVarsel(RV_VV_2, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `to perioder på rad kommer out of order - skal revuderes i riktig rekkefølge`() {
        a1 {
            nyttVedtak(mars)
            val marsId = 1.vedtaksperiode
            assertSisteTilstand(marsId, AVSLUTTET)

            nyPeriode(februar)
            val februarId = 2.vedtaksperiode

            håndterInntektsmelding(listOf(1.februar til 16.februar))
            håndterVilkårsgrunnlag(februarId)
            håndterYtelser(februarId)
            håndterSimulering(februarId)
            håndterUtbetalingsgodkjenning(februarId)
            håndterUtbetalt()

            håndterYtelser(marsId)
            håndterSimulering(marsId)

            assertSisteTilstand(marsId, AVVENTER_GODKJENNING_REVURDERING)
            assertSisteTilstand(februarId, AVSLUTTET)

            nyPeriode(januar)
            val januarId = 3.vedtaksperiode

            assertSisteTilstand(januarId, AVVENTER_VILKÅRSPRØVING)

            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(januarId)
            håndterYtelser(januarId)
            håndterSimulering(januarId)
            håndterUtbetalingsgodkjenning(januarId)
            håndterUtbetalt()

            assertSisteTilstand(januarId, AVSLUTTET)
            assertSisteTilstand(februarId, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(marsId, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `out-of-order søknad medfører revurdering`() {
        a1 {
            nyttVedtak(februar)
            nyPeriode(januar)

            val februarId = 1.vedtaksperiode
            val januarId = 2.vedtaksperiode

            assertSisteTilstand(februarId, AVVENTER_REVURDERING)
            assertSisteTilstand(januarId, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlag(januarId)

            håndterYtelser(januarId)
            håndterSimulering(januarId)
            håndterUtbetalingsgodkjenning(januarId)
            håndterUtbetalt()

            assertSisteTilstand(februarId, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(januarId, AVSLUTTET)
            håndterYtelser(februarId)
            håndterSimulering(februarId)
            håndterUtbetalingsgodkjenning(februarId)
            håndterUtbetalt()
            assertEquals(1.januar, inspektør.skjæringstidspunkt(februarId))
            assertUtbetalingsbeløp(
                januarId,
                forventetArbeidsgiverbeløp = 1431,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 17.januar til 31.januar
            )
            assertUtbetalingsbeløp(
                februarId,
                forventetArbeidsgiverbeløp = 1431,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = februar
            )
        }
    }

    @Test
    fun `en kort out-of-order søknad som flytter skjæringstidspunkt skal trigge revurdering`() {
        a1 {
            nyttVedtak(februar)
            nullstillTilstandsendringer()
            nyPeriode(20.januar til 31.januar)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `out-of-order søknad medfører revurdering -- AvsluttetUtenUtbetaling`() {
        a1 {
            nyPeriode(1.februar til 10.februar)
            nyPeriode(januar)

            val februarId = 1.vedtaksperiode
            val januarId = 2.vedtaksperiode

            assertSisteTilstand(februarId, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(januarId, AVVENTER_INNTEKTSMELDING)

            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(januarId)
            håndterYtelser(januarId)
            håndterSimulering(januarId)
            håndterUtbetalingsgodkjenning(januarId)
            håndterUtbetalt()

            assertSisteTilstand(februarId, AVVENTER_HISTORIKK)
            assertSisteTilstand(januarId, AVSLUTTET)

            håndterYtelser(februarId)
            håndterSimulering(februarId)
            håndterUtbetalingsgodkjenning(februarId)
            håndterUtbetalt()

            assertSisteTilstand(januarId, AVSLUTTET)
            assertSisteTilstand(februarId, AVSLUTTET)
            assertUtbetalingsbeløp(
                januarId,
                forventetArbeidsgiverbeløp = 1431,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 17.januar til 31.januar
            )
            assertUtbetalingsbeløp(
                februarId,
                forventetArbeidsgiverbeløp = 1431,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = februar
            )
        }
    }
}
