package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
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
            val pensjonsgivendeInntekt = it.path("pensjonsgivendeInntekt")
            Søknad.PensjonsgivendeInntekt(
                inntektsår = Year.parse(it.path("aar").asText()),
                næringsinntekt = pensjonsgivendeInntekt.path("pensjonsgivendeInntektAvNaeringsinntekt").asInt().årlig,
                lønnsinntekt = pensjonsgivendeInntekt.path("pensjonsgivendeInntektAvLoennsinntekt").asInt().årlig,
                lønnsinntektBarePensjonsdel = pensjonsgivendeInntekt.path("pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel").asInt().årlig,
                næringsinntektFraFiskeFangstEllerFamiliebarnehage = pensjonsgivendeInntekt.path("pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage").asInt().årlig,
                erFerdigLignet = it.path("erFerdigLignet").asBoolean(true)
            )
        }
        builder.pensjonsgivendeInntekter(pensjonsgivendeInntekter)
        val ventetid = packet["selvstendigNaringsdrivende.ventetid"]
        if (ventetid.isObject) {
            val ventetidperiode = Periode(ventetid.path("fom").asLocalDate(), ventetid.path("tom").asLocalDate())
            builder.ventetid(ventetidperiode)
        }

        val fraværFørSykmelding = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("FRAVAR_FOR_SYKMELDINGEN_V2").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.fraværFørSykmelding(fraværFørSykmelding)

        val harOppgittÅHaForsikring = packet["selvstendigNaringsdrivende"].path("harForsikring").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.harOppgittÅHaForsikring(harOppgittÅHaForsikring)

        val harOppgittNyIArbedislivet = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("INNTEKTSOPPLYSNINGER_NY_I_ARBEIDSLIVET").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.harOppgittNyIArbeidslivet(harOppgittNyIArbedislivet)

        val harOppgittVarigEndring = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("INNTEKTSOPPLYSNINGER_VARIG_ENDRING").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.harOppgittVarigEndring(harOppgittVarigEndring)

        val harOppgittAvvikling = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("INNTEKTSOPPLYSNINGER_VIRKSOMHETEN_AVVIKLET").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.harOppgittAvvikling(harOppgittAvvikling)

        SendtSøknadNavMessage.byggSendtSøknad(builder, packet)
        mediator.behandle(personopplysninger, this, builder.build(meldingsporing), context, packet["historiskeFolkeregisteridenter"].map(JsonNode::asText).map { Personidentifikator(it) }.toSet())
    }
}
