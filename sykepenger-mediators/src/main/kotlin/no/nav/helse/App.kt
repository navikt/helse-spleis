package no.nav.helse

val SPILL_AV_IM_DISABLED = System.getenv("NAIS_CLUSTER_NAME") == "prod-gcp"

fun main() {
    val applicationBuilder = ApplicationBuilder(System.getenv())
    applicationBuilder.start()
    // Teit kommentar
}
