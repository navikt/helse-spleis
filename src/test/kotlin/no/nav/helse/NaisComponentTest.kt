package no.nav.helse

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.nais.nais
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

@KtorExperimentalAPI
class NaisComponentTest {

    private val testEnv = applicationEngineEnvironment {
        connector {
            port = Random.nextInt(1000, 9999)
        }

        module {
            nais()
        }
    }

    @Test
    fun `should respond with metrics`() {
        testServer(environment = testEnv) {
            handleRequest(HttpMethod.Get, "/metrics") { responseStatusCode ->
                assertEquals(HttpStatusCode.OK, responseStatusCode)
                assertTrue(responseBody.contains("process_cpu_seconds_total"))
            }
        }
    }

    @Test
    fun `should be alive`() {
        testServer(environment = testEnv) {
            handleRequest(HttpMethod.Get, "/isalive") { responseStatusCode ->
                assertEquals(HttpStatusCode.OK, responseStatusCode)
            }
        }
    }

    @Test
    fun `should be ready`() {
        testServer(environment = testEnv) {
            handleRequest(HttpMethod.Get, "/isready") { responseStatusCode ->
                assertEquals(HttpStatusCode.OK, responseStatusCode)
            }
        }
    }
}
