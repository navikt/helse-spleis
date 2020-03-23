package no.nav.helse.spleis

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import no.nav.helse.spleis.config.AzureAdAppConfig

internal fun Application.azureAdAppAuthentication(config: AzureAdAppConfig) {
    install(Authentication) {
        jwt {
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
    }
}
