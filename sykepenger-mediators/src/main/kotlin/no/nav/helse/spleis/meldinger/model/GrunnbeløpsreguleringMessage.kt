package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator

internal class GrunnbeløpsreguleringMessage(val packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(
            this,
            Grunnbeløpsregulering(
                id,
                aktørId,
                fødselsnummer,
                skjæringstidspunkt
            ),
            context
        )
    }
}
