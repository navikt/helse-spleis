@file:JvmName("HendelseMediatorMediatorKt")

package no.nav.helse.spleis

import no.nav.helse.Topics
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.*
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
        Påminnelse.fraJson(json)?.also {
            notifyListeners(HendelseListener::onPåminnelse, it)
        }
    }

    private fun onBehov(json: String) {
        val behov = try {
            Behov.fromJson(json)
        } catch (err: Exception) {
            log.info("kan ikke lese behov som json: ${err.message}", err)
            null
        }

        behov?.takeIf(Behov::erLøst)?.let {
            notifyListeners(HendelseListener::onLøstBehov, it)
        }
    }

    private fun onInntektsmelding(json: String) {
        Inntektsmelding.fromInntektsmelding(json)?.let {
            notifyListeners(HendelseListener::onInntektsmelding, it)
        }
    }

    private fun onSøknad(json: String) {
        SøknadHendelse.fromSøknad(json)?.also {
            when (it) {
                is NySøknad -> notifyListeners(HendelseListener::onNySøknad, it)
                is SendtSøknad -> notifyListeners(HendelseListener::onSendtSøknad, it)
            }
        }
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
