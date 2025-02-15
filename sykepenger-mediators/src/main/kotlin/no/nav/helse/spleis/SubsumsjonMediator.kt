package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.etterlevelse.Regelverksporing
import no.nav.helse.spleis.SubsumsjonMediator.SubsumsjonEvent.Companion.paragrafVersjonFormaterer
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

interface Subsumsjonproducer {
    fun send(fnr: String, melding: String)

    class KafkaSubsumsjonproducer(private val topic: String, private val producer: KafkaProducer<String, String>) : Subsumsjonproducer {
        override fun send(fnr: String, melding: String) {
            producer.send(ProducerRecord(topic, fnr, melding))
        }
    }
}

internal class SubsumsjonMediator(
    private val message: HendelseMessage,
    private val versjonAvKode: String
) : Regelverkslogg {

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(SubsumsjonMediator::class.java)
    }

    private val subsumsjoner = mutableListOf<SubsumsjonEvent>()

    override fun logg(sporing: Regelverksporing) {
        val fnr = when (sporing) {
            is Regelverksporing.Arbeidsgiversporing -> sporing.fødselsnummer
            is Regelverksporing.Behandlingsporing -> sporing.fødselsnummer
        }
        val orgnr = when (sporing) {
            is Regelverksporing.Arbeidsgiversporing -> sporing.organisasjonsnummer
            is Regelverksporing.Behandlingsporing -> sporing.organisasjonsnummer
        }
        subsumsjoner.add(SubsumsjonEvent(
            fødselsnummer = fnr,
            organisasjonsnummer = orgnr,
            vedtaksperiodeId = (sporing as? Regelverksporing.Behandlingsporing)?.vedtaksperiodeId,
            behandlingId = (sporing as? Regelverksporing.Behandlingsporing)?.behandlingId,
            lovverk = sporing.subsumsjon.lovverk,
            ikrafttredelse = paragrafVersjonFormaterer.format(sporing.subsumsjon.versjon),
            paragraf = sporing.subsumsjon.paragraf.ref,
            ledd = sporing.subsumsjon.ledd?.nummer,
            punktum = sporing.subsumsjon.punktum?.nummer,
            bokstav = sporing.subsumsjon.bokstav?.ref,
            input = sporing.subsumsjon.input,
            output = sporing.subsumsjon.output,
            utfall = sporing.subsumsjon.utfall.name
        )
        )
    }

    fun ferdigstill(producer: Subsumsjonproducer) {
        if (subsumsjoner.isEmpty()) return
        logg.info("som følge av hendelse id=${message.meldingsporing.id} sendes ${subsumsjoner.size} subsumsjonsmeldinger på rapid")
        subsumsjoner
            .map { subsumsjonMelding(it) }
            .forEach {
                val jsonbody = it.toJson()
                sikkerLogg.info("som følge av hendelse id=${this.message.meldingsporing.id} sender subsumsjon: $jsonbody")
                producer.send(message.meldingsporing.fødselsnummer, jsonbody)
            }
    }

    private fun subsumsjonMelding(event: SubsumsjonEvent): JsonMessage {
        return JsonMessage.newMessage("subsumsjon", mapOf(
            "@id" to event.id,
            "@opprettet" to LocalDateTime.now(),
            "@forårsaket_av" to mapOf(
                "id" to message.meldingsporing.id
            ),
            "subsumsjon" to buildMap {
                this["id"] = event.id
                this["eventName"] = "subsumsjon"
                this["tidsstempel"] = ZonedDateTime.now()
                this["versjon"] = "1.1.0"
                this["kilde"] = "spleis"
                this["versjonAvKode"] = versjonAvKode
                this["fodselsnummer"] = event.fødselsnummer
                this["vedtaksperiodeId"] = event.vedtaksperiodeId
                this["behandlingId"] = event.behandlingId
                this["sporing"] = buildMap {
                    this["organisasjonsnummer"] = listOf(event.organisasjonsnummer)
                    if (event.vedtaksperiodeId != null) this["vedtaksperiode"] = listOf(event.vedtaksperiodeId.toString())
                }
                this["lovverk"] = event.lovverk
                this["lovverksversjon"] = event.ikrafttredelse
                this["paragraf"] = event.paragraf
                this["input"] = event.input
                this["output"] = event.output
                this["utfall"] = event.utfall
                if (event.ledd != null) {
                    this["ledd"] = event.ledd
                }
                if (event.punktum != null) {
                    this["punktum"] = event.punktum
                }
                if (event.bokstav != null) {
                    this["bokstav"] = event.bokstav
                }
            }
        ))
    }

    data class SubsumsjonEvent(
        val id: UUID = UUID.randomUUID(),
        val fødselsnummer: String,
        val organisasjonsnummer: String,
        val vedtaksperiodeId: UUID?,
        val behandlingId: UUID?,
        val lovverk: String,
        val ikrafttredelse: String,
        val paragraf: String,
        val ledd: Int?,
        val punktum: Int?,
        val bokstav: Char?,
        val input: Map<String, Any>,
        val output: Map<String, Any>,
        val utfall: String,
    ) {
        companion object {
            val paragrafVersjonFormaterer = DateTimeFormatter.ISO_DATE
        }
    }
}
