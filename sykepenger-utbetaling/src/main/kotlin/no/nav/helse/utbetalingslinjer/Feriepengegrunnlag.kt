package no.nav.helse.utbetalingslinjer

import java.time.LocalDate

data class Arbeidsgiverferiepengegrunnlag(val orgnummer: String, val utbetalinger: List<Feriepengegrunnlag>)
data class Feriepengegrunnlag(
    val arbeidsgiverUtbetalteDager: List<UtbetaltDag>,
    val personUtbetalteDager: List<UtbetaltDag>
) {

    data class UtbetaltDag(val dato: LocalDate, val beløp: Int)
}