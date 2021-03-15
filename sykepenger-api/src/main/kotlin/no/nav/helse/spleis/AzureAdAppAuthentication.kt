package no.nav.helse.spleis

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import no.nav.helse.spleis.config.AzureAdAppConfig

internal const val API_BRUKER = "api_bruker"
internal const val API_SERVICE = "api_service"

internal fun Application.azureAdAppAuthentication(config: AzureAdAppConfig) {
    install(Authentication) {
        jwt(API_SERVICE) {
            config.configureVerification(this)
        }
    }
}
