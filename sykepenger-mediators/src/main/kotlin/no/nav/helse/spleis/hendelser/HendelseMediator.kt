package no.nav.helse.spleis.hendelser

import no.nav.helse.hendelser.*
import no.nav.helse.spleis.HendelseListener
import no.nav.helse.spleis.HendelseStream
import no.nav.helse.spleis.hendelser.model.BehovMessage
import no.nav.helse.spleis.hendelser.model.InntektsmeldingMessage
import no.nav.helse.spleis.hendelser.model.PåminnelseMessage
import no.nav.helse.spleis.hendelser.model.SøknadMessage
import org.slf4j.LoggerFactory

// Understands how to communicate messages to other objects
// Acts like a GoF Mediator to forward messages to observers
// Uses GoF Observer pattern to notify events
internal class HendelseMediator(rapid: HendelseStream) : Parser.ParserDirector {
    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")
    private val messageProcessor = Processor()
    private val parser = Parser(this)
    private val listeners = mutableListOf<HendelseListener>()

    init {
        rapid.addListener(parser)

        parser.register(SøknadMessage.Factory)
        parser.register(InntektsmeldingMessage.Factory)
        parser.register(BehovMessage.Factory)
        parser.register(PåminnelseMessage.Factory)
    }

    fun addListener(listener: HendelseListener) {
        listeners.add(listener)
    }

    override fun onRecognizedMessage(message: JsonMessage, warnings: MessageProblems) {
        message.accept(messageProcessor)

        if (warnings.hasMessages()) {
            sikkerLogg.info("meldinger om melding: $warnings")
        }
    }

    override fun onUnrecognizedMessage(problems: MessageProblems) {
        sikkerLogg.info("ukjent melding: $problems")
    }

    private inner class Processor : MessageProcessor {
        override fun process(message: SøknadMessage, problems: MessageProblems) {
            val status = message["status"].asText()

            if (status in listOf("NY", "FREMTIDIG")) {
                // TODO: map til ordentlig domenehendelse uten kobling til json
                NySøknad.Builder().build(message.toJson())?.apply {
                    return listeners.forEach { it.onNySøknad(this) }
                }
            }

            if (status == "SENDT") {
                // TODO: map til ordentlig domenehendelse uten kobling til json
                SendtSøknad.Builder().build(message.toJson())?.apply {
                    return listeners.forEach { it.onSendtSøknad(this) }
                }
            }

            problems.error("klarer ikke å mappe søknaden til domenetype")
        }

        override fun process(message: InntektsmeldingMessage, problems: MessageProblems) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Inntektsmelding.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onInntektsmelding(this) }
            } ?: problems.error("klarer ikke å mappe inntektsmelding til domenetype")
        }

        override fun process(message: BehovMessage, problems: MessageProblems) {
            val builders = listOf(
                Ytelser.Builder(),
                Vilkårsgrunnlag.Builder(),
                ManuellSaksbehandling.Builder()
            )

            // TODO: map til ordentlig domenehendelse uten kobling til json
            builders.mapNotNull {
                it.build(message.toJson())
            }.firstOrNull().apply {
                when (this) {
                    is Ytelser -> listeners.forEach { it.onYtelser(this) }
                    is Vilkårsgrunnlag -> listeners.forEach { it.onVilkårsgrunnlag(this) }
                    is ManuellSaksbehandling -> listeners.forEach { it.onManuellSaksbehandling(this) }
                }
            } ?: problems.error("klarer ikke å mappe behov til domenetype")
        }

        override fun process(message: PåminnelseMessage, problems: MessageProblems) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Påminnelse.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onPåminnelse(this) }
            } ?: problems.error("klarer ikke å mappe påminnelse til domenetype")
        }
    }
}
