package no.nav.helse.dto.serialisering

import java.util.*
import no.nav.helse.dto.FeriepengeutbetalinggrunnlagUtDto

data class FeriepengeUtDto(
    val feriepengeberegner: FeriepengeutbetalinggrunnlagUtDto,
    val infotrygdFeriepengebeløpPerson: Double,
    val infotrygdFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpPerson: Double,
    val oppdrag: OppdragUtDto,
    val personoppdrag: OppdragUtDto,
    val utbetalingId: UUID,
    val sendTilOppdrag: Boolean,
    val sendPersonoppdragTilOS: Boolean,
)
