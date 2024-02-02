package no.nav.helse.spleis

import com.auth0.jwk.JwkProviderBuilder
import com.github.navikt.tbd_libs.test_support.TestDataSource
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
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
import io.ktor.server.engine.ApplicationEngine
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
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
import no.nav.helse.spleis.config.KtorConfig
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class RestApiTest {
    private companion object {
        private const val UNG_PERSON_FNR = "12029240045"
        private val UNG_PERSON_FØDSELSDATO = 12.februar(1992)
        private const val ORGNUMMER = "987654321"
        private val MELDINGSREFERANSE = UUID.randomUUID()
        private const val AKTØRID = "42"
    }

    @Test
    fun sporingapi() = blackboxTestApplication {
        "/api/vedtaksperioder".httpGet(HttpStatusCode.OK, mapOf("fnr" to UNG_PERSON_FNR))
    }

    @Test
    fun `hent personJson med fnr`() = blackboxTestApplication{
        "/api/person-json".httpGet(HttpStatusCode.OK, mapOf("fnr" to UNG_PERSON_FNR))
    }

    @Test
    fun `hent personJson med aktørId`() = blackboxTestApplication {
        "/api/person-json".httpGet(HttpStatusCode.OK, mapOf("aktorId" to AKTØRID))
    }

    @Test
    fun `finner ikke melding`() = blackboxTestApplication {
        "/api/hendelse-json/${UUID.randomUUID()}".httpGet(HttpStatusCode.NotFound)
    }

    @Test
    fun `finner melding`() = blackboxTestApplication {
        "/api/hendelse-json/${MELDINGSREFERANSE}".httpGet(HttpStatusCode.OK)
    }

    @Test
    fun `request med manglende eller feil access token`() = blackboxTestApplication {
            val query = """
            {
                person(fnr: \"${UNG_PERSON_FNR}\") { } 
            }
        """

            val body = """{"query": "$query"}"""

            val annenIssuer = Issuer("annen")

        post(body, HttpStatusCode.Unauthorized, accessToken = null)
        post(body, HttpStatusCode.Unauthorized, accessToken = azureTokenStub.createToken("feil_audience"))
        post(body, HttpStatusCode.Unauthorized, accessToken = annenIssuer.createToken())
        post(body, HttpStatusCode.OK, accessToken = azureTokenStub.createToken(Issuer.AUDIENCE))
    }

    private fun blackboxTestApplication(testblokk: suspend BlackboxTestContext.() -> Unit) {
        val randomPort = ServerSocket(0).use { it.localPort }

        val issuer = Issuer("Microsoft AD")
        val azureTokenStub = AzureTokenStub(issuer)

        val azureConfig = AzureAdAppConfig(
            clientId = Issuer.AUDIENCE,
            issuer = issuer.navn,
            jwkProvider = JwkProviderBuilder(azureTokenStub.wellKnownEndpoint().toURL()).build(),
        )
        val client = lagHttpklient(randomPort)

        val testDataSource = databaseContainer.nyTilkobling()
        runBlocking(context = Dispatchers.IO) {
            // starter opp ting, i parallell
            val databaseStartupJob = async { opprettTestdata(testDataSource) }
            val wiremockStartupJob = async { azureTokenStub.startServer() }
            val appStartupJob = async { opprettApplikasjonsserver(testDataSource, randomPort, azureConfig) }

            listOf(databaseStartupJob, wiremockStartupJob, appStartupJob).awaitAll()
            val app = appStartupJob.await()

            testblokk(BlackboxTestContext(client, azureTokenStub))

            // stopper serverne, i parallell
            listOf(
                async { azureTokenStub.stopServer() },
                async { app.stop() }
            ).awaitAll()
        }
        databaseContainer.droppTilkobling(testDataSource)
    }

    private suspend fun opprettApplikasjonsserver(testDataSource: TestDataSource, port: Int, azureConfig: AzureAdAppConfig) = suspendCoroutine<ApplicationEngine> {
        val app = createApp(
            KtorConfig(httpPort = port),
            azureConfig,
            null,
            null,
            { testDataSource.ds }
        )
        app.start(wait = false)
        it.resume(app)
    }

    private fun opprettTestdata(testDataSource: TestDataSource) {
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
            mottatt = LocalDateTime.now()
        )
        val person = Person(AKTØRID, UNG_PERSON_FNR.somPersonidentifikator(), UNG_PERSON_FØDSELSDATO.alder, MaskinellJurist())
        person.håndter(sykmelding)
        person.håndter(inntektsmelding)
        testDataSource.ds.lagrePerson(AKTØRID, UNG_PERSON_FNR, person)
        testDataSource.ds.lagreHendelse(MELDINGSREFERANSE)
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

    private class BlackboxTestContext(val client: HttpClient, val azureTokenStub: AzureTokenStub) {
        suspend fun post(body: String, forventetStatusCode: HttpStatusCode, accessToken: String?) =
            client.post("/graphql") {
                accessToken?.also { bearerAuth(accessToken) }
                setBody(body)
            }.also {
                assertEquals(forventetStatusCode, it.status)
            }

        fun String.httpGet(
            expectedStatus: HttpStatusCode = HttpStatusCode.OK,
            headers: Map<String, String> = emptyMap(),
            testBlock: String.() -> Unit = {}
        ) {
            val token = azureTokenStub.createToken(Issuer.AUDIENCE)

            runBlocking {
                client.get(this@httpGet) {
                    bearerAuth(token)
                    headers.forEach { (k, v) ->
                        header(k, v)
                    }
                }.also {
                    assertEquals(expectedStatus, it.status)
                }.bodyAsText()
            }.also(testBlock)
        }
    }

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

    private class AzureTokenStub(
        private val issuer: Issuer
    ) {
        private val randomPort = ServerSocket(0).use { it.localPort }
        private val wireMockServer: WireMockServer = WireMockServer(randomPort)
        private val jwksPath = "/discovery/v2.0/keys"

        fun wellKnownEndpoint() = URI("http://localhost:$randomPort$jwksPath")

        fun createToken(audience: String) =
            issuer.createToken(audience)

        suspend fun startServer(): Boolean {
            return suspendCoroutine { continuation ->
                //Stub ID provider (for authentication of REST endpoints)
                wireMockServer.start()
                ventPåServeroppstart()
                wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(jwksPath)).willReturn(WireMock.okJson(issuer.jwks)))
                continuation.resume(true) // returnerer true bare for å ha en verdi
            }
        }

        suspend fun stopServer() = suspendCoroutine {
            wireMockServer.stop()
            it.resume(true) // returnerer true bare for å ha en verdi
        }

        private fun ventPåServeroppstart() = retry {
            try {
                Socket("localhost", wireMockServer.port()).use { it.isConnected }
            } catch (err: Exception) {
                false
            }
        }
    }
}
