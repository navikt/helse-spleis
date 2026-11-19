package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.Behov.Behovstype.Sykepengegrunnlag
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.SykepengegrunnlagMessage
import tools.jackson.databind.JsonNode
import java.util.*

internal class SykepengegrunnlagRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator,
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Sykepengegrunnlag)
    override val riverName = "Sykepengegrunnlag"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId")
        message.interestedIn("organisasjonsnummer")
        message.require("yrkesaktivitetstype") {
            if (it.asText() == "ARBEIDSTAKER") {
                requireNotNull(message["organisasjonsnummer"].textValue()) { "Arbeidstaker må ha organisasjonsnummer" }
            }
        }
        message.require("${Sykepengegrunnlag.utgåendeNavn}.skjæringstidspunkt", JsonNode::asLocalDate)
        message.require("@løsning.${Sykepengegrunnlag.utgåendeNavn}.id") { UUID.fromString(it.asText()) }
    }

    override fun createMessage(packet: JsonMessage) =
        SykepengegrunnlagMessage(
            packet,
            Meldingsporing(
                id = packet.meldingsreferanseId(),
                fødselsnummer = packet["fødselsnummer"].asText(),
            ),
        )
}
