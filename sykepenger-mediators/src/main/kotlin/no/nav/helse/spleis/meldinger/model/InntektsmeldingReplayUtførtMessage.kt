package no.nav.helse.spleis.meldinger.model

import java.util.UUID
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.InntektsmeldingReplayUtført
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.spleis.IHendelseMediator

internal class InntektsmeldingReplayUtførtMessage(packet: JsonMessage) : HendelseMessage(packet) {
    override val fødselsnummer = packet["fødselsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
    override val skalDuplikatsjekkes = false

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, InntektsmeldingReplayUtført(id, fødselsnummer, aktørId, organisasjonsnummer, vedtaksperiodeId), context)
    }
}
