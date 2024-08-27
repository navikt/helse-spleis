package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Dagtype.Companion.dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrTidslinje
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator

internal class OverstyrTidslinjeMessage(val packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val dager = packet["dager"]
        .map {
            ManuellOverskrivingDag(
                dato = it["dato"].asLocalDate(),
                type = it["type"].asText().dagtype,
                grad = it["grad"]?.intValue()
            )
        }

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) =
        mediator.behandle(
            this, OverstyrTidslinje(
                meldingsreferanseId = id,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                dager = dager,
                opprettet = opprettet
            ), context
        )
}
