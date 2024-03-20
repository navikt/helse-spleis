package no.nav.helse.spleis.speil.dto

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

data class PersonDTO(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverDTO>,
    val dødsdato: LocalDate?,
    val versjon: Int,
    val vilkårsgrunnlag: Map<UUID, Vilkårsgrunnlag>
)

data class AlderDTO(val fødselsdato: LocalDate, val dødsdato: LocalDate?) {
    fun alderPåDato(dagen: LocalDate): Int {
        val alderPåDagen = ChronoUnit.YEARS.between(fødselsdato, dagen)
        if (dødsdato == null || dagen < dødsdato) return alderPåDagen.toInt()
        return ChronoUnit.YEARS.between(fødselsdato, dødsdato).toInt()
    }
}