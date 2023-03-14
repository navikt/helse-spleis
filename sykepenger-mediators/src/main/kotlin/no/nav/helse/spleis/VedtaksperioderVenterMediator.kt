package no.nav.helse.spleis

import java.util.UUID
import no.nav.helse.person.PersonObserver
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

internal class VedtaksperioderVenterMediator: PersonObserver {

    private val vedtaksperioderVenter = mutableMapOf<UUID, PersonObserver.VedtaksperiodeVenterEvent>()
    override fun vedtaksperiodeVenter(event: PersonObserver.VedtaksperiodeVenterEvent) {
        vedtaksperioderVenter[event.vedtaksperiodeId] = event
    }

    internal fun finalize(context: MessageContext) {
        vedtaksperioderVenter.values.forEach { event ->
            val json = JsonMessage.newMessage(
                "vedtaksperiode_venter", mapOf(
                    "fødselsnummer" to event.fødselsnummer,
                    "aktørId" to event.aktørId,
                    "organisasjonsnummer" to event.organisasjonsnummer,
                    "vedtaksperiodeId" to event.vedtaksperiodeId,
                    "hendelser" to event.hendelser,
                    "ventetSiden" to event.ventetSiden,
                    "venterTil" to event.venterTil,
                    "venterPå" to mapOf(
                        "vedtaksperiodeId" to event.venterPå.vedtaksperiodeId,
                        "organisasjonsnummer" to event.venterPå.organisasjonsnummer,
                        "venteårsak" to mapOf(
                            "hva" to event.venterPå.venteårsak.hva,
                            "hvorfor" to event.venterPå.venteårsak.hvorfor
                        )
                    )
                )
            ).toJson()
            sikkerLogg.info("sender vedtaksperiode_venter:\n\t$json")
            context.publish(event.fødselsnummer, json)
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}