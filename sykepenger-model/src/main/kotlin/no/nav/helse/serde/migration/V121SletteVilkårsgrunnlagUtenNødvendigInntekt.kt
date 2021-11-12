package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V121SletteVilkårsgrunnlagUtenNødvendigInntekt : JsonMigration(version = 121) {

    override val description = "Slette vilkårsgrunnlag som ble opprettet før vi mottok inntektsmelding og som ble lagret kun med skatteopplysninger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        // 1. Finn aktivitetene med erroren
        // 2. Hente ut kontekstId'ene for hver enkelt error
        // 3. Finne den ène konteksstId'en som hører til en vedtaksperiode
        // 4. Hente ut vedtaksperiodeId
        // 5. Finne vilkårsgrunnlag i kontektsten





        val kontekster = jsonNode["aktivetslogg"]["kontektster"]

        val indekser = jsonNode["aktivitetslogg"]["aktiviteter"]
            .filter { it["alvorlighetsgrad"].asText() == "ERROR" }
            .filter { it["melding"].asText() == "Vi har ikke inntektshistorikken vi trenger for skjæringstidspunktet" }
            .map { it["kontekster"] }

        val vilkårsgrunnlag =
            indekser.map { it.map { indeks -> kontekster[indeks.asInt()] }.single { kontekst -> kontekst["kontekstType"].asText() == "Vilkårsgrunnlag" } }
                .map { vilkårsgrunnlag -> vilkårsgrunnlag["meldingsreferanseId"].asText() }


    }
}
