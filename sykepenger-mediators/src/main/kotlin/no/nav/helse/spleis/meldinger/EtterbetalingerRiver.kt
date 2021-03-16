package no.nav.helse.spleis.meldinger

import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.EtterbetalingMessage

internal class EtterbetalingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "Etterbetalingskandidat_v1"
    override val riverName = "Kandidat for etterbetaling"

    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer", "fagsystemId", "organisasjonsnummer", "gyldighetsdato")
        message.rejectKey("@løsning.${Sykepengehistorikk.name}")
    }

    override fun createMessage(packet: JsonMessage) = EtterbetalingMessage(packet)
}
