package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse

class Arbeidsgiveropplysninger(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    aktivitetslogg: Aktivitetslogg
): ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {
}