package no.nav.helse.spleis

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.ApplicationEngine
import io.prometheus.client.CollectorRegistry
import java.net.Socket
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.person.Person
import no.nav.helse.serde.serialize
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.config.DataSourceConfiguration
import no.nav.helse.spleis.config.KtorConfig
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
internal class RestApiTest {
    private companion object {
        private const val UNG_PERSON_FNR = "12029240045"
        private val UNG_PERSON_FØDSELSDATO = 12.februar(1992)
        private const val ORGNUMMER = "987654321"
        private val MELDINGSREFERANSE = UUID.randomUUID()
        private const val AKTØRID = "42"
    }
    private lateinit var dataSource: DataSource

    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private lateinit var jwtStub: JwtStub

    private lateinit var app: ApplicationEngine
    private lateinit var appBaseUrl: String
    private val teller = AtomicInteger()

    @BeforeAll
    internal fun `start embedded environment`() {
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

        app = createApp(
            KtorConfig(httpPort = randomPort),
            AzureAdAppConfig(
                clientId = "spleis_azure_ad_app_id",
                configurationUrl = "${wireMockServer.baseUrl()}/config"
            ),
            null,
            null,
            DataSourceConfiguration(
                jdbcUrl = DB.instance.jdbcUrl,
                databaseUsername = DB.instance.username,
                databasePassword = DB.instance.password
            ),
            teller
        )

        app.start(wait = false)
    }

    @AfterAll
    internal fun `stop embedded environment`() {
        CollectorRegistry.defaultRegistry.clear()
        app.stop(1000L, 1000L)
        wireMockServer.stop()
    }

    @BeforeEach
    internal fun setup() {
        DB.clean()
        dataSource = DB.migrate()

        val fom = LocalDate.of(2018, 9, 10)
        val tom = fom.plusDays(16)
        val sykeperioder = listOf(Sykmeldingsperiode(fom, tom))
        val sykmelding = Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR,
            aktørId = "aktørId",
            orgnummer = ORGNUMMER,
            sykeperioder = sykeperioder
        )
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(
                beløp = 12000.månedlig,
                opphørsdato = null
            ),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR,
            aktørId = "aktørId",
            førsteFraværsdag = LocalDate.of(2018, 1, 1),
            inntektsdato = null,
            beregnetInntekt = 12000.månedlig,
            arbeidsgiverperioder = listOf(Periode(LocalDate.of(2018, 9, 10), LocalDate.of(2018, 9, 10).plusDays(16))),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            harFlereInntektsmeldinger = false,
            avsendersystem = Inntektsmelding.Avsendersystem.NAV_NO,
            mottatt = LocalDateTime.now(),
            opprettet = LocalDateTime.now()
        )
        val person = Person(AKTØRID, UNG_PERSON_FNR.somPersonidentifikator(), UNG_PERSON_FØDSELSDATO.alder, MaskinellJurist())
        person.håndter(sykmelding)
        person.håndter(inntektsmelding)
        dataSource.lagrePerson(AKTØRID, UNG_PERSON_FNR, person)
        dataSource.lagreHendelse(MELDINGSREFERANSE)

        teller.set(0)
    }

    private fun DataSource.lagrePerson(aktørId: String, fødselsnummer: String, person: Person) {
        val serialisertPerson = person.serialize()
        sessionOf(this, returnGeneratedKey = true).use {
            val personId = it.run(queryOf("INSERT INTO person (fnr, aktor_id, skjema_versjon, data) VALUES (?, ?, ?, (to_json(?::json)))",
                fødselsnummer.toLong(), aktørId.toLong(), serialisertPerson.skjemaVersjon, serialisertPerson.json).asUpdateAndReturnGeneratedKey)
            it.run(queryOf("INSERT INTO person_alias (fnr, person_id) VALUES (?, ?);",
                fødselsnummer.toLong(), personId!!).asExecute)

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

    private fun createToken() = jwtStub.createTokenFor()

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
