package no.nav.helse.dto.deserialisering

import java.util.UUID
import no.nav.helse.dto.FeriepengeberegnerDto

data class FeriepengeInnDto(
    val feriepengeberegner: FeriepengeberegnerDto,
    val infotrygdFeriepengebeløpPerson: Double,
    val infotrygdFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpPerson: Double,
    val oppdrag: OppdragInnDto,
    val personoppdrag: OppdragInnDto,
    val utbetalingId: UUID,
    val sendTilOppdrag: Boolean,
    val sendPersonoppdragTilOS: Boolean,
)