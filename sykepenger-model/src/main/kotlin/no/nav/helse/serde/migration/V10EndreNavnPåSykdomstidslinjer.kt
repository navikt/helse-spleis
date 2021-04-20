package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V10EndreNavnPåSykdomstidslinjer : JsonMigration(version = 10) {

    override val description = "Fjerner gamle sykdomstidslinjene"
    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val beregnetTidslinjeKey = "beregnetSykdomstidslinje"
    private val nyHendelsetidslinjeKey = "nyHendelseSykdomstidslinje"
    private val nyBeregnetTidslinjeKey = "nyBeregnetSykdomstidslinje"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                periode.path("sykdomshistorikk").forEach { historikkElement ->
                    historikkElement as ObjectNode
                    historikkElement.replace(hendelsetidslinjeKey, historikkElement[nyHendelsetidslinjeKey])
                    historikkElement.remove(nyHendelsetidslinjeKey)
                    historikkElement.replace(beregnetTidslinjeKey, historikkElement[nyBeregnetTidslinjeKey])
                    historikkElement.remove(nyBeregnetTidslinjeKey)
                }
            }
        }
    }
}
