package no.nav.helse.spleis.config

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.azure.createAzureTokenClientFromEnvironment
import com.github.navikt.tbd_libs.azure.createDefaultAzureTokenClient
import com.github.navikt.tbd_libs.speed.SpeedClient
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTPrincipal
import java.net.URI
import no.nav.helse.spleis.SpekematClient
import no.nav.helse.spleis.objectMapper
import java.net.http.HttpClient

internal class ApplicationConfiguration(env: Map<String, String> = System.getenv()) {
    internal val azureConfig = AzureAdAppConfig(
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
        issuer = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
        jwkProvider = JwkProviderBuilder(URI(env.getValue("AZURE_OPENID_CONFIG_JWKS_URI")).toURL()).build(),
    )

    internal val speedClient = SpeedClient(
        httpClient = HttpClient.newHttpClient(),
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule()),
        tokenProvider = createDefaultAzureTokenClient(
            tokenEndpoint = URI(env.getValue(env.getValue("TOKEN_ENDPOINT_ENV_KEY"))),
            clientId = env.getValue("AZURE_APP_CLIENT_ID"),
            clientSecret = env.getValue("AZURE_APP_CLIENT_SECRET")
        ),
        baseUrl = env.getValue("SPEED_API_URL")
    )

    internal val spekematClient = SpekematClient(
        tokenProvider = createAzureTokenClientFromEnvironment(env),
        objectMapper = objectMapper,
        scope = env.getValue("SPEKEMAT_SCOPE")
    )

    // Håndter on-prem og gcp database tilkobling forskjellig
    internal val dataSourceConfiguration = DataSourceConfiguration(
        jdbcUrl = env["DATABASE_JDBC_URL"],
        gcpProjectId = env["GCP_TEAM_PROJECT_ID"],
        databaseRegion = env["DATABASE_REGION"],
        databaseInstance = env["DATABASE_INSTANCE"],
        databaseUsername = env["DATABASE_SPLEIS_API_USERNAME"],
        databasePassword = env["DATABASE_SPLEIS_API_PASSWORD"],
        databaseName = env["DATABASE_SPLEIS_API_DATABASE"]
    )
}

internal class AzureAdAppConfig(
    private val clientId: String,
    private val issuer: String,
    private val jwkProvider: JwkProvider
) {
    fun configureVerification(configuration: JWTAuthenticationProvider.Config) {
        configuration.verifier(jwkProvider, issuer) {
            withAudience(clientId)
        }
        configuration.validate { credentials -> JWTPrincipal(credentials.payload) }
    }
}
