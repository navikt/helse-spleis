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
    private val sykdomshistorikkElementBuilders = mutableListOf<SykdomshistorikkElementBuilder>()
    private val utbetalingstidslinjeBuilders = mutableListOf<Pair<UUID, UtbetalingstidslinjeBuilder>>()

    fun build(): List<UtbetalingshistorikkElementDTO> {
        val beregningTilSykdomshistorikkElement = utbetalingberegning
            .mapValues { (_, beregningInfo) -> beregningInfo.first }
        val utbetalinger = utbetalingstidslinjeBuilders
            .map { beregningTilSykdomshistorikkElement.getValue(it.first) to UtbetalingshistorikkElementDTO.UtbetalingDTO(it.second.build()) }
            .groupBy { it.first }
            .mapValues { it.value.map { it.second } }

        return sykdomshistorikkElementBuilders.map { it.build(utbetalinger) }.filter { it.utbetalinger.isNotEmpty() }
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
        val utbetalingstidslinjeBuilder = UtbetalingstidslinjeBuilder(mutableListOf(), mutableListOf())
        utbetalingstidslinjeBuilders.add(beregningId to utbetalingstidslinjeBuilder)
        pushState(utbetalingstidslinjeBuilder)
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
        val elementBuilder = SykdomshistorikkElementBuilder(id)
        sykdomshistorikkElementBuilders.add(elementBuilder)
        pushState(elementBuilder)
    }

    private class SykdomshistorikkElementBuilder(private val id: UUID) : BuilderState() {
        private lateinit var hendelsetidslinje: SykdomstidslinjeBuilder
        private lateinit var beregnettidslinje: SykdomstidslinjeBuilder

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
            hendelsetidslinje = SykdomstidslinjeBuilder()
            pushState(hendelsetidslinje)
        }

        override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            beregnettidslinje = SykdomstidslinjeBuilder()
            pushState(beregnettidslinje)
        }

        override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
            popState()
        }
    }
}
