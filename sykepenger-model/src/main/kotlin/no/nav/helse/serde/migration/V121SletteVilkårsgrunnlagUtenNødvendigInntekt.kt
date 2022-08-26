package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V121SletteVilkårsgrunnlagUtenNødvendigInntekt : JsonMigration(version = 121) {

    override val description = "Slette vilkårsgrunnlag som ble opprettet før vi mottok inntektsmelding og som ble lagret kun med skatteopplysninger"

    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {

        val aktørId = jsonNode["aktørId"]
        val kontekster = jsonNode["aktivitetslogg"]["kontekster"]
        val aktiviteterMedError = jsonNode["aktivitetslogg"]["aktiviteter"]
            .filter { it["alvorlighetsgrad"].asText() == "ERROR" }
            .filter { it["melding"].asText() == "Vi har ikke inntektshistorikken vi trenger for skjæringstidspunktet" }

        if (aktiviteterMedError.isEmpty()) return

        val kontektsterPerError = aktiviteterMedError
            .map { it["kontekster"].map(JsonNode::asInt) }

        val vedtaksperiodeIder = kontektsterPerError
            .map { it.map(kontekster::get).first { kontekst -> kontekst["kontekstType"]?.asText() == "Vedtaksperiode" } }
            .map { kontekst -> kontekst["kontekstMap"]["vedtaksperiodeId"].asText() }

        sikkerLogg.info("Prøver å fjerne vilkårsgrunnlag til $aktørId for vedtaksperioder: $vedtaksperiodeIder")

        val skjæringstidspunkter = jsonNode["arbeidsgivere"].flatMap { it["forkastede"] }
            .map { it["vedtaksperiode"] }
            .filter { it["id"].asText() in vedtaksperiodeIder }
            .map { it["skjæringstidspunkt"] }

        jsonNode["vilkårsgrunnlagHistorikk"]
            .forEach { innslag ->
                innslag["vilkårsgrunnlag"]
                    .removeAll { vilkårsgrunnlag ->
                        vilkårsgrunnlag["skjæringstidspunkt"] in skjæringstidspunkter
                            && vilkårsgrunnlag["sykepengegrunnlag"]["arbeidsgiverInntektsopplysninger"].all { it["inntektsopplysning"].has("skatteopplysninger") }
                    }
            }
    }
}
