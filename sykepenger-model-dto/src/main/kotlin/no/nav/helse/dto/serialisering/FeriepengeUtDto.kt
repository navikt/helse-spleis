package no.nav.helse.dto.serialisering

import java.util.*

data class FeriepengeUtDto(
    val feriepengeberegner: FeriepengeutbetalinggrunnlagUtDto,
    val infotrygdFeriepengebeløpPerson: Double,
    val infotrygdFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpPerson: Double,
    val oppdrag: FeriepengeoppdragUtDto,
    val personoppdrag: FeriepengeoppdragUtDto,
    val utbetalingId: UUID,
    val sendTilOppdrag: Boolean,
    val sendPersonoppdragTilOS: Boolean,
)
