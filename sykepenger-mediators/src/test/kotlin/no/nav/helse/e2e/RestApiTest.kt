package no.nav.helse.e2e

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.helse.handleRequest
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.randomPort
import no.nav.helse.responseBody
import no.nav.helse.spleis.db.*
import no.nav.helse.spleis.rest.PersonRestInterface
import no.nav.helse.spleisApi
import org.awaitility.Awaitility.await
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.sql.Connection
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

@TestInstance(Lifecycle.PER_CLASS)
internal class RestApiTest {
    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private const val UTBETALINGSREF = "qwerty"
    }

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection

    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private lateinit var jwtStub: JwtStub

    private lateinit var app: ApplicationEngine
    private lateinit var appBaseUrl: String

    private fun applicationConfig(wiremockBaseUrl: String): Map<String, String> {
        return mapOf(
            "AZURE_CONFIG_URL" to "$wiremockBaseUrl/config",
            "AZURE_CLIENT_ID" to "spleis_azure_ad_app_id",
            "AZURE_CLIENT_SECRET" to "el_secreto",
            "AZURE_REQUIRED_GROUP" to "sykepenger-saksbehandler-gruppe"
        )
    }

    @BeforeAll
    internal fun `start embedded environment`() {
        embeddedPostgres = EmbeddedPostgres.builder().start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection

        //Stub ID provider (for authentication of REST endpoints)
        wireMockServer.start()
        jwtStub = JwtStub("Microsoft Azure AD", wireMockServer)
        stubFor(jwtStub.stubbedJwkProvider())
        stubFor(jwtStub.stubbedConfigProvider())

        val randomPort = randomPort()
        appBaseUrl = "http://localhost:$randomPort"

        val dataSource = HikariDataSource(HikariConfig().apply {
            this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        })

        Flyway
            .configure()
            .dataSource(dataSource)
            .load()
            .also {
                it.clean()
                it.migrate()
            }

        app = embeddedServer(Netty, applicationEngineEnvironment {
            connector { port = randomPort }

            module {
                val env = applicationConfig(wireMockServer.baseUrl())
                spleisApi(env, PersonRestInterface(PersonPostgresRepository(dataSource), UtbetalingsreferansePostgresRepository(dataSource)), HendelseRecorder(dataSource))
            }
        })

        app.start(wait = false)

        LagrePersonDao(dataSource)
            .lagrePerson(Person(AKTØRID, UNG_PERSON_FNR_2018), object : ArbeidstakerHendelse() {
                override fun aktørId() = AKTØRID
                override fun fødselsnummer() = UNG_PERSON_FNR_2018
                override fun organisasjonsnummer() = ORGNUMMER
            })

        LagreUtbetalingDao(dataSource)
            .lagreUtbetaling(UTBETALINGSREF, AKTØRID, ORGNUMMER, UUID.randomUUID())
    }

    @AfterAll
    internal fun `stop embedded environment`() {
        app.stop(1L, 1L, SECONDS)
        wireMockServer.stop()
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @Test
    fun `rest apis`() {
        await().atMost(5, SECONDS).untilAsserted { "/api/person/${AKTØRID}".httpGet(HttpStatusCode.OK) }
        await().atMost(5, SECONDS).untilAsserted { "/api/utbetaling/${UTBETALINGSREF}".httpGet(HttpStatusCode.OK) }
    }

    private fun String.httpGet(expectedStatus: HttpStatusCode = HttpStatusCode.OK, testBlock: String.() -> Unit = {}) {
        val token = jwtStub.createTokenFor(
            subject = "en_saksbehandler_ident",
            groups = listOf("sykepenger-saksbehandler-gruppe"),
            audience = "spleis_azure_ad_app_id"
        )

        val connection = appBaseUrl.handleRequest(HttpMethod.Get, this,
            builder = {
                setRequestProperty(Authorization, "Bearer $token")
            })

        assertEquals(expectedStatus.value, connection.responseCode)
        connection.responseBody.testBlock()
    }
}
