package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.lang.IllegalArgumentException
import no.nav.helse.person.AktivitetsloggObserver
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.intellij.lang.annotations.Language

class DatadelingMediator(hendelse: PersonHendelse, private val fødselsnummer: String): AktivitetsloggObserver {
    private val aktiviteter = mutableListOf<Map<String, Any>>()
    init {
        hendelse.register(this)
    }

    override fun aktivitet(label: Char, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: String) {
        aktiviteter.add(mapOf("nivå" to label.toFulltext(), "melding" to melding, "tidsstempel" to tidsstempel, "kontekster" to kontekster))
    }

    internal fun finalize(context: MessageContext) {
        if (aktiviteter.isEmpty()) return
        context.publish(fødselsnummer, aktiviteter.toJson())
    }

    @Language("JSON")
    private fun MutableList<Map<String, Any>>.toJson(): String {
        return JsonMessage.newMessage(
            "aktivitetslogg_ny_aktivitet",
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "aktiviteter" to jacksonObjectMapper().writeValueAsString(this)
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