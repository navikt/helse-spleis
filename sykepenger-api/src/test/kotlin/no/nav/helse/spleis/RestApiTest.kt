package no.nav.helse.spleis

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
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.Person
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.config.DataSourceConfiguration
import no.nav.helse.spleis.config.KtorConfig
import org.awaitility.Awaitility.await
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.sql.Connection
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import javax.sql.DataSource

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
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway

    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private lateinit var jwtStub: JwtStub

    private lateinit var app: ApplicationEngine
    private lateinit var appBaseUrl: String

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

        dataSource = HikariDataSource(HikariConfig().apply {
            this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        })

        flyway = Flyway
            .configure()
            .dataSource(dataSource)
            .load()

        app = createApp(
            KtorConfig(httpPort = randomPort),
            AzureAdAppConfig(
                clientId = "spleis_azure_ad_app_id",
                configurationUrl = "${wireMockServer.baseUrl()}/config",
                requiredGroup = "sykepenger-saksbehandler-gruppe"
            ),
            DataSourceConfiguration(
                jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            )
        )

        app.start(wait = false)
    }

    @AfterAll
    internal fun `stop embedded environment`() {
        app.stop(1L, 1L, SECONDS)
        wireMockServer.stop()
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @BeforeEach
    internal fun setup() {
        flyway.clean()
        flyway.migrate()

        dataSource.lagrePerson(AKTØRID, UNG_PERSON_FNR_2018, Person(AKTØRID, UNG_PERSON_FNR_2018))
        dataSource.lagreUtbetaling(AKTØRID, ORGNUMMER, UTBETALINGSREF, UUID.randomUUID())
    }

    private fun DataSource.lagrePerson(aktørId: String, fødselsnummer: String, person: Person) {
        val serialisertPerson = person.serialize()
        using(sessionOf(this)) {
            it.run(queryOf("INSERT INTO person (aktor_id, fnr, skjema_versjon, data) VALUES (?, ?, ?, (to_json(?::json)))",
                aktørId, fødselsnummer, serialisertPerson.skjemaVersjon, serialisertPerson.json).asExecute)
        }
    }

    private fun DataSource.lagreUtbetaling(aktørId: String, organisasjonsnummer: String, utbetalingsreferanse: String, vedtaksperiodeId: UUID) {
        using(sessionOf(this)) {
            it.run(queryOf("INSERT INTO utbetalingsreferanse (id, aktor_id, orgnr, vedtaksperiode_id) VALUES (?, ?, ?, ?)",
                utbetalingsreferanse, aktørId, organisasjonsnummer, vedtaksperiodeId.toString()).asExecute)
        }
    }

    @Test
    fun `hent person`() {
        await().atMost(5, SECONDS).untilAsserted { "/api/person/$AKTØRID".httpGet(HttpStatusCode.OK) }
    }

    @Test
    fun `hent utbetaling`() {
        await().atMost(5, SECONDS).untilAsserted { "/api/utbetaling/$UTBETALINGSREF".httpGet(HttpStatusCode.OK) }
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
