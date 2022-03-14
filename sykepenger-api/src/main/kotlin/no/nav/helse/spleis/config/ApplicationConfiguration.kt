package no.nav.helse.spleis.config

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.auth.jwt.*
import io.ktor.server.engine.*
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal class ApplicationConfiguration(env: Map<String, String> = System.getenv()) {
    internal val ktorConfig = KtorConfig(
        httpPort = env["HTTP_PORT"]?.toInt() ?: 8080
    )

    internal val azureConfig = AzureAdAppConfig(
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
        configurationUrl = env.getValue("AZURE_APP_WELL_KNOWN_URL")
    )

    // Håndter on-prem og gcp database tilkobling forskjellig
    internal val dataSourceConfiguration = when (env["NAIS_CLUSTER_NAME"]) {
        "dev-gcp",
        "prod-gcp" -> GcpDataSourceConfiguration(
            jdbcUrl = env["DATABASE_JDBC_URL"],
            gcpProjectId = env["GCP_TEAM_PROJECT_ID"],
            databaseRegion = env["DATABASE_REGION"],
            databaseInstance = env["DATABASE_INSTANCE"],
            databaseUsername = env["DATABASE_SPLEIS_API_USERNAME"],
            databasePassword = env["DATABASE_SPLEIS_API_PASSWORD"],
            databaseName = env["DATABASE_DATABASE"]
        )
        "dev-fss",
        "prod-fss" -> OnPremDataSourceConfiguration(
            jdbcUrl = env["DATABASE_JDBC_URL"],
            databaseHost = env["DATABASE_HOST"],
            databasePort = env["DATABASE_PORT"],
            databaseUsername = env["DATABASE_USERNAME"],
            databasePassword = env["DATABASE_PASSWORD"],
            databaseName = env["DATABASE_NAME"],
            vaultMountPath = env["VAULT_MOUNTPATH"]
        )
        else -> throw IllegalArgumentException("env variable NAIS_CLUSTER_NAME has an unsupported value")
    }
}

internal class KtorConfig(private val httpPort: Int = 8080) {
    fun configure(builder: ApplicationEngineEnvironmentBuilder) {
        builder.connector {
            port = httpPort
        }
    }
}

internal class AzureAdAppConfig(private val clientId: String, configurationUrl: String) {
    private val issuer: String
    private val jwkProvider: JwkProvider
    private val jwksUri: String

    init {
        configurationUrl.getJson().also {
            this.issuer = it["issuer"].textValue()
            this.jwksUri = it["jwks_uri"].textValue()
        }

        jwkProvider = JwkProviderBuilder(URL(this.jwksUri)).build()
    }

    fun configureVerification(configuration: JWTAuthenticationProvider.Configuration) {
        configuration.verifier(jwkProvider, issuer) {
            withAudience(clientId)
        }
        configuration.validate { credentials -> JWTPrincipal(credentials.payload) }
    }

    private fun String.getJson(): JsonNode {
        val (responseCode, responseBody) = this.fetchUrl()
        if (responseCode >= 300 || responseBody == null) throw RuntimeException("got status $responseCode from ${this}.")
        return jacksonObjectMapper().readTree(responseBody)
    }

    private fun String.fetchUrl() = with(URL(this).openConnection() as HttpURLConnection) {
        requestMethod = "GET"
        val stream: InputStream? = if (responseCode < 300) this.inputStream else this.errorStream
        responseCode to stream?.bufferedReader()?.readText()
    }
}
