package no.nav.helse.spleis

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.utboks.UtgåendeMelding
import org.slf4j.LoggerFactory

internal class DatadelingMediator(
    private val aktivitetslogg: Aktivitetslogg,
    private val message: HendelseMessage
) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun leggIUtboks(context: BehandlingContext) {
        if (aktivitetslogg.aktiviteter.isEmpty()) return
        sikkerlogg.info(
            "Publiserer aktiviteter som følge av hendelse med {}, {}",
            keyValue("hendelseId", message.meldingsporing.id),
            keyValue("fødselsnummer", message.meldingsporing.fødselsnummer)
        )
        val aktivitetMap = aktivitetslogg.aktiviteter.map { aktivitet ->
            when (aktivitet) {
                is Aktivitet.Info -> aktivitetMap("INFO", aktivitet)
                is Aktivitet.FunksjonellFeil -> {
                    if (aktivitet.kode.avviklet) sikkerlogg.warn("${aktivitet.kode} er ikke avviklet, men i bruk i spleis. Endre?")
                    aktivitetMap("FUNKSJONELL_FEIL", aktivitet) + mapOf("varselkode" to aktivitet.kode.name)
                }

                is Aktivitet.Varsel -> {
                    if (aktivitet.kode.avviklet) sikkerlogg.warn("${aktivitet.kode} er ikke avviklet, men i bruk i spleis. Endre?")
                    aktivitetMap("VARSEL", aktivitet) + mapOf("varselkode" to aktivitet.kode.name)
                }
            }
        }
        context.leggIUtboks { personidentifikator ->
            UtgåendeMelding.nyRapidmelding(
                eventName = "aktivitetslogg_ny_aktivitet",
                personidentifikator = personidentifikator,
                innhold = mapOf("aktiviteter" to aktivitetMap)
            )
        }
    }

    private fun aktivitetMap(nivå: String, aktivitet: Aktivitet) =
        mapOf(
            "id" to aktivitet.id,
            "nivå" to nivå,
            "melding" to aktivitet.melding,
            "tidsstempel" to aktivitet.tidsstempel,
            "kontekster" to aktivitet.kontekster.map {
                mapOf(
                    "konteksttype" to it.kontekstType,
                    "kontekstmap" to it.kontekstMap
                )
            }
        )
}
