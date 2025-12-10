package no.nav.helse.dbscript

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.Base64
import no.nav.helse.dbscript.DbScript.ConnectionInfo

internal object ManuellOppkobling {
    fun start(block: (ConnectionInfo) -> Unit) {
        block(ventPåConnectionInfo())
    }

    private fun ventPåConnectionInfo(): ConnectionInfo {
        println("## Fyll inn databaseport. Defaulten er '5432'")
        val defaultPort = "5432"
        val port = Input.ventPåInput(defaultPort) { it.length == 4 && kotlin.runCatching { it.toInt() }.isSuccess }
        val defaultEpost = hentEpostFraGCloud()
        when (defaultEpost) {
            null -> println("## Fyll inn brukernavn (epost)")
            else -> println("## Fyll inn brukernavn (epost). Defaulten er '$defaultEpost'")
        }
        val epost = Input.ventPåEpost(defaultEpost)
        val jdbcUrl = "jdbc:postgresql://localhost:$port/spleis?user=${epost.verdi}"
        println(" - Bruker JdbcUrl '$jdbcUrl'")
        println()
        return ConnectionInfo(jdbcUrl, epost)
    }

    private fun hentEpostFraGCloud(): String? {
        val identityToken = hentIdentityTokenFraGCloud() ?: return null
        return try {
            val (_, payload, _) = identityToken.split('.', limit = 3)
            val json = objectMapper.readTree(Base64.getDecoder().decode(payload))
            return json.path("email").asText().takeIf { it.lowercase().endsWith("@nav.no") }
        } catch (_: Exception) { null }
    }

    private fun hentIdentityTokenFraGCloud() = try {
        Runtime.getRuntime().exec(arrayOf("gcloud", "auth", "print-identity-token")).let {
            val token = it.inputReader().readText().trim()
            it.waitFor()
            token.takeUnless { it.isBlank() }
        }
    } catch (_: Exception) { null }

    private val objectMapper = jacksonObjectMapper()
}
