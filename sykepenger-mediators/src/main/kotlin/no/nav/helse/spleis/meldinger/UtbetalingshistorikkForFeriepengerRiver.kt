package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.SykepengehistorikkForFeriepenger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkForFeriepengerMessage
import java.time.LocalDateTime

internal class UtbetalingshistorikkForFeriepengerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(SykepengehistorikkForFeriepenger)
    override val riverName = "UtbetalingshistorikkForFeriepenger"

    override fun validate(message: JsonMessage) {
        message.require("@besvart") { require(it.asLocalDateTime() > LocalDateTime.now().minusHours(1)) }
        validerSykepengehistorikk(message)
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingshistorikkForFeriepengerMessage(packet)

    internal companion object {
        fun validerSykepengehistorikk(message: JsonMessage) {
            message.requireKey("${SykepengehistorikkForFeriepenger.name}.historikkFom")
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
        }
    }
}
