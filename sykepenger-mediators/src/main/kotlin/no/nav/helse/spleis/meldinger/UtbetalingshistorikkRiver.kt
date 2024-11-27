package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import java.time.LocalDateTime
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage

internal class UtbetalingshistorikkRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator,
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Sykepengehistorikk)
    override val riverName = "Utbetalingshistorikk"

    init {
        river.precondition { message ->
            message.require("@behov") { require(it.size() == behov.size) }
            message.require("@besvart") {
                require(it.asLocalDateTime() > LocalDateTime.now().minusHours(1))
            }
            message.forbid("fagsystemId")
        }
    }

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId", "tilstand")
        validerSykepengehistorikk(message)
    }

    override fun createMessage(packet: JsonMessage) =
        UtbetalingshistorikkMessage(
            packet,
            Meldingsporing(
                id = packet["@id"].asText().toUUID(),
                fødselsnummer = packet["fødselsnummer"].asText(),
            ),
        )

    internal companion object {
        fun validerSykepengehistorikk(message: JsonMessage) {
            message.requireArray("@løsning.${Sykepengehistorikk.name}") {
                requireKey("statslønn")
                requireArray("inntektsopplysninger") {
                    require("sykepengerFom", JsonNode::asLocalDate)
                    requireKey("inntekt", "orgnummer", "refusjonTilArbeidsgiver")
                }
                requireArray("utbetalteSykeperioder") {
                    interestedIn("fom", JsonNode::asLocalDate)
                    interestedIn("tom", JsonNode::asLocalDate)
                    requireKey("dagsats", "utbetalingsGrad", "orgnummer")
                    requireKey("typeKode")
                }
                requireKey("arbeidsKategoriKode")
            }
        }
    }
}
