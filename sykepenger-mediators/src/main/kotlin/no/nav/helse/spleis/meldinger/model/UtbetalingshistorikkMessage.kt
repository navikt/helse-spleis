package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

// Understands a JSON message representing an Ytelserbehov
internal class UtbetalingshistorikkMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : BehovMessage(packet) {

    companion object {
        internal fun JsonMessage.utbetalinger() = this["@løsning.${Sykepengehistorikk.name}"]
            .flatMap { it.path("utbetalteSykeperioder") }
            .filter(::erGyldigPeriode)
            .mapNotNull { utbetaling ->
                val fom = utbetaling["fom"].asLocalDate()
                val tom = utbetaling["tom"].asLocalDate()
                when (utbetaling["typeKode"].asText()) {
                    "0", "1" -> {
                        val grad = utbetaling["utbetalingsGrad"].asInt().prosent
                        val inntekt = Utbetalingsperiode.inntekt(utbetaling["dagsats"].asInt().daglig, grad)
                        val orgnummer = utbetaling["orgnummer"].asText()
                        PersonUtbetalingsperiode(orgnummer, fom, tom, grad, inntekt)
                    }

                    "5", "6" -> {
                        val grad = utbetaling["utbetalingsGrad"].asInt().prosent
                        val inntekt = Utbetalingsperiode.inntekt(utbetaling["dagsats"].asInt().daglig, grad)
                        val orgnummer = utbetaling["orgnummer"].asText()
                        ArbeidsgiverUtbetalingsperiode(orgnummer, fom, tom, grad, inntekt)
                    }

                    "9" -> Friperiode(fom, tom)
                    else -> null
                }
            }

        private fun erGyldigPeriode(node: JsonNode): Boolean {
            return if (erUtbetalingsperiode(node)) {
                harGyldigUtbetalingsgrad(node) && harGyldigTidsintervall(node)
            } else harGyldigTidsintervall(node)
        }

        private fun harGyldigUtbetalingsgrad(node: JsonNode): Boolean {
            val utbetalingsGrad = node["utbetalingsGrad"].asInt()
            return utbetalingsGrad > 0
        }

        private fun erUtbetalingsperiode(node: JsonNode) = node["typeKode"].asText() in listOf("0", "1", "5", "6")

        private fun harGyldigTidsintervall(node: JsonNode): Boolean {
            val fom = node["fom"].asOptionalLocalDate()
            val tom = node["tom"].asOptionalLocalDate()
            return fom != null && tom != null && tom >= fom
        }
    }

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val besvart = packet["@besvart"].asLocalDateTime()

    private val utbetalinger = packet.utbetalinger()

    private fun infotrygdhistorikk(meldingsreferanseId: MeldingsreferanseId) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = besvart,
            hendelseId = meldingsreferanseId,
            perioder = utbetalinger
        )

    private fun utbetalingshistorikk() =
        Utbetalingshistorikk(
            meldingsreferanseId = meldingsporing.id,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId.toUUID(),
            element = infotrygdhistorikk(meldingsporing.id),
            besvart = besvart
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, utbetalingshistorikk(), context)
    }
}
