package no.nav.helse.spleis

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import java.time.LocalDateTime
import java.util.*

internal class BehovMediator(
    private val rapidsConnection: RapidsConnection,
    private val sikkerLogg: Logger
) {
    internal fun håndter(hendelse: ArbeidstakerHendelse) {
        hendelse.kontekster().forEach { if (!it.hasErrors()) håndter(hendelse, it.behov()) }
    }

    private fun håndter(hendelse: ArbeidstakerHendelse, behov: List<Aktivitetslogg.Aktivitet.Behov>) {
        behov.groupBy { it.kontekst() }.forEach { (kontekst, behov) ->
            val behovsliste = mutableListOf<String>()
            val id = UUID.randomUUID()
            mutableMapOf(
                "@event_name" to "behov",
                "@opprettet" to LocalDateTime.now(),
                "@id" to id,
                "@behov" to behovsliste
            )
                .apply {
                    putAll(kontekst)
                    behov.forEach {
                        require(it.type.name !in behovsliste) { "Kan ikke produsere samme behov $it.type.name på samme kontekst" }
                        require(it.detaljer().filterKeys { this.containsKey(it) }
                            .isEmpty()) { "Kan ikke produsere behov med duplikate detaljer" }
                        behovsliste.add(it.type.name)
                        putAll(it.detaljer())
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
