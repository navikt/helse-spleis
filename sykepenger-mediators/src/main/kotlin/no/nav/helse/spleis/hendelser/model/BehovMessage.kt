package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Utbetaling
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import java.util.*

// Understands a JSON message representing a Need with solution
internal abstract class BehovMessage(
    originalMessage: String,
    problems: MessageProblems
) :
    HendelseMessage(originalMessage, problems) {
    init {
        requireKey(
            "@behov", "@id", "@opprettet",
            "@final", "@løsning", "@besvart",
            "aktørId", "fødselsnummer",
            "organisasjonsnummer", "vedtaksperiodeId"
        )
        requireValue("@final", true)
    }

    override val id: UUID get() = UUID.fromString(this["@id"].asText())
}

// Understands a JSON message representing an Ytelserbehov
internal class YtelserMessage(
    originalMessage: String,
    private val problems: MessageProblems
) :
    BehovMessage(originalMessage, problems) {
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
            foreldrepengeytelse = this["@løsning.Foreldrepenger.Foreldrepengeytelse"].takeIf(JsonNode::isObject)?.let(::asPeriode),
            svangerskapsytelse = this["@løsning.Foreldrepenger.Svangerskapsytelse"].takeIf(JsonNode::isObject)?.let(::asPeriode),
            aktivitetslogg = aktivitetslogg
        )

        val utbetalingshistorikk = Utbetalingshistorikk(
            ukjentePerioder = this["@løsning.Sykepengehistorikk"].flatMap {
                it.path("ukjentePerioder")
            },
            utbetalinger = this["@løsning.Sykepengehistorikk"].flatMap {
                it.path("utbetalteSykeperioder")
            }.map { utbetaling ->
                val fom = utbetaling["fom"].asLocalDate()
                val tom = utbetaling["tom"].asLocalDate()
                val typekode = utbetaling["typeKode"].asText()
                val dagsats = utbetaling["dagsats"].asInt()
                when (typekode) {
                    "0" -> {
                        Utbetalingshistorikk.Periode.Utbetaling(fom, tom, dagsats)
                    }
                    "1" -> {
                        ReduksjonMedlem(fom, tom, dagsats)
                    }
                    in listOf("2", "3") -> {
                        Etterbetaling(fom, tom, dagsats)
                    }
                    "4" -> {
                        KontertRegnskap(fom, tom, dagsats)
                    }
                    "5" -> {
                        RefusjonTilArbeidsgiver(fom, tom, dagsats)
                    }
                    "6" -> {
                        ReduksjonArbeidsgiverRefusjon(fom, tom, dagsats)
                    }
                    "7" -> {
                        Tilbakeført(fom, tom, dagsats)
                    }
                    "8" -> {
                        Konvertert(fom, tom, dagsats)
                    }
                    "9" -> {
                        Ferie(fom, tom, dagsats)
                    }
                    "O" -> {
                        Opphold(fom, tom, dagsats)
                    }
                    "S" -> {
                        Sanksjon(fom, tom, dagsats)
                    }
                    "" -> {
                        Ukjent(fom, tom, dagsats)
                    }
                    else -> problems.severe("Fikk en ukjent typekode:$typekode")
                }
            },
            inntektshistorikk = this["@løsning.Sykepengehistorikk"].flatMap {
                it.path("inntektsopplysninger")
            }.map { opplysning ->
                Inntektsopplysning(
                    sykepengerFom = opplysning["sykepengerFom"].asLocalDate(),
                    inntektPerMåned = opplysning["inntekt"].asInt(),
                    orgnummer = opplysning["orgnummer"].asText()
                )
            },
            graderingsliste = this["@løsning.Sykepengehistorikk"].flatMap {
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
            fødselsnummer = this["fødselsnummer"].asText(),
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            utbetalingshistorikk = utbetalingshistorikk,
            foreldrepermisjon = foreldrepermisjon,
            aktivitetslogg = aktivitetslogg
        )
    }

    object Factory : MessageFactory<YtelserMessage> {
        override fun createMessage(message: String, problems: MessageProblems) = YtelserMessage(message, problems)
    }
}

// Understands a JSON message representing a Vilkårsgrunnlagsbehov
internal class VilkårsgrunnlagMessage(
    originalMessage: String,
    private val problems: MessageProblems
) :
    BehovMessage(originalMessage, problems) {
    init {
        requireAll("@behov", Inntektsberegning, EgenAnsatt, Opptjening)
        requireKey("@løsning.${Inntektsberegning.name}")
        requireKey("@løsning.${EgenAnsatt.name}")
        requireKey("@løsning.${Opptjening.name}")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asVilkårsgrunnlag(): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            orgnummer = this["organisasjonsnummer"].asText(),
            inntektsmåneder = this["@løsning.${Inntektsberegning.name}"].map {
                Vilkårsgrunnlag.Måned(
                    årMåned = it["årMåned"].asYearMonth(),
                    inntektsliste = it["inntektsliste"].map { it["beløp"].asDouble() }
                )
            },
            arbeidsforhold = Vilkårsgrunnlag.MangeArbeidsforhold(this["@løsning.${Opptjening.name}"]
                .map {
                    Vilkårsgrunnlag.Arbeidsforhold(
                        orgnummer = it["orgnummer"].asText(),
                        fom = it["ansattSiden"].asLocalDate(),
                        tom = it["ansattTil"].asOptionalLocalDate()
                    )
                }
            ),
            erEgenAnsatt = this["@løsning.${EgenAnsatt.name}"].asBoolean()
        )
    }

    object Factory : MessageFactory<VilkårsgrunnlagMessage> {
        override fun createMessage(message: String, problems: MessageProblems) =
            VilkårsgrunnlagMessage(message, problems)
    }
}

// Understands a JSON message representing a Manuell saksbehandling-behov
internal class ManuellSaksbehandlingMessage(
    originalMessage: String,
    private val problems: MessageProblems
) :
    BehovMessage(originalMessage, problems) {
    init {
        requireAll("@behov", Godkjenning)
        requireKey("@løsning.${Godkjenning.name}.godkjent")
        requireKey("saksbehandlerIdent")
        interestedIn("godkjenttidspunkt")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asManuellSaksbehandling() =
        ManuellSaksbehandling(
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            saksbehandler = this["saksbehandlerIdent"].asText(),
            godkjenttidspunkt = this["godkjenttidspunkt"].asLocalDateTime(),
            utbetalingGodkjent = this["@løsning.${Godkjenning.name}.godkjent"].asBoolean()
        )

    object Factory : MessageFactory<ManuellSaksbehandlingMessage> {
        override fun createMessage(message: String, problems: MessageProblems) =
            ManuellSaksbehandlingMessage(message, problems)
    }
}

internal class UtbetalingMessage(
    originalMessage: String,
    problems: MessageProblems
) :
    BehovMessage(originalMessage, problems) {
    init {
        requireAll("@behov", Behovtype.Utbetaling)
        requireKey("@løsning.${Behovtype.Utbetaling.name}")
        requireKey("@løsning.${Behovtype.Utbetaling.name}.status")
        requireKey("@løsning.${Behovtype.Utbetaling.name}.melding")
        requireKey("utbetalingsreferanse")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asUtbetaling(): Utbetaling {
        return Utbetaling(
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            orgnummer = this["organisasjonsnummer"].asText(),
            utbetalingsreferanse = this["utbetalingsreferanse"].asText(),
            status = enumValueOf(this["@løsning.${Behovtype.Utbetaling.name}.status"].asText()),
            melding = this["@løsning.${Behovtype.Utbetaling.name}.melding"].asText()
        )
    }

    object Factory : MessageFactory<UtbetalingMessage> {
        override fun createMessage(message: String, problems: MessageProblems) = UtbetalingMessage(message, problems)
    }
}
