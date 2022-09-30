package no.nav.helse.spleis

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.Logger

internal class BehovMediator(private val sikkerLogg: Logger) {
    internal fun håndter(context: MessageContext, hendelse: PersonHendelse) {
        hendelse.kontekster().forEach {
            if (!it.harFunksjonelleFeilEllerVerre()) {
                håndter(context, hendelse, it.behov())
            }
        }
    }

    private fun håndter(context: MessageContext, hendelse: PersonHendelse, behov: List<Aktivitetslogg.Aktivitet.Behov>) {
        behov
            .groupBy { it.kontekst() }
            .grupperBehovTilDetaljer()
            .forEach { (kontekst, behovMap) ->
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

    private fun Map<Map<String, String>, List<Aktivitetslogg.Aktivitet.Behov>>.grupperBehovTilDetaljer() =
        mapValues { (kontekst, behovliste) ->
            behovliste
                .groupBy({ it.type.name }, { it.detaljer() })
                .ikkeTillatUnikeDetaljerPåSammeBehov(kontekst, behovliste)
        }

    private fun <K: Any> Map<K, List<Map<String, Any?>>>.ikkeTillatUnikeDetaljerPåSammeBehov(kontekst: Map<String, String>, behovliste: List<Aktivitetslogg.Aktivitet.Behov>) =
        mapValues { (_, detaljerList) ->
            // tillater duplikate detaljer-maps, så lenge de er like
            detaljerList
                .distinct()
                .also { detaljer ->
                    require(detaljer.size == 1) {
                        sikkerLogg.error("Forsøkte å sende duplikate behov på kontekst ${kontekst.entries.joinToString { "${it.key}=${it.value}" }}")
                        "Kan ikke produsere samme behov på samme kontekst med ulike detaljer. Forsøkte å be om ${behovliste.joinToString { it.type.name }}"
                    }
                }
                .single()
        }
}
