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
            verifier(config.jwkProvider, config.issuer)
            validate { credentials ->
                val authorizedParty: String?  = credentials.payload.getClaim("azp").asString()

                if (config.clientId !in credentials.payload.audience || (config.spesialistClientId != null && authorizedParty != config.spesialistClientId)) {
                    log.info("${credentials.payload.subject} with audience ${credentials.payload.audience} and authorized party $authorizedParty is not authorized to use this app, denying access")
                    return@validate null
                }

                JWTPrincipal(credentials.payload)
            }
        }
    }
}
