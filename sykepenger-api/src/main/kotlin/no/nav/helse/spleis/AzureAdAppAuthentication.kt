package no.nav.helse.spleis

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import no.nav.helse.spleis.config.AzureAdAppConfig

internal const val API_SERVICE = "api_service"

internal fun Application.azureAdAppAuthentication(config: AzureAdAppConfig) {
    install(Authentication) {
        jwt(API_SERVICE) {
            config.configureVerification(this)
        }
    }
}
