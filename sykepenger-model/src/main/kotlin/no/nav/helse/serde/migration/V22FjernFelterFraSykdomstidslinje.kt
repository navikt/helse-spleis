package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V22FjernFelterFraSykdomstidslinje : JsonMigration(version = 22) {
    override val description: String = "Fjerner to ubrukte felter fra sykdomstidslinje"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver -> (
            arbeidsgiver.path("vedtaksperioder").flatMap { periode -> periode.path("sykdomshistorikk") } +
                arbeidsgiver.path("forkastede").flatMap { periode -> periode.path("sykdomshistorikk") } +
                arbeidsgiver.path("sykdomshistorikk")
            )
            .flatMap { element ->
                listOf(element.path("hendelseSykdomstidslinje"), element.path("beregnetSykdomstidslinje"))
            }
            .forEach { tidslinje ->
                tidslinje as ObjectNode
                tidslinje.remove("id")
                tidslinje.remove("tidsstempel")
            }
        }
    }
}
