package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Søknad
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.Personopplysninger
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import java.time.Year

internal class SendtSøknadSelvstendigMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
    private val builder: SendtSøknadBuilder = SendtSøknadBuilder(packet["arbeidssituasjon"].asText()),
) : SøknadMessage(packet, builder.selvstendig()) {
    override fun _behandle(
        mediator: IHendelseMediator,
        personopplysninger: Personopplysninger,
        packet: JsonMessage,
        context: BehandlingContext,
    ) {
        builder.sendt(packet["sendtNav"].asLocalDateTime())
        val pensjonsgivendeInntekter =
            packet["selvstendigNaringsdrivende.inntekt.inntektsAar"].map { it ->
                val pensjonsgivendeInntekt = it.path("pensjonsgivendeInntektPerSkatteordning")
                Søknad.PensjonsgivendeInntekt(
                    inntektsår = Year.parse(it.path("aar").asText()),
                    næringsinntekt =
                        pensjonsgivendeInntekt
                            .sumOf {
                                it.path("pensjonsgivendeInntektAvNaeringsinntekt").asInt()
                            }.årlig,
                    lønnsinntekt =
                        pensjonsgivendeInntekt
                            .sumOf {
                                it.path("pensjonsgivendeInntektAvLoennsinntekt").asInt()
                            }.årlig,
                    lønnsinntektBarePensjonsdel =
                        pensjonsgivendeInntekt
                            .sumOf {
                                it.path("pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel").asInt()
                            }.årlig,
                    næringsinntektFraFiskeFangstEllerFamiliebarnehage =
                        pensjonsgivendeInntekt
                            .sumOf {
                                it.path("pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage").asInt()
                            }.årlig,
                    erFerdigLignet = it.path("erFerdigLignet").asBoolean(true),
                    datoForFastsetting = pensjonsgivendeInntekt.maxOf { it.path("datoForFastsetting").asLocalDate() },
                )
            }
        builder.pensjonsgivendeInntekter(pensjonsgivendeInntekter)

        val harOppgittNyIArbedislivet =
            packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("NARINGSDRIVENDE_NY_I_ARBEIDSLIVET").asBoolean()
        builder.harOppgittNyIArbeidslivet(harOppgittNyIArbedislivet)

        val harOppgittVarigEndring =
            packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("NARINGSDRIVENDE_VARIG_ENDRING").asBoolean()
        builder.harOppgittVarigEndring(harOppgittVarigEndring)

        val harOppgittAvvikling =
            packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("NARINGSDRIVENDE_VIRKSOMHETEN_AVVIKLET").asBoolean()
        builder.harOppgittAvvikling(harOppgittAvvikling)

        val harOppgittOpprettholdtInntekt =
            packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("NARINGSDRIVENDE_OPPRETTHOLDT_INNTEKT").asBoolean()
        builder.harOppgittOpprettholdtInntekt(harOppgittOpprettholdtInntekt)

        val harOppgittOppholdIUtlandet =
            packet["selvstendigNaringsdrivende.hovedSporsmalSvar"].path("NARINGSDRIVENDE_OPPHOLD_I_UTLANDET").asBoolean()
        builder.harOppgittOppholdIUtlandet(harOppgittOppholdIUtlandet)

        packet["meldingTilNavDagerFraSykmelding"].takeUnless { it.isMissingOrNull() }?.forEach {
            builder.meldingTilNavDager(fom = it.path("fom").asLocalDate(), tom = it.path("tom").asLocalDate())
        }
        SendtSøknadNavMessage.byggSendtSøknad(builder, packet)
        mediator.behandle(
            personopplysninger,
            this,
            builder.build(meldingsporing),
            context,
            packet["historiskeFolkeregisteridenter"].map(JsonNode::asText).map { Personidentifikator(it) }.toSet(),
        )
    }
}
