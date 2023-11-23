package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Yrkesaktivitet
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Personopplysninger

internal class NyArbeidsledigSøknadMessage(packet: JsonMessage, private val builder: NySøknadBuilder = NySøknadBuilder()) : SøknadMessage(packet, builder, packet["tidligereArbeidsgiverOrgnummer"].asText(Yrkesaktivitet.Arbeidsledig)) {
    override fun _behandle(mediator: IHendelseMediator, personopplysninger: Personopplysninger, packet: JsonMessage, context: MessageContext) {
        builder.fremtidigSøknad(packet["fremtidig_søknad"].asBoolean())
        mediator.behandle(personopplysninger, this, builder.build(), context, packet["historiskeFolkeregisteridenter"].map(JsonNode::asText).map(String::somPersonidentifikator).toSet())
    }
}
