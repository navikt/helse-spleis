package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import org.slf4j.LoggerFactory

internal class DatadelingMediator(
    private val aktivitetslogg: Aktivitetslogg,
    private val meldingsreferanseId: UUID,
    private val fødselsnummer: String,
    private val aktørId: String
) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun ferdigstill(context: MessageContext) {
        if (aktivitetslogg.aktiviteter.isEmpty()) return
        sikkerlogg.info(
            "Publiserer aktiviteter som følge av hendelse med {}, {}, {}",
            keyValue("hendelseId", meldingsreferanseId),
            keyValue("fødselsnummer", fødselsnummer),
            keyValue("aktørId", aktørId)
        )
        val aktivitetMap = aktivitetslogg.aktiviteter.map { aktivitet ->
            when (aktivitet) {
                is Aktivitet.Behov -> aktivitetMap("BEHOV", aktivitet)
                is Aktivitet.Info -> aktivitetMap("INFO", aktivitet)
                is Aktivitet.LogiskFeil -> aktivitetMap("LOGISK_FEIL", aktivitet)
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
        context.publish(fødselsnummer, aktivitetMap.toJson())
    }

    private fun aktivitetMap(nivå: String, aktivitet: Aktivitet) =
        mapOf(
            "id" to aktivitet.id,
            "nivå" to nivå,
            "melding" to aktivitet.melding,
            "tidsstempel" to aktivitet.tidsstempel,
            "kontekster" to aktivitet.kontekster.map { it.toMap() }
        )

    private fun Collection<Map<String, Any>>.toJson(): String {
        return JsonMessage.newMessage(
            "aktivitetslogg_ny_aktivitet",
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "aktiviteter" to this
            )
        ).toJson()
    }
}