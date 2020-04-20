package no.nav.helse.spleis

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.hendelser.*
import no.nav.helse.spleis.hendelser.model.HendelseMessage
import org.slf4j.LoggerFactory
import org.slf4j.MDC

internal class MessageMediator(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: HendelseMediator,
    private val hendelseRecorder: HendelseRecorder
) {
    private companion object {
        private val log = LoggerFactory.getLogger(MessageMediator::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        DelegatedRapid(rapidsConnection).also {
            NyeSøknader(it, this)
            SendtArbeidsgiverSøknader(it, this)
            SendtNavSøknader(it, this)
            Inntektsmeldinger(it, this)
            Ytelser(it, this)
            Vilkårsgrunnlag(it, this)
            ManuelleSaksbehandlinger(it, this)
            Utbetalinger(it, this)
            Påminnelser(it, this)
            Simuleringer(it, this)
            KansellerUtbetalinger(it, this)
        }
    }

    private var messageRecognized = false
    private val riverSevereErrors = mutableListOf<Pair<String, MessageProblems>>()
    private val riverErrors = mutableListOf<Pair<String, MessageProblems>>()

    fun beforeRiverHandling(message: String) {
        messageRecognized = false
        riverSevereErrors.clear()
        riverErrors.clear()
    }

    fun onRecognizedMessage(message: HendelseMessage, context: RapidsConnection.MessageContext) {
        messageRecognized = true
        handleMessage(message, context)
    }

    fun afterRiverHandling(message: String) {
        if (messageRecognized) return
        if (riverErrors.isNotEmpty()) return sikkerLogg.warn("kunne ikke gjenkjenne melding:\n\t$message\n\nProblemer:\n${riverErrors.joinToString(separator = "\n") { "${it.first}:\n${it.second}" }}")
        sikkerLogg.debug("ukjent melding:\n\t$message\n\nProblemer:\n${riverSevereErrors.joinToString(separator = "\n") { "${it.first}:\n${it.second}" }}")
    }

    fun onRiverError(riverName: String, problems: MessageProblems, context: RapidsConnection.MessageContext) {
        riverErrors.add(riverName to problems)
    }

    fun onRiverSevere(riverName: String, error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        riverSevereErrors.add(riverName to error.problems)
    }

    private fun handleMessage(message: HendelseMessage, context: RapidsConnection.MessageContext) {
        withMDC(mapOf(
            "melding_id" to message.id.toString(),
            "melding_type" to (message::class.simpleName ?: "ukjent")
        )) {
            sikkerLogg.info("gjenkjente melding id={} for fnr={} som {}", message.id, message.fødselsnummer, message::class.simpleName)
            try {
                message.accept(hendelseRecorder)
                message.accept(hendelseMediator)
            } catch (err: Aktivitetslogg.AktivitetException) {
                withMDC(err.kontekst()) {
                    sikkerLogg.error("alvorlig feil i aktivitetslogg: ${err.message}", err)
                }
            } catch (err: Exception) {
                withMDC(mapOf(
                    "melding_id" to message.id.toString(),
                    "melding_type" to (message::class.simpleName ?: "ukjent")
                )) {
                    log.error("alvorlig feil i aktivitetslogg: ${err.message}", err)
                    withMDC(mapOf("fødselsnummer" to message.fødselsnummer)) {
                        sikkerLogg.error("alvorlig feil i aktivitetslogg: ${err.message}", err)
                    }
                }
            }
        }
    }

    private fun withMDC(context: Map<String, String>, block: () -> Unit) {
        val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
        try {
            MDC.setContextMap(contextMap + context)
            block()
        } finally {
            MDC.setContextMap(contextMap)
        }
    }

    private inner class DelegatedRapid(
        private val rapidsConnection: RapidsConnection
    ) : RapidsConnection(), RapidsConnection.MessageListener {

        init {
            rapidsConnection.register(this)
        }

        override fun onMessage(message: String, context: MessageContext) {
            beforeRiverHandling(message)
            listeners.forEach { it.onMessage(message, context) }
            afterRiverHandling(message)
        }

        override fun publish(message: String) {
            rapidsConnection.publish(message)
        }

        override fun publish(key: String, message: String) {
            rapidsConnection.publish(key, message)
        }

        override fun start() = throw IllegalStateException()
        override fun stop() = throw IllegalStateException()
    }
}
