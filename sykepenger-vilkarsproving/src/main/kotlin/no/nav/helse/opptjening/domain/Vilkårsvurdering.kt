package no.nav.helse.opptjening.domain

import java.time.LocalDate
import java.util.UUID

sealed interface Vilkårsvurdering {
    val id: UUID
    val fødselsnummer: String
    val skjæringstidspunkt: LocalDate
    val kodeverkkode: Kodeverkkode?
    val erKomplett: Boolean
}
