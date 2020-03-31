package no.nav.helse.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.HendelseMediator
import no.nav.helse.spleis.db.HendelseRecorder
import no.nav.helse.spleis.db.LagrePersonDao
import no.nav.helse.spleis.db.LagreUtbetalingDao
import no.nav.helse.spleis.db.PersonPostgresRepository
import no.nav.helse.testhelpers.januar
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.*
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class AbstractEndToEndMediatorTest {
    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private val INNTEKT = 31000.00.toBigDecimal()

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    protected val testRapid = TestRapid()
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var hendelseMediator: HendelseMediator

    @BeforeAll
    internal fun setupAll() {
        embeddedPostgres = EmbeddedPostgres.builder().start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection
        val hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
        dataSource = HikariDataSource(hikariConfig)

        hendelseMediator = HendelseMediator(
            rapidsConnection = testRapid,
            personRepository = PersonPostgresRepository(dataSource),
            lagrePersonDao = LagrePersonDao(dataSource),
            lagreUtbetalingDao = LagreUtbetalingDao(dataSource),
            hendelseRecorder = HendelseRecorder(dataSource)
        )
    }

    @AfterAll
    internal fun teardown() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @BeforeEach
    internal fun setupEach() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .load()
            .also {
                it.clean()
                it.migrate()
            }

        testRapid.reset()
    }

    private fun createHikariConfig(jdbcUrl: String) =
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

    protected fun sendNySøknad(vararg perioder: SoknadsperiodeDTO) {
        val nySøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.NY,
            id = UUID.randomUUID().toString(),
            sykmeldingId = UUID.randomUUID().toString(),
            aktorId = AKTØRID,
            fnr = UNG_PERSON_FNR_2018,
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = ORGNUMMER),
            fom = perioder.minBy { it.fom!! }?.fom,
            tom = perioder.maxBy { it.tom!! }?.tom,
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            startSyketilfelle = LocalDate.now(),
            sendtNav = null,
            egenmeldinger = emptyList(),
            fravar = emptyList(),
            soknadsperioder = perioder.toList(),
            opprettet = LocalDateTime.now()
        )
        testRapid.sendTestMessage(nySøknad.toJsonNode().apply {
            this as ObjectNode
            put("@id", UUID.randomUUID().toString())
            put("@event_name", "ny_søknad")
            put("@opprettet", LocalDateTime.now().toString())
        }.toString())
    }

    protected fun sendSøknad(vedtaksperiodeIndeks: Int, perioder: List<SoknadsperiodeDTO>, egenmeldinger: List<PeriodeDTO> = emptyList()) {
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Inntektsberegning))
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Opptjening))
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, EgenAnsatt))

        val sendtSøknad = SykepengesoknadDTO(
            status = SoknadsstatusDTO.SENDT,
            id = UUID.randomUUID().toString(),
            aktorId = AKTØRID,
            fnr = UNG_PERSON_FNR_2018,
            arbeidsgiver = ArbeidsgiverDTO(orgnummer = ORGNUMMER),
            fom = perioder.minBy { it.fom!! }?.fom,
            tom = perioder.maxBy { it.tom!! }?.tom,
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            startSyketilfelle = LocalDate.now(),
            sendtNav = perioder.maxBy { it.tom!! }?.tom?.atStartOfDay(),
            egenmeldinger = egenmeldinger,
            fravar = emptyList(),
            soknadsperioder = perioder.toList(),
            opprettet = LocalDateTime.now()
        )
        testRapid.sendTestMessage(sendtSøknad.toJsonNode().apply {
            this as ObjectNode
            put("@id", UUID.randomUUID().toString())
            put("@event_name", "sendt_søknad_nav")
            put("@opprettet", LocalDateTime.now().toString())
        }.toString())
    }

    protected fun sendInnteksmelding(
        vedtaksperiodeIndeks: Int,
        arbeidsgiverperiode: List<Periode>,
        førsteFraværsdag: LocalDate
    ) {
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Inntektsberegning))
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Opptjening))
        assertFalse(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, EgenAnsatt))

        val inntektsmelding = Inntektsmelding(
            inntektsmeldingId = UUID.randomUUID().toString(),
            arbeidstakerFnr = UNG_PERSON_FNR_2018,
            arbeidstakerAktorId = AKTØRID,
            virksomhetsnummer = ORGNUMMER,
            arbeidsgiverFnr = null,
            arbeidsgiverAktorId = null,
            arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
            arbeidsforholdId = null,
            beregnetInntekt = INNTEKT,
            refusjon = Refusjon(INNTEKT, LocalDate.now()),
            endringIRefusjoner = emptyList(),
            opphoerAvNaturalytelser = emptyList(),
            gjenopptakelseNaturalytelser = emptyList(),
            arbeidsgiverperioder = arbeidsgiverperiode,
            status = Status.GYLDIG,
            arkivreferanse = "",
            ferieperioder = emptyList(),
            foersteFravaersdag = førsteFraværsdag,
            mottattDato = LocalDateTime.now()
        )
        testRapid.sendTestMessage(inntektsmelding.toJsonNode().apply {
            this as ObjectNode
            put("@id", UUID.randomUUID().toString())
            put("@event_name", "inntektsmelding")
            put("@opprettet", LocalDateTime.now().toString())
        }.toString())
    }

    private fun sendGeneriskBehov(
        vedtaksperiodeIndeks: Int,
        behov: List<String> = listOf(),
        løsninger: Map<String, Any> = emptyMap(),
        ekstraFelter: Map<String, Any> = emptyMap(),
        tilstand: TilstandType = TilstandType.START
    ) = testRapid.sendTestMessage(
        objectMapper.writeValueAsString(
            ekstraFelter + mapOf(
                "@id" to UUID.randomUUID().toString(),
                "@opprettet" to LocalDateTime.now(),
                "@event_name" to "behov",
                "@behov" to behov,
                "tilstand" to tilstand.name,
                "aktørId" to AKTØRID,
                "fødselsnummer" to UNG_PERSON_FNR_2018,
                "organisasjonsnummer" to ORGNUMMER,
                "vedtaksperiodeId" to testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks),
                "@løsning" to løsninger,
                "@final" to true,
                "@besvart" to LocalDateTime.now()
            )
        )
    )

    protected fun sendNyPåminnelse() {
        objectMapper.writeValueAsString(
            mapOf(
                "@id" to UUID.randomUUID().toString(),
                "@opprettet" to LocalDateTime.now(),
                "@event_name" to "påminnelse",
                "aktørId" to AKTØRID,
                "fødselsnummer" to UNG_PERSON_FNR_2018,
                "organisasjonsnummer" to ORGNUMMER,
                "vedtaksperiodeId" to UUID.randomUUID().toString(),
                "tilstand" to TilstandType.START.name,
                "antallGangerPåminnet" to 0,
                "tilstandsendringstidspunkt" to LocalDateTime.now().toString(),
                "påminnelsestidspunkt" to LocalDateTime.now().toString(),
                "nestePåminnelsestidspunkt" to LocalDateTime.now().toString()
            )
        ).also { testRapid.sendTestMessage(it) }
    }

    protected fun sendManuellSaksbehandling(vedtaksperiodeIndeks: Int, godkjent: Boolean = true) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Godkjenning))
        sendGeneriskBehov(
            vedtaksperiodeIndeks = vedtaksperiodeIndeks,
            behov = listOf("Godkjenning"),
            ekstraFelter = mapOf(
                "saksbehandlerIdent" to "en_saksbehandler",
                "godkjenttidspunkt" to LocalDateTime.now()
            ),
            løsninger = mapOf(
                "Godkjenning" to mapOf(
                    "godkjent" to godkjent
                )
            ),
            tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Godkjenning)
        )
    }

    protected fun sendYtelserUtenHistorikk(vedtaksperiodeIndeks: Int) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Sykepengehistorikk))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Foreldrepenger))
        sendGeneriskBehov(
            vedtaksperiodeIndeks = vedtaksperiodeIndeks,
            behov = listOf("Sykepengehistorikk", "Foreldrepenger"),
            løsninger = mapOf(
                "Sykepengehistorikk" to emptyList<Any>(),
                "Foreldrepenger" to emptyMap<String, String>()
            ),
            tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Sykepengehistorikk)
        )
    }

    protected fun sendVilkårsgrunnlag(vedtaksperiodeIndeks: Int, egenAnsatt: Boolean = false, inntekter: List<Pair<YearMonth, Double>> = 1.rangeTo(12).map { YearMonth.of(2018, it) to INNTEKT.toDouble() }, opptjening: List<Triple<String, LocalDate, LocalDate?>> = listOf(Triple(ORGNUMMER, 1.januar(2010), null))) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Inntektsberegning))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, EgenAnsatt))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Opptjening))
        sendGeneriskBehov(
            vedtaksperiodeIndeks = vedtaksperiodeIndeks,
            behov = listOf("Inntektsberegning", "EgenAnsatt", "Opptjening"),
            tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Inntektsberegning),
            løsninger = mapOf(
                "EgenAnsatt" to egenAnsatt,
                "Inntektsberegning" to inntekter
                    .groupBy { it.first }
                    .map {
                        mapOf(
                            "årMåned" to it.key,
                            "inntektsliste" to it.value.map { mapOf("beløp" to it.second) }
                        )
                    },
                "Opptjening" to opptjening.map {
                    mapOf(
                        "orgnummer" to it.first,
                        "ansattSiden" to it.second,
                        "ansattTil" to it.third
                    )
                }
            )
        )
    }

    protected fun sendSimulering(vedtaksperiodeIndeks: Int, simuleringOK: Boolean = true) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Simulering))
        sendGeneriskBehov(
            vedtaksperiodeIndeks = vedtaksperiodeIndeks,
            behov = listOf("Simulering"),
            tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Simulering),
            løsninger = mapOf(
                "Simulering" to mapOf(
                    "status" to if (simuleringOK) "OK" else "FEIL",
                    "feilmelding" to if (simuleringOK) "" else "FEIL I SIMULERING",
                    "simulering" to if (!simuleringOK) null else mapOf(
                        "gjelderId" to UNG_PERSON_FNR_2018,
                        "gjelderNavn" to "Korona",
                        "datoBeregnet" to "2020-01-01",
                        "totalBelop" to 9999,
                        "periodeList" to listOf(
                            mapOf(
                                "fom" to "2020-01-01",
                                "tom" to "2020-01-02",
                                "utbetaling" to listOf(
                                    mapOf(
                                        "fagSystemId" to "1231203123123",
                                        "utbetalesTilId" to ORGNUMMER,
                                        "utbetalesTilNavn" to "Koronavirus",
                                        "forfall" to "2020-01-03",
                                        "feilkonto" to true,
                                        "detaljer" to listOf(
                                            mapOf(
                                                "faktiskFom" to "2020-01-01",
                                                "faktiskTom" to "2020-01-02",
                                                "konto" to "12345678910og1112",
                                                "belop" to 9999,
                                                "tilbakeforing" to false,
                                                "sats" to 1111,
                                                "typeSats" to "DAGLIG",
                                                "antallSats" to 9,
                                                "uforegrad" to 100,
                                                "klassekode" to "SPREFAG-IOP",
                                                "klassekodeBeskrivelse" to "Sykepenger, Refusjon arbeidsgiver",
                                                "utbetalingsType" to "YTELSE",
                                                "refunderesOrgNr" to ORGNUMMER
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    protected fun sendUtbetaling(vedtaksperiodeIndeks: Int, utbetalingOK: Boolean = true) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Utbetaling))
        sendGeneriskBehov(
            vedtaksperiodeIndeks = vedtaksperiodeIndeks,
            behov = listOf("Utbetaling"),
            tilstand = testRapid.inspektør.tilstandForEtterspurteBehov(vedtaksperiodeIndeks, Utbetaling),
            løsninger = mapOf(
                "Utbetaling" to mapOf(
                    "status" to if (utbetalingOK) "FERDIG" else "FEIL",
                    "melding" to if (!utbetalingOK) "FEIL fra Spenn" else ""
                )
            ),
            ekstraFelter = mapOf(
                "utbetalingsreferanse" to "123456789"
            )
        )
    }

    protected fun assertTilstander(vedtaksperiodeIndeks: Int, vararg tilstand: String) {
        assertEquals(tilstand.toList(), testRapid.inspektør.tilstander(testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks)))
    }

    protected class TestRapid() : RapidsConnection() {
        private val context = TestContext()
        private val messages = mutableListOf<Pair<String?, String>>()
        internal val inspektør get() = RapidInspektør(messages.toList())

        internal fun reset() {
            messages.clear()
        }

        fun sendTestMessage(message: String) {
            listeners.forEach { it.onMessage(message, context) }
        }

        override fun publish(message: String) {
            messages.add(null to message)
        }

        override fun publish(key: String, message: String) {
            messages.add(key to message)
        }

        override fun start() {}

        override fun stop() {}

        private inner class TestContext : MessageContext {
            override fun send(message: String) {
                publish(message)
            }

            override fun send(key: String, message: String) {
                publish(key, message)
            }
        }
    }

    protected class RapidInspektør(private val messages: List<Pair<String?, String>>) {
        private val jsonmeldinger = mutableMapOf<Int, JsonNode>()
        private val vedtaksperiodeIder get() = mutableSetOf<UUID>().apply {
            events("vedtaksperiode_endret") {
                this.add(UUID.fromString(it.path("vedtaksperiodeId").asText()))
            }
        }
        private val tilstander get() = mutableMapOf<UUID, MutableList<String>>().apply {
            events("vedtaksperiode_endret") {
                val id = UUID.fromString(it.path("vedtaksperiodeId").asText())
                this.getOrPut(id) { mutableListOf() }.add(it.path("gjeldendeTilstand").asText())
            }
        }
        private val behov get() = mutableMapOf<UUID, MutableList<Pair<Behovtype, TilstandType>>>().apply {
            events("behov") {
                val id = UUID.fromString(it.path("vedtaksperiodeId").asText())
                val tilstand = TilstandType.valueOf(it.path("tilstand").asText())
                this.getOrPut(id) { mutableListOf() }.apply {
                    it.path("@behov").onEach {
                        add(valueOf(it.asText()) to tilstand)
                    }
                }
            }
        }

        private fun events(name: String, onEach: (JsonNode) -> Unit) = messages.forEachIndexed { indeks, _ ->
            val message = melding(indeks)
            if (name == message.path("@event_name").asText()) onEach(message)
        }

        val vedtaksperiodeteller get() = vedtaksperiodeIder.size

        fun melding(indeks: Int) = jsonmeldinger.getOrPut(indeks) { objectMapper.readTree(messages[indeks].second) }
        fun antall() = messages.size

        fun vedtaksperiodeId(indeks: Int) = vedtaksperiodeIder.elementAt(indeks)
        fun tilstander(vedtaksperiodeId: UUID) = tilstander[vedtaksperiodeId]?.toList() ?: emptyList()
        fun etterspurteBehov(vedtaksperiodeIndeks: Int, behovtype: Behovtype) = behov[vedtaksperiodeId(vedtaksperiodeIndeks)]?.any { it.first == behovtype } ?: false
        fun tilstandForEtterspurteBehov(vedtaksperiodeIndeks: Int, behovtype: Behovtype) = behov.getValue(vedtaksperiodeId(vedtaksperiodeIndeks)).first { it.first == behovtype }.second
    }
}
