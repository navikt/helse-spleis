package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

class Dødsmelding(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    private val dødsdato: LocalDate
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg()) {

    internal fun dødsdato() = dødsdato
}
