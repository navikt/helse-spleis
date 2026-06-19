package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import java.sql.Connection
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory

internal class Utboks {
    private val utgåendeMeldinger = mutableListOf<UtgåendeMelding>()
    private var tilstand: Tilstand = Tilstand.Åpen

    fun nyMelding(melding: UtgåendeMelding) = apply {
        tilstand.nyMelding(melding, this)
    }

    fun lagre(connection: Connection, message: HendelseMessage) {
        check(!connection.autoCommit) { "Meldingene må lagres ned i samme transaksjon som personen lagres ned." }
        tilstand = Tilstand.Lukket
        sikkerLogg.info("Lagrer ${utgåendeMeldinger.size} meldinger fra utboksen")
        // TODO: Lagre i db
    }

    fun send(messageContext: MessageContext, message: HendelseMessage) {
        sikkerLogg.info("Sender ${utgåendeMeldinger.size} meldinger fra utboksen")
        utgåendeMeldinger
            .map { utgåendeMelding ->
                when (utgåendeMelding.eventName) {
                    "behov" -> {
                        val behov = utgåendeMelding.json.path("@behov").map { it.asText() }
                        sikkerLogg.info("sender behov (${behov.joinToString()}):\n\t${utgåendeMelding.json}")
                    }
                    else -> sikkerLogg.info("sender ${utgåendeMelding.eventName}:\n\t${utgåendeMelding.json}")
                }
                OutgoingMessage(body = utgåendeMelding.json.toString(), key = utgåendeMelding.key)
            }.sendOutgoingMessage(messageContext, message)
        // TODO: Marker OK-meldingene sendt i DB
    }

    private fun List<OutgoingMessage>.sendOutgoingMessage(messageContext: MessageContext, message: HendelseMessage) {
        if (this.isEmpty()) return
        message.logOutgoingMessages(sikkerLogg, this.size)
        val (ok, failed) = messageContext.publish(this)

        if (failed.isEmpty()) return
        val førsteFeil = failed.first().error
        val feilmelding = "Feil ved sending av ${failed.size} melding(er), ${ok.size} melding(er) gikk ok!\n" +
            "Disse meldingene feilet:\n" +
            failed.joinToString(separator = "\n") { "#${it.index}: ${it.error.message}\n\t${it.message}" }
        throw RuntimeException(feilmelding, førsteFeil)
    }

    private sealed interface Tilstand {
        fun nyMelding(melding: UtgåendeMelding, utboks: Utboks)

        data object Åpen: Tilstand {
            override fun nyMelding(melding: UtgåendeMelding, utboks: Utboks) {
                utboks.utgåendeMeldinger.add(melding)
            }
        }
        data object Lukket: Tilstand {
            override fun nyMelding(melding: UtgåendeMelding, utboks: Utboks) {
                error("Utboksen er lukket, kan ikke legge til melding")
            }
        }
    }

    internal companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        // Sendes utom utboks, "uviktige" meldinger, eller annet som gjør at de ikke kan/ikke gir mening å sendes via utboks
        internal fun MessageContext.fireAndForget(melding: UtgåendeMelding) =
            this.publish(listOf(OutgoingMessage(key = melding.key, body = melding.json.toString())))
    }
}
