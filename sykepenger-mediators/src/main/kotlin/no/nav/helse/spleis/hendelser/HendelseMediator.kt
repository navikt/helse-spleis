package no.nav.helse.spleis.hendelser

import no.nav.helse.hendelser.*
import no.nav.helse.spleis.HendelseListener
import no.nav.helse.spleis.HendelseStream
import no.nav.helse.spleis.hendelser.model.*
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

        parser.register(NySøknadMessage.Factory)
        parser.register(FremtidigSøknadMessage.Factory)
        parser.register(SendtSøknadMessage.Factory)
        parser.register(InntektsmeldingMessage.Factory)
        parser.register(YtelserMessage.Factory)
        parser.register(VilkårsgrunnlagMessage.Factory)
        parser.register(ManuellSaksbehandlingMessage.Factory)
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
        override fun process(message: NySøknadMessage, problems: MessageProblems) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            NySøknad.Builder().build(message.toJson())?.apply {
                return listeners.forEach { it.onNySøknad(this) }
            } ?: problems.error("klarer ikke å mappe søknaden til domenetype")
        }

        override fun process(message: FremtidigSøknadMessage, problems: MessageProblems) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            NySøknad.Builder().build(message.toJson())?.apply {
                return listeners.forEach { it.onNySøknad(this) }
            } ?: problems.error("klarer ikke å mappe søknaden til domenetype")
        }

        override fun process(message: SendtSøknadMessage, problems: MessageProblems) {
           // TODO: map til ordentlig domenehendelse uten kobling til json
            SendtSøknad.Builder().build(message.toJson())?.apply {
                return listeners.forEach { it.onSendtSøknad(this) }
            } ?: problems.error("klarer ikke å mappe søknaden til domenetype")
        }

        override fun process(message: InntektsmeldingMessage, problems: MessageProblems) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Inntektsmelding.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onInntektsmelding(this) }
            } ?: problems.error("klarer ikke å mappe inntektsmelding til domenetype")
        }

        override fun process(message: YtelserMessage, problems: MessageProblems) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Ytelser.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onYtelser(this) }
            } ?: problems.error("klarer ikke å mappe ytelser til domenetype")
        }

        override fun process(message: VilkårsgrunnlagMessage, problems: MessageProblems) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Vilkårsgrunnlag.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onVilkårsgrunnlag(this) }
            } ?: problems.error("klarer ikke å mappe vilkårsgrunnlag til domenetype")
        }

        override fun process(message: ManuellSaksbehandlingMessage, problems: MessageProblems) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            ManuellSaksbehandling.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onManuellSaksbehandling(this) }
            } ?: problems.error("klarer ikke å mappe manuellsaksbehandling til domenetype")
        }

        override fun process(message: PåminnelseMessage, problems: MessageProblems) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Påminnelse.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onPåminnelse(this) }
            } ?: problems.error("klarer ikke å mappe påminnelse til domenetype")
        }
    }
}
