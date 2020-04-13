package no.nav.helse.spleis

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.hendelser.model.HendelseMessage
import org.slf4j.Logger
import java.time.LocalDateTime
import java.util.*

internal class BehovMediator(
    private val rapidsConnection: RapidsConnection,
    private val sikkerLogg: Logger
) {
    internal fun håndter(message: HendelseMessage, hendelse: ArbeidstakerHendelse) {
        hendelse.kontekster().forEach { if (!it.hasErrors()) håndter(message, hendelse, it.behov()) }
    }

    private fun håndter(message: HendelseMessage, hendelse: ArbeidstakerHendelse, behov: List<Aktivitetslogg.Aktivitet.Behov>) {
        behov.groupBy { it.kontekst() }.forEach { (kontekst, behov) ->
            val behovsliste = mutableListOf<String>()
            val id = UUID.randomUUID()
            mutableMapOf(
                "@event_name" to "behov",
                "@opprettet" to LocalDateTime.now(),
                "@id" to id,
                "@behov" to behovsliste,
                "@forårsaket_av" to mapOf(
                    "event_name" to message.navn,
                    "id" to message.id,
                    "opprettet" to message.opprettet
                )
            )
                .apply {
                    putAll(kontekst)
                    behov.forEach { behov ->
                        require(behov.type.name !in behovsliste) { "Kan ikke produsere samme behov ${behov.type.name} på samme kontekst" }
                        require(behov.detaljer().filterKeys { this.containsKey(it) && this[it] != behov.detaljer()[it] }
                            .isEmpty()) { "Kan ikke produsere behov med duplikate detaljer" }
                        behovsliste.add(behov.type.name)
                        putAll(behov.detaljer())
                    }
                }
                .let { JsonMessage.newMessage(it) }
                .also {
                    sikkerLogg.info("sender {} som {}", id, it.toJson())
                    rapidsConnection.publish(hendelse.fødselsnummer(), it.toJson())
                }
        }
    }

}
