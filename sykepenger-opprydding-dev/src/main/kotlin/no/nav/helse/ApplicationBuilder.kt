package no.nav.helse

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {
    // Sikrer at man ikke får tilgang til db andre steder enn i dev-gcp
    private val dataSourceBuilder = when (env["NAIS_CLUSTER_NAME"]) {
        "dev-gcp" -> DataSourceBuilder(env)
        else -> throw IllegalArgumentException("env variable NAIS_CLUSTER_NAME has an unsupported value. Supported values: [dev-gcp]. Prohibited values: [prod-gcp, dev-fss, prod-fss]")
    }
    private val rapidsConnection = RapidApplication.create(env)

    init {
        rapidsConnection.register(this)
        SlettPersonRiver(rapidsConnection, PersonRepository(dataSourceBuilder.getDataSource()))
    }

    internal fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dataSourceBuilder.migrate()
    }
}