package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.util.UUID

data class PersonDTO(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverDTO>,
    val dødsdato: LocalDate?,
    val versjon: Int,
    val vilkårsgrunnlag: Map<UUID, Vilkårsgrunnlag>
)