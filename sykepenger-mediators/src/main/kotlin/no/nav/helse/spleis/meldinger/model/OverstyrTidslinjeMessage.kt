package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Dagtype.Companion.dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrTidslinje
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import no.nav.helse.hendelser.til
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class OverstyrTidslinjeMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val dager = packet["dager"].flatMap { dag ->
        val fom = dag.path("dato").asLocalDate()
        val tom = dag.path("tom").asOptionalLocalDate()?.takeUnless { it < fom } ?: fom
        val periode = fom til tom
        periode.map { dato -> ManuellOverskrivingDag(
            dato = dato,
            type = dag["type"].asText().dagtype,
            grad = dag.get("grad")?.intValue()
        )}
    }

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) =
        mediator.behandle(
            this, OverstyrTidslinje(
                meldingsreferanseId = meldingsporing.id,
                fødselsnummer = meldingsporing.fødselsnummer,
                aktørId = meldingsporing.aktørId,
                organisasjonsnummer = organisasjonsnummer,
                dager = dager,
                opprettet = opprettet
            ), context
        )
}
