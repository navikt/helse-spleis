package no.nav.helse.spleis

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import no.nav.helse.spleis.config.AzureAdAppConfig

const val API_BRUKER = "api_bruker"
const val API_SERVICE = "api_service"

internal fun Application.azureAdAppAuthentication(config: AzureAdAppConfig) {
    install(Authentication) {
        jwt(API_BRUKER) {
            verifier(config.jwkProvider, config.issuer)
            validate { credentials ->
                val groupsClaim = credentials.payload.getClaim("groups").asList(String::class.java)
                if (config.requiredGroup !in groupsClaim || config.clientId !in credentials.payload.audience) {
                    log.info("${credentials.payload.subject} with audience ${credentials.payload.audience} is not authorized to use this app, denying access")
                    return@validate null
                }

                JWTPrincipal(credentials.payload)
            }
        }
        jwt(API_SERVICE) {
            verifier(config.jwkProvider, config.issuer)
            validate { credentials ->
                val authorizedParty: String?  = credentials.payload.getClaim("azp").asString()

                if (config.clientId !in credentials.payload.audience && authorizedParty == config.spesialistClientId) {
                    log.info("${credentials.payload.subject} with audience ${credentials.payload.audience} and authorized party $authorizedParty is not authorized to use this app, denying access")
                    return@validate null
                }

                JWTPrincipal(credentials.payload)
            }
        }
    }
}
