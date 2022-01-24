package no.nav.helse.serde.api.dto

import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.api.DagtypeDTO
import no.nav.helse.serde.api.IkkeUtbetaltDagDTO
import no.nav.helse.serde.api.NavDagDTO
import no.nav.helse.serde.api.TilstandstypeDTO.*
import no.nav.helse.serde.api.UtbetalingstidslinjedagDTO
import no.nav.helse.serde.api.builders.OppdragDTO
import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO.UtbetalingDTO.Companion.tilstandFor
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.serde.reflection.Utbetalingstatus.*
import no.nav.helse.desember
import no.nav.helse.januar
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingDTOTest {
    @Test
    fun `tilstand for vedtaksperiode`() {
        val utbetaling = utbetaling(UTBETALT, listOf(1.NAV))
        assertEquals(Utbetalt, tilstandFor(1.januar til 31.januar, TilstandType.AVSLUTTET, utbetaling, emptyList()))
        assertEquals(Annullert, tilstandFor(1.januar til 31.januar, TilstandType.AVSLUTTET, utbetaling, listOf(utbetaling.somAnnullering())))
    }

    @Test
    fun `tilstand for vedtaksperiode med feilet utbetaling`() {
        val utbetaling = utbetaling(UTBETALING_FEILET, listOf(1.NAV))
        assertEquals(Feilet, tilstandFor(1.januar til 31.januar, TilstandType.AVSLUTTET, utbetaling, emptyList()))
        assertEquals(AnnulleringFeilet, tilstandFor(1.januar til 31.januar, TilstandType.AVSLUTTET, utbetaling, listOf(utbetaling.somAnnullering(UTBETALING_FEILET))))
    }

    @Test
    fun `tilstand for annullering`() {
        assertEquals(TilAnnullering, annullering(IKKE_UTBETALT).tilstandFor(1.januar til 31.januar))
        assertEquals(TilAnnullering, annullering(OVERFØRT).tilstandFor(1.januar til 31.januar))
        assertEquals(TilAnnullering, annullering(SENDT).tilstandFor(1.januar til 31.januar))
        assertEquals(Annullert, annullering(ANNULLERT).tilstandFor(1.januar til 31.januar))
        assertEquals(AnnulleringFeilet, annullering(UTBETALING_FEILET).tilstandFor(1.januar til 31.januar))
    }

    @Test
    fun `tilstand for utbetaling`() {
        assertEquals(TilUtbetaling, utbetaling(IKKE_UTBETALT).tilstandFor(1.januar til 31.januar))
        assertEquals(TilUtbetaling, utbetaling(OVERFØRT).tilstandFor(1.januar til 31.januar))
        assertEquals(TilUtbetaling, utbetaling(SENDT).tilstandFor(1.januar til 31.januar))
        assertEquals(KunFerie, utbetaling(GODKJENT_UTEN_UTBETALING).tilstandFor(1.januar til 31.januar))
        assertEquals(KunFerie, utbetaling(UTBETALT, listOf(1.FRI)).tilstandFor(1.januar til 31.januar))
        assertEquals(KunFerie, utbetaling(UTBETALT, listOf(1.NAV(31.desember(2017)), 1.FRI(1.januar))).tilstandFor(1.januar til 31.januar))
        assertEquals(Utbetalt, utbetaling(UTBETALT, listOf(1.FRI(31.desember(2017)), 1.NAV(1.januar))).tilstandFor(1.januar til 31.januar))
        assertEquals(Utbetalt, utbetaling(UTBETALT, listOf(1.NAV)).tilstandFor(1.januar til 31.januar))
        assertEquals(Feilet, utbetaling(UTBETALING_FEILET).tilstandFor(1.januar til 31.januar))
    }

    @Test
    fun `tilstand for revurdering`() {
        assertEquals(Utbetalt, revurdering(IKKE_UTBETALT).tilstandFor(1.januar til 31.januar))
        assertEquals(Utbetalt, revurdering(OVERFØRT).tilstandFor(1.januar til 31.januar))
        assertEquals(Utbetalt, revurdering(SENDT).tilstandFor(1.januar til 31.januar))
        assertEquals(Utbetalt, revurdering(GODKJENT_UTEN_UTBETALING).tilstandFor(1.januar til 31.januar))
        assertEquals(Utbetalt, revurdering(UTBETALT).tilstandFor(1.januar til 31.januar))
        assertEquals(Utbetalt, revurdering(UTBETALING_FEILET).tilstandFor(1.januar til 31.januar))
    }

    private fun annullering(status: Utbetalingstatus) = utbetaling(ANNULLERING, status)
    private fun utbetaling(status: Utbetalingstatus, utbetalingstidslinje: List<UtbetalingstidslinjedagDTO> = emptyList()) = utbetaling(UTBETALING, status, utbetalingstidslinje)
    private fun revurdering(status: Utbetalingstatus) = utbetaling(REVURDERING, status)

    private fun utbetaling(type: Utbetalingtype, status: Utbetalingstatus, utbetalingstidslinje: List<UtbetalingstidslinjedagDTO> = emptyList()) = UtbetalingshistorikkElementDTO.UtbetalingDTO(
        utbetalingId = UUID.randomUUID(),
        korrelasjonsId = UUID.randomUUID(),
        utbetalingstidslinje = utbetalingstidslinje,
        beregningId = UUID.randomUUID(),
        type = type,
        maksdato = LocalDate.MAX,
        status = status,
        gjenståendeSykedager = null,
        forbrukteSykedager = null,
        arbeidsgiverNettoBeløp = 0,
        personNettoBeløp = 0,
        arbeidsgiverOppdrag = OppdragDTO("fagsystemId", LocalDateTime.now(), null, emptyList()),
        personOppdrag = OppdragDTO("fagsystemId", LocalDateTime.now(), null, emptyList()),
        tidsstempel = LocalDateTime.now(),
        vurdering = null
    )

    private fun UtbetalingshistorikkElementDTO.UtbetalingDTO.somAnnullering(status: Utbetalingstatus = ANNULLERT) = UtbetalingshistorikkElementDTO(
        hendelsetidslinje = emptyList(),
        beregnettidslinje = emptyList(),
        vilkårsgrunnlagHistorikkId = UUID.randomUUID(),
        tidsstempel = LocalDateTime.now(),
        utbetaling = this.copy(
            utbetalingId = UUID.randomUUID(),
            type = ANNULLERING,
            status = status
        )
    )

    private val Int.NAV get() = NAV(1.januar)
    private fun Int.NAV(dato: LocalDate) = NavDagDTO(inntekt = 35000, dato = dato, utbetaling = 1000, personbeløp = 0, arbeidsgiverbeløp = 1000, refusjonsbeløp = 1000, grad = 100.0, totalGrad = 100.0)
    private val Int.FRI get() = FRI(1.januar)
    private fun Int.FRI(dato: LocalDate) = IkkeUtbetaltDagDTO(DagtypeDTO.Feriedag, 35000, dato)
}
