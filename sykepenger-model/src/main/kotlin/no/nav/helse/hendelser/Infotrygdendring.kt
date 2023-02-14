package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

class Infotrygdendring(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg())
