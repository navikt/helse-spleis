package no.nav.helse.spleis.e2e

import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.TilstandType.UTBETALING_FEILET
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingFeiletE2ETest : AbstractEndToEndTest() {

    @Test
    fun `revurdering feilet med ett oppdrag status avvist som bygger på tidligere`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(Oppdragstatus.AVVIST)

        håndterPåminnelse(2.vedtaksperiode, UTBETALING_FEILET)

        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        val første = inspektør.utbetaling(0)
        assertEquals(Utbetalingstatus.FORKASTET, inspektør.utbetalingtilstand(2))
        inspektør.utbetaling(3).inspektør.also { utbetalingInspektør ->
            assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertEquals(Endringskode.ENDR, utbetalingInspektør.arbeidsgiverOppdrag.inspektør.endringskode)
            assertEquals(2, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag.harUtbetalinger())
            assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].fom)
            assertEquals(30.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].tom)
            assertEquals(1.februar, utbetalingInspektør.arbeidsgiverOppdrag[1].fom)
            assertEquals(28.februar, utbetalingInspektør.arbeidsgiverOppdrag[1].tom)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag[0].erForskjell())
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag[1].erForskjell())
        }
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            UTBETALING_FEILET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status avvist`() {
        tilGodkjent(1.januar, 31.januar, 100.prosent, 1.januar)
        håndterUtbetalt(status = Oppdragstatus.AVVIST)
        nullstillTilstandsendringer()
        håndterPåminnelse(1.vedtaksperiode, UTBETALING_FEILET)
        håndterYtelser(1.vedtaksperiode)
        assertEquals(Utbetalingstatus.FORKASTET, inspektør.utbetalingtilstand(0))
        inspektør.utbetaling(1).inspektør.also { utbetalingInspektør ->
            assertForventetFeil(
                forklaring = "cornercase kanskje håndtere på en annen måte?",
                nå = {
                    assertNotEquals(
                        utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(),
                        inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId()
                    )
                },
                ønsket = {
                    assertEquals(
                        utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(),
                        inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId()
                    )
                }
            )
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag.harUtbetalinger())
            assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].fom)
            assertEquals(31.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].tom)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag[0].erForskjell())
        }
        assertTilstander(1.vedtaksperiode, UTBETALING_FEILET, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status avvist som bygger på tidligere`() {
        nyttVedtak(1.januar, 31.januar)
        forlengTilGodkjentVedtak(1.februar, 28.februar)
        håndterUtbetalt(status = Oppdragstatus.AVVIST)
        nullstillTilstandsendringer()
        håndterPåminnelse(2.vedtaksperiode, UTBETALING_FEILET)
        håndterYtelser(2.vedtaksperiode)
        val første = inspektør.utbetaling(0)
        assertEquals(Utbetalingstatus.FORKASTET, inspektør.utbetalingtilstand(1))
        inspektør.utbetaling(2).inspektør.also { utbetalingInspektør ->
            assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag.harUtbetalinger())
            assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].fom)
            assertEquals(28.februar, utbetalingInspektør.arbeidsgiverOppdrag[0].tom)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag[0].erForskjell())
        }
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, UTBETALING_FEILET, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status avvist og ett som er ok`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INNTEKT/2, null, emptyList()))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(status = Oppdragstatus.AKSEPTERT, fagsystemId = inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        håndterUtbetalt(status = Oppdragstatus.AVVIST, fagsystemId = inspektør.utbetaling(0).inspektør.personOppdrag.inspektør.fagsystemId())
        nullstillTilstandsendringer()
        håndterPåminnelse(1.vedtaksperiode, UTBETALING_FEILET)
        håndterYtelser(1.vedtaksperiode)
        val første = inspektør.utbetaling(0)
        val andre = inspektør.utbetaling(1)
        håndterSimulering(1.vedtaksperiode, andre.inspektør.utbetalingId, andre.inspektør.personOppdrag.inspektør.fagsystemId(), Fagområde.Sykepenger)
        assertEquals(Utbetalingstatus.FORKASTET, første.inspektør.tilstand)
        andre.inspektør.also { utbetalingInspektør ->
            assertForventetFeil(
                forklaring = "cornercase som vi løser på sikt",
                nå = {
                    assertNotEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
                    assertNotEquals(utbetalingInspektør.personOppdrag.inspektør.fagsystemId(), første.inspektør.personOppdrag.inspektør.fagsystemId())
                    assertTilstander(1.vedtaksperiode, UTBETALING_FEILET, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
                },
                ønsket = {
                    assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
                    assertEquals(utbetalingInspektør.personOppdrag.inspektør.fagsystemId(), første.inspektør.personOppdrag.inspektør.fagsystemId())
                    assertFalse(utbetalingInspektør.arbeidsgiverOppdrag.harUtbetalinger())
                    assertFalse(utbetalingInspektør.arbeidsgiverOppdrag[0].erForskjell())
                    assertTilstander(1.vedtaksperiode, UTBETALING_FEILET, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
                }

            )
            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertEquals(1, utbetalingInspektør.personOppdrag.size)

            assertTrue(utbetalingInspektør.personOppdrag.harUtbetalinger())
            assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].fom)
            assertEquals(31.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].tom)

            assertEquals(17.januar, utbetalingInspektør.personOppdrag[0].fom)
            assertEquals(31.januar, utbetalingInspektør.personOppdrag[0].tom)
            assertTrue(utbetalingInspektør.personOppdrag[0].erForskjell())
        }
    }

    @Test
    fun `utbetaling feilet med ett oppdrag status ok og ett som er avvist`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INNTEKT/2, null, emptyList()))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(status = Oppdragstatus.AVVIST, fagsystemId = inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        håndterUtbetalt(status = Oppdragstatus.AKSEPTERT, fagsystemId = inspektør.utbetaling(0).inspektør.personOppdrag.inspektør.fagsystemId())
        nullstillTilstandsendringer()
        håndterPåminnelse(1.vedtaksperiode, UTBETALING_FEILET)
        håndterYtelser(1.vedtaksperiode)
        val første = inspektør.utbetaling(0)
        val andre = inspektør.utbetaling(1)
        håndterSimulering(1.vedtaksperiode, andre.inspektør.utbetalingId, andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), Fagområde.SykepengerRefusjon)
        assertEquals(Utbetalingstatus.FORKASTET, første.inspektør.tilstand)
        andre.inspektør.also { utbetalingInspektør ->
            assertForventetFeil(
                forklaring = "cornercase kanskje håndtere på en annen måte",
                nå = {
                    assertNotEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
                    assertNotEquals(utbetalingInspektør.personOppdrag.inspektør.fagsystemId(), første.inspektør.personOppdrag.inspektør.fagsystemId())
                    assertTrue(utbetalingInspektør.personOppdrag.harUtbetalinger())
                    assertTilstander(1.vedtaksperiode, UTBETALING_FEILET, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)

                },
                ønsket = {
                    assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
                    assertEquals(utbetalingInspektør.personOppdrag.inspektør.fagsystemId(), første.inspektør.personOppdrag.inspektør.fagsystemId())
                    assertFalse(utbetalingInspektør.personOppdrag.harUtbetalinger())
                    assertFalse(utbetalingInspektør.personOppdrag[0].erForskjell())
                    assertTilstander(1.vedtaksperiode, UTBETALING_FEILET, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
                }


            )

            assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
            assertEquals(1, utbetalingInspektør.personOppdrag.size)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag.harUtbetalinger())

            assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].fom)
            assertEquals(31.januar, utbetalingInspektør.arbeidsgiverOppdrag[0].tom)
            assertTrue(utbetalingInspektør.arbeidsgiverOppdrag[0].erForskjell())
            assertEquals(17.januar, utbetalingInspektør.personOppdrag[0].fom)
            assertEquals(31.januar, utbetalingInspektør.personOppdrag[0].tom)
        }

    }

    @Test
    fun `nyere perioder må vente til periode med feilet utbetaling er ok`() {
        nyttVedtak(1.januar, 31.januar, status = Oppdragstatus.AVVIST)
        nyPeriode(1.mars til 31.mars)
        håndterInntektsmelding(listOf(1.mars til 16.mars))

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, UTBETALING_FEILET)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
    }
}
