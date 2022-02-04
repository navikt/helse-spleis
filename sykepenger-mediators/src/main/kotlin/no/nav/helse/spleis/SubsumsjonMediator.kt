package no.nav.helse.spleis

import no.nav.helse.Toggle
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory
import java.time.LocalDateTime.now

internal class SubsumsjonMediator(
    private val jurist: MaskinellJurist,
    private val fødselsnummer: String,
    private val message: HendelseMessage,
    private val versjonAvKode: String
) {

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(PersonMediator::class.java)
    }

    fun finalize(rapidsConnection: RapidsConnection) {
        val events = jurist.events()
        if (events.isEmpty() || Toggle.SubsumsjonHendelser.disabled) return
        logg.info("som følge av hendelse id=${message.id} sendes ${events.size} subsumsjonsmeldinger på rapid")
        jurist
            .events()
            .map { subsumsjonMelding(fødselsnummer = fødselsnummer, event = it) }
            .forEach {
                rapidsConnection.publish(key = fødselsnummer, message = it.toJson().also { message ->
                    sikkerLogg.info("som følge av hendelse id=${this.message.id} sender subsumsjon: $message")
                })
            }
    }

    private fun subsumsjonMelding(fødselsnummer: String, event: MaskinellJurist.SubsumsjonEvent): JsonMessage {
        val tidsstempel = now()
        return JsonMessage.newMessage(
            mapOf(
                "@id" to event.id,
                "@event_name" to "subsumsjon",
                "@opprettet" to tidsstempel,
                "subsumsjon" to mutableMapOf(
                    "id" to event.id,
                    "eventName" to "subsumsjon",
                    "tidsstempel" to tidsstempel,
                    "versjon" to "1.0.0",
                    "kilde" to "spleis",
                    "versjonAvKode" to versjonAvKode,
                    "fodselsnummer" to fødselsnummer,
                    "sporing" to event.sporing.mapValues { listOf(it.value) },
                    "lovverk" to event.lovverk,
                    "lovverksversjon" to event.ikrafttredelse,
                    "paragraf" to event.paragraf,
                    "input" to event.input,
                    "output" to event.output,
                    "utfall" to event.utfall
                ).apply {
                    compute("ledd") { _, _ -> event.ledd }
                    compute("punktum") { _, _ -> event.punktum }
                    compute("bokstav") { _, _ -> event.bokstav }
                }
            )
        )
    }
}
