package no.nav.helse.spleis.påminnelse

import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.spleis.SakMediator
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream

internal class PåminnelseConsumer(
    streamsBuilder: StreamsBuilder,
    private val påminnelseTopic: String,
    private val mediator: SakMediator
) {

    init {
        build(streamsBuilder)
    }

    fun build(builder: StreamsBuilder): StreamsBuilder {
        builder.stream<String, String>(
            listOf(påminnelseTopic), Consumed.with(Serdes.String(), Serdes.String())
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST)
        )
            .mapValues { _, json ->
                Påminnelse.fraJson(json)
            }.filterNotNull()
            .foreach { _, påminnelse -> mediator.håndter(påminnelse) }

        return builder
    }

    private inline fun <reified V> KStream<*, V?>.filterNotNull(): KStream<*, V> =
        this.filter { _, value -> value != null }
            .mapValues { _, value -> value as V }
}

