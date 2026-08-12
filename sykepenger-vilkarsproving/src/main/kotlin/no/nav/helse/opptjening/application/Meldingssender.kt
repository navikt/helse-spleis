package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.domain.Opptjening

interface Meldingssender {
    fun sendOpptjeningsgrunnlagBehov(fødselsnummer: String, skjæringstidspunkt: LocalDate)
    fun sendOpptjeningsløsning(fødselsnummer: String, skjæringstidspunkt: LocalDate, opptjening: Opptjening)
}
