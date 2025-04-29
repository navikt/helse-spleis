package no.nav.helse.dto.deserialisering

import java.util.UUID

data class FeriepengeInnDto(
    val feriepengeberegner: FeriepengeutbetalinggrunnlagInnDto,
    val infotrygdFeriepengebeløpPerson: Double,
    val infotrygdFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpPerson: Double,
    val oppdrag: FeriepengeoppdragInnDto,
    val personoppdrag: FeriepengeoppdragInnDto,
    val utbetalingId: UUID,
    val sendTilOppdrag: Boolean,
    val sendPersonoppdragTilOS: Boolean,
)
