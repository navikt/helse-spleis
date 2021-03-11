package no.nav.helse.serde.api.builders

import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingshistorikkBuilder : BuilderState() {
    private val utbetalingberegninger = Utbetalingberegninger()
    private val sykdomshistorikkElementBuilders = mutableListOf<SykdomshistorikkElementBuilder>()
    private val utbetalingstidslinjeBuilders = mutableListOf<UtbetalingstidslinjeInfo>()

    fun build(): List<UtbetalingshistorikkElementDTO> {
        val utbetalinger = UtbetalingstidslinjeInfo.utbetalinger(utbetalingstidslinjeBuilders, utbetalingberegninger)
        return sykdomshistorikkElementBuilders.map { it.build(utbetalinger) }.filter { it.utbetalinger.isNotEmpty() }
    }

    private data class UtbetalingstidslinjeInfo(
        private val beregningId: UUID,
        private val type: String,
        private val maksdato: LocalDate,
        private val status: String,
        private val gjenståendeSykedager: Int?,
        private val forbrukteSykedager: Int?,
        private val arbeidsgiverNettoBeløp: Int,
        private val builder: UtbetalingstidslinjeBuilder
    ) {
        fun utbetaling() = UtbetalingshistorikkElementDTO.UtbetalingDTO(
            utbetalingstidslinje = builder.build(),
            beregningId = beregningId,
            type = type,
            maksdato = maksdato,
            status = status,
            gjenståendeSykedager = gjenståendeSykedager,
            forbrukteSykedager = forbrukteSykedager,
            arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp
        )

        companion object {
            fun utbetalinger(
                liste: List<UtbetalingstidslinjeInfo>,
                utbetalingberegninger: Utbetalingberegninger
            ): Map<UUID, List<UtbetalingshistorikkElementDTO.UtbetalingDTO>> {
                val resultat = mutableMapOf<UUID, MutableList<UtbetalingshistorikkElementDTO.UtbetalingDTO>>()
                liste.forEach { resultat.getOrPut(utbetalingberegninger.sykdomshistorikkelementId(it.beregningId)) { mutableListOf() }.add(it.utbetaling()) }
                return resultat
            }
        }
    }

    private class Utbetalingberegninger {
        private val liste = mutableListOf<BeregningInfo>()

        fun add(beregningInfo: BeregningInfo) = liste.add(beregningInfo)
        fun sykdomshistorikkelementId(beregningId: UUID) = BeregningInfo.sykdomshistorikkelementId(liste, beregningId)
    }

    private class BeregningInfo(
        private val beregningId: UUID,
        private val sykdomshistorikkElementId: UUID,
        private val tidsstempel: LocalDateTime
    ) {
        companion object {
            fun sykdomshistorikkelementId(beregningInfo: List<BeregningInfo>, beregningId: UUID) =
                beregningInfo.first { it.beregningId == beregningId }.sykdomshistorikkElementId
        }
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
        utbetalingstidslinjeBuilders.add(
            UtbetalingstidslinjeInfo(
                beregningId = beregningId,
                type = type.name,
                maksdato = maksdato,
                status = Utbetalingstatus.fraTilstand(tilstand).name,
                gjenståendeSykedager = gjenståendeSykedager,
                forbrukteSykedager = forbrukteSykedager,
                arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
                builder = utbetalingstidslinjeBuilder
            )
        )
        pushState(utbetalingstidslinjeBuilder)
    }

    override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        popState()
    }

    override fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        popState()
    }

    override fun visitUtbetalingstidslinjeberegning(id: UUID, tidsstempel: LocalDateTime, sykdomshistorikkElementId: UUID) {
        utbetalingberegninger.add(BeregningInfo(id, sykdomshistorikkElementId, tidsstempel))
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
