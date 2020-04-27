package no.nav.helse.spleis

import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.meldinger.*
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory

internal class MessageMediator(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: IHendelseMediator,
    private val hendelseRecorder: HendelseRecorder
) : IMessageMediator {
    private companion object {
        private val log = LoggerFactory.getLogger(MessageMediator::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        DelegatedRapid(rapidsConnection).also {
            NyeSøknaderRiver(it, this)
            SendtArbeidsgiverSøknaderRiver(it, this)
            SendtNavSøknaderRiver(it, this)
            InntektsmeldingerRiver(it, this)
            YtelserRiver(it, this)
            VilkårsgrunnlagRiver(it, this)
            GammelUtbetalingsgodkjenningerRiver(it, this)
            UtbetalingsgodkjenningerRiver(it, this)
            UtbetalingerOverførtRiver(it, this)
            UtbetalingerRiver(it, this)
            PåminnelserRiver(it, this)
            SimuleringerRiver(it, this)
            KansellerUtbetalingerRiver(it, this)
        }
    }

    private var messageRecognized = false
    private val riverErrors = mutableListOf<Pair<String, MessageProblems>>()

    fun beforeRiverHandling(message: String) {
        messageRecognized = false
        riverErrors.clear()
    }

    override fun onRecognizedMessage(message: HendelseMessage, context: RapidsConnection.MessageContext) {
        messageRecognized = true
        withMDC(mapOf(
            "melding_id" to message.id.toString(),
            "melding_type" to (message::class.simpleName ?: "ukjent")
        )) {
            sikkerLogg.info("gjenkjente melding id={} for fnr={} som {}:\n{}", message.id, message.fødselsnummer, message::class.simpleName, message.toJson())
            handleMessage(message, context)
        }
    }

    override fun onRiverError(riverName: String, problems: MessageProblems, context: RapidsConnection.MessageContext) {
        riverErrors.add(riverName to problems)
    }

    fun afterRiverHandling(message: String) {
        if (messageRecognized || riverErrors.isEmpty()) return
        sikkerLogg.warn("kunne ikke gjenkjenne melding:\n\t$message\n\nProblemer:\n${riverErrors.joinToString(separator = "\n") { "${it.first}:\n${it.second}" }}")
    }

    private fun handleMessage(message: HendelseMessage, context: RapidsConnection.MessageContext) {
        try {
            hendelseRecorder.lagreMelding(message)
            hendelseMediator.behandle(message)
        } catch (err: Exception) {
            errorHandler(err, message)
        }
    }

    private fun errorHandler(err: Exception, message: HendelseMessage? = null) {
        log.error("alvorlig feil: ${err.message}", err)
        withMDC(message?.let { mapOf("fødselsnummer" to message.fødselsnummer) } ?: emptyMap()) {
            sikkerLogg.error("alvorlig feil: ${err.message}", err)
        }
    }

    private inner class DelegatedRapid(
        private val rapidsConnection: RapidsConnection
    ) : RapidsConnection(), RapidsConnection.MessageListener {

        init {
            rapidsConnection.register(this)
        }

        override fun onMessage(message: String, context: MessageContext) {
            try {
                beforeRiverHandling(message)
                listeners.forEach { it.onMessage(message, context) }
                afterRiverHandling(message)
            } catch (err: Exception) {
                errorHandler(err)
            }
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

internal interface IMessageMediator {
    fun onRecognizedMessage(message: HendelseMessage, context: RapidsConnection.MessageContext)
    fun onRiverError(riverName: String, problems: MessageProblems, context: RapidsConnection.MessageContext)
}
