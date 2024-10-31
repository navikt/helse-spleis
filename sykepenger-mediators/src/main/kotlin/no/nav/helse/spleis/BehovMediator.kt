package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import org.slf4j.Logger

internal class BehovMediator(private val sikkerLogg: Logger) {
    internal fun håndter(context: MessageContext, hendelse: Hendelse, aktivitetslogg: Aktivitetslogg) {
        if (aktivitetslogg.harFunksjonelleFeilEllerVerre()) return
        if (aktivitetslogg.behov.isEmpty()) return
        aktivitetslogg
            .behov
            .groupBy { it.kontekster }
            .forEach { (kontekster, behovMedSammeKontekster) ->
                val kontekstMap = kontekster.fold(emptyMap<String, String>()) { result, item -> result + item.kontekstMap }
                val behovMap =  behovMedSammeKontekster
                    .groupBy({ it.type.name }, { it.detaljer() })
                    .ikkeTillatUnikeDetaljerPåSammeBehov(kontekstMap, behovMedSammeKontekster)

                val meldingMap = kontekstMap + behovMap
                val behovmelding = JsonMessage.newNeed(behovMap.keys, meldingMap).toJson()

                sikkerLogg.info("sender behov for {}:\n{}", behovMap.keys, behovmelding)
                context.publish(hendelse.behandlingsporing.fødselsnummer, behovmelding)
            }
    }

    private fun <K: Any> Map<K, List<Map<String, Any?>>>.ikkeTillatUnikeDetaljerPåSammeBehov(kontekst: Map<String, String>, behovliste: List<Aktivitet.Behov>) =
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
