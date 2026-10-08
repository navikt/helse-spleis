package no.nav.helse.dto.deserialisering

import java.time.LocalDate
import java.time.Year

data class FeriepengeutbetalinggrunnlagInnDto(
    val opptjeningsår: Year,
    val utbetalteDager: List<UtbetaltDagInnDto>,
    val feriepengedager: List<UtbetaltDagInnDto>,
) {
    sealed class UtbetaltDagInnDto {
        abstract val orgnummer: String
        abstract val dato: LocalDate
        abstract val beløp: Int

        data class InfotrygdArbeidsgiver(
            override val orgnummer: String,
            override val dato: LocalDate,
            override val beløp: Int,
        ) : UtbetaltDagInnDto()

        data class InfotrygdPerson(
            override val orgnummer: String,
            override val dato: LocalDate,
            override val beløp: Int,
        ) : UtbetaltDagInnDto()

        data class SpleisArbeidsgiver(
            override val orgnummer: String,
            override val dato: LocalDate,
            override val beløp: Int,
        ) : UtbetaltDagInnDto()

        data class SpleisPerson(
            override val orgnummer: String,
            override val dato: LocalDate,
            override val beløp: Int,
        ) : UtbetaltDagInnDto()
    }
}
