package no.nav.helse.dto.serialisering

import no.nav.helse.dto.FeriepengeberegnerDto
import java.util.UUID

data class FeriepengeUtDto(
    val feriepengeberegner: FeriepengeberegnerDto,
    val infotrygdFeriepengebeløpPerson: Double,
    val infotrygdFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpPerson: Double,
    val oppdrag: OppdragUtDto,
    val personoppdrag: OppdragUtDto,
    val utbetalingId: UUID,
    val sendTilOppdrag: Boolean,
    val sendPersonoppdragTilOS: Boolean
)
