package no.nav.helse.spleis.testhelpers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Toggle
import no.nav.helse.person.Person
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.*
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.config.DataSourceConfiguration
import no.nav.helse.spleis.config.KtorConfig
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.økonomi.Inntekt
import org.awaitility.Awaitility
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.testcontainers.containers.PostgreSQLContainer
import java.net.Socket
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

internal class ApiTestServer(private val port: Int = randomPort()) {
    private val postgres = PostgreSQLContainer<Nothing>("postgres:13")
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway

    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private lateinit var jwtStub: JwtStub

    private lateinit var app: ApplicationEngine
    private lateinit var appBaseUrl: String
    private val teller = AtomicInteger()

    internal fun clean() {
        flyway.clean()
        flyway.migrate()
        teller.set(0)
    }

    internal fun tearDown() {
        app.stop(1000L, 1000L)
        wireMockServer.stop()
        postgres.stop()
    }

    internal fun start() {
        mockkStatic("no.nav.helse.spleis.RequestResponseTracingKt")
        every { any<Application>().requestResponseTracing(any()) } returns Unit

        mockkStatic("no.nav.helse.spleis.NaisKt")
        every { any<Application>().nais(any()) } returns Unit

        postgres.start()

        //Stub ID provider (for authentication of REST endpoints)
        wireMockServer.start()
        Awaitility.await("vent på WireMockServer har startet")
            .atMost(5, TimeUnit.SECONDS)
            .until {
                try {
                    Socket("localhost", wireMockServer.port()).use { it.isConnected }
                } catch (err: Exception) {
                    false
                }
            }
        jwtStub = JwtStub("Microsoft Azure AD", wireMockServer)
        WireMock.stubFor(jwtStub.stubbedJwkProvider())
        WireMock.stubFor(jwtStub.stubbedConfigProvider())

        appBaseUrl = "http://localhost:$port"

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
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
            KtorConfig(httpPort = port),
            AzureAdAppConfig(
                clientId = "spleis_azure_ad_app_id",
                configurationUrl = "${wireMockServer.baseUrl()}/config"
            ),
            DataSourceConfiguration(
                jdbcUrl = postgres.jdbcUrl,
                databaseUsername = postgres.username,
                databasePassword = postgres.password
            ),
            teller
        )

        app.start(wait = false)

        // Her vil togglen ha samme state som i testen som kjøres siden ApiTestServer#start blir kalt fra samme JUnit tråd som testen blir kjørt fra
        // ThreadLocal#asContextElement passer på at coroutines restorer staten til ThreadLocal uansett hvilke worker-thread som kjører koden i samme context
        val speilContext = Toggle.SpeilApiV2.threadLocal().asContextElement()
        app.application.intercept(ApplicationCallPipeline.Features) {
            // Om vi hadde prøvd å hente Toggle herifra ville vi vært i en av Ktors worker threads, for disse ville Togglen ikke ha samme state som JUnit tråden
            withContext(speilContext) { proceed() }
        }
    }

    private fun createToken() = jwtStub.createTokenFor(
        subject = "en_saksbehandler_ident",
        groups = listOf("sykepenger-saksbehandler-gruppe"),
        audience = "spleis_azure_ad_app_id"
    )

    internal fun httpPost(
        path: String,
        expectedStatus: HttpStatusCode = HttpStatusCode.OK,
        headers: Map<String, String> = emptyMap(),
        body: String = "",
        testBlock: String.() -> Unit = {}
    ) {
        val token = createToken()
        val connection = appBaseUrl.handleRequest(HttpMethod.Get, path) {
            doOutput = true
            setRequestProperty(HttpHeaders.Authorization, "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (key, value) ->
                setRequestProperty(key, value)
            }
            val input = body.toByteArray(Charsets.UTF_8)
            outputStream.write(input, 0, input.size)
        }
        Assertions.assertEquals(expectedStatus.value, connection.responseCode)
        testBlock(connection.responseBody)
    }

    internal fun lagrePerson(aktørId: String, fødselsnummer: String, person: Person) {
        val serialisertPerson = person.serialize()
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO person (aktor_id, fnr, skjema_versjon, data) VALUES (?, ?, ?, (to_json(?::json)))",
                    aktørId.toLong(), fødselsnummer.toLong(), serialisertPerson.skjemaVersjon, serialisertPerson.json
                ).asExecute
            )
        }
    }

    private fun lagreHendelse(
        fødselsnummer: String,
        meldingsReferanse: UUID,
        meldingstype: HendelseDao.Meldingstype = HendelseDao.Meldingstype.INNTEKTSMELDING,
        data: String = "{}"
    ) {
        using(sessionOf(dataSource)) {
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

    internal fun lagreInntektsmelding(fødselsnummer: String, meldingsReferanse: UUID, beregnetInntekt: Inntekt, førsteFraværsdag: LocalDate) {
        lagreHendelse(
            fødselsnummer = fødselsnummer,
            meldingsReferanse = meldingsReferanse,
            meldingstype = HendelseDao.Meldingstype.INNTEKTSMELDING,
            data = """
                {
                    "beregnetInntekt": "$beregnetInntekt",
                    "mottattDato": "${LocalDateTime.now()}",
                    "@opprettet": "${LocalDateTime.now()}",
                    "foersteFravaersdag": "$førsteFraværsdag",
                    "@id": "$meldingsReferanse"
                }
            """.trimIndent()
        )
    }

    internal fun lagreSykmelding(fødselsnummer: String, meldingsReferanse: UUID, fom: LocalDate, tom: LocalDate) {
        lagreHendelse(
            fødselsnummer = fødselsnummer,
            meldingsReferanse = meldingsReferanse,
            meldingstype = HendelseDao.Meldingstype.NY_SØKNAD,
            data = """
                {
                    "@opprettet": "${LocalDateTime.now()}",
                    "@id": "$meldingsReferanse",
                    "fom": "$fom",
                    "tom": "$tom"
                }
            """.trimIndent()
        )
    }

    internal fun lagreSøknadNav(fødselsnummer: String, meldingsReferanse: UUID, fom: LocalDate, tom: LocalDate, sendtNav: LocalDateTime) {
        lagreHendelse(
            fødselsnummer = fødselsnummer,
            meldingsReferanse = meldingsReferanse,
            meldingstype = HendelseDao.Meldingstype.SENDT_SØKNAD_NAV,
            data = """
                {
                    "@opprettet": "${LocalDateTime.now()}",
                    "@id": "$meldingsReferanse",
                    "fom": "$fom",
                    "tom": "$tom",
                    "sendtNav": "$sendtNav"
                }
            """.trimIndent()
        )
    }

}
