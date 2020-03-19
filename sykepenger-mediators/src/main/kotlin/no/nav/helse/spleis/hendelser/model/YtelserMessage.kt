package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing an Ytelserbehov
internal class YtelserMessage(originalMessage: String, private val problems: MessageProblems) : BehovMessage(originalMessage, problems) {
    init {
        requireAll("@behov", Sykepengehistorikk, Foreldrepenger)
        requireKey("@løsning.${Foreldrepenger.name}")
        requireKey("@løsning.${Sykepengehistorikk.name}")
        interestedIn("@løsning.${Foreldrepenger.name}.Foreldrepengeytelse")
        interestedIn("@løsning.${Foreldrepenger.name}.Svangerskapsytelse")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asYtelser(): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        val foreldrepermisjon = Foreldrepermisjon(
            foreldrepengeytelse = this["@løsning.${Foreldrepenger.name}.Foreldrepengeytelse"]
                .takeIf(JsonNode::isObject)?.let(::asPeriode),
            svangerskapsytelse = this["@løsning.${Foreldrepenger.name}.Svangerskapsytelse"]
                .takeIf(JsonNode::isObject)?.let(::asPeriode),
            aktivitetslogg = aktivitetslogg
        )

        val utbetalingshistorikk = Utbetalingshistorikk(
            ukjentePerioder = this["@løsning.${Sykepengehistorikk.name}"].flatMap {
                it.path("ukjentePerioder")
            },
            utbetalinger = this["@løsning.${Sykepengehistorikk.name}"].flatMap {
                it.path("utbetalteSykeperioder")
            }.map { utbetaling ->
                val fom = utbetaling["fom"].asLocalDate()
                val tom = utbetaling["tom"].asLocalDate()
                val typekode = utbetaling["typeKode"].asText()
                val dagsats = utbetaling["dagsats"].asInt()
                when (typekode) {
                    "0" -> Periode.Utbetaling(fom, tom, dagsats)
                    "1" -> Periode.ReduksjonMedlem(fom, tom, dagsats)
                    in listOf("2", "3") -> Periode.Etterbetaling(fom, tom, dagsats)
                    "4" -> Periode.KontertRegnskap(fom, tom, dagsats)
                    "5" -> Periode.RefusjonTilArbeidsgiver(fom, tom, dagsats)
                    "6" -> Periode.ReduksjonArbeidsgiverRefusjon(fom, tom, dagsats)
                    "7" -> Periode.Tilbakeført(fom, tom, dagsats)
                    "8" -> Periode.Konvertert(fom, tom, dagsats)
                    "9" -> Periode.Ferie(fom, tom, dagsats)
                    "O" -> Periode.Opphold(fom, tom, dagsats)
                    "S" -> Periode.Sanksjon(fom, tom, dagsats)
                    "" -> Periode.Ukjent(fom, tom, dagsats)
                    else -> problems.severe("Fikk en ukjent typekode:$typekode")
                }
            },
            inntektshistorikk = this["@løsning.${Sykepengehistorikk.name}"].flatMap {
                it.path("inntektsopplysninger")
            }.map { opplysning ->
                Utbetalingshistorikk.Inntektsopplysning(
                    sykepengerFom = opplysning["sykepengerFom"].asLocalDate(),
                    inntektPerMåned = opplysning["inntekt"].asInt(),
                    orgnummer = opplysning["orgnummer"].asText()
                )
            },
            graderingsliste = this["@løsning.${Sykepengehistorikk.name}"].flatMap {
                it.path("graderingsliste")
            }.map {
                Utbetalingshistorikk.Graderingsperiode(
                    it["fom"].asLocalDate(),
                    it["tom"].asLocalDate(),
                    it["grad"].asDouble()
                )
            },
            aktivitetslogg = aktivitetslogg
        )

        return Ytelser(
            meldingsreferanseId = this.id,
            aktørId = this["aktørId"].asText(),
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            utbetalingshistorikk = utbetalingshistorikk,
            foreldrepermisjon = foreldrepermisjon,
            aktivitetslogg = aktivitetslogg
        )
    }

    object Factory : MessageFactory<YtelserMessage> {
        override fun createMessage(message: String, problems: MessageProblems) =
            YtelserMessage(message, problems)
    }
}
