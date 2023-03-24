package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.ForkastSykmeldingsperioderMessage

internal class ForkastSykmeldingsperioderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {

    override val eventName = "forkast_sykmeldingsperioder"
    override val riverName = "forkast_sykmeldingsperioder"

    override fun validate(message: JsonMessage) {
        message.requireKey(
            "@id",
            "aktørId",
            "fødselsnummer",
            "organisasjonsnummer",
            "fom",
            "tom"
        )
    }

    override fun createMessage(packet: JsonMessage) = ForkastSykmeldingsperioderMessage(packet)
}
