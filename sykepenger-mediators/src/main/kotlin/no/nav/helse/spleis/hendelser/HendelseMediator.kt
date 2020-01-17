package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.HendelseListener
import no.nav.helse.spleis.HendelseStream
import no.nav.helse.spleis.hendelser.model.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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

    override fun onRecognizedMessage(message: JsonMessage, warnings: Aktivitetslogger) {
        message.accept(messageProcessor)

        if (warnings.hasMessages()) {
            sikkerLogg.info("meldinger om melding: $warnings")
        }
    }

    override fun onUnrecognizedMessage(aktivitetslogger: Aktivitetslogger) {
        sikkerLogg.info("ukjent melding: $aktivitetslogger")
    }

    private inner class Processor : MessageProcessor {
        override fun process(message: NySøknadMessage, aktivitetslogger: Aktivitetslogger) {
            try {
                val modelNySøknad = ModelNySøknad(
                    hendelseId = UUID.randomUUID(),
                    fnr = message["fnr"].asText(),
                    aktørId = message["aktorId"].asText(),
                    orgnummer = message["arbeidsgiver"].path("orgnummer").asText(),
                    rapportertdato = message["opprettet"].asText().let { LocalDateTime.parse(it) },
                    sykeperioder = message["soknadsperioder"].map {
                        Triple(
                            first = it.path("fom").asLocalDate(),
                            second = it.path("tom").asLocalDate(),
                            third = it.path("sykmeldingsgrad").asInt()
                        )
                    },
                    aktivitetslogger = aktivitetslogger,
                    originalJson = message.toJson()
                )

                listeners.forEach { it.onNySøknad(modelNySøknad, aktivitetslogger) }

                if (aktivitetslogger.hasMessages()) {
                    sikkerLogg.info("meldinger om ny søknad: $aktivitetslogger")
                }
            } catch (err: Aktivitetslogger) {
                sikkerLogg.info("feil om ny søknad: ${err.message}", err)
            }
        }

        private fun JsonNode.asLocalDate() =
            asText().let { LocalDate.parse(it) }


        override fun process(message: FremtidigSøknadMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            NySøknad.Builder().build(message.toJson())?.apply {
                return listeners.forEach { it.onNySøknad(this) }
            } ?: aktivitetslogger.error("klarer ikke å mappe søknaden til domenetype")
        }

        override fun process(message: SendtSøknadMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            SendtSøknad.Builder().build(message.toJson())?.apply {
                return listeners.forEach { it.onSendtSøknad(this) }
            } ?: aktivitetslogger.error("klarer ikke å mappe søknaden til domenetype")
        }

        override fun process(message: InntektsmeldingMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Inntektsmelding.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onInntektsmelding(this) }
            } ?: aktivitetslogger.error("klarer ikke å mappe inntektsmelding til domenetype")
        }

        override fun process(message: YtelserMessage, aktivitetslogger: Aktivitetslogger) {
            try {
                val foreldrepenger = message["@løsning"].path("Foreldrepenger").let {
                    ModelForeldrepenger(
                        foreldrepengeytelse = it.path("Foreldrepengeytelse").takeIf(JsonNode::isObject)?.let(::asPeriode),
                        svangerskapsytelse = it.path("Svangerskapsytelse").takeIf(JsonNode::isObject)?.let(::asPeriode),
                        aktivitetslogger = aktivitetslogger
                    )
                }

                val sykepengehistorikk = ModelSykepengehistorikk(
                    perioder = message["@løsning"].path("Sykepengehistorikk").map(::asPeriode),
                    aktivitetslogger = aktivitetslogger
                )

                val ytelser = ModelYtelser(
                    hendelseId = UUID.randomUUID(),
                    aktørId = message["aktørId"].asText(),
                    fødselsnummer = message["fødselsnummer"].asText(),
                    organisasjonsnummer = message["organisasjonsnummer"].asText(),
                    vedtaksperiodeId = message["vedtaksperiodeId"].asText(),
                    sykepengehistorikk = sykepengehistorikk,
                    foreldrepenger = foreldrepenger,
                    rapportertdato = message["@besvart"].asLocalDateTime(),
                    originalJson = message.toJson()
                )

                listeners.forEach { it.onYtelser(ytelser) }

                if (aktivitetslogger.hasMessages()) {
                    sikkerLogg.info("meldinger om ytelser: $aktivitetslogger")
                }
            } catch (err: Aktivitetslogger) {
                sikkerLogg.info("feil om ytelser: ${err.message}", err)
            }


        }

        private fun JsonNode.asLocalDateTime() =
            asText().let { LocalDateTime.parse(it) }

        private fun asPeriode(jsonNode: JsonNode) =
            jsonNode.path("fom").asLocalDate() to jsonNode.path("tom").asLocalDate()

        override fun process(message: VilkårsgrunnlagMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Vilkårsgrunnlag.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onVilkårsgrunnlag(this) }
            } ?: aktivitetslogger.error("klarer ikke å mappe vilkårsgrunnlag til domenetype")
        }

        override fun process(message: ManuellSaksbehandlingMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            ManuellSaksbehandling.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onManuellSaksbehandling(this) }
            } ?: aktivitetslogger.error("klarer ikke å mappe manuellsaksbehandling til domenetype")
        }

        override fun process(message: PåminnelseMessage, aktivitetslogger: Aktivitetslogger) {
            // TODO: map til ordentlig domenehendelse uten kobling til json
            Påminnelse.Builder().build(message.toJson())?.apply {
                listeners.forEach { it.onPåminnelse(this) }
            } ?: aktivitetslogger.error("klarer ikke å mappe påminnelse til domenetype")
        }
    }
}
