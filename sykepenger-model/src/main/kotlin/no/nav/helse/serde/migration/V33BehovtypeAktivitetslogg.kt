package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSammenligningsgrunnlag

internal class V33BehovtypeAktivitetslogg : JsonMigration(version = 33) {

    override val description = "Migrerer navneendring for behovtype InntekterForSammenligningsgrunnlag i aktivitetslogg"

    override fun doMigration(jsonNode: ObjectNode) {
        val aktivitetslogg = jsonNode["aktivitetslogg"] as ObjectNode

        aktivitetslogg["aktiviteter"]
            .filter { it.has("behovtype") && it["behovtype"].asText() == "Inntektsberegning" }
            .onEach { aktivitet ->
                aktivitet as ObjectNode
                aktivitet.put("behovtype", InntekterForSammenligningsgrunnlag.name)
            }
    }
}
