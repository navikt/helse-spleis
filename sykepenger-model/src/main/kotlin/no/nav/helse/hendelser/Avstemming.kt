package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import java.util.*

class Avstemming(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, Aktivitetslogg())
