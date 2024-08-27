package no.nav.helse.spleis.meldinger.model

import java.util.UUID
import no.nav.helse.hendelser.AnmodningOmForkasting
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing a Påminnelse
internal class AnmodningOmForkastingMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText().let { UUID.fromString(it) }
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val force = packet["force"].takeIf { it.isBoolean }?.asBoolean() ?: false

    private val anmodning = AnmodningOmForkasting(
        meldingsreferanseId = id,
        aktørId = aktørId,
        organisasjonsnummer = organisasjonsnummer,
        fødselsnummer = fødselsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        force = force
    )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, anmodning, context)
    }
}
