package no.nav.helse.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.HelseBuilder
import no.nav.helse.testhelpers.januar
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.*
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KunEnArbeidsgiverTest {
    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private val INNTEKT = 31000.00.toBigDecimal()

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val testRapid = TestRapid()
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var helseBuilder: HelseBuilder

    @Test
    fun `påminnelse for vedtaksperiode som ikke finnes`() {
        sendNyPåminnelse()
        assertEquals(1, testRapid.inspektør.antall())
        assertEquals("vedtaksperiode_ikke_funnet", testRapid.inspektør.melding(0).path("@event_name").asText())
    }

    @Test
    fun `ingen historie med Søknad først`() {
        sendNySøknad(0, SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendInnteksmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenHistorikk(0)
        sendManuellSaksbehandling(0)
        sendUtbetaling(0)

        assertTilstander(0, "MOTTATT_SYKMELDING_FERDIG_GAP", "AVVENTER_GAP", "AVVENTER_VILKÅRSPRØVING_GAP", "AVVENTER_HISTORIKK", "AVVENTER_GODKJENNING", "TIL_UTBETALING", "AVSLUTTET")
    }

    @BeforeAll
    internal fun setupAll() {
        embeddedPostgres = EmbeddedPostgres.builder().start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection
        val hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
        dataSource = HikariDataSource(hikariConfig)
        helseBuilder = HelseBuilder(dataSource, testRapid)
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

    private fun sendNySøknad(vedtaksperiodeIndeks: Int, vararg perioder: SoknadsperiodeDTO) {
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
        testRapid.sendTestMessage(nySøknad.toJsonNode().toString())
    }

    private fun sendSøknad(vedtaksperiodeIndeks: Int, vararg perioder: SoknadsperiodeDTO) {
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
            sendtNav = LocalDateTime.now(),
            egenmeldinger = emptyList(),
            fravar = emptyList(),
            soknadsperioder = perioder.toList(),
            opprettet = LocalDateTime.now()
        )
        testRapid.sendTestMessage(sendtSøknad.toJsonNode().toString())
    }

    private fun sendInnteksmelding(
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
        testRapid.sendTestMessage(inntektsmelding.toJsonNode().toString())
    }

    private fun sendGeneriskBehov(
        vedtaksperiodeIndeks: Int,
        behov: List<String> = listOf(),
        løsninger: Map<String, Any> = emptyMap(),
        ekstraFelter: Map<String, Any> = emptyMap()
    ) = testRapid.sendTestMessage(
        objectMapper.writeValueAsString(
            ekstraFelter + mapOf(
                "@id" to UUID.randomUUID().toString(),
                "@opprettet" to LocalDateTime.now(),
                "@behov" to behov,
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

    private fun sendNyPåminnelse() {
        objectMapper.writeValueAsString(
            mapOf(
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

    private fun sendManuellSaksbehandling(vedtaksperiodeIndeks: Int, godkjent: Boolean = true) {
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
            )
        )
    }

    private fun sendYtelserUtenHistorikk(vedtaksperiodeIndeks: Int) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Sykepengehistorikk))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Foreldrepenger))
        sendGeneriskBehov(
            vedtaksperiodeIndeks = vedtaksperiodeIndeks,
            behov = listOf("Sykepengehistorikk", "Foreldrepenger"),
            løsninger = mapOf(
                "Sykepengehistorikk" to emptyList<Any>(),
                "Foreldrepenger" to emptyMap<String, String>()
            )
        )
    }

    private fun sendVilkårsgrunnlag(vedtaksperiodeIndeks: Int, egenAnsatt: Boolean = false, inntekter: List<Pair<YearMonth, Double>> = 1.rangeTo(12).map { YearMonth.of(2018, it) to INNTEKT.toDouble() }, opptjening: List<Triple<String, LocalDate, LocalDate?>> = listOf(Triple(ORGNUMMER, 1.januar(2010), null))) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Inntektsberegning))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, EgenAnsatt))
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Opptjening))
        sendGeneriskBehov(
            vedtaksperiodeIndeks = vedtaksperiodeIndeks,
            behov = listOf("Inntektsberegning", "EgenAnsatt", "Opptjening"),
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

    private fun sendUtbetaling(vedtaksperiodeIndeks: Int, utbetalingOK: Boolean = true) {
        assertTrue(testRapid.inspektør.etterspurteBehov(vedtaksperiodeIndeks, Utbetaling))
        sendGeneriskBehov(
            vedtaksperiodeIndeks = vedtaksperiodeIndeks,
            behov = listOf("Utbetaling"),
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

    private fun assertTilstander(vedtaksperiodeIndeks: Int, vararg tilstand: String) {
        assertEquals(tilstand.toList(), testRapid.inspektør.tilstander(testRapid.inspektør.vedtaksperiodeId(vedtaksperiodeIndeks)))
    }

    private class TestRapid() : RapidsConnection() {
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

    private class RapidInspektør(private val messages: List<Pair<String?, String>>) {
        private val jsonmeldinger = mutableMapOf<Int, JsonNode>()
        private val vedtaksperiodeIder get() = mutableMapOf<Int, UUID>().apply {
            var vedtaksperiodeteller = 0
            events("vedtaksperiode_endret") {
                val id = UUID.fromString(it.path("vedtaksperiodeId").asText())
                this.putIfAbsent(vedtaksperiodeteller, id)
                vedtaksperiodeteller += 1
            }
        }
        private val tilstander get() = mutableMapOf<UUID, MutableList<String>>().apply {
            events("vedtaksperiode_endret") {
                val id = UUID.fromString(it.path("vedtaksperiodeId").asText())
                this.getOrPut(id) { mutableListOf() }.add(it.path("gjeldendeTilstand").asText())
            }
        }
        private val behov get() = mutableMapOf<UUID, MutableList<Behovtype>>().apply {
            events("behov") {
                val id = UUID.fromString(it.path("vedtaksperiodeId").asText())
                this.getOrPut(id) { mutableListOf() }.apply {
                    it.path("@behov").onEach {
                        add(Behovtype.valueOf(it.asText()))
                    }
                }
            }
        }

        private fun events(name: String, onEach: (JsonNode) -> Unit) = messages.forEachIndexed { indeks, _ ->
            val message = melding(indeks)
            if (name == message.path("@event_name").asText()) onEach(message)
        }

        fun melding(indeks: Int) = jsonmeldinger.getOrPut(indeks) { objectMapper.readTree(messages[indeks].second) }
        fun antall() = messages.size

        fun vedtaksperiodeId(indeks: Int) = requireNotNull(vedtaksperiodeIder[indeks]) { "Fant ikke vedtaksperiode" }
        fun tilstander(vedtaksperiodeId: UUID) = tilstander[vedtaksperiodeId]?.toList() ?: emptyList()
        fun etterspurteBehov(vedtaksperiodeIndeks: Int, behovtype: Behovtype) = behov[vedtaksperiodeId(vedtaksperiodeIndeks)]?.any { it == behovtype } ?: false
    }
}
