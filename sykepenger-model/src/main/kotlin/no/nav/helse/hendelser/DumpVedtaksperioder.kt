package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

class DumpVedtaksperioder(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg()) {
}