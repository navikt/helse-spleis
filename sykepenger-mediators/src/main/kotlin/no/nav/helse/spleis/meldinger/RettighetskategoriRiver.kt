package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori
import no.nav.helse.spleis.Behov.Behovstype.Rettighetskategori
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.RettighetskategoriMessage
import tools.jackson.databind.JsonNode
import java.util.*

internal class RettighetskategoriRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator,
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Rettighetskategori)
    override val riverName = "Rettighetskategori"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId")
        message.interestedIn("organisasjonsnummer")
        message.require("yrkesaktivitetstype") {
            if (it.asText() ==
                "ARBEIDSTAKER"
            ) {
                requireNotNull(message["organisasjonsnummer"].textValue()) { "Arbeidstaker må ha organisasjonsnummer" }
            }
        }
        message.require("${Rettighetskategori.utgåendeNavn}.skjæringstidspunkt", JsonNode::asLocalDate)
        message.require("@løsning.${Rettighetskategori.utgåendeNavn}.id") { UUID.fromString(it.asText()) }
        message.require("@løsning.${Rettighetskategori.utgåendeNavn}.hovedkategori") { Hovedkategori.valueOf(it.asText()) }
    }

    override fun createMessage(packet: JsonMessage) =
        RettighetskategoriMessage(
            packet,
            Meldingsporing(
                id = packet.meldingsreferanseId(),
                fødselsnummer = packet["fødselsnummer"].asText(),
            ),
        )
}
