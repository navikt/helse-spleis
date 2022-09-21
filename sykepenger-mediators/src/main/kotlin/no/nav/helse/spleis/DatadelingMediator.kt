package no.nav.helse.spleis

import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.AktivitetsloggObserver
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.person.Varselkode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

class DatadelingMediator(private val hendelse: PersonHendelse): AktivitetsloggObserver {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val aktiviteter = mutableListOf<Map<String, Any>>()
    init {
        hendelse.register(this)
    }

    private fun aktivitetMap(id: UUID, label: Char, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: LocalDateTime) =
        mapOf(
            "id" to id,
            "nivå" to label.toFulltext(),
            "melding" to melding,
            "tidsstempel" to tidsstempel,
            "kontekster" to kontekster.map { it.toMap() }
        )

    override fun aktivitet(id: UUID, label: Char, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: LocalDateTime) {
        aktiviteter.add(aktivitetMap(id, label, melding, kontekster, tidsstempel))
    }

    override fun varsel(id: UUID, label: Char, kode: Varselkode?, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: LocalDateTime) {
        val aktivitetMap = aktivitetMap(id, label, melding, kontekster, tidsstempel).toMutableMap()
        if (kode != null) aktivitetMap["varselkode"] = kode
        aktiviteter.add(aktivitetMap.toMap())
    }

    internal fun finalize(context: MessageContext) {
        if (aktiviteter.isEmpty()) return
        sikkerlogg.info(
            "Publiserer aktiviteter som følge av hendelse med {}, {}, {}",
            keyValue("hendelseId", hendelse.meldingsreferanseId()),
            keyValue("fødselsnummer", hendelse.fødselsnummer()),
            keyValue("aktørId", hendelse.aktørId())
        )
        context.publish(hendelse.fødselsnummer(), aktiviteter.toJson())
    }

    private fun MutableList<Map<String, Any>>.toJson(): String {
        return JsonMessage.newMessage(
            "aktivitetslogg_ny_aktivitet",
            mapOf(
                "fødselsnummer" to hendelse.fødselsnummer(),
                "aktiviteter" to this
            )
        ).toJson()
    }

    private fun Char.toFulltext(): String {
        return when (this) {
            'I' -> "INFO"
            'N' -> "BEHOV"
            'W' -> "VARSEL"
            'E' -> "FUNKSJONELL_FEIL"
            'S' -> "LOGISK_FEIL"
            else -> throw IllegalArgumentException("$this er ikke en støttet aktivitetstype")
        }
    }
}