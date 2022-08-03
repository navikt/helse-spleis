package no.nav.helse.spleis

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.Logger

internal class BehovMediator(private val sikkerLogg: Logger) {
    internal fun håndter(context: MessageContext, hendelse: PersonHendelse) {
        hendelse.kontekster().forEach {
            if (!it.hasErrorsOrWorse()) {
                håndter(context, hendelse, it.behov())
            }
        }
    }

    private fun håndter(context: MessageContext, hendelse: PersonHendelse, behov: List<Aktivitetslogg.Aktivitet.Behov>) {
        behov
            .groupBy { it.kontekst() }
            .onEach { (kontekst, behovMap) ->
                require(behovMap.size == behovMap.map { it.type.name }.toSet().size) {
                    sikkerLogg.error("Forsøkte å sende duplikate behov på kontekst ${kontekst.entries.joinToString { "${it.key}=${it.value}" }}")
                    "Kan ikke produsere samme behov på samme kontekst. Forsøkte å be om ${behovMap.joinToString { it.type.name }}"
                }
            }
            .forEach { (kontekst, liste) ->
                val behovMap = liste.associate { behov -> behov.type.name to behov.detaljer() }
                mutableMapOf<String, Any>()
                    .apply {
                        putAll(kontekst)
                        putAll(behovMap)
                    }
                    .let { JsonMessage.newNeed(behovMap.keys, it) }
                    .also {
                        sikkerLogg.info("sender behov for {}:\n{}", behovMap.keys, it.toJson())
                        context.publish(hendelse.fødselsnummer(), it.toJson())
                    }
            }
    }

}
