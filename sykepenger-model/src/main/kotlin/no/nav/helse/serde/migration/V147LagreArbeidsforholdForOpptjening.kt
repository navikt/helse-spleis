package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.serde.migration.V147LagreArbeidsforholdForOpptjening.Arbeidsforhold
import no.nav.helse.serde.migration.V147LagreArbeidsforholdForOpptjening.Opptjeningsgrunnlag
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class V147LagreArbeidsforholdForOpptjening : JsonMigration(version = 147) {
    override val description: String =
        "Lagrer arbeidsforhold relevant til opptjening i vilkårsgrunnlag og arbeidsforhold-historikken"
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    /* DETTE ER VÅR PLAN:
    * To trinns rakett:
    * 1. for hver vilkårsgrunnlagmelding med meldingsreferanse: migrer inn opptjening i vilkårsgrunnlag med samme meldingsreferanse
    * 2. kobler sammen opptjening for vilkårsgrunnlag med overstyrt inntekt
    *       a) finn den originale vilkårsgrunnlaget som ble overstyrt via antallOpptjeningsdagerErMinst
    *       b) kopier opptjening inn i revurderte vilkårsgrunnøag
    *
    * Om vi ikke finner via meldingsreferanse
    *   - lag dummyopptjening
    * */
    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val meldinger = meldingerSupplier.hentMeldinger()
        val vilkårsgrunnlagMeldinger =
            meldinger.filterValues { it.first == "VILKÅRSGRUNNLAG" }
                .mapValues { (_, melding) -> serdeObjectMapper.readTree(melding.second) }

        val fødselsnummer = jsonNode["fødselsnummer"].asText()

        vilkårsgrunnlagMeldinger.forEach { vilkårsgrunnlagMelding ->
            val vilkårsgrunnlagMedMeldingsreferanse = jsonNode.get("vilkårsgrunnlagHistorikk")
                .flatMap { it.get("vilkårsgrunnlag") }
                .filter { it.get("type").asText() == "Vilkårsprøving" }
                .filter { it.hasNonNull("meldingsreferanseId") }
                .filter { it.get("meldingsreferanseId").asText() == vilkårsgrunnlagMelding.key.toString() }

            if (vilkårsgrunnlagMedMeldingsreferanse.isEmpty()) {
                return@forEach
            }
            val opptjeningFraMelding = vilkårsgrunnlagMelding.value.tilOpptjeningsgrunnlag(
                LocalDate.parse(vilkårsgrunnlagMedMeldingsreferanse.first().get("skjæringstidspunkt").asText())
            )
            leggTilArbeidsforholdBruktTilOpptjeningIArbeidsforholdhistorikken(
                jsonNode.get("arbeidsgivere") as ArrayNode,
                opptjeningFraMelding
            )

            vilkårsgrunnlagMedMeldingsreferanse
                .map { it as ObjectNode }
                .forEach {
                    it.set<ObjectNode>("opptjening", opptjeningFraMelding.tilOpptjening())
                }
        }

        val alleVilkårsgrunnlag = jsonNode.get("vilkårsgrunnlagHistorikk")
            .flatMap { it.get("vilkårsgrunnlag") }

        alleVilkårsgrunnlag
            .filter { !it.hasNonNull("opptjening") }
            .map { it as ObjectNode }
            .forEach { vilkårsgrunnlagUtenOpptjening ->
                val matchendeAntallOpptjeningsdager = alleVilkårsgrunnlag.firstOrNull { vilkårsgrunnlag ->
                    vilkårsgrunnlagUtenOpptjening.antallOpptjeningsdager() == vilkårsgrunnlag.antallOpptjeningsdager()
                        && vilkårsgrunnlagUtenOpptjening["sammenligningsgrunnlag"] == vilkårsgrunnlag["sammenligningsgrunnlag"]
                        && vilkårsgrunnlag.hasNonNull("opptjening")
                }
                if (matchendeAntallOpptjeningsdager != null) {
                    vilkårsgrunnlagUtenOpptjening.set<ObjectNode>("opptjening", matchendeAntallOpptjeningsdager["opptjening"].deepCopy())
                }
            }

        alleVilkårsgrunnlag
            .filter { !it.hasNonNull("opptjening") }
            .map { it as ObjectNode }
            .forEach {
                val skjæringstidspunkt = LocalDate.parse(it["skjæringstidspunkt"].asText())
                val vilkårsgrunnlagId = it["vilkårsgrunnlagId"].asText()
                val opptjeningFom = skjæringstidspunkt.minusDays(it.antallOpptjeningsdager().asLong())
                sikkerLogg.info("Genererer dummy-arbeidsforhold for vilkårsgrunnlagId=$vilkårsgrunnlagId "
                    + "og fødselsnummer=$fødselsnummer")
                val generertArbeidsforhold = listOf(
                    Opptjeningsgrunnlag.OpptjeningsgrunnlagArbeidsforhold(
                        orgnummer = "MANGLET_ORGNUMMER_VED_MIGRERING",
                        ansattFom = opptjeningFom.toString(),
                        ansattTom = null
                    )
                )
                it.set<ObjectNode>("opptjening", Opptjeningsgrunnlag(
                    arbeidsforhold = generertArbeidsforhold,
                    opptjeningsperiode = opptjeningFom til skjæringstidspunkt,
                    skjæringstidspunkt = skjæringstidspunkt
                ).tilOpptjening())
            }
    }

    private fun JsonNode.antallOpptjeningsdager() = get("antallOpptjeningsdagerErMinst")

    private fun ObjectNode.emptyArray(name: String) = apply { withArray(name) }

    private fun ArrayNode.finnEllerOpprettArbeidsgiver(orgnummer: String) : ObjectNode {
        return firstOrNull { it["organisasjonsnummer"].asText() == orgnummer } as ObjectNode?
            ?: addObject()
                .put("organisasjonsnummer", orgnummer)
                .put("id", UUID.randomUUID().toString())
                .emptyArray("inntektshistorikk")
                .emptyArray("sykdomshistorikk")
                .emptyArray("vedtaksperioder")
                .emptyArray("forkastede")
                .emptyArray("utbetalinger")
                .emptyArray("beregnetUtbetalingstidslinjer")
                .emptyArray("feriepengeutbetalinger")
                .emptyArray("refusjonOpphører")
                .emptyArray("refusjonshistorikk")
                .emptyArray("arbeidsforholdhistorikk")
                .emptyArray("inntektsmeldingInfo")
    }

    private fun leggTilArbeidsforholdBruktTilOpptjeningIArbeidsforholdhistorikken(
        arbeidsgivere: ArrayNode,
        opptjening: Opptjeningsgrunnlag
    ) {

        opptjening.arbeidsforhold
            .forEach { arbeidsforhold ->
                val arbeidsforholdhistorikkJson = arbeidsgivere.finnEllerOpprettArbeidsgiver(arbeidsforhold.orgnummer)
                    .get("arbeidsforholdhistorikk") as ArrayNode

                val arbeidsforholdhistorikkInnslagJson = arbeidsforholdhistorikkJson
                    .firstOrNull { LocalDate.parse(it.get("skjæringstidspunkt").asText()) == opptjening.skjæringstidspunkt }
                    ?.get("arbeidsforhold") as ArrayNode?
                    ?: arbeidsforholdhistorikkJson.addObject()
                        .put("skjæringstidspunkt", opptjening.skjæringstidspunkt.toString())
                        .put("id", UUID.randomUUID().toString())
                        .withArray("arbeidsforhold")


                val arbeidsforholdhistorikk = arbeidsforholdhistorikkInnslagJson.toArbeidsforholdhistorikk()

                val nyttArbeidsforhold = Arbeidsforhold(ansattFom = arbeidsforhold.ansattFom, ansattTom = arbeidsforhold.ansattTom)
                if (nyttArbeidsforhold in arbeidsforholdhistorikk)
                    return@forEach

                arbeidsforholdhistorikkInnslagJson
                    .addObject()
                    .put("ansattFom", nyttArbeidsforhold.ansattFom)
                    .put("ansattTom", nyttArbeidsforhold.ansattTom)
                    .put("deaktivert", false)
            }
    }


    data class Opptjeningsgrunnlag(
        val arbeidsforhold: List<OpptjeningsgrunnlagArbeidsforhold>,
        val opptjeningsperiode: Periode,
        val skjæringstidspunkt: LocalDate
    ) {
        data class OpptjeningsgrunnlagArbeidsforhold(
            val orgnummer: String,
            val ansattFom: String,
            val ansattTom: String? = null
        )
    }

    data class Arbeidsforhold(
        val ansattFom: String,
        val ansattTom: String? = null
    )


}

private fun Iterable<JsonNode>.toArbeidsforholdhistorikk() =
    map { arbeidsforhold ->
        Arbeidsforhold(
            ansattFom = arbeidsforhold.get("ansattFom").asText(),
            ansattTom = arbeidsforhold.optional("ansattTom")?.asText()
        )
    }


private fun Opptjeningsgrunnlag.tilOpptjening(): ObjectNode {
    val arbeidsforholdNoder = serdeObjectMapper.createArrayNode()
    arbeidsforhold
        .groupBy { it.orgnummer }
        .forEach { (orgnummer, grupperteArbeidsforhold) ->
        arbeidsforholdNoder.addObject()
            .put("orgnummer", orgnummer)
            .putArray("ansattPerioder")
            .addAll(grupperteArbeidsforhold.map { forhold ->
                serdeObjectMapper.createObjectNode()
                    .put("ansattFom", forhold.ansattFom)
                    .put("ansattTom", forhold.ansattTom)
                    .put("deaktivert", false)
            })
    }

    return serdeObjectMapper.createObjectNode()
        .put("opptjeningFom", opptjeningsperiode.start.toString())
        .put("opptjeningTom", opptjeningsperiode.endInclusive.toString())
        .set("arbeidsforhold", arbeidsforholdNoder)
}

private fun Periode.rettFørEllerOverlapper(dato: LocalDate) = start < dato && endInclusive.plusDays(1) >= dato

internal fun Collection<Periode>.sammenhengende(skjæringstidspunkt: LocalDate) = sortedByDescending { it.start }
    .fold(skjæringstidspunkt til skjæringstidspunkt) { acc, periode ->
        if (periode.rettFørEllerOverlapper(acc.start)) periode.start til acc.endInclusive
        else acc
    }

private fun JsonNode.optional(field: String) = takeIf { it.hasNonNull(field) }?.get(field)

private fun JsonNode.tilOpptjeningsgrunnlag(skjæringstidspunkt: LocalDate): Opptjeningsgrunnlag {

    val løsning = get("@løsning")
    val opptjening = løsning.optional("Opptjening") ?: løsning.get("ArbeidsforholdV2")

    val arbeidsforhold = opptjening.map {
        Opptjeningsgrunnlag.OpptjeningsgrunnlagArbeidsforhold(
            ansattFom = it.get("ansattSiden").asText(),
            ansattTom = it.optional("ansattTil")?.asText(),
            orgnummer = it.get("orgnummer").asText()
        )
    }

    val opptjeningsperiode = arbeidsforhold
        .map { LocalDate.parse(it.ansattFom) til (it.ansattTom?.let(LocalDate::parse) ?: skjæringstidspunkt) }
        .sammenhengende(skjæringstidspunkt)

    return Opptjeningsgrunnlag(
        arbeidsforhold = arbeidsforhold.filter { LocalDate.parse(it.ansattFom) in opptjeningsperiode },
        opptjeningsperiode = opptjeningsperiode,
        skjæringstidspunkt = skjæringstidspunkt
    )
}
