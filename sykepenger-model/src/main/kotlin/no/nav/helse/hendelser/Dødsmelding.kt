package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Alder

class Dødsmelding(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    private val dødsdato: LocalDate
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId) {

    internal fun dødsdato(alder: Alder) = alder.medDød(this.dødsdato)
}
