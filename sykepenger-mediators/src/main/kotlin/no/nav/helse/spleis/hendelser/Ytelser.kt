package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.hendelser.model.YtelserMessage

internal class Ytelser(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Sykepengehistorikk, Foreldrepenger)
    override val riverName = "Ytelser"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("@løsning.${Foreldrepenger.name}")
        packet.requireKey("@løsning.${Sykepengehistorikk.name}")
        packet.interestedIn("@løsning.${Foreldrepenger.name}.Foreldrepengeytelse")
        packet.interestedIn("@løsning.${Foreldrepenger.name}.Svangerskapsytelse")
        packet.requireArray("@løsning.${Sykepengehistorikk.name}") {
            requireArray("inntektsopplysninger") {
                require("sykepengerFom", JsonNode::asLocalDate)
                requireKey("inntekt", "orgnummer")
            }
            requireArray("utbetalteSykeperioder") {
                interestedIn("fom") { it.asLocalDate() }
                interestedIn("tom") { it.asLocalDate() }
                requireAny("typeKode", listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "O", "S", ""))
            }
        }
    }

    override fun createMessage(packet: JsonMessage) = YtelserMessage(packet)
}
