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

    private var log = LoggerFactory.getLogger(HendelseMediator::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

    private val parser = Parser(this)
    private val listeners = mutableListOf<HendelseListener>()

    init {
        rapid.addListener(parser)

        parser.register(SøknadMessage.Recognizer(SøknadMediator()))
        parser.register(InntektsmeldingMessage.Recognizer(InntektsmeldingMediator()))
        parser.register(BehovMessage.Recognizer(BehovMediator()))
        parser.register(PåminnelseMessage.Recognizer(PåminnelseMediator()))
    }

    fun addListener(listener: HendelseListener) {
        listeners.add(listener)
    }

    override fun onUnrecognizedMessage(problems: MessageProblems) {
        sikkerLogg.info("ukjent melding: $problems")
    }

    private inner class SøknadMediator : MessageDirector<SøknadMessage> {

        override fun onMessage(message: SøknadMessage, warnings: MessageProblems) {
            message.toJson().also { json ->
                NySøknad.Builder().build(json)?.apply {
                    listeners.forEach { it.onNySøknad(this) }
                } ?: SendtSøknad.Builder().build(json)?.apply {
                    listeners.forEach { it.onSendtSøknad(this) }
                } ?: warnings.error("klarer ikke å mappe søknaden til domenetype")
            }

            if (warnings.hasMessages()) {
                log.info("meldinger om søknad: $warnings")
            }
        }
    }

    internal inner class InntektsmeldingMediator : MessageDirector<InntektsmeldingMessage> {

        override fun onMessage(message: InntektsmeldingMessage, warnings: MessageProblems) {
            Inntektsmelding.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onInntektsmelding(this) }
            } ?: warnings.error("klarer ikke å mappe inntektsmelding til domenetype")

            if (warnings.hasMessages()) {
                log.info("meldinger om inntektsmelding: $warnings")
            }
        }
    }

    private inner class BehovMediator : MessageDirector<BehovMessage> {

        override fun onMessage(message: BehovMessage, warnings: MessageProblems) {
            val builders = listOf(
                Ytelser.Builder(),
                Vilkårsgrunnlag.Builder(),
                ManuellSaksbehandling.Builder()
            )

            builders.mapNotNull {
                it.build(message.toJson())
            }.firstOrNull().apply {
                when (this) {
                    is Ytelser -> listeners.forEach { it.onYtelser(this) }
                    is Vilkårsgrunnlag -> listeners.forEach { it.onVilkårsgrunnlag(this) }
                    is ManuellSaksbehandling -> listeners.forEach { it.onManuellSaksbehandling(this) }
                }
            } ?: warnings.error("klarer ikke å mappe behov til domenetype")

            if (warnings.hasMessages()) {
                log.info("meldinger om behov: $warnings")
            }
        }
    }

    private inner class PåminnelseMediator : MessageDirector<PåminnelseMessage> {

        override fun onMessage(message: PåminnelseMessage, warnings: MessageProblems) {
            Påminnelse.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onPåminnelse(this) }
            } ?: warnings.error("klarer ikke å mappe påminnelse til domenetype")

            if (warnings.hasMessages()) {
                log.info("meldinger om påminnelse: $warnings")
            }
        }
    }
}
