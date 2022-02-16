package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class V145LagreArbeidsforholdForOpptjening : JsonMigration(version = 145) {
    override val description: String = "Lagrer arbeidsforhold relevant til opptjening i vilkårsgrunnlag og arbeidsforhold-historikken"
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        /*
         [v] hent vilkårsgrunnlag
         [v] finn tilhørende vedtaksperiode, hent skjæringstidspunkt
         finn hvilke arbeidsforhold som ville bli brukt for opptjening for skjæringstidspunkt
         om vi ikke har noen relevante arbeidsforhold for et skjæringstidspunktet lag en insane default (opptjening fra 1970 eller noe)
         oppdater arbeidsforhold historikken
          * om vi har arbeidsgiveren fra før, legg den inn i historikken
          * om vi ikke har arbeidsgiveren fra før, opprett en ny arbeidsgiver og legg inn innslag
         legg inn alle relevante arbeidsforhold inn i vilkårsgrunnlag
         */
        val vilkårsgrunnlagMeldinger = meldingerSupplier.hentMeldinger()
            .values
            .filter { (navn, _) -> navn == "VILKÅRSGRUNNLAG" }
            .map { (_, json) -> serdeObjectMapper.readTree(json) }
            .map { skjæringstidspunktFor(jsonNode, it["vedtaksperiodeId"].asText()) to hentArbeidsforhold(it) }

        val skjæringstidspunkter = vilkårsgrunnlagMeldinger.map { (skjæringstidspunkt, _) -> skjæringstidspunkt }
        if (skjæringstidspunkter.isNotEmpty()) {
            sikkerLogg.info("Fant skjæringstidspunkter $skjæringstidspunkter fnr=${jsonNode["fødselsnummer"]}")
        }

        val skjæringstidspunkterFraSpleis = jsonNode["vilkårsgrunnlagHistorikk"]
            .flatMap { it["vilkårsgrunnlag"] }
            .filter { it["type"].asText() == "Vilkårsprøving" }
            .map { it["skjæringstidspunkt"].asText() }
            .sorted()
            .distinct()

        val skjæringstidspunkterViIkkeFinnerMeldingFor = skjæringstidspunkterFraSpleis
            .filter { it !in skjæringstidspunkter }

        if (skjæringstidspunkterViIkkeFinnerMeldingFor.isNotEmpty()) {
            sikkerLogg.info(
                "Fant skjæringstidspunkt(er) i vilkårsgrunnlagshistorikken vi ikke finner melding for $skjæringstidspunkterViIkkeFinnerMeldingFor"
                    + " fnr=${jsonNode["fødselsnummer"]}"
            )
        }
    }

    private fun hentArbeidsforhold(jsonNode: JsonNode) = jsonNode["@løsning"].arbeidsforholdNode().map {
        Arbeidsforhold(LocalDate.parse(it["ansattSiden"].asText()), it.optional("ansattTil")?.asText()?.let(LocalDate::parse), it["orgnummer"].asText())
    }

    private fun JsonNode.arbeidsforholdNode() = optional("ArbeidsforholdV2") ?: get("Opptjening")

    private fun JsonNode.optional(field: String) = takeIf { it.hasNonNull(field) }?.get(field)

    fun skjæringstidspunktFor(jsonNode: ObjectNode, vedtaksperiodeId: String): String? =
        jsonNode["arbeidsgivere"]
            .flatMap { it["vedtaksperioder"] + it["forkastede"].map { forkastet -> forkastet["vedtaksperiode"] } }
            .firstOrNull { it["id"].asText() == vedtaksperiodeId }
            ?.get("skjæringstidspunkt")
            ?.asText()
            .also { if (it == null) sikkerLogg.info("Fant ikke skjæringstidspunkt for $vedtaksperiodeId") }

    private data class Arbeidsforhold(val fom: LocalDate, val tom: LocalDate?, val orgnummer: String)
}
