package no.nav.helse.spleis

import com.auth0.jwk.JwkProviderBuilder
import com.github.navikt.tbd_libs.test_support.TestDataSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import java.net.ServerSocket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.config.KtorConfig
import org.junit.jupiter.api.Assertions

internal class Applikasjonsservere(private val poolSize: Int) {
    constructor() : this(POOL_SIZE)
    companion object {
        private val JUNIT_PARALLELISM = System.getProperty("junit.jupiter.execution.parallel.config.fixed.parallelism")?.toInt() ?: 1

        private const val MIN_POOL_SIZE = 1
        private val MAX_POOL_SIZE = Runtime.getRuntime().availableProcessors()
        private val POOL_SIZE = minOf(MAX_POOL_SIZE, maxOf(MIN_POOL_SIZE, JUNIT_PARALLELISM))
    }

    private val issuer = Issuer("Microsoft AD")
    private val azureTokenStub = AzureTokenStub(issuer)
    private val azureConfig = AzureAdAppConfig(
        clientId = Issuer.AUDIENCE,
        issuer = issuer.navn,
        jwkProvider = JwkProviderBuilder(azureTokenStub.wellKnownEndpoint().toURL()).build(),
    )
    private val tilgjengelige by lazy {
        ArrayBlockingQueue(poolSize, false, opprettApplikasjonsserver())
    }

    init {
        runBlocking(Dispatchers.IO) {
            azureTokenStub.startServer()
        }
    }

    fun nyAppserver(): Applikasjonserver {
        return tilgjengelige.poll(20, TimeUnit.SECONDS) ?: throw RuntimeException("Ventet i 20 sekunder uten å få en ledig appserver")
    }

    fun kjørTest(testdata: (TestDataSource) -> Unit, testblokk: suspend BlackboxTestContext.() -> Unit) {
        val appserver = nyAppserver()
        try {
            appserver.kjørTest(testdata, testblokk)
        } finally {
            returner(appserver)
        }
    }

    fun returner(appserver: Applikasjonserver) {
        check(tilgjengelige.offer(appserver)) {
            "Kunne ikke returnere appserveren"
        }
    }

    fun ryddOpp() {
        runBlocking(Dispatchers.IO) {
            tilgjengelige
                .map { async { it.stopp() } }
                .plusElement(async { azureTokenStub.stopServer() })
                .awaitAll()

        }
    }

    private fun opprettApplikasjonsserver() = (1..poolSize).map {
        val navn = "appserver_$it"
        println("oppretter appserver $navn")
        Applikasjonserver(navn, azureConfig, issuer)
    }

    internal class Applikasjonserver(private val navn: String, azureConfig: AzureAdAppConfig, issuer: Issuer) {
        private val randomPort = ServerSocket(0).use { it.localPort }
        private lateinit var testDataSource: TestDataSource
        private val registry = CollectorRegistry()
        private val spekematClient = mockk<SpekematClient>()
        private val app =
            createApp(KtorConfig(httpPort = randomPort), azureConfig, spekematClient, null, { testDataSource.ds }, registry)
        private val client = lagHttpklient(randomPort)
        private val testContext = BlackboxTestContext(client, issuer)

        private var startetOpp = false

        fun kjørTest(testdata: (TestDataSource) -> Unit = {}, testblokk: suspend BlackboxTestContext.() -> Unit) {
            testDataSource = databaseContainer.nyTilkobling()
            try {
                startOpp(testdata)
                runBlocking {
                    testblokk(testContext)
                }
            } finally {
                databaseContainer.droppTilkobling(testDataSource)
            }
        }

        private fun startOpp(testdata: (TestDataSource) -> Unit) {
            runBlocking(context = Dispatchers.IO) {
                // starter opp ting, i parallell
                listOf(
                    launch { testdata(testDataSource) },
                    launch { if (!startetOpp) app.start(wait = false) }
                ).joinAll()
            }
            startetOpp = true
        }

        fun stopp() {
            app.stop()
        }

        private companion object {

            private fun lagHttpklient(port: Int) =
                HttpClient {
                    defaultRequest {
                        host = "localhost"
                        this.port = port
                    }
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                }
        }
    }

    internal class BlackboxTestContext(val client: HttpClient, val issuer: Issuer) {
        suspend fun post(body: String, forventetStatusCode: HttpStatusCode, accessToken: String?) =
            client.post("/graphql") {
                accessToken?.also { bearerAuth(accessToken) }
                setBody(body)
            }.also {
                Assertions.assertEquals(forventetStatusCode, it.status)
            }

        fun String.httpGet(
            expectedStatus: HttpStatusCode = HttpStatusCode.OK,
            headers: Map<String, String> = emptyMap(),
            testBlock: String.() -> Unit = {}
        ) {
            val token = issuer.createToken(Issuer.AUDIENCE)

            runBlocking {
                client.get(this@httpGet) {
                    bearerAuth(token)
                    headers.forEach { (k, v) ->
                        header(k, v)
                    }
                }.also {
                    Assertions.assertEquals(expectedStatus, it.status)
                }.bodyAsText()
            }.also(testBlock)
        }
    }
}