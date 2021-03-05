package no.nav.helse.serde.api.dto

import no.nav.helse.serde.api.SykdomstidslinjedagDTO
import no.nav.helse.serde.api.UtbetalingstidslinjedagDTO
import java.time.LocalDate
import java.util.*

data class UtbetalingshistorikkElementDTO(
    val hendelsetidslinje: List<SykdomstidslinjedagDTO>,
    val beregnettidslinje: List<SykdomstidslinjedagDTO>,
    val utbetalinger: List<UtbetalingDTO>
) {
    data class UtbetalingDTO(
        val utbetalingstidslinje: List<UtbetalingstidslinjedagDTO>,
        val beregningId: UUID,
        val type: String,
        val maksdato: LocalDate
    )
}
