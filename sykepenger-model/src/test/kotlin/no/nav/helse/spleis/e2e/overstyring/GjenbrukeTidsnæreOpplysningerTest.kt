package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass
import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertArbeidsgiverInntektsopplysning
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Dagtype.Pleiepengerdag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Melding
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mandag
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.person.inntekt.InntektsgrunnlagView
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.manuellSykedag
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import no.nav.helse.økonomi.inspectors.inspektør

internal class GjenbrukeTidsnæreOpplysningerTest : AbstractDslTest() {

    @Test
    fun `Stuckiness med helg involvert - da gjenbruker vi tidsnære opplysninger`() {
        a1 { håndterSykmelding(januar) }
        a2 {
            håndterSykmelding(1.januar til fredag(19.januar))
            håndterSykmelding(mandag(22.januar) til 31.januar)
        }
        a1 { håndterSøknad(januar) }
        a2 {
            håndterSøknad(1.januar til fredag(19.januar))
            håndterSøknad(mandag(22.januar) til 31.januar)
        }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 { assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode)) }
        a2 {
            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        }
        /**
         * Kjære leser av testen: Alt før dette er litt kjedelig, men nå blir det gøy
         * Ting ser slik ut:
         *
         *      A1 |---------------januar--------------|
         *      A2 |-----------|  |--------------------|
         *
         * - Det lille hullet vi ikke har søknad for på A2 er helg (lørdag-søndag)
         * - Nå kommer det en korrigerende inntektsmelding fra A1 som sier
         *   at AGP startet mandagen A2 sin andre periode starter
         *

         */
        a1 {
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
            håndterInntektsmelding(listOf(mandag(22.januar) til 6.februar))
            assertVarsel(Varselkode.RV_IM_24, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IV_7, 1.vedtaksperiode.filter())
            assertEquals("AAAAARR AAAAARR AAAAARR SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
        }
        a2 {
            assertEquals("SSSSSHH SSSSSHH SSSSS?? SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(1.januar, inspektør.førsteFraværsdag(1.vedtaksperiode))

            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertEquals(22.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(1.januar, inspektør.førsteFraværsdag(2.vedtaksperiode))
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `revurdere seg inn i en situasjon hvor man ikke har noen første fraværsdag, men gjenbrukbare opplysninger biter læll`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(12.februar, 28.februar))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), Ferie(1.mars, 14.mars))
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))

            håndterOverstyrTidslinje((15.mars til 31.mars).map { ManuellOverskrivingDag(it, Pleiepengerdag) })
            håndterYtelser(3.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 3.vedtaksperiode.filter())
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

            håndterOverstyrTidslinje((15.februar til 28.februar).map { ManuellOverskrivingDag(it, Pleiepengerdag) })
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `Først litt gjenbruk av tidsnære opplysninger etterfulgt av overstyring til andre ytelser som gjør at vi ikke trengte dem allikevel`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Svangerskapspengerdag)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
        }
        a2 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Svangerskapspengerdag)))
            assertEquals(listOf(1.februar), inspektør.skjæringstidspunkter(2.vedtaksperiode))
        }
        a1 {
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
        a1 {
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertEquals(1.februar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        }
        a2 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertEquals(1.februar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        }
        a1 {
            håndterOverstyrTidslinje(februar.map { ManuellOverskrivingDag(it, Dagtype.Svangerskapspengerdag) })
            assertEquals(1.februar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        }
        a2 {
            håndterOverstyrTidslinje(februar.map { ManuellOverskrivingDag(it, Dagtype.Svangerskapspengerdag) })
            assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        }
        a1 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `kort periode i forkant endrer skjæringstidspunktet tilbake - da gjenbruker vi tidsnære opplysninger`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 5.januar, 100.prosent))
            håndterSøknad(Sykdom(6.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(3.januar til 18.januar))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.januar, 2.januar, 100.prosent))
            assertVarsel(RV_IV_7, 2.vedtaksperiode.filter())
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            inspektør.sykdomstidslinje.inspektør.also { sykdomstidslinjeInspektør ->
                assertInstanceOf(Dag.Sykedag::class.java, sykdomstidslinjeInspektør[1.januar])
                assertInstanceOf(Dag.Sykedag::class.java, sykdomstidslinjeInspektør[2.januar])
            }
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `vedtaksperiode strekker seg tilbake og endrer skjæringstidspunktet`() {
        a1 {
            tilGodkjenning(10.januar til 31.januar)
            val vilkårsgrunnlagFørEndring = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!
            nullstillTilstandsendringer()
            håndterOverstyrTidslinje(
                listOf(
                    ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)
                )
            )

            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())
            assertEquals(9.januar til 31.januar, inspektør.periode(1.vedtaksperiode))

            val dagen = inspektør.sykdomstidslinje[9.januar]
            assertEquals(Dag.Sykedag::class, dagen::class)
            assertTrue(dagen.kommerFra(OverstyrTidslinje::class))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            val vilkårsgrunnlagEtterEndring = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!

            assertTidsnærInntektsopplysning(a1, vilkårsgrunnlagFørEndring.inspektør.inntektsgrunnlag, vilkårsgrunnlagEtterEndring.inspektør.inntektsgrunnlag)
            assertTilstander(
                1.vedtaksperiode,
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
            tilGodkjenning(10.januar til 31.januar, arbeidsgiverperiode = listOf(10.januar til 25.januar)) // 1. jan - 9. jan blir omgjort til arbeidsdager ved innsending av IM her
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            nullstillTilstandsendringer()
            // Saksbehandler korrigerer; 9.januar var vedkommende syk likevel
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)))

            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)

            assertSykdomstidslinjedag(9.januar, Dag.Sykedag::class, OverstyrTidslinje::class)
            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertEquals(1.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
            assertEquals(10.januar til 31.januar, inspektør.periode(2.vedtaksperiode))

            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `endrer skjæringstidspunkt på sykefraværstilfelle etter - endrer ikke dato for inntekter`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 9.januar, 100.prosent), Arbeid(1.januar, 9.januar))
            håndterSøknad(Sykdom(15.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(15.januar til 29.januar))
            assertVarsel(Varselkode.RV_IM_3, 2.vedtaksperiode.filter())
        }
        a2 {
            håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(10.januar til 25.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            nullstillTilstandsendringer()
        }
        val sykepengegrunnlagFør = a2 {
            inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
        }

        a1 {
            // Saksbehandler korrigerer; 9.januar var vedkommende syk likevel
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(9.januar, Dagtype.Sykedag, 100)))
            assertSykdomstidslinjedag(9.januar, Dag.Sykedag::class, OverstyrTidslinje::class)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

        }
        val sykepengegrunnlagEtter = a2 {
            inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
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
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `vedtaksperiode flytter skjæringstidspunktet frem`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje(
                listOf(
                    ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag, 100),
                    ManuellOverskrivingDag(4.januar, Dagtype.Arbeidsdag, 100),
                )
            )
            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(januar, inspektør.periode(1.vedtaksperiode))

            assertUtbetalingsdag(1.vedtaksperiode, 18.januar, Utbetalingsdag.ArbeidsgiverperiodeDag::class, Inntekt.INGEN, Inntekt.INGEN)
            assertUtbetalingsdag(1.vedtaksperiode, 19.januar, Utbetalingsdag.NavDag::class, 1431.daglig, Inntekt.INGEN)

            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `vedtaksperiode flytter skjæringstidspunktet frem etter utbetalt`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje(
                listOf(
                    ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag, 100),
                    ManuellOverskrivingDag(4.januar, Dagtype.Arbeidsdag, 100),
                )
            )
            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(RV_IV_7, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(januar, inspektør.periode(1.vedtaksperiode))

            assertUtbetalingsdag(1.vedtaksperiode, 18.januar, Utbetalingsdag.ArbeidsgiverperiodeDag::class, Inntekt.INGEN, Inntekt.INGEN)
            assertUtbetalingsdag(1.vedtaksperiode, 19.januar, Utbetalingsdag.NavDag::class, 1431.daglig, Inntekt.INGEN)

            val førsteUtbetaling = inspektør.utbetaling(0)
            val revurdering = inspektør.utbetaling(1)
            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        }
    }

    @Test
    fun `flytter arbeidsgiverperioden frem 16 dager etter utbetalt`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje((1.januar til 16.januar).map { dag ->
                ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
            })
            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertEquals(17.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(januar, inspektør.periode(1.vedtaksperiode))

            val førsteUtbetaling = inspektør.utbetaling(0)
            val revurdering = inspektør.utbetaling(1)
            val februarutbetaling = inspektør.utbetaling(2)

            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
            assertEquals(januar, revurdering.periode)

            assertNotEquals(førsteUtbetaling.korrelasjonsId, februarutbetaling.korrelasjonsId)
            assertEquals(februar, februarutbetaling.periode)

            assertTilstander(
                1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING,
                AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET
            )
        }
    }

    @Test
    fun `flytter arbeidsgiverperioden frem 10 dager etter utbetalt`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje((1.januar til 10.januar).map { dag ->
                ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
            })
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(RV_IV_7, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertSykdomstidslinjedag(1.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertSykdomstidslinjedag(4.januar, Dag.Arbeidsdag::class, OverstyrTidslinje::class)
            assertEquals(11.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(januar, inspektør.periode(1.vedtaksperiode))

            val førsteUtbetaling = inspektør.utbetaling(0)
            val revurdering = inspektør.utbetaling(1)
            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
            assertEquals(januar, revurdering.periode)
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

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        }
    }

    @Test
    fun `flytter arbeidsgiverperioden frem 10 dager etter utbetalt - med tidligere utbetalt vedtak`() {
        a1 {
            nyttVedtak(januar)

            håndterSøknad(mars)
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje((1.mars til 10.mars).map { dag ->
                ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
            })
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            assertVarsel(RV_IV_7, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            assertVarsler(listOf(RV_IV_7, Varselkode.RV_UT_23), 2.vedtaksperiode.filter())

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertEquals(11.mars, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(mars, inspektør.periode(2.vedtaksperiode))

            val førsteUtbetaling = inspektør.utbetaling(1)
            val revurdering = inspektør.utbetaling(2)
            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
            assertEquals(mars, revurdering.periode)
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
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        }
    }

    @Test
    fun `innfører ny arbeidsgiverperiode på en førstegangsbehandling etter tidligere utbetalt`() {
        a1 {
            nyttVedtak(januar)
            nyttVedtak(14.februar til 10.mars, arbeidsgiverperiode = listOf(1.januar til 16.januar))
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje((14.februar til 16.februar).map { dag ->
                ManuellOverskrivingDag(dag, Dagtype.Arbeidsdag, 100)
            })

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            assertVarsel(RV_IV_7, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertEquals(17.februar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(14.februar til 10.mars, inspektør.periode(2.vedtaksperiode))

            val januarutbetaling = inspektør.utbetaling(0)
            val februarutbetaling = inspektør.utbetaling(1)
            val revurderingFebruar = inspektør.utbetaling(2)

            assertNotEquals(januarutbetaling.korrelasjonsId, februarutbetaling.korrelasjonsId)
            assertEquals(januar, januarutbetaling.periode)
            assertEquals(14.februar til 10.mars, februarutbetaling.periode)

            februarutbetaling.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(1, oppdrag.size)
                oppdrag[0].inspektør.also { linje ->
                    assertEquals(14.februar til 9.mars, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
            assertEquals(0, februarutbetaling.personOppdrag.size)

            assertEquals(februarutbetaling.korrelasjonsId, revurderingFebruar.korrelasjonsId)
            assertEquals(14.februar til 10.mars, revurderingFebruar.periode)
            assertEquals(0, revurderingFebruar.personOppdrag.size)
            assertEquals(Endringskode.ENDR, revurderingFebruar.arbeidsgiverOppdrag.inspektør.endringskode)
            revurderingFebruar.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(2, oppdrag.size)
                oppdrag[0].inspektør.also { linje ->
                    assertEquals(14.februar til 9.mars, linje.fom til linje.tom)
                    assertEquals(Endringskode.ENDR, linje.endringskode)
                    assertEquals(14.februar, linje.datoStatusFom)
                }
                oppdrag[1].inspektør.also { linje ->
                    assertEquals(5.mars til 9.mars, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
        }
    }

    @Test
    fun `innfører ny arbeidsgiverperiode ved å lage feriedager på en førstegangsbehandling etter tidligere utbetalt`() {
        a1 {
            nyttVedtak(januar)
            nyttVedtak(14.februar til 10.mars, arbeidsgiverperiode = listOf(1.januar til 16.januar))
            nullstillTilstandsendringer()
            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }
            håndterOverstyrTidslinje((14.februar til 16.februar).map { dag ->
                ManuellOverskrivingDag(dag, Dagtype.Feriedag, 100)
            })

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            assertVarsler(listOf(RV_IV_7, Varselkode.RV_UT_23), 2.vedtaksperiode.filter())

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.inntektsgrunnlag ?: fail { "finner ikke vilkårsgrunnlag" }

            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)

            assertEquals(17.februar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(14.februar til 10.mars, inspektør.periode(2.vedtaksperiode))

            val januarutbetaling = inspektør.utbetaling(0)
            val februarutbetaling = inspektør.utbetaling(1)
            val revurderingFebruar = inspektør.utbetaling(2)

            assertNotEquals(januarutbetaling.korrelasjonsId, februarutbetaling.korrelasjonsId)
            assertEquals(januar, januarutbetaling.periode)
            assertEquals(14.februar til 10.mars, februarutbetaling.periode)

            februarutbetaling.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(1, oppdrag.size)
                oppdrag[0].inspektør.also { linje ->
                    assertEquals(14.februar til 9.mars, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
            assertEquals(0, februarutbetaling.personOppdrag.size)

            assertNotEquals(januarutbetaling.korrelasjonsId, revurderingFebruar.korrelasjonsId)
            assertEquals(februarutbetaling.korrelasjonsId, revurderingFebruar.korrelasjonsId)
            assertEquals(14.februar til 10.mars, revurderingFebruar.periode)
            assertEquals(0, revurderingFebruar.personOppdrag.size)
            assertEquals(2, revurderingFebruar.arbeidsgiverOppdrag.size)
            assertEquals(Endringskode.ENDR, revurderingFebruar.arbeidsgiverOppdrag.inspektør.endringskode)
            revurderingFebruar.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(2, oppdrag.size)
                oppdrag[0].inspektør.also { linje ->
                    assertEquals(14.februar til 9.mars, linje.fom til linje.tom)
                    assertEquals(Endringskode.ENDR, linje.endringskode)
                    assertEquals(14.februar, linje.datoStatusFom)
                }
                oppdrag[1].inspektør.also { linje ->
                    assertEquals(5.mars til 9.mars, linje.fom til linje.tom)
                    assertEquals(Endringskode.NY, linje.endringskode)
                }
            }
        }
    }

    @Test
    fun `innfører arbeidsdager på halen av første vedtaksperiode`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            val sykepengegrunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag

            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Arbeidsdag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            val sykepengegrunnlagEtter = inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.inspektør.inntektsgrunnlag
            assertVarsler(listOf(Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            assertTidsnærInntektsopplysning(a1, sykepengegrunnlagFør, sykepengegrunnlagEtter)
        }
    }

    @Test
    fun `varsel når det er 60 dager eller mer mellom ny og gammel første fraværsdag`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(mars)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(april)
            håndterYtelser(4.vedtaksperiode)
            håndterSimulering(4.vedtaksperiode)
            håndterUtbetalingsgodkjenning(4.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), Arbeid(20.mars, 31.mars))
            assertVarsel(RV_IV_7, 4.vedtaksperiode.filter())
        }
    }

    @Test
    fun `varsel når det er 16 dager eller mer opphold`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(mars)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Arbeid(13.februar, 28.februar))
            assertVarsel(RV_IV_7, 3.vedtaksperiode.filter())
        }
    }

    @Test
    fun `ikke varsel når det var opphold`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            nyttVedtak(5.mars til 31.mars, arbeidsgiverperiode = listOf(1.januar til 16.januar))

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Arbeid(13.februar, 28.februar))
            assertVarsler(emptyList(), 3.vedtaksperiode.filter())
        }
    }

    @Test
    fun `ikke varsel når det er en korrigert søknad som ikke endrer noe`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(30.januar, 31.januar))
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `ikke varsel når det er en korrigert søknad som ikke endrer noe ettersom inntektsdatoen er lik som før`() {
        a1 {
            nyttVedtak(januar)
            nyttVedtak(juni)
            håndterSøknad(Sykdom(1.juni, 30.juni, 100.prosent), Arbeid(29.juni, 30.juni))
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `ikke varsel når det er en forlengelse korrigeres til ferie`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `gjenbruker saksbehandlerinntekt`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT - 50.daglig, listOf(Triple(1.januar, null, INNTEKT))))
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Arbeid(20.januar, 31.januar))

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, INNTEKT - 50.daglig, forventetKorrigertInntekt = INNTEKT - 50.daglig)
            }
        }
    }

    @Test
    fun `gjenbruker saksbehandlerinntekt som overstyrer annen saksbehandler`() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT - 50.daglig, listOf(Triple(1.januar, null, INNTEKT)))))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)

            val andreOverstyring = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT - 500.daglig, listOf(Triple(1.januar, null, INNTEKT)))), meldingsreferanseId = andreOverstyring)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT - 500.daglig)))

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Arbeid(20.januar, 31.januar))

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterSkjønnsmessigFastsettelse(1.februar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT - 500.daglig)))

            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, INNTEKT - 500.daglig, forventetKorrigertInntekt = INNTEKT - 500.daglig)
            }
            assertInntektsgrunnlag(1.februar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT - 500.daglig, forventetFastsattÅrsinntekt = INNTEKT - 500.daglig)
            }
        }
    }

    @Test
    fun `gjenbruker grunnlaget for skjønnsmessig fastsettelse`() {
        a1 {
            val beregnetInntektIM = INNTEKT * 2
            val skjønnsmessigFastsattInntekt = INNTEKT * 1.5

            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntektIM)
            nullstillTilstandsendringer()
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK)
            nullstillTilstandsendringer()
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = skjønnsmessigFastsattInntekt)))
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Arbeid(20.januar, 31.januar))

            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterSkjønnsmessigFastsettelse(1.februar, listOf(OverstyrtArbeidsgiveropplysning(a1, beregnetInntektIM)))

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, beregnetInntektIM, forventetFastsattÅrsinntekt = skjønnsmessigFastsattInntekt)
            }
            assertInntektsgrunnlag(1.februar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, beregnetInntektIM, forventetFastsattÅrsinntekt = beregnetInntektIM)
            }
        }
    }

    @Test
    fun `ikke varsel når det er hull i agp`() {
        a1 {
            håndterSøknad(Sykdom(17.januar, 20.januar, 100.prosent))
            håndterSøknad(Sykdom(21.januar, 8.februar, 100.prosent))
            håndterSøknad(Sykdom(9.februar, 28.februar, 100.prosent))
            håndterInntektsmelding(
                listOf(
                    17.januar til 20.januar,
                    2.februar til 13.februar
                )
            )
            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknad(Sykdom(9.februar, 28.februar, 100.prosent), Ferie(28.februar, 28.februar))
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `gjenbruker den underliggende inntektsmeldingen ved skjønnsmessig fastsatt`() {
        a1 {
            // Planke
            håndterSøknad(Sykdom(8.januar, 31.januar, 100.prosent))
            val inntektsmeldingId = håndterInntektsmelding(listOf(8.januar til 23.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertInntektsgrunnlag(8.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, forventetKildeId = inntektsmeldingId)
            }

            // Skjønnsmessig fastsetter
            val skjønnsmessigId = UUID.randomUUID()
            håndterSkjønnsmessigFastsettelse(8.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.25)), meldingsreferanseId = skjønnsmessigId)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(8.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, forventetFastsattÅrsinntekt = INNTEKT * 1.25, forventetKildeId = inntektsmeldingId)
            }
            // Flytter skjæringstidspunkt ved å legge til sykdomsdager i snuten
            håndterOverstyrTidslinje((1.januar til 7.januar).map { manuellSykedag(it) })
            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(RV_IV_7, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, forventetKildeId = inntektsmeldingId)
            }
        }
    }

    @Test
    fun `gjenbruker den siste saksbehandlerinntekten om det er overstyrt mange ganger`() {
        a1 {
            // Planke
            håndterSøknad(Sykdom(8.januar, 31.januar, 100.prosent))
            val inntektsmeldingId = håndterInntektsmelding(listOf(8.januar til 23.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(8.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, forventetKildeId = inntektsmeldingId)
            }

            // Overstyrer en gang
            val overstyring1Id = UUID.randomUUID()
            val overstyring1Inntekt = INNTEKT * 1.05
            håndterOverstyrArbeidsgiveropplysninger(8.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, overstyring1Inntekt)), meldingsreferanseId = overstyring1Id)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(8.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, overstyring1Inntekt, forventetKorrigertInntekt = overstyring1Inntekt, forventetKildeId = inntektsmeldingId)
            }

            // Overstyrer en gang til
            val overstyring2Id = UUID.randomUUID()
            val overstyring2Inntekt = INNTEKT * 1.10
            håndterOverstyrArbeidsgiveropplysninger(8.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, overstyring2Inntekt)), meldingsreferanseId = overstyring2Id)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(8.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, overstyring2Inntekt, forventetKorrigertInntekt = overstyring2Inntekt, forventetKildeId = inntektsmeldingId)
            }

            // Flytter skjæringstidspunkt ved å legge til sykdomsdager i snuten
            håndterOverstyrTidslinje((1.januar til 7.januar).map { manuellSykedag(it) })
            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, overstyring2Inntekt, forventetKildeId = inntektsmeldingId)
            }
        }
    }

    private fun assertTidsnærInntektsopplysning(orgnummer: String, inntektsgrunnlagFør: InntektsgrunnlagView, inntektsgrunnlagEtter: InntektsgrunnlagView) {
        val inntektsopplysningerFørEndring = inntektsgrunnlagFør.inspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(orgnummer)
        val inntektsopplysningerEtterEndring = inntektsgrunnlagEtter.inspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(orgnummer)

        with (inntektsopplysningerFørEndring) {
            assertEquals(inspektør.faktaavklartInntekt.inspektør.hendelseId, inntektsopplysningerEtterEndring.inspektør.faktaavklartInntekt.inspektør.hendelseId)
            assertEquals(inspektør.faktaavklartInntekt.inspektør.tidsstempel, inntektsopplysningerEtterEndring.inspektør.faktaavklartInntekt.inspektør.tidsstempel)
            assertArbeidsgiverInntektsopplysning(inntektsopplysningerEtterEndring.omregnetÅrsinntekt.beløp, inntektsopplysningerEtterEndring.fastsattÅrsinntekt)
        }

    }

    private fun TestPerson.TestArbeidsgiver.assertSykdomstidslinjedag(dato: LocalDate, dagtype: KClass<out Dag>, kommerFra: Melding) {
        val dagen = inspektør.sykdomstidslinje[dato]
        assertEquals(dagtype, dagen::class)
        assertTrue(dagen.kommerFra(kommerFra))
    }

    private fun TestPerson.TestArbeidsgiver.assertUtbetalingsdag(vedtaksperiodeId: UUID, dato: LocalDate, dagtype: KClass<out Utbetalingsdag>, arbeidsgiverbeløp: Inntekt, personbeløp: Inntekt) {
        val utbetalingstidslinje = inspektør.utbetalingstidslinjer(vedtaksperiodeId)
        val dagen = utbetalingstidslinje[dato]

        assertEquals(dagtype, dagen::class)
        assertEquals(arbeidsgiverbeløp, dagen.økonomi.inspektør.arbeidsgiverbeløp)
        assertEquals(personbeløp, dagen.økonomi.inspektør.personbeløp)
    }
}
