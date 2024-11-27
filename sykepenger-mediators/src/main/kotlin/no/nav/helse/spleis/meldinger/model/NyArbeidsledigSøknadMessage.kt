package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.Personidentifikator
import no.nav.helse.person.Arbeidsledig
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.Personopplysninger

internal class NyArbeidsledigSøknadMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing, private val builder: NySøknadBuilder = NySøknadBuilder()) : SøknadMessage(packet, builder, packet["tidligereArbeidsgiverOrgnummer"].asText(Arbeidsledig)) {
    override fun _behandle(mediator: IHendelseMediator, personopplysninger: Personopplysninger, packet: JsonMessage, context: MessageContext) {
        builder.fremtidigSøknad(packet["fremtidig_søknad"].asBoolean())
        mediator.behandle(personopplysninger, this, builder.build(meldingsporing), context, packet["historiskeFolkeregisteridenter"].map(JsonNode::asText).map { Personidentifikator(it) }.toSet())
    }
}
