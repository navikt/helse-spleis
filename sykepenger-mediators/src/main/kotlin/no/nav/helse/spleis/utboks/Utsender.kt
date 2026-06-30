package no.nav.helse.spleis.utboks

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.util.concurrent.Future
import no.nav.helse.ApplicationBuilder
import no.nav.helse.Toggle
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
    private fun preprosesser(utgåendeMeldinger: List<UtgåendeMelding>, sendt: Instant): List<UtgåendeMelding> {
        check(utgåendeMeldinger.distinctBy { it.id }.size == utgåendeMeldinger.size) { "Duplikate id'er i utgående meldinger" }
        return utgåendeMeldinger.map { utgåendeMelding ->
            utgåendeMelding.copy(
                json = utgåendeMelding.json.apply {
                    put("@sendt", sendt.toString())
                }
            )
        }
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

    class KafkaUtsender private constructor(
        private val producer: KafkaProducer<String, String>,
        private val loglevelVedFeil: Level,
        private val vedFeil: () -> Unit
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
                try {
                    Bufferresultat.Sendeklar(producer.send(record))
                } catch (exception: Exception) {
                    Bufferresultat.Feil(exception)
                }
            }.map { (utgåendeMelding, bufferresultat)  ->
                when (bufferresultat) {
                    is Bufferresultat.Feil -> Sendingsresultat.Feil(utgåendeMelding, bufferresultat.exception)
                    is Bufferresultat.Sendeklar -> try {
                        Sendingsresultat.Ok(utgåendeMelding, bufferresultat.metadata.get())
                    } catch (exception: Exception) {
                        Sendingsresultat.Feil(utgåendeMelding, exception)
                    }
                }
            }

            val ok = sendingsresultat.filterIsInstance<Sendingsresultat.Ok>()
            val feil = sendingsresultat.filterIsInstance<Sendingsresultat.Feil>()

            if (feil.isNotEmpty()) {
                val feilmelding =
                    "Feil ved sending av ${feil.size} melding(er), ${ok.size} melding(er) gikk ok!\n" +
                    "Disse meldingene feilet:\n\n" +
                    feil.joinToString(separator = "\n") { "${it.utgåendeMelding.id}: ${it.exception.message}\n\t${it.utgåendeMelding.json}" }

                sikkerlogg.atLevel(loglevelVedFeil).log(feilmelding, feil.first().exception)
                vedFeil()
            }

            return ok.map(Sendingsresultat.Ok::utgåendeMelding) to feil.map(Sendingsresultat.Feil::utgåendeMelding)
        }

        private sealed interface Bufferresultat {
            data class Sendeklar(val metadata: Future<RecordMetadata>): Bufferresultat
            data class Feil(val exception: Exception): Bufferresultat
        }

        private sealed interface Sendingsresultat {
            val utgåendeMelding: UtgåendeMelding
            data class Ok(override val utgåendeMelding: UtgåendeMelding, val metadata: RecordMetadata): Sendingsresultat
            data class Feil(override val utgåendeMelding: UtgåendeMelding, val exception: Exception): Sendingsresultat
        }

        internal companion object {
            fun opprett(producer: KafkaProducer<String, String>, applicationBuilder: ApplicationBuilder) = when (Toggle.BrukUtboks.enabled) {
                true -> KafkaUtsender(producer, loglevelVedFeil = Level.WARN, vedFeil = { /* gjør ingenting - vil bli forsøkt sendt senere */ })
                false -> KafkaUtsender(producer, loglevelVedFeil = Level.ERROR, vedFeil = {
                    applicationBuilder.stop()
                    error("Feil ved utsending av meldinger fra utboks. Se sikkerlogg for detaljer.")
                })
            }
        }
    }
}

