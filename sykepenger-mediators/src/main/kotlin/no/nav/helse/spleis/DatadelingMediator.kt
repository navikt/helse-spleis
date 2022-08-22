package no.nav.helse.spleis

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.AktivitetsloggObserver
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

class DatadelingMediator(private val hendelse: PersonHendelse): AktivitetsloggObserver {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }

    private val aktiviteter = mutableListOf<Map<String, Any>>()
    init {
        hendelse.register(this)
    }

    override fun aktivitet(label: Char, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: String) {
        aktiviteter.add(
            mapOf(
                "nivå" to label.toFulltext(),
                "melding" to melding,
                "tidsstempel" to LocalDateTime.parse(tidsstempel, formatter),
                "kontekster" to kontekster.map { it.toMap() }
            )
        )
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
            'I' -> "Info"
            'W' -> "Warning"
            'E' -> "Error"
            'S' -> "Severe"
            else -> throw IllegalArgumentException("$this er ikke en støttet aktivitetstype")
        }
    }
}