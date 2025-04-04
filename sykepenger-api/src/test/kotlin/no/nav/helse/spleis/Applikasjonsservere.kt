package no.nav.helse.spleis

import com.auth0.jwk.JwkProviderBuilder
import com.github.navikt.tbd_libs.signed_jwt_issuer_test.Issuer
import com.github.navikt.tbd_libs.speed.SpeedClient
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
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import java.net.ServerSocket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helse.spleis.config.AzureAdAppConfig
import org.junit.jupiter.api.Assertions

private class SuspendableIssuer {
    val issuer = Issuer("Microsoft AD", "spleis_azure_ad_app_id")
    suspend fun start(): Boolean {
        return suspendCoroutine { continuation ->
            issuer.start()
            retry {
                if (issuer.startet()) true
                else error("Issuer ${issuer.navn} ble aldri klar!!")
            }
            continuation.resume(true) // returnerer true bare for å ha en verdi
        }
    }

    suspend fun stop() = suspendCoroutine {
        issuer.stop()
        it.resume(true) // returnerer true bare for å ha en verdi
    }
}

internal class Applikasjonsservere(private val poolSize: Int) {
    constructor() : this(POOL_SIZE)

    companion object {
        private val JUNIT_PARALLELISM = System.getProperty("junit.jupiter.execution.parallel.config.fixed.parallelism")?.toInt() ?: 1

        private const val MIN_POOL_SIZE = 1
        private val MAX_POOL_SIZE = Runtime.getRuntime().availableProcessors()
        private val POOL_SIZE = minOf(MAX_POOL_SIZE, maxOf(MIN_POOL_SIZE, JUNIT_PARALLELISM))
    }

    private val suspendableIssuer = SuspendableIssuer()
    private val azureConfig = AzureAdAppConfig(
        clientId = "spleis_azure_ad_app_id",
        issuer = suspendableIssuer.issuer.navn,
        jwkProvider = JwkProviderBuilder(suspendableIssuer.issuer.jwksUri().toURL()).build(),
    )
    private val tilgjengelige by lazy {
        ArrayBlockingQueue(poolSize, false, opprettApplikasjonsserver())
    }

    init {
        runBlocking(Dispatchers.IO) {
            suspendableIssuer.start()
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
                .plusElement(async { suspendableIssuer.stop() })
                .awaitAll()

        }
    }

    private fun opprettApplikasjonsserver() = (1..poolSize).map {
        val navn = "appserver_$it"
        println("oppretter appserver $navn")
        Applikasjonserver(azureConfig, suspendableIssuer.issuer)
    }

    internal class Applikasjonserver(azureConfig: AzureAdAppConfig, issuer: Issuer) {
        private val randomPort = ServerSocket(0).use { it.localPort }
        private lateinit var testDataSource: TestDataSource
        private val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        private val speedClient = mockk<SpeedClient>()
        private val spekematClient = mockk<SpekematClient>()
        private val app =
            createApp(azureConfig, speedClient, spekematClient, { testDataSource.ds }, registry, randomPort)
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
            val token = issuer.accessToken()

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

        fun String.httpPost(
            expectedStatus: HttpStatusCode = HttpStatusCode.OK,
            postBody: Map<String, String> = emptyMap(),
            testBlock: String.() -> Unit = {}
        ) {
            val token = issuer.accessToken()

            runBlocking {
                client.post(this@httpPost) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(postBody)
                }.also {
                    Assertions.assertEquals(expectedStatus, it.status)
                }.bodyAsText()
            }.also(testBlock)
        }
    }
}
