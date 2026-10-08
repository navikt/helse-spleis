package no.nav.helse.dto.serialisering

import java.time.LocalDate
import java.time.Year

data class FeriepengeutbetalinggrunnlagUtDto(
    val opptjeningsår: Year,
    val utbetalteDager: List<UtbetaltDagUtDto>,
    val feriepengedager: List<UtbetaltDagUtDto>,
)

sealed class UtbetaltDagUtDto {
    abstract val orgnummer: String
    abstract val dato: LocalDate
    abstract val beløp: Int

    data class InfotrygdArbeidsgiver(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int,
    ) : UtbetaltDagUtDto()

    data class InfotrygdPerson(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int,
    ) : UtbetaltDagUtDto()

    data class SpleisArbeidsgiver(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int,
    ) : UtbetaltDagUtDto()

    data class SpleisPerson(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int,
    ) : UtbetaltDagUtDto()
}
