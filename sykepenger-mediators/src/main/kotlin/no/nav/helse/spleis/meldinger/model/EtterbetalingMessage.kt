package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate

internal class EtterbetalingMessage(val packet: MessageDelegate) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val fagsystemId = packet["fagsystemId"].asText()
    private val gyldighetsdato = packet["gyldighetsdato"].asLocalDate()


    override fun behandle(mediator: IHendelseMediator) =
        mediator.behandle(
            this,
            Grunnbeløpsregulering(id, aktørId, fødselsnummer, organisasjonsnummer, gyldighetsdato, fagsystemId)
        )
}
