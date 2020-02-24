package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Topics
import no.nav.helse.behov.BehovType
import no.nav.helse.hendelser.HendelseObserver
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import java.time.LocalDateTime
import java.util.*

internal class BehovMediator(
    private val producer: KafkaProducer<String, String>,
    private val aktivitetslogg: Aktivitetslogg,
    private val sikkerLogg: Logger
) : HendelseObserver {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
    }

    private val behov = mutableMapOf<UUID, MutableList<BehovType>>()
    override fun onBehov(kontekstId:UUID, behov: BehovType) {
        this.behov.getOrPut(kontekstId) { mutableListOf() }.add(behov)
    }

    internal fun finalize(hendelse: ArbeidstakerHendelse) {
        if (behov.isEmpty()) return

        behov.values.forEach { behovliste ->
            sikkerLogg.info("sender ${behov.size} needs: ${behovliste.map { it.navn }} pga. ${hendelse::class.simpleName}")
            producer.send(
                ProducerRecord(
                    Topics.rapidTopic,
                    behovliste.first().fødselsnummer,
                    behovliste.toJson(aktivitetslogg).also {
                        sikkerLogg.info("sender $it pga. ${hendelse::class.simpleName}")
                    })
            )
        }

    }

    private fun List<BehovType>.toJson(aktivitetslogg: Aktivitetslogg) =
        fold(objectMapper.createObjectNode()) { acc: ObjectNode, behovType: BehovType ->
            val node = objectMapper.convertValue<ObjectNode>(behovType.toMap())
            if (acc.harKonflikterMed(node))
                aktivitetslogg.severe("Prøvde å sette sammen behov med konfliktende verdier, $acc og $node")
            acc.withArray("@behov").add(behovType.navn)
            acc.setAll(node)
        }
            .put("@event_name", "behov")
            .set<ObjectNode>("@opprettet", objectMapper.convertValue(
                LocalDateTime.now()
            ))
            .set<ObjectNode>("@id", objectMapper.convertValue(
                UUID.randomUUID()
            ))
            .toString()

    private fun ObjectNode.harKonflikterMed(other: ObjectNode) = fields()
        .asSequence()
        .filter { it.key in other.fieldNames().asSequence() }
        .any { it.value != other[it.key] }

}
