package no.nav.helse.dto.deserialisering

import java.util.UUID

data class FeriepengeInnDto(
    val feriepengeberegner: FeriepengeutbetalinggrunnlagInnDto,
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
