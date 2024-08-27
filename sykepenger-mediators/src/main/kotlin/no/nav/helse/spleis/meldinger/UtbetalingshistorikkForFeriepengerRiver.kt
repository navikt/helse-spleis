package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.SykepengehistorikkForFeriepenger
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkForFeriepengerMessage

/**
 * Entry point for å starte utbetaling av feriepenger
 */
internal class UtbetalingshistorikkForFeriepengerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(SykepengehistorikkForFeriepenger)
    override val riverName = "UtbetalingshistorikkForFeriepenger"

    override fun validate(message: JsonMessage) {
        validerSykepengehistorikk(message)
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingshistorikkForFeriepengerMessage(packet)

    internal companion object {
        fun validerSykepengehistorikk(message: JsonMessage) {
            message.requireKey("${SykepengehistorikkForFeriepenger.name}.historikkFom")
            message.requireKey("@løsning.${SykepengehistorikkForFeriepenger.name}.feriepengerSkalBeregnesManuelt")
            message.requireArray("@løsning.${SykepengehistorikkForFeriepenger.name}.utbetalinger") {
                interestedIn("fom", JsonNode::asLocalDate)
                interestedIn("tom", JsonNode::asLocalDate)
                requireKey("dagsats", "utbetalingsGrad", "orgnummer")
                requireAny("typeKode", listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "O", "S", ""))
            }
            message.requireArray("@løsning.${SykepengehistorikkForFeriepenger.name}.feriepengehistorikk") {
                interestedIn("fom", JsonNode::asLocalDate)
                interestedIn("tom", JsonNode::asLocalDate)
                requireKey("beløp", "orgnummer")
            }
            message.requireKey("@løsning.${SykepengehistorikkForFeriepenger.name}.arbeidskategorikoder")
        }
    }
}
