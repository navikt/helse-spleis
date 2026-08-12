package no.nav.helse.opptjening.domain

import java.time.LocalDate

sealed interface Vilkårsvurdering {
    val fødselsnummer: String
    val skjæringstidspunkt: LocalDate
    val kodeverkkode: Kodeverkkode
}
