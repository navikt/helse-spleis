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

internal class SendtSøknadSelvstendigMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing, private val builder: SendtSøknadBuilder = SendtSøknadBuilder(packet["arbeidssituasjon"].asText())) : SøknadMessage(packet, builder.selvstendig()) {
    override fun _behandle(mediator: IHendelseMediator, personopplysninger: Personopplysninger, packet: JsonMessage, context: MessageContext) {
        builder.sendt(packet["sendtNav"].asLocalDateTime())
        val pensjonsgivendeInntekter = packet["selvstendigNaringsdrivende.inntekt.inntektsAar"].map {
            Søknad.PensjonsgivendeInntekt(
                inntektsår = Year.parse(it.path("aar").asText()),
                næringsinntekt = it.path("pensjonsgivendeInntekt").path("pensjonsgivendeInntektAvNaeringsinntekt").asInt().årlig,
                lønnsinntekt = it.path("pensjonsgivendeInntekt").path("pensjonsgivendeInntektAvLoennsinntekt").asInt().årlig,
                lønnsinntektBarePensjonsdel = it.path("pensjonsgivendeInntekt").path("pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel").asInt().årlig,
                næringsinntektFraFiskeFangstEllerFamiliebarnehage = it.path("pensjonsgivendeInntekt").path("pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage").asInt().årlig
            )
        }
        builder.pensjonsgivendeInntekter(pensjonsgivendeInntekter)
        val ventetid = packet["selvstendigNaringsdrivende.ventetid"]
        if (ventetid.isObject) {
            val ventetidperiode = Periode(ventetid.path("fom").asLocalDate(), ventetid.path("tom").asLocalDate())
            builder.ventetid(ventetidperiode)
        }
        SendtSøknadNavMessage.byggSendtSøknad(builder, packet)
        mediator.behandle(personopplysninger, this, builder.build(meldingsporing), context, packet["historiskeFolkeregisteridenter"].map(JsonNode::asText).map { Personidentifikator(it) }.toSet())
    }
}
