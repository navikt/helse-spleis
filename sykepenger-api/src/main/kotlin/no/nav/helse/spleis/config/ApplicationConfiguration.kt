package no.nav.helse.spleis.config

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.engine.ApplicationEngineEnvironmentBuilder
import io.ktor.server.engine.connector
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal class ApplicationConfiguration(env: Map<String, String> = System.getenv()) {
    internal val ktorConfig = KtorConfig(
        httpPort = env["HTTP_PORT"]?.toInt() ?: 8080
    )

    internal val azureConfig = AzureAdAppConfig(
        clientId = "/var/run/secrets/nais.io/azure/client_id".readFile() ?: env.getValue("AZURE_CLIENT_ID"),
        configurationUrl = env["AZURE_CONFIG_URL"],
        issuer = env["AZURE_ISSUER"],
        jwksUri = env["AZURE_JWKS_URI"],
        requiredGroup = env.getValue("AZURE_REQUIRED_GROUP"),
        spesialistClientId = env.getValue("SPESIALIST_CLIENT_ID")
    )

    internal val dataSourceConfiguration = DataSourceConfiguration(
        jdbcUrl = env["DATABASE_JDBC_URL"],
        databaseHost = env["DATABASE_HOST"],
        databasePort = env["DATABASE_PORT"],
        databaseUsername = env["DATABASE_USERNAME"],
        databasePassword = env["DATABASE_PASSWORD"],
        databaseName = env["DATABASE_NAME"],
        vaultMountPath = env["VAULT_MOUNTPATH"]
    )
}

internal class KtorConfig(private val httpPort: Int = 8080) {
    fun configure(builder: ApplicationEngineEnvironmentBuilder) {
        builder.connector {
            port = httpPort
        }
    }
}

internal class AzureAdAppConfig(
    internal val clientId: String,
    configurationUrl: String?,
    issuer: String? = null,
    jwksUri: String? = null,
    internal val spesialistClientId: String,
    internal val requiredGroup: String) {
    internal val issuer: String
    internal val jwkProvider: JwkProvider
    private val jwksUri: String

    init {
        if (issuer != null && jwksUri != null) {
            this.issuer = issuer
            this.jwksUri = jwksUri
        } else {
            requireNotNull(configurationUrl) { "Configuration url must be set if issuer or jwksUri is not set" }
                .getJson().also {
                    this.issuer = it["issuer"].textValue()
                    this.jwksUri = it["jwks_uri"].textValue()
                }
        }

        jwkProvider = JwkProviderBuilder(URL(this.jwksUri)).build()
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

private fun String.readFile() =
    try {
        File(this).readText(Charsets.UTF_8)
    } catch (err: FileNotFoundException) {
        null
    }
