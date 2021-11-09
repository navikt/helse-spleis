package no.nav.helse.spleis

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.server.engine.*
import io.mockk.every
import io.mockk.mockkStatic
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.serialize
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.config.AzureAdAppConfig
import no.nav.helse.spleis.config.DataSourceConfiguration
import no.nav.helse.spleis.config.KtorConfig
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.testhelpers.TestObservatør
import no.nav.helse.spleis.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.spleis.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
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

@TestInstance(Lifecycle.PER_CLASS)
internal class GraphQLApiTest {
    private companion object {
        private const val UNG_PERSON_FNR = "12020052345"
        private const val ORGNUMMER = "987654321"
        private val MELDINGSREFERANSE = UUID.randomUUID()
        private const val AKTØRID = "42"
        val INNTEKT = 31000.00.månedlig
        val FOM = 10.september
        val TOM = 26.september
    }

    protected lateinit var person: Person
    protected lateinit var observatør: TestObservatør

    private val Int.vedtaksperiode: IdInnhenter get() = { orgnummer -> this.vedtaksperiode(orgnummer) }
    private fun Int.vedtaksperiode(orgnummer: String) = observatør.vedtaksperiode(orgnummer, this - 1)

    private val postgres = PostgreSQLContainer<Nothing>("postgres:13")
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway

    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private lateinit var jwtStub: JwtStub

    private lateinit var app: ApplicationEngine
    private lateinit var appBaseUrl: String
    private val teller = AtomicInteger()

    @BeforeAll
    internal fun `start embedded environment`() {
        mockkStatic("no.nav.helse.spleis.RequestResponseTracingKt")
        every { any<Application>().requestResponseTracing(any()) } returns Unit

        mockkStatic("no.nav.helse.spleis.NaisKt")
        every { any<Application>().nais(any()) } returns Unit

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

        val person = Person(AKTØRID, UNG_PERSON_FNR.somFødselsnummer())
        observatør = TestObservatør().also { person.addObserver(it) }

        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(ytelser())
        person.håndter(vilkårsgrunnlag())
        person.håndter(simulering())
//        person.håndter(utbetalingsgodkjenning())
//        person.håndter(
//            UtbetalingOverført(
//                meldingsreferanseId = UUID.randomUUID(),
//                aktørId = AKTØRID,
//                fødselsnummer = UNG_PERSON_FNR,
//                orgnummer = ORGNUMMER,
//                fagsystemId = "tilfeldig-string",
//                utbetalingId = UUID.randomUUID().toString(),
//                avstemmingsnøkkel = 123456L,
//                overføringstidspunkt = LocalDateTime.now()
//            )
//        )
//        person.håndter(utbetaling())

        dataSource.lagrePerson(AKTØRID, UNG_PERSON_FNR, person)
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
        fødselsnummer: String = UNG_PERSON_FNR,
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
    fun `tester person-resolver`() {
        Toggles.SpeilApiV2.enable()

        val query = """
            {
                person(fnr: ${UNG_PERSON_FNR.toLong()}) {
                    aktorId,
                    fodselsnummer,
                    arbeidsgivere {
                        organisasjonsnummer,
                        id,
                        generasjoner {
                            id,
                            perioder {
                                id,
                                fom,
                                tom,
                                behandlingstype,
                                periodetype,
                                inntektskilde,
                                erForkastet,
                                opprettet
                            }
                        }
                    },
                    dodsdato,
                    versjon
                }
            }
        """.trimIndent()

        "/graphql".httpPost(
            body = """
                {
                    "query": "$query"
                }
            """.trimIndent()
        ) {
            this
        }

        Toggles.SpeilApiV2.disable()
    }

    @Test
    fun `tester generasjon-resolver`() {
        Toggles.SpeilApiV2.enable()

        val query = """
            {
                generasjon(fnr: ${UNG_PERSON_FNR.toLong()}, orgnr: \"$ORGNUMMER\", indeks: 0) {
                    fom,
                    tom
                }
            }
        """.trimIndent()

        "/graphql".httpPost(
            body = """
                {
                    "query": "$query"
                }
            """.trimIndent()
        ) {
            this
        }

        Toggles.SpeilApiV2.disable()
    }

    private fun createToken() = jwtStub.createTokenFor(
        subject = "en_saksbehandler_ident",
        groups = listOf("sykepenger-saksbehandler-gruppe"),
        audience = "spleis_azure_ad_app_id"
    )

    private fun String.httpPost(
        expectedStatus: HttpStatusCode = HttpStatusCode.OK,
        headers: Map<String, String> = emptyMap(),
        body: String = "",
        testBlock: String.() -> Unit = {}
    ) {
        val token = createToken()

        val connection = appBaseUrl.handleRequest(HttpMethod.Get, this) {
            doOutput = true
            setRequestProperty(Authorization, "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (key, value) ->
                setRequestProperty(key, value)
            }

            val input = body.toByteArray(Charsets.UTF_8)
            outputStream.write(input, 0, input.size)
        }

        assertEquals(expectedStatus.value, connection.responseCode)
        connection.responseBody.testBlock()
    }

    protected fun sykmelding(
        id: UUID = UUID.randomUUID(),
        sykeperioder: List<Sykmeldingsperiode> = listOf(Sykmeldingsperiode(FOM, TOM, 100.prosent)),
        orgnummer: String = ORGNUMMER,
        sykmeldingSkrevet: LocalDateTime = FOM.atStartOfDay(),
        mottatt: LocalDateTime = TOM.plusDays(1).atStartOfDay(),
        fnr: String = UNG_PERSON_FNR,
    ): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = id,
            fnr = fnr,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            sykeperioder = sykeperioder,
            sykmeldingSkrevet = sykmeldingSkrevet,
            mottatt = mottatt
        )
    }

    private fun søknad(
        id: UUID = UUID.randomUUID(),
        vararg perioder: Søknad.Søknadsperiode = arrayOf(Søknad.Søknadsperiode.Sykdom(FOM, TOM, 100.prosent)),
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNav: LocalDate = TOM.plusDays(1),
        orgnummer: String = ORGNUMMER,
        sykmeldingSkrevet: LocalDateTime = FOM.atStartOfDay(),
        fnr: String = UNG_PERSON_FNR,
    ): Søknad {
        return Søknad(
            meldingsreferanseId = id,
            fnr = fnr,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            sendtTilNAV = sendtTilNav.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = sykmeldingSkrevet
        )
    }

    private fun inntektsmelding(
        id: UUID = UUID.randomUUID(),
        arbeidsgiverperioder: List<Periode> = listOf(Periode(FOM, TOM)),
        beregnetInntekt: Inntekt = INNTEKT,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
        orgnummer: String = ORGNUMMER,
        harOpphørAvNaturalytelser: Boolean = false,
        arbeidsforholdId: String? = null,
        fnr: String = UNG_PERSON_FNR,
    ): Inntektsmelding {
        return Inntektsmelding(
            meldingsreferanseId = id,
            refusjon = refusjon,
            orgnummer = orgnummer,
            fødselsnummer = fnr,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            arbeidsforholdId = arbeidsforholdId,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
            mottatt = LocalDateTime.now()
        )
    }

    private fun vilkårsgrunnlag(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        arbeidsforhold: List<Arbeidsforhold> = listOf(Arbeidsforhold(orgnummer, LocalDate.of(2018, 1, 1))),
        opptjening: Opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
        inntektsvurdering: Inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                Periode(FOM.minusYears(1), FOM.minusDays(1)) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ),
        inntektsvurderingForSykepengegrunnlag: InntektForSykepengegrunnlag = InntektForSykepengegrunnlag(
            inntekter = inntektperioderForSykepengegrunnlag {
                Periode(FOM.minusMonths(3), FOM.minusDays(1)) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ),
        fnr: String = UNG_PERSON_FNR
    ): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
            aktørId = AKTØRID,
            fødselsnummer = fnr.somFødselsnummer(),
            orgnummer = orgnummer,
            inntektsvurdering = inntektsvurdering,
            inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            opptjeningvurdering = opptjening,
            arbeidsforhold = arbeidsforhold
        )
    }

    private fun ytelser(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        utbetalinger: List<Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        orgnummer: String = ORGNUMMER,
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now(),
        fnr: String = UNG_PERSON_FNR
    ): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        val meldingsreferanseId = UUID.randomUUID()

        val utbetalingshistorikk =
            Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = AKTØRID,
                fødselsnummer = fnr,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
                arbeidskategorikoder = arbeidskategorikoder,
                harStatslønn = statslønn,
                perioder = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                ugyldigePerioder = emptyList(),
                besvart = besvart
            )
        return Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
            utbetalingshistorikk = utbetalingshistorikk,
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepenger,
                svangerskapsytelse = svangerskapspenger,
                aktivitetslogg = aktivitetslogg
            ),
            pleiepenger = Pleiepenger(
                perioder = pleiepenger,
                aktivitetslogg = aktivitetslogg
            ),
            omsorgspenger = Omsorgspenger(
                perioder = omsorgspenger,
                aktivitetslogg = aktivitetslogg
            ),
            opplæringspenger = Opplæringspenger(
                perioder = opplæringspenger,
                aktivitetslogg = aktivitetslogg
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = institusjonsoppholdsperioder,
                aktivitetslogg = aktivitetslogg
            ),
            dødsinfo = Dødsinfo(dødsdato),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
            dagpenger = Dagpenger(dagpenger),
            aktivitetslogg = aktivitetslogg
        )
    }

    private fun simulering(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        simuleringOK: Boolean = true,
        orgnummer: String = ORGNUMMER,
        fom: LocalDate = FOM,
        tom: LocalDate = TOM
    ) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR,
            orgnummer = orgnummer,
            simuleringOK = simuleringOK,
            melding = "",
            simuleringResultat = Simulering.SimuleringResultat(
                totalbeløp = 2000,
                perioder = listOf(
                    Simulering.SimulertPeriode(
                        periode = Periode(fom, tom),
                        utbetalinger = listOf(
                            Simulering.SimulertUtbetaling(
                                forfallsdato = tom.plusDays(1),
                                utbetalesTil = Simulering.Mottaker(
                                    id = orgnummer,
                                    navn = "Org Orgesen AS"
                                ),
                                feilkonto = false,
                                detaljer = listOf(
                                    Simulering.Detaljer(
                                        periode = Periode(fom, tom),
                                        konto = "81549300",
                                        beløp = 2000,
                                        klassekode = Simulering.Klassekode(
                                            kode = "SPREFAG-IOP",
                                            beskrivelse = "Sykepenger, Refusjon arbeidsgiver"
                                        ),
                                        uføregrad = 100,
                                        utbetalingstype = "YTEL",
                                        tilbakeføring = false,
                                        sats = Simulering.Sats(
                                            sats = 1000,
                                            antall = 2,
                                            type = "DAG"
                                        ),
                                        refunderesOrgnummer = orgnummer
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

    private fun utbetalingsgodkjenning(
        vedtaksperiodeIdInnhenter: IdInnhenter = 1.vedtaksperiode,
        utbetalingGodkjent: Boolean = true,
        orgnummer: String = ORGNUMMER,
        automatiskBehandling: Boolean = false,
//        utbetalingId: UUID = UUID.fromString(
//            inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning).kontekst()["utbetalingId"]
//                ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning).kontekst()}")
//        ),
        utbetalingId: UUID = UUID.randomUUID()
    ) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR,
        organisasjonsnummer = orgnummer,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeIdInnhenter(orgnummer).toString(),
        saksbehandler = "Ola Nordmann",
        saksbehandlerEpost = "ola.nordmann@nav.no",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling,
    )


    protected fun utbetaling(
        fagsystemId: String = "tilfeldig-string",
        status: UtbetalingHendelse.Oppdragstatus = AKSEPTERT,
        orgnummer: String = ORGNUMMER,
        meldingsreferanseId: UUID = UUID.randomUUID()
    ) =
        UtbetalingHendelse(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR,
            orgnummer = orgnummer,
            fagsystemId = fagsystemId,
            utbetalingId = UUID.randomUUID().toString(),
            status = status,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )


}


fun Int.januar(year: Int = 2018) = LocalDate.of(year, 1, this)
fun Int.februar(year: Int = 2018) = LocalDate.of(year, 2, this)
fun Int.mars(year: Int = 2018) = LocalDate.of(year, 3, this)
fun Int.april(year: Int = 2018) = LocalDate.of(year, 4, this)
fun Int.mai(year: Int = 2018) = LocalDate.of(year, 5, this)
fun Int.juni(year: Int = 2018) = LocalDate.of(year, 6, this)
fun Int.juli(year: Int = 2018) = LocalDate.of(year, 7, this)
fun Int.august(year: Int = 2018) = LocalDate.of(year, 8, this)
fun Int.september(year: Int = 2018) = LocalDate.of(year, 9, this)
fun Int.oktober(year: Int = 2018) = LocalDate.of(year, 10, this)
fun Int.november(year: Int = 2018) = LocalDate.of(year, 11, this)
fun Int.desember(year: Int = 2018) = LocalDate.of(year, 12, this)

val Int.januar get() = this.januar()
val Int.februar get() = this.februar()
val Int.mars get() = this.mars()
val Int.april get() = this.april()
val Int.mai get() = this.mai()
val Int.juni get() = this.juni()
val Int.juli get() = this.juli()
val Int.august get() = this.august()
val Int.september get() = this.september()
val Int.oktober get() = this.oktober()
val Int.november get() = this.november()
val Int.desember get() = this.desember()

internal typealias IdInnhenter = (orgnummer: String) -> UUID
