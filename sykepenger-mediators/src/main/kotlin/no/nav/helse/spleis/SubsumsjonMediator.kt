package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.etterlevelse.KontekstType
import no.nav.helse.etterlevelse.Subsumsjon
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.spleis.SubsumsjonMediator.SubsumsjonEvent.Companion.paragrafVersjonFormaterer
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

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
) : Subsumsjonslogg {

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(SubsumsjonMediator::class.java)
    }

    private val subsumsjoner = mutableListOf<SubsumsjonEvent>()

    override fun logg(subsumsjon: Subsumsjon) {
        bekreftAtSubsumsjonerHarKnytningTilBehandling(subsumsjon)
        subsumsjoner.add(SubsumsjonEvent(
            sporing = subsumsjon.kontekster
                .filterNot { it.type == KontekstType.Fødselsnummer }
                .groupBy({ it.type }) { it.verdi },
            lovverk = subsumsjon.lovverk,
            ikrafttredelse = paragrafVersjonFormaterer.format(subsumsjon.versjon),
            paragraf = subsumsjon.paragraf.ref,
            ledd = subsumsjon.ledd?.nummer,
            punktum = subsumsjon.punktum?.nummer,
            bokstav = subsumsjon.bokstav?.ref,
            input = subsumsjon.input,
            output = subsumsjon.output,
            utfall = subsumsjon.utfall.name
        )
        )
    }

    private fun bekreftAtSubsumsjonerHarKnytningTilBehandling(subsumsjon: Subsumsjon) {
        val kritiskeTyper = setOf(KontekstType.Fødselsnummer, KontekstType.Organisasjonsnummer)
        check(kritiskeTyper.all { kritiskType ->
            subsumsjon.kontekster.count { it.type == kritiskType } == 1
        }) {
            "en av $kritiskeTyper mangler/har duplikat:\n${subsumsjon.kontekster.joinToString(separator = "\n")}"
        }
        // todo: sjekker for mindre enn 1 også ettersom noen subsumsjoner skjer på arbeidsgivernivå. det burde vi forsøke å flytte/fikse slik at
        // alt kan subsummeres i kontekst av en behandling.
        check(subsumsjon.kontekster.count { it.type == KontekstType.Vedtaksperiode } <= 1) {
            "det er flere kontekster av ${KontekstType.Vedtaksperiode}:\n${subsumsjon.kontekster.joinToString(separator = "\n")}"
        }
    }

    fun ferdigstill(producer: Subsumsjonproducer) {
        if (subsumsjoner.isEmpty()) return
        logg.info("som følge av hendelse id=${message.meldingsporing.id} sendes ${subsumsjoner.size} subsumsjonsmeldinger på rapid")
        subsumsjoner
            .map { subsumsjonMelding(it) }
            .forEach {
                val jsonbody = it.toJson()
                sikkerLogg.info("som følge av hendelse id=${this.message.meldingsporing.id} sender subsumsjon: $message")
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
                this["versjon"] = "1.0.0"
                this["kilde"] = "spleis"
                this["versjonAvKode"] = versjonAvKode
                this["fodselsnummer"] = message.meldingsporing.fødselsnummer
                this["sporing"] = event.sporing.map { it.key.tilEkstern() to it.value }.toMap()
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

    private fun KontekstType.tilEkstern() = when (this) {
        KontekstType.Fødselsnummer -> "fodselsnummer"
        KontekstType.Organisasjonsnummer -> "organisasjonsnummer"
        KontekstType.Vedtaksperiode -> "vedtaksperiode"
        KontekstType.Sykmelding -> "sykmelding"
        KontekstType.Søknad -> "soknad"
        KontekstType.Inntektsmelding -> "inntektsmelding"
        KontekstType.InntektFraAOrdningen -> "inntektFraAOrdningen"
        KontekstType.OverstyrTidslinje -> "overstyrtidslinje"
        KontekstType.OverstyrArbeidsgiveropplysninger -> "overstyrarbeidsgiveropplysninger"
        KontekstType.OverstyrInntekt -> "overstyrinntekt"
        KontekstType.OverstyrRefusjon -> "overstyrrefusjon"
        KontekstType.OverstyrArbeidsforhold -> "overstyrarbeidsforhold"
        KontekstType.SkjønnsmessigFastsettelse -> "skjønnsmessigfastsettelse"
        KontekstType.AndreYtelser -> "andreytelser"
    }

    data class SubsumsjonEvent(
        val id: UUID = UUID.randomUUID(),
        val sporing: Map<KontekstType, List<String>>,
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
