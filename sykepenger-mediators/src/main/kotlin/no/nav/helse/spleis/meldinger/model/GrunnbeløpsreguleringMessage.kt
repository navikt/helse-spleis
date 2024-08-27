package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Grunnbeløpsregulering
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
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
                skjæringstidspunkt,
                opprettet
            ),
            context
        )
    }
}
