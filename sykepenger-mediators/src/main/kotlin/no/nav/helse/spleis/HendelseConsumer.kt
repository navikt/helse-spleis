package no.nav.helse.spleis

import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.inntektsmelding.Inntektsmelding
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.søknad.Sykepengesøknad
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

internal class HendelseConsumer {

    private val log = LoggerFactory.getLogger(HendelseConsumer::class.java)

    private val messageListeners = mutableListOf<MessageListener>()
    private val stateListeners = mutableListOf<KafkaStreams.StateListener>()

    private var kafkaStreams: KafkaStreams? = null

    init {
        stateListeners.add(stopOnError())
    }

    fun addListener(listener: MessageListener) {
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
            påminnelser { notifyListeners(MessageListener::onPåminnelse, it) }
            løsteBehov { notifyListeners(MessageListener::onLøstBehov, it) }
            inntektsmeldinger { notifyListeners(MessageListener::onInntektsmelding, it) }
            søknader().apply {
                nyeSøknader { notifyListeners(MessageListener::onNySøknad, it) }
                fremtidigeSøknader { notifyListeners(MessageListener::onFremtidigSøknad, it) }
                sendteSøknader { notifyListeners(MessageListener::onSendtSøknad, it) }
            }
        }.build()

    private fun <Event> notifyListeners(
        listener: MessageListener.(Event) -> Unit,
        event: Event
    ) {
        messageListeners.forEach { listener(it, event) }
    }

    private companion object {
        private val strings = Serdes.String()
        private val consumeStrings
            get() = Consumed.with(strings, strings)
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)

        private fun StreamsBuilder.påminnelser(onMessage: (Påminnelse) -> Unit) {
            stream<String, String>(listOf(Topics.påminnelseTopic), consumeStrings)
                .mapValues(Påminnelse.Companion::fraJson)
                .filterNotNull()
                .foreach(onMessage)
        }

        private fun StreamsBuilder.løsteBehov(onMessage: (Behov) -> Unit) {
            stream<String, String>(listOf(Topics.behovTopic), consumeStrings)
                .mapValues { json ->
                    try {
                        Behov.fromJson(json)
                    } catch (err: Exception) {
                        null
                    }
                }.filterNotNull()
                .filterValues(Behov::erLøst)
                .foreach(onMessage)
        }

        private fun StreamsBuilder.inntektsmeldinger(onMessage: (Inntektsmelding) -> Unit) {
            stream<String, String>(listOf(Topics.inntektsmeldingTopic), consumeStrings)
                .mapValues(Inntektsmelding.Companion::fromJson)
                .filterNotNull()
                .foreach(onMessage)
        }

        private fun KStream<String, Sykepengesøknad>.fremtidigeSøknader(onMessage: (Sykepengesøknad) -> Unit) {
            filterValues { it.status == "FREMTIDIG" }.foreach(onMessage)
        }

        private fun KStream<String, Sykepengesøknad>.nyeSøknader(onMessage: (Sykepengesøknad) -> Unit) {
            filterValues { it.status == "NY" }.foreach(onMessage)
        }

        private fun KStream<String, Sykepengesøknad>.sendteSøknader(onMessage: (Sykepengesøknad) -> Unit) {
            filterValues { it.status == "SENDT" }.foreach(onMessage)
        }

        private fun StreamsBuilder.søknader(): KStream<String, Sykepengesøknad> {
            return stream<String, String>(listOf(Topics.søknadTopic), consumeStrings)
                .mapValues(Sykepengesøknad.Companion::fromJson)
                .filterNotNull()
        }
    }

    interface MessageListener {
        fun onPåminnelse(påminnelse: Påminnelse) {}
        fun onLøstBehov(behov: Behov) {}
        fun onInntektsmelding(inntektsmelding: Inntektsmelding) {}
        fun onFremtidigSøknad(søknad: Sykepengesøknad) {}
        fun onNySøknad(søknad: Sykepengesøknad) {}
        fun onSendtSøknad(søknad: Sykepengesøknad) {}
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
