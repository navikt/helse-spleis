package no.nav.helse.serde.api.builders

import no.nav.helse.serde.api.SimuleringsdataDTO
import no.nav.helse.serde.api.UtbetalingerDTO
import java.time.LocalDateTime

class OppdragDTO(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val simuleringsResultat: SimuleringsdataDTO?,
    val utbetalingslinjer: List<UtbetalingerDTO.UtbetalingslinjeDTO>
)
