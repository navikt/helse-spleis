package no.nav.helse.spleis

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class AzureTokenStub(
    private val issuer: Issuer
) {
    private val randomPort = ServerSocket(0).use { it.localPort }
    private val wireMockServer: WireMockServer = WireMockServer(randomPort)
    private val jwksPath = "/discovery/v2.0/keys"

    fun wellKnownEndpoint() = URI("http://localhost:$randomPort$jwksPath")

    suspend fun startServer(): Boolean {
        return suspendCoroutine { continuation ->
            //Stub ID provider (for authentication of REST endpoints)
            wireMockServer.start()
            ventPåServeroppstart()
            wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo(jwksPath)).willReturn(WireMock.okJson(issuer.jwks))
            )
            continuation.resume(true) // returnerer true bare for å ha en verdi
        }
    }

    suspend fun stopServer() = suspendCoroutine {
        wireMockServer.stop()
        it.resume(true) // returnerer true bare for å ha en verdi
    }

    private fun ventPåServeroppstart() = retry {
        try {
            Socket("localhost", wireMockServer.port()).use { it.isConnected }
        } catch (err: Exception) {
            false
        }
    }
}