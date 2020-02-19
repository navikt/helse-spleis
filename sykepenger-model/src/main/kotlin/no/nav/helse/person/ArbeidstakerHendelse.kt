package no.nav.helse.person

import no.nav.helse.behov.BehovType
import no.nav.helse.hendelser.HendelseObserver
import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    private val hendelseId: UUID,
    internal val aktivitetslogger: Aktivitetslogger,
    private val aktivitetslogg: Aktivitetslogg
) : IAktivitetslogger by aktivitetslogger, IAktivitetslogg by aktivitetslogg {
    private val hendelseObservers = mutableListOf<HendelseObserver>()

    fun addObserver(hendelseObserver: HendelseObserver) = hendelseObservers.add(hendelseObserver)

    fun hendelseId() = hendelseId

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String
    abstract fun organisasjonsnummer(): String

    internal fun need(behov: BehovType) {
        aktivitetslogg.need(melding = behov.navn)
        hendelseObservers.forEach { it.onBehov(behov) }
    }
}
