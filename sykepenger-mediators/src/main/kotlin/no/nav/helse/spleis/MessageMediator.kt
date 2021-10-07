package no.nav.helse.spleis

import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.serde.DeserializationException
import no.nav.helse.serde.migration.JsonMigrationException
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.meldinger.*
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory
import java.sql.SQLException

internal class MessageMediator(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: IHendelseMediator,
    private val hendelseRepository: HendelseRepository
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
            InntektsmeldingerReplayRiver(it, this)
            UtbetalingshistorikkRiver(it, this)
            UtbetalingshistorikkForFeriepengerRiver(it, this)
            YtelserRiver(it, this)
            VilkårsgrunnlagRiver(it, this)
            UtbetalingsgrunnlagRiver(it, this)
            UtbetalingsgodkjenningerRiver(it, this)
            UtbetalingerOverførtRiver(it, this)
            UtbetalingerRiver(it, this)
            PåminnelserRiver(it, this)
            PersonPåminnelserRiver(it, this)
            UtbetalingpåminnelserRiver(it, this)
            SimuleringerRiver(it, this)
            AnnullerUtbetalingerRiver(it, this)
            PersonAvstemmingRiver(it, this)
            OverstyrTidlinjeRiver(it, this)
            EtterbetalingerRiver(it, this)
            EtterbetalingerRiverMedHistorikk(it, this)
            OverstyrInntektRiver(it, this)
        }
    }

    private var messageRecognized = false
    private val riverErrors = mutableListOf<Pair<String, MessageProblems>>()

    fun beforeRiverHandling() {
        messageRecognized = false
        riverErrors.clear()
    }

    override fun onRecognizedMessage(message: HendelseMessage, context: MessageContext) {
        try {
            messageRecognized = true
            message.logRecognized(sikkerLogg)
            hendelseRepository.lagreMelding(message)
            hendelseMediator.behandle(message)
        } catch (err: JsonMigrationException) {
            severeErrorHandler(err, message)
        } catch (err: DeserializationException) {
            severeErrorHandler(err, message)
        } catch (err: SQLException) {
            severeErrorHandler(err, message)
        } catch (err: Exception) {
            errorHandler(err, message)
        }
    }

    override fun onRiverError(riverName: String, problems: MessageProblems, context: MessageContext) {
        riverErrors.add(riverName to problems)
    }

    fun afterRiverHandling(message: String) {
        if (messageRecognized || riverErrors.isEmpty()) return
        sikkerLogg.warn("kunne ikke gjenkjenne melding:\n\t$message\n\nProblemer:\n${riverErrors.joinToString(separator = "\n") { "${it.first}:\n${it.second}" }}")
    }

    private fun severeErrorHandler(err: Exception, message: HendelseMessage) {
        errorHandler(err, message)
        throw err
    }

    private fun errorHandler(err: Exception, message: HendelseMessage) {
        errorHandler(err, message.toJson(), message.secureDiagnosticinfo())
    }

    private fun errorHandler(err: Exception, message: String, context: Map<String, String> = emptyMap()) {
        log.error("alvorlig feil: ${err.message} (se sikkerlogg for melding)", err)
        withMDC(context) { sikkerLogg.error("alvorlig feil: ${err.message}\n\t$message", err) }
    }

    private inner class DelegatedRapid(
        private val rapidsConnection: RapidsConnection
    ) : RapidsConnection(), RapidsConnection.MessageListener {

        init {
            rapidsConnection.register(this)
        }

        override fun onMessage(message: String, context: MessageContext) {
            beforeRiverHandling()
            notifyMessage(message, context)
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

internal interface IMessageMediator {
    fun onRecognizedMessage(message: HendelseMessage, context: MessageContext)
    fun onRiverError(riverName: String, problems: MessageProblems, context: MessageContext)
}
