package no.nav.helse.spleis

import no.nav.helse.Topics
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

// Understands how to a read stream of messages from Kafka
internal class HendelseStream(private val topics: List<String>) {

    private val log = LoggerFactory.getLogger(HendelseStream::class.java)

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
            stream<String, String>(topics, consumeStrings)
                .through(Topics.rapidTopic)
                .foreach(::notifyListeners)
        }.build()

    private fun notifyListeners(message: String) {
        messageListeners.forEach { it.onMessage(message) }
    }

    interface MessageListener {
        fun onMessage(message: String)
    }

    private companion object {
        private val strings = Serdes.String()
        private val consumeStrings = Consumed.with(strings, strings)
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)
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
