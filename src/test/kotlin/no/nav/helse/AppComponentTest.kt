package no.nav.helse

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppComponentTest {

    @Test
    fun `should respond with metrics`() {
        testServer {
            handleRequest(HttpMethod.Get, "/metrics") { responseStatusCode ->
                assertEquals(HttpStatusCode.OK, responseStatusCode)
                assertTrue(responseBody.contains("process_cpu_seconds_total"))
            }
        }
    }

    @Test
    fun `should be alive`() {
        testServer {
            handleRequest(HttpMethod.Get, "/isalive") { responseStatusCode ->
                assertEquals(HttpStatusCode.OK, responseStatusCode)
            }
        }
    }

    @Test
    fun `should be ready`() {
        testServer {
            handleRequest(HttpMethod.Get, "/isready") { responseStatusCode ->
                assertEquals(HttpStatusCode.OK, responseStatusCode)
            }
        }
    }
}
