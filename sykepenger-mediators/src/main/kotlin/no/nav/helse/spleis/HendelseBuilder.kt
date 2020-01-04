package no.nav.helse.spleis

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.*
import no.nav.helse.person.ArbeidstakerHendelse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

internal class HendelseBuilder() {

    private val log = LoggerFactory.getLogger(HendelseBuilder::class.java)
    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

    private val messageListeners = mutableListOf<HendelseListener>()
    private val stateListeners = mutableListOf<KafkaStreams.StateListener>()

    private var kafkaStreams: KafkaStreams? = null

    init {
        stateListeners.add(stopOnError())
    }

    fun addListener(listener: HendelseListener) {
        messageListeners.add(listener)
    }

    fun addStateListener(stateListener: KafkaStreams.StateListener) {
        stateListeners.add(stateListener)
    }

    fun start(props: Properties) {
        if (kafkaStreams != null) return
        kafkaStreams = KafkaStreams(build(), props)
            .also(::addStateListeners)
            .also(::stopOnUncaughtExceptions)
            .also(KafkaStreams::start)
    }


    fun stop() {
        requireNotNull(kafkaStreams)
            .close(Duration.ofSeconds(10))
    }

    private fun onPåminnelse(json: String) {
        json.loggHendelse(ArbeidstakerHendelse.Hendelsetype.Påminnelse.name)
        Påminnelse.fraJson(json)?.also {
            notifyListeners(HendelseListener::onPåminnelse, it)
        }
    }

    private fun onBehov(json: String) {
        json.loggHendelse("Behov")

        val behov = try {
            Behov.fromJson(json)
        } catch (err: Exception) {
            return log.info("kan ikke lese behov som json: ${err.message}", err)
        }

        if (!behov.erLøst()) return

        if (behov.hendelsetype() == ArbeidstakerHendelse.Hendelsetype.Ytelser) {
            json.loggHendelse(ArbeidstakerHendelse.Hendelsetype.Ytelser.name)
            Ytelser(behov).also {
                notifyListeners(HendelseListener::onYtelser, it)
            }
        } else if (behov.hendelsetype() == ArbeidstakerHendelse.Hendelsetype.ManuellSaksbehandling) {
            json.loggHendelse(ArbeidstakerHendelse.Hendelsetype.ManuellSaksbehandling.name)
            ManuellSaksbehandling(behov).also {
                notifyListeners(HendelseListener::onManuellSaksbehandling, it)
            }
        } else if (behov.hendelsetype() == ArbeidstakerHendelse.Hendelsetype.Vilkårsgrunnlag) {
            json.loggHendelse(ArbeidstakerHendelse.Hendelsetype.Vilkårsgrunnlag.name)
            Vilkårsgrunnlag(behov).also {
                notifyListeners(HendelseListener::onVilkårsgrunnlag, it)
            }
        }
    }

    private fun onInntektsmelding(json: String) {
        json.loggHendelse(ArbeidstakerHendelse.Hendelsetype.Inntektsmelding.name)
        Inntektsmelding.fromInntektsmelding(json)?.let {
            notifyListeners(HendelseListener::onInntektsmelding, it)
        }
    }

    private fun onSøknad(json: String) {
        json.loggHendelse("Søknad")

        SøknadHendelse.fromSøknad(json)?.also {
            when (it) {
                is NySøknad -> notifyListeners(HendelseListener::onNySøknad, it)
                is SendtSøknad -> notifyListeners(HendelseListener::onSendtSøknad, it)
            }
        }
    }

    private fun String.loggHendelse(hendelsetype: String) {
        sikkerLogg.info(this, keyValue(
            "hendelsetype", hendelsetype
        ))
    }

    private fun stopOnError() =
        KafkaStreams.StateListener { newState, _ ->
            if (newState == KafkaStreams.State.ERROR) {
                log.error("stopping stream because stream went into error state")
                stop()
            }
        }

    private fun stopOnUncaughtExceptions(kafkaStreams: KafkaStreams) {
        kafkaStreams.setUncaughtExceptionHandler { _, err ->
            log.error("Caught exception in stream: ${err.message}", err)
            stop()
        }
    }

    private fun addStateListeners(kafkaStreams: KafkaStreams) {
        kafkaStreams.setStateListener { newState, oldState ->
            stateListeners.forEach { it.onChange(newState, oldState) }
        }
    }

    private fun build() =
        StreamsBuilder().apply {
            påminnelser(::onPåminnelse)
            løsteBehov(::onBehov)
            inntektsmeldinger(::onInntektsmelding)
            søknader(::onSøknad)
        }.build()

    private fun <Event> notifyListeners(
        listener: HendelseListener.(Event) -> Unit,
        event: Event
    ) {
        messageListeners.forEach { listener(it, event) }
    }

    private companion object {
        private val strings = Serdes.String()
        private val consumeStrings
            get() = Consumed.with(strings, strings)
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)

        private fun StreamsBuilder.påminnelser(onMessage: (String) -> Unit) {
            stream<String, String>(listOf(Topics.påminnelseTopic), consumeStrings)
                .foreach(onMessage)
        }

        private fun StreamsBuilder.løsteBehov(onMessage: (String) -> Unit) {
            stream<String, String>(listOf(Topics.behovTopic), consumeStrings)
                .foreach(onMessage)
        }

        private fun StreamsBuilder.inntektsmeldinger(onMessage: (String) -> Unit) {
            stream<String, String>(listOf(Topics.inntektsmeldingTopic), consumeStrings)
                .foreach(onMessage)
        }

        private fun StreamsBuilder.søknader(onMessage: (String) -> Unit) {
            stream<String, String>(listOf(Topics.søknadTopic), consumeStrings)
                .foreach(onMessage)
        }
    }
}

private fun <K, V> KStream<K, V>.foreach(action: (V) -> Unit) = this.foreach { _, value ->
    action(value)
}

private fun <K, V> KStream<K, V>.filterValues(filter: (V) -> Boolean): KStream<K, V> = this.filter { _, value ->
    filter(value)
}

private inline fun <K, reified V> KStream<K, V?>.filterNotNull(): KStream<K, V> = filterValues { it != null }
    .mapValues { _, value -> value as V }
