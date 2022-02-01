package no.nav.helse.spleis

import no.nav.helse.Toggle
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory
import java.time.LocalDateTime.now

internal class SubsumsjonMediator(private val jurist: MaskinellJurist, private val hendelse: PersonHendelse, private val message: HendelseMessage, private val versjonAvKode: String) {

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(PersonMediator::class.java)
    }

    fun finalize(rapidsConnection: RapidsConnection) {
        val events = jurist.events()
        if (events.isEmpty() || Toggle.SubsumsjonHendelser.disabled) return
        logg.info("som følge av hendelse id=${message.id} sendes ${events.size} subsumsjonsmeldinger på rapid")
        jurist.events().map { subsumsjonMelding(fødselsnummer = hendelse.fødselsnummer(), event = it) }.forEach {
            rapidsConnection.publish(key = hendelse.fødselsnummer(), message = it.toJson().also {sikkerLogg.info("som følge av hendelse id=${message.id} sender subsumsjon: $it") })
        }
    }

    private fun subsumsjonMelding(fødselsnummer: String, event: MaskinellJurist.SubsumsjonEvent) =
        JsonMessage.newMessage(
            mutableMapOf(
                "@id" to event.id,
                "@event_name" to "subsumsjon",
                "@opprettet" to now(),
                "versjon" to "1.0.0",
                "kilde" to "spleis",
                "versjon_av_kode" to versjonAvKode,
                "fodselsnummer" to fødselsnummer,
                "sporing" to event.sporing,
                "lovverk" to event.lovverk,
                "lovverk_versjon" to event.ikrafttredelse,
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
}
