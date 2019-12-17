package no.nav.helse.hendelser

import no.nav.helse.behov.Behov
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.VedtaksperiodeHendelse
import java.util.*

class ManuellSaksbehandling private constructor(hendelseId: UUID, private val behov: Behov) :
    ArbeidstakerHendelse(hendelseId, Hendelsetype.ManuellSaksbehandling), VedtaksperiodeHendelse {

    constructor(behov: Behov) : this(UUID.randomUUID(), behov)

    fun saksbehandler(): String = requireNotNull(behov["saksbehandlerIdent"])

    fun utbetalingGodkjent(): Boolean =
        (behov.løsning() as Map<*, *>?)?.get("godkjent") == true

    override fun vedtaksperiodeId(): String = behov.vedtaksperiodeId()

    override fun aktørId(): String = behov.aktørId()

    override fun fødselsnummer(): String = behov.fødselsnummer()

    override fun organisasjonsnummer(): String = behov.organisasjonsnummer()

    override fun opprettet() = requireNotNull(behov.besvart())

    override fun toJson(): String {
        return behov.toJson()
    }
}
