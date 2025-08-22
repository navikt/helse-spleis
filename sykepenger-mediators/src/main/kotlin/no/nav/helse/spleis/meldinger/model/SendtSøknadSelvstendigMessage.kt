package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.Year
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.Personopplysninger
import no.nav.helse.økonomi.Inntekt.Companion.årlig

internal class SendtSøknadSelvstendigMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing, private val builder: SendtSøknadBuilder = SendtSøknadBuilder()) : SøknadMessage(packet, builder.selvstendig()) {
    override fun _behandle(mediator: IHendelseMediator, personopplysninger: Personopplysninger, packet: JsonMessage, context: MessageContext) {
        builder.sendt(packet["sendtNav"].asLocalDateTime())
        val pensjonsgivendeInntekter = packet["selvstendigNaringsdrivende"]["naringsdrivendeInntekt"]["inntekt"].map {
            Søknad.PensjonsgivendeInntekt(
                inntektsår = Year.parse(it["inntektsaar"].asText()),
                næringsinntekt = it["pensjonsgivendeInntekt"]["pensjonsgivendeInntektAvNaeringsinntekt"].asInt().årlig
            )
        }
        builder.pensjonsgivendeInntekter(pensjonsgivendeInntekter)
        val ventetid = Periode(packet["selvstendigNaringsdrivende.naringsdrivendeVenteperiode.fom"].asLocalDate(), packet["selvstendigNaringsdrivende.naringsdrivendeVenteperiode.tom"].asLocalDate())
        builder.ventetid(ventetid)
        SendtSøknadNavMessage.byggSendtSøknad(builder, packet)
        mediator.behandle(personopplysninger, this, builder.build(meldingsporing), context, packet["historiskeFolkeregisteridenter"].map(JsonNode::asText).map { Personidentifikator(it) }.toSet())
    }
}
