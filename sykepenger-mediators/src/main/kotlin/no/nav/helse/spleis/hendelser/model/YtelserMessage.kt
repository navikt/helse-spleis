package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing an Ytelserbehov
internal class YtelserMessage(packet: JsonMessage) : BehovMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()

    private val foreldrepenger = packet["@løsning.${Foreldrepenger.name}.Foreldrepengeytelse"]
        .takeIf(JsonNode::isObject)?.let(::asPeriode)
    private val svangerskapsytelse = packet["@løsning.${Foreldrepenger.name}.Svangerskapsytelse"]
        .takeIf(JsonNode::isObject)?.let(::asPeriode)

    private val utbetalinger = packet["@løsning.${Sykepengehistorikk.name}"].flatMap {
        it.path("utbetalteSykeperioder")
    }.mapNotNull { utbetaling ->
        val fom = utbetaling["fom"].asOptionalLocalDate()
        val tom = utbetaling["tom"].asOptionalLocalDate()
        val dagsats = utbetaling["dagsats"].asInt()
        if (fom == null || tom == null) {
            return@mapNotNull Periode.Ugyldig(fom, tom, dagsats)
        }
        when (utbetaling["typeKode"].asText()) {
            "0" -> Periode.Utbetaling(fom, tom, dagsats)
            "1" -> Periode.ReduksjonMedlem(fom, tom, dagsats)
            "2", "3" -> Periode.Etterbetaling(fom, tom, dagsats)
            "4" -> Periode.KontertRegnskap(fom, tom, dagsats)
            "5" -> Periode.RefusjonTilArbeidsgiver(fom, tom, dagsats)
            "6" -> Periode.ReduksjonArbeidsgiverRefusjon(fom, tom, dagsats)
            "7" -> Periode.Tilbakeført(fom, tom, dagsats)
            "8" -> Periode.Konvertert(fom, tom, dagsats)
            "9" -> Periode.Ferie(fom, tom, dagsats)
            "O" -> Periode.Opphold(fom, tom, dagsats)
            "S" -> Periode.Sanksjon(fom, tom, dagsats)
            "" -> Periode.Ukjent(fom, tom, dagsats)
            else -> null // filtered away in river validation
        }
    }

    private val inntektshistorikk = packet["@løsning.${Sykepengehistorikk.name}"].flatMap {
        it.path("inntektsopplysninger")
    }.map { opplysning ->
        Utbetalingshistorikk.Inntektsopplysning(
            sykepengerFom = opplysning["sykepengerFom"].asLocalDate(),
            inntektPerMåned = opplysning["inntekt"].asInt(),
            orgnummer = opplysning["orgnummer"].asText()
        )
    }
    private val graderingsliste = packet["@løsning.${Sykepengehistorikk.name}"].flatMap {
        it.path("graderingsliste")
    }.map {
        Utbetalingshistorikk.Graderingsperiode(
            it["fom"].asLocalDate(),
            it["tom"].asLocalDate(),
            it["grad"].asDouble()
        )
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asYtelser(): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        val foreldrepermisjon = Foreldrepermisjon(
            foreldrepengeytelse = foreldrepenger,
            svangerskapsytelse = svangerskapsytelse,
            aktivitetslogg = aktivitetslogg
        )

        val utbetalingshistorikk = Utbetalingshistorikk(
            utbetalinger = utbetalinger,
            inntektshistorikk = inntektshistorikk,
            graderingsliste = graderingsliste,
            aktivitetslogg = aktivitetslogg
        )

        return Ytelser(
            meldingsreferanseId = this.id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingshistorikk = utbetalingshistorikk,
            foreldrepermisjon = foreldrepermisjon,
            aktivitetslogg = aktivitetslogg
        )
    }
}
