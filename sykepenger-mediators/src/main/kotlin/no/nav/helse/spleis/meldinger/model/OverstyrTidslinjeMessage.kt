package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator

internal class OverstyrTidslinjeMessage(val packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val dager = packet["dager"]
        .map {
            ManuellOverskrivingDag(
                dato = it["dato"].asLocalDate(),
                type = Dagtype.valueOf(it["dagtype"].asText()),
                grad = it["grad"]?.intValue()
            )
        }

    override fun behandle(mediator: IHendelseMediator) =
        mediator.behandle(
            this, OverstyrTidslinje(
                meldingsreferanseId = id,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                dager = dager
            )
        )
}
