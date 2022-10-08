package no.nav.helse.opprydding

import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(env: Map<String, String>) {
    // Sikrer at man ikke får tilgang til db andre steder enn i dev-gcp
    private val dataSourceBuilder = when (env["NAIS_CLUSTER_NAME"]) {
        "dev-gcp" -> DataSourceBuilder(env)
        else -> throw IllegalArgumentException("env variable NAIS_CLUSTER_NAME has an unsupported value. Supported values: [dev-gcp]. Prohibited values: [prod-gcp, dev-fss, prod-fss]")
    }
    private val rapidsConnection = RapidApplication.create(env)

    init {
        SlettPersonRiver(rapidsConnection, PersonRepository(dataSourceBuilder.getDataSource()))
    }

    internal fun start() = rapidsConnection.start()
}