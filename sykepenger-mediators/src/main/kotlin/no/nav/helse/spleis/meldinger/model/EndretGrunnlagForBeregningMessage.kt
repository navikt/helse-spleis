package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.EndretGrunnlagForBeregning
import no.nav.helse.hendelser.Grunnlag
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import tools.jackson.databind.JsonNode
import java.time.LocalDate

internal class EndretGrunnlagForBeregningMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
    grunnlagsformat: Grunnlagsformat,
) : HendelseMessage(packet) {
    internal val fom = grunnlagsformat.fom(packet)

    internal val endretGrunnlag =
        when (grunnlagsformat) {
            Grunnlagsformat.GraderteAndreYtelser -> Grunnlag.GraderteAndreYtelser
            Grunnlagsformat.Inntektsendringer -> Grunnlag.Inntektsendringer
            Grunnlagsformat.Avslagsdager -> Grunnlag.Avslagsdager
        }

    internal val avsender =
        when (packet["manuellVurdering"].asBoolean(false)) {
            true -> Avsender.SAKSBEHANDLER
            false -> Avsender.SYSTEM
        }

    override fun behandle(
        mediator: IHendelseMediator,
        context: BehandlingContext,
    ) {
        mediator.behandle(
            message = this,
            endretGrunnlagForBeregning =
                EndretGrunnlagForBeregning(
                    meldingsreferanseId = meldingsporing.id,
                    fom = fom,
                    endretGrunnlag = endretGrunnlag,
                    avsender = avsender,
                ),
            context = context,
        )
    }

    sealed interface Grunnlagsformat {
        val eventName: String

        fun precondition(packet: JsonMessage) {}

        fun validate(message: JsonMessage)

        fun fom(packet: JsonMessage): LocalDate

        sealed class MedFom(
            override val eventName: String,
            protected val fomPath: String,
        ) : Grunnlagsformat {
            override fun validate(message: JsonMessage) {
                message.require(fomPath, JsonNode::asLocalDate)
            }

            override fun fom(packet: JsonMessage) = packet[fomPath].asLocalDate()
        }

        data object GraderteAndreYtelser : MedFom(eventName = "graderte_andre_ytelser_endret", fomPath = "fom")

        data object Inntektsendringer : MedFom(eventName = "inntektsendringer", fomPath = "inntektsendringFom") {
            override fun precondition(packet: JsonMessage) {
                packet.requireKey(fomPath)
            }
        }

        data object Avslagsdager : Grunnlagsformat {
            override val eventName = "avslagsdager_endret"

            override fun validate(message: JsonMessage) {
                message.require("perioder") { perioder ->
                    require(perioder.isArray && !perioder.isEmpty) { "perioder må ha minst én periode" }
                    perioder.values().forEach { periode ->
                        val fom = periode.path("fom").asLocalDate()
                        val tom = periode.path("tom").asLocalDate()
                        require(!fom.isAfter(tom)) { "fom kan ikke være etter tom" }
                    }
                }
            }

            override fun fom(packet: JsonMessage) = packet["perioder"].values().minOf { it.path("fom").asLocalDate() }
        }
    }
}
