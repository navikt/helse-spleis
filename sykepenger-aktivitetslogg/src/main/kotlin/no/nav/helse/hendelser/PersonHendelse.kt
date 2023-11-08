package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

abstract class PersonHendelse protected constructor(
    meldingsreferanseId: UUID,
    protected val fødselsnummer: String,
    protected val aktørId: String,
    aktivitetslogg: IAktivitetslogg
) : Hendelse(meldingsreferanseId, aktivitetslogg) {
    fun aktørId() = aktørId
    fun fødselsnummer() = fødselsnummer

    override fun kontekst() = mapOf(
        "aktørId" to aktørId(),
        "fødselsnummer" to fødselsnummer()
    )

    override val innsendt: LocalDateTime = LocalDateTime.now()
    override val avsender: Avsender = Avsender.SYSTEM
}
