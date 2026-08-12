package no.nav.helse.opptjening.application

import java.time.Instant
import java.time.LocalDate
import no.nav.helse.opptjening.domain.Arbeidssituasjon

class Opptjeningsbehov(
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidssituasjon: Arbeidssituasjon,
    tidspunktForKvittertUt: Instant?
) {
    var tidspunktForKvittertUt: Instant? = tidspunktForKvittertUt
        private set

    fun kvitterUt() {
        tidspunktForKvittertUt = Instant.now()
    }
}
