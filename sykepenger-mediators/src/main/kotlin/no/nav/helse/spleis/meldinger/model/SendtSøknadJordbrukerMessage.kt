package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.Year
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Søknad
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.Personopplysninger
import no.nav.helse.økonomi.Inntekt.Companion.årlig

internal class SendtSøknadJordbrukerMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing, private val builder: SendtSøknadBuilder = SendtSøknadBuilder(packet["arbeidssituasjon"].asText())) : SøknadMessage(packet, builder.jordbruker()) {
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

        val fraværFørSykmelding = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("FRAVAR_FOR_SYKMELDINGEN_V2").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.fraværFørSykmelding(fraværFørSykmelding)

        val harOppgittNyIArbedislivetPåGamleMåten = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("INNTEKTSOPPLYSNINGER_NY_I_ARBEIDSLIVET").takeUnless { it.isMissingOrNull() }?.asBoolean()
        val harOppgittNyIArbedislivetPåNyeMåten = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("NARINGSDRIVENDE_NY_I_ARBEIDSLIVET").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.harOppgittNyIArbeidslivet((harOppgittNyIArbedislivetPåGamleMåten == true) || (harOppgittNyIArbedislivetPåNyeMåten == true))

        val harOppgittVarigEndringPåGamleMåten = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("INNTEKTSOPPLYSNINGER_VARIG_ENDRING").takeUnless { it.isMissingOrNull() }?.asBoolean()
        val harOppgittVarigEndringPåNyeMåten = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("NARINGSDRIVENDE_VARIG_ENDRING").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.harOppgittVarigEndring((harOppgittVarigEndringPåNyeMåten == true) || (harOppgittVarigEndringPåGamleMåten == true))

        val harOppgittAvviklingPåGamleMåten = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("INNTEKTSOPPLYSNINGER_VIRKSOMHETEN_AVVIKLET").takeUnless { it.isMissingOrNull() }?.asBoolean()
        val harOppgittAvviklingPåNyeMåten = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("NARINGSDRIVENDE_VIRKSOMHETEN_AVVIKLET").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.harOppgittAvvikling((harOppgittAvviklingPåGamleMåten == true) || (harOppgittAvviklingPåNyeMåten == true))

        val harOppgittOpprettholdtInntekt = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("NARINGSDRIVENDE_OPPRETTHOLDT_INNTEKT").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.harOppgittOpprettholdtInntekt(harOppgittOpprettholdtInntekt)

        val harOppgittOppholdIUtlandet = packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("NARINGSDRIVENDE_OPPHOLD_I_UTLANDET").takeUnless { it.isMissingOrNull() }?.asBoolean()
        builder.harOppgittOppholdIUtlandet(harOppgittOppholdIUtlandet)

        SendtSøknadNavMessage.byggSendtSøknad(builder, packet)
        mediator.behandle(personopplysninger, this, builder.build(meldingsporing), context, packet["historiskeFolkeregisteridenter"].map(JsonNode::asText).map { Personidentifikator(it) }.toSet())
    }
}
