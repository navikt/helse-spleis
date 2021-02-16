package no.nav.helse.serde.api.builders

import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingshistorikkBuilder() : BuilderState() {
    private val utbetalingberegning = mutableMapOf<UUID, Pair<UUID, LocalDateTime>>()
    private val sykdomshistorikkElementStater = mutableListOf<SykdomshistorikkElementState>()
    private val utbetalingstidslinjeStater = mutableListOf<Pair<UUID, UtbetalingstidslinjeState>>()

    fun build(): List<UtbetalingshistorikkElementDTO> {
        val beregningTilSykdomshistorikkElement = utbetalingberegning
            .mapValues { (_, beregningInfo) -> beregningInfo.first }
        val utbetalinger = utbetalingstidslinjeStater
            .map { beregningTilSykdomshistorikkElement.getValue(it.first) to UtbetalingshistorikkElementDTO.UtbetalingDTO(it.second.build()) }
            .groupBy { it.first }
            .mapValues { it.value.map { it.second } }

        return sykdomshistorikkElementStater.map { it.build(utbetalinger) }.filter { it.utbetalinger.isNotEmpty() }
    }

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        beregningId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) {
        val utbetalingstidslinjeState = UtbetalingstidslinjeState(mutableListOf(), mutableListOf())
        utbetalingstidslinjeStater.add(beregningId to utbetalingstidslinjeState)
        pushState(utbetalingstidslinjeState)
    }

    override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        popState()
    }

    override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        popState()
    }

    override fun visitUtbetalingstidslinjeberegning(id: UUID, tidsstempel: LocalDateTime, sykdomshistorikkElementId: UUID) {
        utbetalingberegning[id] = sykdomshistorikkElementId to tidsstempel
    }

    override fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
        val sykdomshistorikkElementState = SykdomshistorikkElementState(id)
        sykdomshistorikkElementStater.add(sykdomshistorikkElementState)
        pushState(sykdomshistorikkElementState)
    }

    private class SykdomshistorikkElementState(private val id: UUID) : BuilderState() {
        private lateinit var hendelsetidslinje: SykdomstidslinjeState
        private lateinit var beregnettidslinje: SykdomstidslinjeState

        fun build(sykdomshistorikkbetalinger: Map<UUID, List<UtbetalingshistorikkElementDTO.UtbetalingDTO>>) =
            UtbetalingshistorikkElementDTO(
                hendelsetidslinje = hendelsetidslinje.build(),
                beregnettidslinje = beregnettidslinje.build(),
                utbetalinger = sykdomshistorikkbetalinger[id] ?: emptyList()
            )

        override fun preVisitHendelseSykdomstidslinje(
            tidslinje: Sykdomstidslinje,
            hendelseId: UUID?,
            tidsstempel: LocalDateTime
        ) {
            hendelsetidslinje = SykdomstidslinjeState()
            pushState(hendelsetidslinje)
        }

        override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            beregnettidslinje = SykdomstidslinjeState()
            pushState(beregnettidslinje)
        }

        override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
            popState()
        }
    }
}
