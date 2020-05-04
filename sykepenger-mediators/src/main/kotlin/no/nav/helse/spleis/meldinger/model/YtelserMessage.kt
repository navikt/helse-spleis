package no.nav.helse.spleis.meldinger.model

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
import no.nav.helse.spleis.IHendelseMediator

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
        val grad = utbetaling["utbetalingsGrad"].asInt()
        if (fom == null || tom == null || fom > tom) Periode.Ugyldig(fom, tom)
        else when (utbetaling["typeKode"].asText()) {
            "0" -> Periode.Utbetaling(fom, tom, dagsats, grad)
            "1" -> Periode.ReduksjonMedlem(fom, tom, dagsats, grad)
            "2", "3" -> Periode.Etterbetaling(fom, tom)
            "4" -> Periode.KontertRegnskap(fom, tom)
            "5" -> Periode.RefusjonTilArbeidsgiver(fom, tom, dagsats, grad)
            "6" -> Periode.ReduksjonArbeidsgiverRefusjon(fom, tom, dagsats, grad)
            "7" -> Periode.Tilbakeført(fom, tom)
            "8" -> Periode.Konvertert(fom, tom)
            "9" -> Periode.Ferie(fom, tom)
            "O" -> Periode.Opphold(fom, tom)
            "S" -> Periode.Sanksjon(fom, tom)
            "" -> Periode.Ukjent(fom, tom)
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

    private val aktivitetslogg = Aktivitetslogg()
    private val foreldrepermisjon = Foreldrepermisjon(
        foreldrepengeytelse = foreldrepenger,
        svangerskapsytelse = svangerskapsytelse,
        aktivitetslogg = aktivitetslogg
    )

    private val utbetalingshistorikk = Utbetalingshistorikk(
        utbetalinger = utbetalinger,
        inntektshistorikk = inntektshistorikk,
        aktivitetslogg = aktivitetslogg
    )

    private val ytelser get() = Ytelser(
        meldingsreferanseId = this.id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        utbetalingshistorikk = utbetalingshistorikk,
        foreldrepermisjon = foreldrepermisjon,
        aktivitetslogg = aktivitetslogg
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, ytelser)
    }
}
