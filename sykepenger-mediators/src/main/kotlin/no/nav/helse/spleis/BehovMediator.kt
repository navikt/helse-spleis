package no.nav.helse.spleis

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.Logger
import java.time.LocalDateTime
import java.util.*

internal class BehovMediator(
    private val rapidsConnection: RapidsConnection,
    private val sikkerLogg: Logger
) {
    internal fun håndter(message: HendelseMessage, hendelse: PersonHendelse) {
        hendelse.kontekster().forEach {
            if (!it.hasErrorsOrWorse()) {
                håndter(message, hendelse, it.behov())
            }
        }
    }

    private fun håndter(message: HendelseMessage, hendelse: PersonHendelse, behov: List<Aktivitetslogg.Aktivitet.Behov>) {
        behov
            .groupBy { it.kontekst() }
            .onEach { (_, behovMap) ->
                require(behovMap.size == behovMap.map { it.type.name }.toSet().size) { "Kan ikke produsere samme behov på samme kontekst" }
            }
            .forEach { (kontekst, liste) ->
                val id = UUID.randomUUID()
                val behovMap = liste.associate { behov -> behov.type.name to behov.detaljer() }

                mutableMapOf(
                    "@event_name" to "behov",
                    "@opprettet" to LocalDateTime.now(),
                    "@id" to id,
                    "@behov" to behovMap.keys,
                    "@forårsaket_av" to message.tracinginfo()
                )
                    .apply {
                        putAll(kontekst)
                        putAll(behovMap)
                    }
                    .let { JsonMessage.newMessage(it) }
                    .also {
                        sikkerLogg.info("sender {} som {}", id, it.toJson())
                        rapidsConnection.publish(hendelse.fødselsnummer(), it.toJson())
                    }
            }
    }

}
