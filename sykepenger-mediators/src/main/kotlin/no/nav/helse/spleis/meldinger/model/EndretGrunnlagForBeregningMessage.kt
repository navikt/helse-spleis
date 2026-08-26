package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.EndretGrunnlagForBeregning
import no.nav.helse.hendelser.Grunnlag
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class EndretGrunnlagForBeregningMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
    grunnlagsformat: Grunnlagsformat
) : HendelseMessage(packet) {

    internal val fom = packet[grunnlagsformat.fomPath].asLocalDate()

    internal val endretGrunnlag = when (grunnlagsformat) {
        Grunnlagsformat.GraderteAndreYtelser -> Grunnlag.GraderteAndreYtelser
        Grunnlagsformat.Inntektsendringer -> Grunnlag.Inntektsendringer
    }

    internal val avsender = when (packet["manuellVurdering"].asBoolean(false)) {
        true -> Avsender.SAKSBEHANDLER
        false -> Avsender.SYSTEM
    }

    override fun behandle(mediator: IHendelseMediator, context: BehandlingContext) {
        mediator.behandle(
            message = this,
            endretGrunnlagForBeregning = EndretGrunnlagForBeregning(
                meldingsreferanseId = meldingsporing.id,
                fom = fom,
                endretGrunnlag = endretGrunnlag,
                avsender = avsender
            ),
            context = context
        )
    }

    sealed interface Grunnlagsformat {
        val eventName: String
        val fomPath: String
        fun precondition(packet: JsonMessage) {}

        data object GraderteAndreYtelser: Grunnlagsformat {
            override val eventName = "graderte_andre_ytelser_endret"
            override val fomPath = "fom"
        }
        data object Inntektsendringer: Grunnlagsformat {
            override val eventName = "inntektsendringer"
            override val fomPath = "inntektsendringFom"
            override fun precondition(packet: JsonMessage) {
                packet.requireKey(fomPath)
            }
        }
    }
}
