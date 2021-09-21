package no.nav.helse.spleis

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.server.engine.*
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.Person
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.config.DataSourceConfiguration
import no.nav.helse.spleis.config.KtorConfig
import no.nav.helse.spleis.dao.HendelseDao
import org.awaitility.Awaitility.await
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.io.TempDir
import java.net.Socket
import java.nio.file.Path
import java.sql.Connection
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

@TestInstance(Lifecycle.PER_CLASS)
internal class RestApiTest {
    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private val MELDINGSREFERANSE = UUID.randomUUID()
        private const val AKTØRID = "42"
    }

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway

    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private lateinit var jwtStub: JwtStub

    private lateinit var app: ApplicationEngine
    private lateinit var appBaseUrl: String
    private val teller = AtomicInteger()
    private val spesialistClientId = UUID.randomUUID().toString()

    @BeforeAll
    internal fun `start embedded environment`(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection

        //Stub ID provider (for authentication of REST endpoints)
        wireMockServer.start()
        await("vent på WireMockServer har startet")
            .atMost(5, SECONDS)
            .until {
                try {
                    Socket("localhost", wireMockServer.port()).use { it.isConnected }
                } catch (err: Exception) {
                    false
                }
            }
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
                configurationUrl = "${wireMockServer.baseUrl()}/config"
            ),
            DataSourceConfiguration(
                jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            ),
            teller
        )

        app.start(wait = false)
    }

    @AfterAll
    internal fun `stop embedded environment`() {
        app.stop(1000L, 1000L)
        wireMockServer.stop()
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @BeforeEach
    internal fun setup() {
        flyway.clean()
        flyway.migrate()

        dataSource.lagrePerson(AKTØRID, UNG_PERSON_FNR_2018, Person(AKTØRID, UNG_PERSON_FNR_2018))
        dataSource.lagreHendelse(MELDINGSREFERANSE)

        teller.set(0)
    }

    private fun DataSource.lagrePerson(aktørId: String, fødselsnummer: String, person: Person) {
        val serialisertPerson = person.serialize()
        using(sessionOf(this)) {
            it.run(
                queryOf(
                    "INSERT INTO person (aktor_id, fnr, skjema_versjon, data) VALUES (?, ?, ?, (to_json(?::json)))",
                    aktørId.toLong(), fødselsnummer.toLong(), serialisertPerson.skjemaVersjon, serialisertPerson.json
                ).asExecute
            )
        }
    }


    private fun DataSource.lagreHendelse(
        meldingsReferanse: UUID,
        meldingstype: HendelseDao.Meldingstype = HendelseDao.Meldingstype.INNTEKTSMELDING,
        fødselsnummer: String = UNG_PERSON_FNR_2018,
        data: String = "{}"
    ) {
        using(sessionOf(this)) {
            it.run(
                queryOf(
                    "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (?, ?, ?, (to_json(?::json)))",
                    fødselsnummer.toLong(),
                    meldingsReferanse.toString(),
                    meldingstype.toString(),
                    data
                ).asExecute
            )
        }
    }

    @Test
    fun `hent person`() {
         "/api/person-snapshot".httpGet(HttpStatusCode.OK, mapOf("fnr" to UNG_PERSON_FNR_2018))
    }

    @Test
    fun `hent personJson med fnr`() {
         "/api/person-json".httpGet(HttpStatusCode.OK, mapOf("fnr" to UNG_PERSON_FNR_2018))
    }

    @Test
    fun `hent personJson med aktørId`() {
         "/api/person-json".httpGet(HttpStatusCode.OK, mapOf("aktorId" to AKTØRID))
    }


    @Test
    fun `finner ikke melding`() {
         "/api/hendelse-json/${UUID.randomUUID()}".httpGet(HttpStatusCode.NotFound)
    }

    @Test
    fun `finner melding`() {
        "/api/hendelse-json/${MELDINGSREFERANSE}".httpGet(HttpStatusCode.OK)
    }

    @Disabled("Tester bruk av preStopHook")
    @Test
    fun preStop() {
        teller.set(3)
        thread {
            Thread.sleep(900)
            do {
                Thread.sleep(100)
            } while (teller.decrementAndGet() != 0)
        }
        val handleRequest = appBaseUrl.handleRequest(HttpMethod.Get, "/stop")
        val ms = measureTimeMillis { handleRequest.responseCode }
        assertTrue(ms >= 1400)
    }

    private fun String.httpGet(
        expectedStatus: HttpStatusCode = HttpStatusCode.OK,
        headers: Map<String, String> = emptyMap(),
        testBlock: String.() -> Unit = {}
    ) {
        val token = jwtStub.createTokenFor(
            subject = "en_saksbehandler_ident",
            groups = listOf("sykepenger-saksbehandler-gruppe"),
            audience = "spleis_azure_ad_app_id"
        )

        val connection = appBaseUrl.handleRequest(HttpMethod.Get, this,
            builder = {
                setRequestProperty(Authorization, "Bearer $token")
                headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            })

        assertEquals(expectedStatus.value, connection.responseCode)
        connection.responseBody.testBlock()
    }
}
