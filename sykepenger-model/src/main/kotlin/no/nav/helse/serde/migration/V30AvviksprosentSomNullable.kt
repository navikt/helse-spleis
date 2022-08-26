package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode


internal class V30AvviksprosentSomNullable : JsonMigration(version = 30) {

    override val description = "Gjør avviksprosent nullable i stedet for NaN"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            (arbeidsgiver.path("vedtaksperioder") + arbeidsgiver.path("forkastede"))
                .forEach { periode ->
                    val dataForVilkårsvurdering = periode.path("dataForVilkårsvurdering")
                    if (dataForVilkårsvurdering.path("avviksprosent").asDouble().isNaN())
                        (dataForVilkårsvurdering as ObjectNode).putNull("avviksprosent")
                }
        }
    }
}
