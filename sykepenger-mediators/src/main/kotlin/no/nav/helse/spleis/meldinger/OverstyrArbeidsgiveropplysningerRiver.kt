package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsgiveropplysningerMessage

internal class OverstyrArbeidsgiveropplysningerRiver(rapidsConnection: RapidsConnection, messageMediator: IMessageMediator): HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "overstyr_arbeidsgiveropplysninger"
    override val riverName = "Overstyr arbeidsgiveropplysninger"

    override fun createMessage(packet: JsonMessage) = OverstyrArbeidsgiveropplysningerMessage(packet)

    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer", "skjæringstidspunkt", "arbeidsgiveropplysninger")
        message.require("arbeidsgiveropplysninger") {
            it.fields().asSequence().toList().forEach { (orgnummer, arbeidsgiveropplysning) ->
                require(orgnummer.isNotBlank())
                // require(arbeidsgiveropplysning["subsumsjon"]["paragraf"].isTextual)
                require(arbeidsgiveropplysning["forklaring"].isTextual)
                require(arbeidsgiveropplysning["månedligInntekt"].isNumber)
                require(arbeidsgiveropplysning["refusjonsopplysninger"].all { refusjonsopplysning ->
                    refusjonsopplysning.has("fom") && refusjonsopplysning["beløp"].isNumber
                })
            }
        }
    }
}