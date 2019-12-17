package no.nav.helse.sak

import no.nav.helse.hendelser.Hendelsetype
import java.time.LocalDateTime
import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    private val hendelseId: UUID,
    private val hendelsetype: Hendelsetype
) {
    fun hendelseId() = hendelseId
    fun hendelsetype() = hendelsetype

    abstract fun opprettet(): LocalDateTime

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String
    abstract fun organisasjonsnummer(): String

    open fun kanBehandles() = true

    abstract fun toJson(): String

    override fun equals(other: Any?) =
        other is ArbeidstakerHendelse && other.hendelseId == this.hendelseId

    override fun hashCode() = hendelseId.hashCode()
}
