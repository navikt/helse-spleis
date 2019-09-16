package no.nav.helse.nais

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.createLivenessCheck
import no.nav.helse.randomPort
import org.apache.kafka.streams.KafkaStreams
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NaisRoutesTest {
    private val streams: KafkaStreams = mockk()
    private val port: Int = randomPort()
    private val client = HttpClient()

    private lateinit var application: ApplicationEngine
    @BeforeAll
    fun setup() {
        application = embeddedServer(Netty, port) {
            nais(isAliveCheck = createLivenessCheck(streams))
        }

        application.start(wait = false)
    }

    @BeforeEach
    fun before() {
        every { streams.state() } returns KafkaStreams.State.RUNNING
    }

    @AfterAll
    fun tearDown() {
        application.stop(10, 10, TimeUnit.SECONDS)
    }

    @Test
    fun `gir 5xx error ved feilende liveness sjekk`() {
        every { streams.state() } returns KafkaStreams.State.NOT_RUNNING
        val response = runBlocking { client.get<HttpResponse>("http://localhost:$port/isalive") }
        Assertions.assertTrue(response.status.value in 500 until 600, "GET on /isalive should result in server error on faulty liveness check")
    }

    @Test
    fun `gir ok status ved ok liveness sjekk`() {
        val response = runBlocking { client.get<HttpResponse>("http://localhost:$port/isalive") }
        Assertions.assertTrue(response.status.value in 200 until 300, "GET on /isalive should result in successful status on ok liveness check")
    }
}