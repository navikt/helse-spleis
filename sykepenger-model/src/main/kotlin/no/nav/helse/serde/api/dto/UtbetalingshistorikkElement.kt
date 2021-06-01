package no.nav.helse.serde.api.dto

import no.nav.helse.serde.api.SykdomstidslinjedagDTO
import no.nav.helse.serde.api.UtbetalingstidslinjedagDTO
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class UtbetalingshistorikkElementDTO(
    val hendelsetidslinje: List<SykdomstidslinjedagDTO>,
    val beregnettidslinje: List<SykdomstidslinjedagDTO>,
    val utbetaling: UtbetalingDTO
) {
    val beregningId = utbetaling.beregningId

    data class UtbetalingDTO(
        val utbetalingstidslinje: List<UtbetalingstidslinjedagDTO>,
        val beregningId: UUID,
        val type: String,
        val maksdato: LocalDate,
        val status: String,
        val gjenståendeSykedager: Int?,
        val forbrukteSykedager: Int?,
        val arbeidsgiverNettoBeløp: Int,
        val arbeidsgiverFagsystemId: String,
        val vurdering: VurderingDTO?
    ) {
        fun erAnnullering() = type == "ANNULLERING"
        data class VurderingDTO(
            val godkjent: Boolean,
            val tidsstempel: LocalDateTime,
            val automatisk: Boolean,
            val ident: String
        )
    }
}
