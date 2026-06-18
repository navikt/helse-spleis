package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import no.nav.helse.person.EventBus
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.utboks.EventBusOversetter
import org.slf4j.LoggerFactory

internal class PersonMediator(
    private val message: HendelseMessage
) {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    fun ferdigstill(context: MessageContext, eventBus: EventBus) {
        EventBusOversetter(eventBus, message).utgåendeMeldinger()
            .map { utgåendeMelding ->
                when (utgåendeMelding.eventName) {
                    "behov" -> {
                        val behov = utgåendeMelding.json.path("@behov").map { it.asText() }
                        sikkerLogg.info("sender behov (${behov.joinToString()}):\n\t${utgåendeMelding.json}")
                    }
                    else -> sikkerLogg.info("sender ${utgåendeMelding.eventName}:\n\t${utgåendeMelding.json}")
                }
                OutgoingMessage(body = utgåendeMelding.json.toString(), key = utgåendeMelding.key)
            }
            .sendUtgåendeMeldinger(context)
    }

    private fun List<OutgoingMessage>.sendUtgåendeMeldinger(context: MessageContext) {
        if (this.isEmpty()) return
        message.logOutgoingMessages(sikkerLogg, this.size)
        val (ok, failed) = context.publish(this)

        if (failed.isEmpty()) return
        val førsteFeil = failed.first().error
        val feilmelding = "Feil ved sending av ${failed.size} melding(er), ${ok.size} melding(er) gikk ok!\n" +
            "Disse meldingene feilet:\n" +
            failed.joinToString(separator = "\n") { "#${it.index}: ${it.error.message}\n\t${it.message}" }
        throw RuntimeException(feilmelding, førsteFeil)
    }
}
