package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Opptjeningvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.spleis.hendelser.MessageProcessor
import java.time.LocalDate

// Understands a JSON message representing a Vilkårsgrunnlagsbehov
internal class VilkårsgrunnlagMessage(packet: JsonMessage) : BehovMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val dagpenger: List<Pair<LocalDate, LocalDate>>
    private val ugyldigeDagpengeperioder: List<Pair<LocalDate, LocalDate>>
    private val arbeidsavklaringspenger: List<Pair<LocalDate, LocalDate>>
    private val ugyldigeArbeidsavklaringspengeperioder: List<Pair<LocalDate, LocalDate>>

    private val erEgenAnsatt = packet["@løsning.${EgenAnsatt.name}"].asBoolean()
    private val inntekter = packet["@løsning.${Inntektsberegning.name}"].map {
        it["årMåned"].asYearMonth() to it["inntektsliste"].map {
            it.path("orgnummer").takeIf(JsonNode::isTextual)?.asText() to it["beløp"].asDouble()
        }
    }.associate { it }
    private val arbeidsforhold = packet["@løsning.${Opptjening.name}"].map {
        Opptjeningvurdering.Arbeidsforhold(
            orgnummer = it["orgnummer"].asText(),
            fom = it["ansattSiden"].asLocalDate(),
            tom = it["ansattTil"].asOptionalLocalDate()
        )
    }

    init {
        packet["@løsning.${Dagpenger.name}"]
            .map(::asDatePair)
            .partition { it.first <= it.second }
            .also {
                dagpenger = it.first
                ugyldigeDagpengeperioder = it.second
            }
        packet["@løsning.${Dagpenger.name}"].map(::asDatePair)
            .partition { it.first <= it.second }
            .also {
                arbeidsavklaringspenger = it.first
                ugyldigeArbeidsavklaringspengeperioder = it.second
            }
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asVilkårsgrunnlag(): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            orgnummer = organisasjonsnummer,
            inntektsvurdering = Inntektsvurdering(
                perioder = inntekter
            ),
            opptjeningvurdering = Opptjeningvurdering(
                arbeidsforhold = arbeidsforhold
            ),
            erEgenAnsatt = erEgenAnsatt,
            dagpenger = no.nav.helse.hendelser.Dagpenger(dagpenger.map {
                Periode(
                    it.first,
                    it.second
                )
            }),
            arbeidsavklaringspenger = no.nav.helse.hendelser.Arbeidsavklaringspenger(arbeidsavklaringspenger.map { Periode(it.first, it.second) })
        ).also {
            if (ugyldigeDagpengeperioder.isNotEmpty()) it.warn("Arena inneholdt en eller flere Dagpengeperioder med ugyldig fom/tom")
            if (ugyldigeArbeidsavklaringspengeperioder.isNotEmpty()) it.warn("Arena inneholdt en eller flere AAP-perioder med ugyldig fom/tom")
        }
    }

    private fun asDatePair(jsonNode: JsonNode) =
        jsonNode.path("fom").asLocalDate() to jsonNode.path("tom").asLocalDate()
}
