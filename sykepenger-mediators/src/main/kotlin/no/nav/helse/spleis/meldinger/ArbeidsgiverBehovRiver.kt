package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator

internal abstract class ArbeidsgiverBehovRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {

    init {
        river.validate(::validateBehov)
    }

    private fun validateBehov(packet: JsonMessage) {
        packet.requireKey("organisasjonsnummer")
        packet.interestedIn("yrkesaktivitetstype")
    }

    protected fun JsonMessage.interestedInArray(key: String, elementsValidation: (JsonMessage.() -> Unit)? = null) {
        interestedIn(key) {
            requireArray(key, elementsValidation)
        }
    }

    protected fun JsonMessage.requireArrayEllerObjectMedArray(key: String, arraynavn: String, elementsValidation: (JsonMessage.() -> Unit)? = null) {
        require(key) {
            if (it is ArrayNode) requireArray(key, elementsValidation)
            if (it is ObjectNode) requireArray("$key.$arraynavn", elementsValidation)
        }
    }

    protected fun JsonMessage.interestedInArrayEllerObjectMedArray(key: String, arraynavn: String, elementsValidation: (JsonMessage.() -> Unit)? = null) {
        interestedIn(key) {
            requireArrayEllerObjectMedArray(key, arraynavn, elementsValidation)
        }
    }
}
