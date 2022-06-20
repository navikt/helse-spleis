package no.nav.helse.spleis

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.server.engine.*
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.person.Person
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.serde.serialize
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.config.KtorConfig
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.awaitility.Awaitility.await
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.testcontainers.containers.PostgreSQLContainer
import java.net.Socket
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import no.nav.helse.spleis.config.DataSourceConfiguration

@TestInstance(Lifecycle.PER_CLASS)
internal class RestApiTest {
    private companion object {
        private const val UNG_PERSON_FNR = "12029240045"
        private const val ORGNUMMER = "987654321"
        private val MELDINGSREFERANSE = UUID.randomUUID()
        private const val AKTØRID = "42"
    }

    private val postgres = PostgreSQLContainer<Nothing>("postgres:14").apply {
        withReuse(true)
        withLabel("app-navn", "spleis")
    }
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway

    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private lateinit var jwtStub: JwtStub

    private lateinit var app: ApplicationEngine
    private lateinit var appBaseUrl: String
    private val teller = AtomicInteger()

    @BeforeAll
    internal fun `start embedded environment`() {
        postgres.start()

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
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 3
            minimumIdle = 1
            initializationFailTimeout = 5000
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
                jdbcUrl = postgres.jdbcUrl,
                databaseUsername = postgres.username,
                databasePassword = postgres.password
            ),
            teller
        )

        app.start(wait = false)
    }

    @AfterAll
    internal fun `stop embedded environment`() {
        app.stop(1000L, 1000L)
        wireMockServer.stop()
        postgres.stop()
    }

    @BeforeEach
    internal fun setup() {
        flyway.clean()
        flyway.migrate()

        val fom = LocalDate.of(2018, 9, 10)
        val tom = fom.plusDays(16)
        val sykeperioder = listOf(Sykmeldingsperiode(fom, tom, 100.prosent))
        val sykmelding = Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = "fnr",
            aktørId = "aktørId",
            orgnummer = ORGNUMMER,
            sykeperioder = sykeperioder,
            sykmeldingSkrevet = fom.atStartOfDay(),
            mottatt = tom.atStartOfDay()
        )
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(
                beløp = 12000.månedlig,
                opphørsdato = null
            ),
            orgnummer = ORGNUMMER,
            fødselsnummer = "fnr",
            aktørId = "aktørId",
            førsteFraværsdag = LocalDate.of(2018, 1, 1),
            beregnetInntekt = 12000.månedlig,
            arbeidsgiverperioder = listOf(Periode(LocalDate.of(2018, 9, 10), LocalDate.of(2018, 9, 10).plusDays(16))),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )
        val person = Person(AKTØRID, UNG_PERSON_FNR.somFødselsnummer(), MaskinellJurist())
        person.håndter(sykmelding)
        person.håndter(inntektsmelding)
        dataSource.lagrePerson(AKTØRID, UNG_PERSON_FNR, person)
        dataSource.lagreHendelse(MELDINGSREFERANSE)

        teller.set(0)
    }

    private fun DataSource.lagrePerson(aktørId: String, fødselsnummer: String, person: Person) {
        val serialisertPerson = person.serialize()
        sessionOf(this).use {
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
        fødselsnummer: String = UNG_PERSON_FNR,
        data: String = "{}"
    ) {
        sessionOf(this).use {
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
    fun sporingapi() {
        "/api/vedtaksperioder".httpGet(HttpStatusCode.OK, mapOf("fnr" to UNG_PERSON_FNR))
    }

    @Test
    fun `hent personJson med fnr`() {
        "/api/person-json".httpGet(HttpStatusCode.OK, mapOf("fnr" to UNG_PERSON_FNR))
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

    private fun createToken() = jwtStub.createTokenFor(
        subject = "en_saksbehandler_ident",
        groups = listOf("sykepenger-saksbehandler-gruppe"),
        audience = "spleis_azure_ad_app_id"
    )

    private fun String.httpGet(
        expectedStatus: HttpStatusCode = HttpStatusCode.OK,
        headers: Map<String, String> = emptyMap(),
        testBlock: String.() -> Unit = {}
    ) {
        val token = createToken()

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
