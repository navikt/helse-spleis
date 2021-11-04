package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.Grunnbeløp
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class V114LagreSykepengegrunnlag : JsonMigration(version = 114) {

    override val description: String = "Beregne og lagre sykepengegrunnlag for alle vilkårsgrunnlagelementert"

    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        internal fun genererSykepengegrunnlag(vilkårsgrunnlag: ObjectNode, person: ObjectNode) {
            val skjæringstidspunkt = vilkårsgrunnlag["skjæringstidspunkt"].asText()
            val vilkårsgrunnlagtype = vilkårsgrunnlag["type"].asText()
            val sykepengegrunnlag = vilkårsgrunnlag.with("sykepengegrunnlag")

            sykepengegrunnlag.put("sykepengegrunnlag", 0.0)
            sykepengegrunnlag.put("grunnlagForSykepengegrunnlag", 0.0)

            val arbeidsgiverInntektsopplysninger = sykepengegrunnlag.withArray("arbeidsgiverInntektsopplysninger")


            val grunnlagForSykepengegrunnlag = person["arbeidsgivere"].sumOf { arbeidsgiver ->
                leggTilArbeidsgiverInntektsopplysning(
                    arbeidsgiverInntektsopplysninger,
                    arbeidsgiver as ObjectNode,
                    skjæringstidspunkt,
                    person["fødselsnummer"].asText(),
                    vilkårsgrunnlagtype
                )
            } * 12
            val sykepengegrunnlag2 = minOf(grunnlagForSykepengegrunnlag, Grunnbeløp.`6G`.beløp(LocalDate.parse(skjæringstidspunkt)).reflection { årlig, _, _, _ -> årlig })

            sykepengegrunnlag.put("sykepengegrunnlag", sykepengegrunnlag2)
            sykepengegrunnlag.put("grunnlagForSykepengegrunnlag", grunnlagForSykepengegrunnlag)
        }

        private fun leggTilArbeidsgiverInntektsopplysning(
            arbeidsgiverInntektsopplysninger: ArrayNode,
            arbeidsgiver: ObjectNode,
            skjæringstidspunkt: String,
            fnr: String,
            vilkårsgrunnlagtype: String
        ): Double {
            val inntektsopplysning = finnInntektsopplysning(
                inntektshistorikk = arbeidsgiver["inntektshistorikk"],
                skjæringstidspunkt = skjæringstidspunkt,
                vilkårsgrunnlagtype = vilkårsgrunnlagtype,
                vedtaksperioder = arbeidsgiver["vedtaksperioder"] as ArrayNode,
                forkastedeVedtaksperioder = (arbeidsgiver["forkastede"] as ArrayNode).map { it["vedtaksperiode"] }
            )

            if (inntektsopplysning != null) {
                val arbeidsgiverInntektsopplysning = arbeidsgiverInntektsopplysninger.addObject()
                arbeidsgiverInntektsopplysning.put("orgnummer", arbeidsgiver["organisasjonsnummer"].asText())
                arbeidsgiverInntektsopplysning.set<JsonNode>("inntektsopplysning", inntektsopplysning)

                return if (inntektsopplysning.has("skatteopplysninger")) beregnBeløpFraSkatt(inntektsopplysning.withArray("skatteopplysninger")) else inntektsopplysning["beløp"].asDouble()
            }
            sikkerLogg.info("Migrering V114: Fant ikke inttektsopplysning for nåværende skjæringstidspunkt $skjæringstidspunkt for fnr $fnr")

            return 0.0
        }

        private fun finnInntektsmelding(inntektsopplysning: JsonNode, vedtaksperioder: Iterable<JsonNode>, skjæringstidspunkt: String): Boolean {
            val inntektsmeldingId = vedtaksperioder
                .firstOrNull { it["fom"].asText() == skjæringstidspunkt }
                ?.get("inntektsmeldingInfo")?.get("id") ?: return false

            val hendelseId = inntektsopplysning["hendelseId"] ?: return false

            return hendelseId.asText() == inntektsmeldingId.asText()
        }

        private fun finnInntektsopplysning(
            inntektshistorikk: JsonNode,
            skjæringstidspunkt: String,
            vilkårsgrunnlagtype: String,
            vedtaksperioder: Iterable<JsonNode>,
            forkastedeVedtaksperioder: Iterable<JsonNode>
        ): JsonNode? {
            val inntektsopplysninger = inntektshistorikk.firstOrNull()?.get("inntektsopplysninger") as ArrayNode? ?: return null

            if(vilkårsgrunnlagtype == "Infotrygd") {
                return finnInfotrygdInntektsopplysning(inntektsopplysninger, skjæringstidspunkt) ?: finnNærmesteInntektsopplysningEtterSkjæringstidspunkt(inntektsopplysninger, skjæringstidspunkt)
            }

            return finnSaksbehandlerInntektsopplysning(inntektsopplysninger, skjæringstidspunkt)
                ?: finnInntektsmeldingInntektsopplysningFraVedtaksperiode(inntektsopplysninger, skjæringstidspunkt, vedtaksperioder)
                ?: finnInntektsmeldingInntektsopplysningFraVedtaksperiode(inntektsopplysninger, skjæringstidspunkt, forkastedeVedtaksperioder)
                ?: finnInntektsmeldingInntektsopplysningFraSkjæringstidspunkt(inntektsopplysninger, skjæringstidspunkt)
                ?: finnSykepengegrunnlagInntektsopplysning(inntektsopplysninger, skjæringstidspunkt)
        }

        private fun finnSaksbehandlerInntektsopplysning(inntektsopplysninger: ArrayNode, skjæringstidspunkt: String) = inntektsopplysninger
            .filter { "SAKSBEHANDLER" == it["kilde"]?.asText() }
            .firstOrNull { skjæringstidspunkt == it["dato"]?.asText()  }

        private fun finnInfotrygdInntektsopplysning(inntektsopplysninger: ArrayNode, skjæringstidspunkt: String) = inntektsopplysninger
            .filter { "INFOTRYGD" == it["kilde"]?.asText() }
            .firstOrNull { skjæringstidspunkt == it["dato"]?.asText()  }

        private fun finnInntektsmeldingInntektsopplysningFraVedtaksperiode(
            inntektsopplysninger: ArrayNode,
            skjæringstidspunkt: String,
            vedtaksperioder: Iterable<JsonNode>
        ) = inntektsopplysninger
            .filter { "INNTEKTSMELDING" == it["kilde"]?.asText() }
            .firstOrNull { finnInntektsmelding(it, vedtaksperioder, skjæringstidspunkt) }

        private fun finnInntektsmeldingInntektsopplysningFraSkjæringstidspunkt(
            inntektsopplysninger: ArrayNode,
            skjæringstidspunkt: String,
        ) = inntektsopplysninger
            .filter { "INNTEKTSMELDING" == it["kilde"]?.asText() }
            .firstOrNull { skjæringstidspunkt == it["dato"]?.asText()  }

        private fun finnSykepengegrunnlagInntektsopplysning(
            inntektsopplysninger: ArrayNode,
            skjæringstidspunkt: String,
        ) = inntektsopplysninger
            .filter { it.has("skatteopplysninger") && it["skatteopplysninger"].first()["kilde"].asText() == "SKATT_SYKEPENGEGRUNNLAG" }
            .firstOrNull { skjæringstidspunkt == it["skatteopplysninger"].first()["dato"].asText() }

        private fun finnNærmesteInntektsopplysningEtterSkjæringstidspunkt(inntektsopplysninger: ArrayNode, skjæringstidspunkt: String) = inntektsopplysninger
            .sortedBy { if (it.has("skatteopplysninger")) it["skatteopplysninger"].first()["dato"].asText() else it["dato"].asText() }
            .firstOrNull {
                val dato = if (it.has("skatteopplysninger")) it["skatteopplysninger"].first()["dato"].asText() else it["dato"].asText()
                dato > skjæringstidspunkt
            }
            ?.takeIf { it.has("kilde") && it["kilde"].asText() == "INFOTRYGD" }

        private fun finnDato(inntektsopplysning: JsonNode) =
            if (inntektsopplysning.has("skatteopplysninger")) inntektsopplysning["skatteopplysninger"].first()["dato"].asText() else inntektsopplysning["dato"].asText()

        private fun beregnBeløpFraSkatt(skatteopplysninger: ArrayNode) = skatteopplysninger
            .sumOf { it["beløp"].asDouble() }
            .div(3)

        private fun finnInntektsopplysningGammal(
            inntektshistorikk: JsonNode,
            skjæringstidspunkt: String,
            vilkårsgrunnlagtype: String,
            vedtaksperioder: ArrayNode
        ): JsonNode? =
            inntektshistorikk.firstOrNull()
                ?.get("inntektsopplysninger")
                ?.filter { vilkårsgrunnlagtype != "Infotrygd" || "INFOTRYGD" == it["kilde"]?.asText() }
                ?.filterNot { it.has("skatteopplysninger") && it["skatteopplysninger"].first()["kilde"].asText() == "SKATT_SAMMENLIGNINGSGRUNNLAG" }
                ?.filter {
                    skjæringstidspunkt == finnDato(it) || finnInntektsmelding(it, vedtaksperioder, skjæringstidspunkt)
                }?.maxByOrNull {
                    when (if (it.has("skatteopplysninger")) it["skatteopplysninger"].first()["kilde"].asText() else it["kilde"].asText()) {
                        "SAKSBEHANDLER" -> 100
                        "INFOTRYGD" -> 80
                        "INNTEKTSMELDING" -> 60
                        "SKATT_SYKEPENGEGRUNNLAG" -> 40
                        else -> 0
                    }
                } ?:
            inntektshistorikk.firstOrNull()
                ?.get("inntektsopplysninger")
                ?.sortedBy { if(it.has("skatteopplysninger")) it["skatteopplysninger"].first()["dato"].asText() else it["dato"].asText() }
                ?.firstOrNull {
                    val dato =  if(it.has("skatteopplysninger")) it["skatteopplysninger"].first()["dato"].asText() else it["dato"].asText()
                    dato > skjæringstidspunkt
                }
                ?.takeIf { it.has("kilde") && it["kilde"].asText() == "INFOTRYGD" }
    }

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["vilkårsgrunnlagHistorikk"].flatMap {
            it["vilkårsgrunnlag"]
        }.forEach { genererSykepengegrunnlag((it as ObjectNode), jsonNode) }
    }
}
