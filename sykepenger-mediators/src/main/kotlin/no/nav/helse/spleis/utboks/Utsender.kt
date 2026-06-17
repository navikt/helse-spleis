package no.nav.helse.spleis.utboks

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

data class Kvittering(
    val sendt: Instant,
    val ok: List<UtgåendeMelding>,
    val feilet: List<UtgåendeMelding>
)

internal abstract class Utsender {
    private fun preprosesser(utgåendeMeldinger: List<UtgåendeMelding>, sendt: Instant) = utgåendeMeldinger.map { utgåendeMelding ->
        check(utgåendeMeldinger.distinctBy { it.id }.size == utgåendeMeldinger.size) { "Duplikate id'er i utgående meldinger" }
        utgåendeMelding.copy(
            json = utgåendeMelding.json.apply {
                put("@sendt", sendt.toString())
            }
        )
    }

    fun send(utgåendeMelding: List<UtgåendeMelding>): Kvittering {
        val sendt = Instant.now()
        val (ok, feilet) = utførSending(utgåendeMeldinger = preprosesser(utgåendeMelding, sendt), sendt = sendt)
        val feiledeMeldingIder = feilet.map { it.id }
        return Kvittering(sendt = sendt, ok = ok, feilet = utgåendeMelding.filter { it.id in feiledeMeldingIder })
    }

    protected abstract fun utførSending(utgåendeMeldinger: List<UtgåendeMelding>, sendt: Instant): Pair<List<UtgåendeMelding>, List<UtgåendeMelding>>

    protected companion object {
        val objectmapper = jacksonObjectMapper()
        val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    data class KafkaUtsender(
        private val producer: KafkaProducer<String, String>,
        private val loglevelVedFeil: Level = Level.ERROR,
        private val vedFeil: () -> Unit = {}
    ): Utsender() {

        override fun utførSending(utgåendeMeldinger: List<UtgåendeMelding>, sendt: Instant): Pair<List<UtgåendeMelding>, List<UtgåendeMelding>> {
            if (utgåendeMeldinger.isEmpty()) return emptyList<UtgåendeMelding>() to emptyList()

            val sendingsresultat = utgåendeMeldinger.associateWith { utgåendeMelding ->
                val topic = when (utgåendeMelding.mottaker) {
                    UtgåendeMelding.Mottaker.RAPID -> "tbd.rapid.v1"
                    UtgåendeMelding.Mottaker.SUBSUMSJON -> "tbd.subsumsjon.v1"
                }
                ProducerRecord(topic, utgåendeMelding.key, utgåendeMelding.json.toString())
            }.mapValues { (_, record) ->
                producer.send(record)
            }.map { (utgåendeMelding, future) ->
                try {
                    Sendingsresultat.Ok(utgåendeMelding,future.get())
                } catch (exception: Exception) {
                    Sendingsresultat.Feil(utgåendeMelding, exception)
                }
            }

            val ok = sendingsresultat.filterIsInstance<Sendingsresultat.Ok>()
            val feil = sendingsresultat.filterIsInstance<Sendingsresultat.Feil>()

            if (feil.isNotEmpty()) {
                sikkerlogg.atLevel(loglevelVedFeil).log("Feil ved sending av meldinger. Forsøkte å sende ${utgåendeMeldinger.size} meldinger. ${ok.size} ble sendt, og ${feil.size} feilet", feil.first().exception)
                vedFeil()
            }

            return ok.map(Sendingsresultat.Ok::utgåendeMelding) to feil.map(Sendingsresultat.Feil::utgåendeMelding)
        }

        private sealed interface Sendingsresultat {
            val utgåendeMelding: UtgåendeMelding
            data class Ok(override val utgåendeMelding: UtgåendeMelding, val metadata: RecordMetadata): Sendingsresultat
            data class Feil(override val utgåendeMelding: UtgåendeMelding, val exception: Exception): Sendingsresultat
        }
    }
}

