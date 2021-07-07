package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V108UtvidetUtbetalingstidslinjeBeregning : JsonMigration(version = 108) {
    override val description: String = "UtbetalingstidslinjeBeregning peker på innslag i inntektshistorikk og vilkårsgrunnlag-historikk"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val vilkårsgrunnlagHistorikkInnslagId = jsonNode["vilkårsgrunnlagHistorikk"].firstOrNull()?.path("id")?.asText()
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            val beregnetUtbetalingstidslinjer = arbeidsgiver["beregnetUtbetalingstidslinjer"]
            if (beregnetUtbetalingstidslinjer.isEmpty) return
            val inntektshistorikkInnslagId = arbeidsgiver["inntektshistorikk"].firstOrNull()?.path("id")?.asText()
            beregnetUtbetalingstidslinjer.forEach {
                val beregning = it as ObjectNode
                beregning.put(
                    "vilkårsgrunnlagHistorikkInnslagId",
                    requireNotNull(vilkårsgrunnlagHistorikkInnslagId) { "Mangler vilkårsgrunnlaghistorikk, men har beregnetUtbetalingstidslinje" }
                )
                beregning.put(
                    "inntektshistorikkInnslagId",
                    requireNotNull(inntektshistorikkInnslagId) { "Mangler inntektshistorikk, men har beregnetUtbetalingstidslinje" }
                )
            }
        }
    }
}
