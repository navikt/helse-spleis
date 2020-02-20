package no.nav.helse.person

import no.nav.helse.behov.BehovType
import no.nav.helse.hendelser.HendelseObserver

abstract class ArbeidstakerHendelse protected constructor(
    internal val aktivitetslogger: Aktivitetslogger,
    internal val aktivitetslogg: Aktivitetslogg
) : IAktivitetslogger by aktivitetslogger, IAktivitetslogg by aktivitetslogg {
    private val hendelseObservers = mutableListOf<HendelseObserver>()

    fun addObserver(hendelseObserver: HendelseObserver) = hendelseObservers.add(hendelseObserver)

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String
    abstract fun organisasjonsnummer(): String

    internal fun need(behov: BehovType) {
        aktivitetslogg.need(melding = behov.navn)
        hendelseObservers.forEach { it.onBehov(behov) }
    }
}
