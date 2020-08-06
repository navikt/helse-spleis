package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Arbeidsavklaringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Dagpenger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
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
            arbeidsgiver(it) to it["beløp"].asDouble().månedlig
        }
    }.toMap()

    private fun arbeidsgiver(node: JsonNode) = when {
        node.path("orgnummer").isTextual -> node.path("orgnummer").asText()
        node.path("fødselsnummer").isTextual -> node.path("fødselsnummer").asText()
        node.path("aktørId").isTextual -> node.path("aktørId").asText()
        else -> null
    }

    private val arbeidsforhold = packet["@løsning.${Opptjening.name}"].map {
        Opptjeningvurdering.Arbeidsforhold(
            orgnummer = it["orgnummer"].asText(),
            fom = it["ansattSiden"].asLocalDate(),
            tom = it["ansattTil"].asOptionalLocalDate()
        )
    }

    private val medlemskapstatus = when (packet["@løsning.${Medlemskap.name}.resultat.svar"].asText()) {
        "JA" -> Medlemskapsvurdering.Medlemskapstatus.Ja
        "NEI" -> Medlemskapsvurdering.Medlemskapstatus.Nei
        else -> Medlemskapsvurdering.Medlemskapstatus.VetIkke
    }

    init {
        packet["@løsning.${Dagpenger.name}.meldekortperioder"]
            .map(::asDatePair)
            .partition { it.first <= it.second }
            .also {
                dagpenger = it.first
                ugyldigeDagpengeperioder = it.second
            }
        packet["@løsning.${Arbeidsavklaringspenger.name}.meldekortperioder"].map(::asDatePair)
            .partition { it.first <= it.second }
            .also {
                arbeidsavklaringspenger = it.first
                ugyldigeArbeidsavklaringspengeperioder = it.second
            }
    }

    private val vilkårsgrunnlag get() = Vilkårsgrunnlag(
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
        medlemskapsvurdering = Medlemskapsvurdering(
            medlemskapstatus = medlemskapstatus
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

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, vilkårsgrunnlag)
    }

    private fun asDatePair(jsonNode: JsonNode) =
        jsonNode.path("fom").asLocalDate() to jsonNode.path("tom").asLocalDate()
}
