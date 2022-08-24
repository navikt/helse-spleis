package no.nav.helse.spleis

import java.time.LocalDateTime
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
        private const val MaksimaltAntallAktiviteterPerMelding = 500
    }

    private val meldinger = mutableListOf<String>()
    private val aktiviteter = mutableListOf<Map<String, Any>>()
    init {
        hendelse.register(this)
    }

    override fun aktivitet(label: Char, melding: String, kontekster: List<SpesifikkKontekst>, tidsstempel: LocalDateTime) {
        aktiviteter.add(
            mapOf(
                "nivå" to label.toFulltext(),
                "melding" to melding,
                "tidsstempel" to tidsstempel,
                "kontekster" to kontekster.map { it.toMap() }
            )
        )
        sjekkMeldingsgrense()
    }

    private fun sjekkMeldingsgrense() {
        if (aktiviteter.size < MaksimaltAntallAktiviteterPerMelding) return
        lagMelding()
    }

    private fun lagMelding() {
        if (aktiviteter.isEmpty()) return
        meldinger.add(aktiviteter.toJson())
        aktiviteter.clear()
    }

    internal fun finalize(context: MessageContext) {
        lagMelding()
        if (meldinger.isEmpty()) return
        sikkerlogg.info(
            "Publiserer aktiviteter (fordelt på ${meldinger.size} meldinger (${meldinger.joinToString { "${it.length} bytes" }})) som følge av hendelse med {}, {}, {}",
            keyValue("hendelseId", hendelse.meldingsreferanseId()),
            keyValue("fødselsnummer", hendelse.fødselsnummer()),
            keyValue("aktørId", hendelse.aktørId())
        )
        meldinger.forEach { context.publish(hendelse.fødselsnummer(), it) }
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
            'W' -> "VARSEL"
            'E' -> "FUNKSJONELL_FEIL"
            'S' -> "LOGISK_FEIL"
            else -> throw IllegalArgumentException("$this er ikke en støttet aktivitetstype")
        }
    }
}