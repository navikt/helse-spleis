package no.nav.helse.spleis.speil.dto

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

data class PersonDTO(
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverDTO>,
    val dødsdato: LocalDate?,
    val versjon: Int,
    val vilkårsgrunnlag: Map<UUID, Vilkårsgrunnlag>
)

data class AlderDTO(val fødselsdato: LocalDate, val dødsdato: LocalDate?) {
    fun alderPåDato(dagen: LocalDate): Int {
        return ChronoUnit.YEARS.between(fødselsdato, listOfNotNull(dagen, dødsdato).min()).toInt()
    }
}
