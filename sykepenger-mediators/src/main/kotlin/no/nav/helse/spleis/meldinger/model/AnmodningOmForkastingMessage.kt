package no.nav.helse.spleis.meldinger.model

import java.util.UUID
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.spleis.IHendelseMediator

internal class AnmodningOmForkastingMessage(
    packet: JsonMessage,
    vedtaksperiodeId: UUID,
    organisasjonsnummer: String,
    aktørId: String,
    override val fødselsnummer: String,
    force: Boolean
) : HendelseMessage(packet) {

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
