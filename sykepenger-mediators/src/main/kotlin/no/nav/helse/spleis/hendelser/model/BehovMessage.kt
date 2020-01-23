package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.behov.Behovstype
import no.nav.helse.hendelser.ModelForeldrepenger
import no.nav.helse.hendelser.ModelSykepengehistorikk
import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import no.nav.helse.hendelser.ModelYtelser
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.*
import java.util.*

// Understands a JSON message representing a Need with solution
internal abstract class BehovMessage(originalMessage: String, aktivitetslogger: Aktivitetslogger) :
    JsonMessage(originalMessage, aktivitetslogger) {
    init {
        requiredKey(
            "@behov", "@id", "@opprettet",
            "@final", "@løsning", "@besvart",
            "hendelse", "aktørId", "fødselsnummer",
            "organisasjonsnummer", "vedtaksperiodeId"
        )
        requiredValue("@final", true)
    }
}

// Understands a JSON message representing an Ytelserbehov
internal class YtelserMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger) :
    BehovMessage(originalMessage, aktivitetslogger) {
    init {
        requiredValues("@behov", Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger)
        requiredKey("@løsning.${Behovstype.Foreldrepenger.name}")
        requiredKey("@løsning.${Behovstype.Sykepengehistorikk.name}")
        interestedIn("@løsning.${Behovstype.Foreldrepenger.name}.Foreldrepengeytelse")
        interestedIn("@løsning.${Behovstype.Foreldrepenger.name}.Svangerskapsytelse")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asModelYtelser(): ModelYtelser {
        val foreldrepenger = ModelForeldrepenger(
            foreldrepengeytelse = this["@løsning.Foreldrepenger.Foreldrepengeytelse"].takeIf(JsonNode::isObject)?.let(::asPeriode),
            svangerskapsytelse = this["@løsning.Foreldrepenger.Svangerskapsytelse"].takeIf(JsonNode::isObject)?.let(::asPeriode),
            aktivitetslogger = aktivitetslogger
        )

        val sykepengehistorikk = ModelSykepengehistorikk(
            perioder = this["@løsning.Sykepengehistorikk"].map(::asPeriode),
            aktivitetslogger = aktivitetslogger
        )

        return ModelYtelser(
            hendelseId = UUID.randomUUID(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = this["fødselsnummer"].asText(),
            organisasjonsnummer = this["organisasjonsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            sykepengehistorikk = sykepengehistorikk,
            foreldrepenger = foreldrepenger,
            rapportertdato = this["@besvart"].asLocalDateTime(),
            originalJson = this.toJson()
        )
    }

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger): YtelserMessage {
            return YtelserMessage(message, problems)
        }
    }
}

// Understands a JSON message representing a Vilkårsgrunnlagsbehov
internal class VilkårsgrunnlagMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger) :
    BehovMessage(originalMessage, aktivitetslogger) {
    init {
        requiredValues("@behov", Behovstype.Inntektsberegning, Behovstype.EgenAnsatt)
        requiredKey("@løsning.${Behovstype.Inntektsberegning.name}")
        requiredKey("@løsning.${Behovstype.EgenAnsatt.name}")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asModelVilkårsgrunnlag(): ModelVilkårsgrunnlag {
        return ModelVilkårsgrunnlag(
            UUID.randomUUID(),
            this["vedtaksperiodeId"].asText(),
            this["aktørId"].asText(),
            this["fødselsnummer"].asText(),
            this["organisasjonsnummer"].asText(),
            this["@besvart"].asLocalDateTime(),
            this["@løsning.${Behovstype.Inntektsberegning.name}"].map {
                ModelVilkårsgrunnlag.Måned(
                    it["årMåned"].asYearMonth(),
                    it["inntektsliste"].map { ModelVilkårsgrunnlag.Inntekt(it["beløp"].asDouble()) }
                )
            },
            this["@løsning.${Behovstype.EgenAnsatt.name}"].asBoolean(),
            aktivitetslogger,
            this.toJson()
        )
    }

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger): VilkårsgrunnlagMessage {
            return VilkårsgrunnlagMessage(message, problems)
        }
    }
}

// Understands a JSON message representing a Manuell saksbehandling-behov
internal class ManuellSaksbehandlingMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger) :
    BehovMessage(originalMessage, aktivitetslogger) {
    init {
        requiredValues("@behov", Behovstype.GodkjenningFraSaksbehandler)
        requiredKey("@løsning.${Behovstype.GodkjenningFraSaksbehandler.name}")

    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger): ManuellSaksbehandlingMessage {
            return ManuellSaksbehandlingMessage(message, problems)
        }
    }
}
