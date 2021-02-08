package no.nav.helse.serde.api.dto

import no.nav.helse.serde.api.SykdomstidslinjedagDTO
import no.nav.helse.serde.api.UtbetalingstidslinjedagDTO
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

data class UtbetalingshistorikkElementDTO(
    val hendelsetidslinje: TidslinjeDTO,
    val beregnettidslinje: TidslinjeDTO,
    val utbetalinger: List<UtbetalingDTO>
) {
    data class UtbetalingDTO(
        val utbetalingstidslinje: UtbetalingstidslinjeDTO
    )
}

typealias TidslinjeDTO = List<SykdomstidslinjedagDTO>
typealias UtbetalingstidslinjeDTO = List<UtbetalingstidslinjedagDTO>
