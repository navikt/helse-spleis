package no.nav.helse.spleis.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.spleis.BehovMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.MigrateMessage
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehovMediatorTest {

    private companion object {
        private const val fødselsnummer = "01010112345"

        private lateinit var behovMediator: BehovMediator

        private val objectMapper =
            jacksonObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(JavaTimeModule())

        private val eksempelmelding =
            MigrateMessage(
                JsonMessage.newMessage("testevent", emptyMap()).also {
                    it.requireKey("@event_name")
                },
                Meldingsporing(UUID.randomUUID(), fødselsnummer),
            )
    }

    private val messages = mutableListOf<Pair<String?, String>>()
    private val testRapid =
        object : RapidsConnection() {
            override fun publish(message: String) {
                messages.add(null to message)
            }

            override fun publish(key: String, message: String) {
                messages.add(key to message)
            }

            override fun rapidName(): String {
                return "Testrapid"
            }

            override fun start() {}

            override fun stop() {}
        }

    @BeforeEach
    fun setup() {
        behovMediator = BehovMediator(mockk(relaxed = true))
        messages.clear()
    }

    @Test
    fun `grupperer behov`() {
        val aktivitetslogg = Aktivitetslogg()

        aktivitetslogg.kontekst(eksempelmelding)

        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        aktivitetslogg.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        aktivitetslogg.kontekst(vedtaksperiode1)
        aktivitetslogg.behov(
            Sykepengehistorikk,
            "Trenger sykepengehistorikk",
            mapOf("historikkFom" to LocalDate.now()),
        )
        aktivitetslogg.behov(Foreldrepenger, "Trenger foreldrepengeytelser")
        val arbeidsgiver2 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 2")
        aktivitetslogg.kontekst(arbeidsgiver2)
        val vedtaksperiode2 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 2")
        aktivitetslogg.kontekst(vedtaksperiode2)
        aktivitetslogg.kontekst(TestKontekst("Tilstand", "Tilstand 1"))
        aktivitetslogg.kontekst(TestKontekst("Tilstand", "Tilstand 2"))
        aktivitetslogg.behov(Utbetaling, "Skal utbetale")

        behovMediator.håndter(testRapid, eksempelmelding, aktivitetslogg)

        assertEquals(2, messages.size)
        assertEquals(fødselsnummer, messages[0].first)
        assertEquals(fødselsnummer, messages[1].first)

        objectMapper.readTree(messages[0].second).also {
            assertEquals("behov", it["@event_name"].asText())
            assertTrue(it.hasNonNull("@id"))
            assertDoesNotThrow { UUID.fromString(it["@id"].asText()) }
            assertTrue(it.hasNonNull("@behovId"))
            assertDoesNotThrow { UUID.fromString(it["@behovId"].asText()) }
            assertTrue(it.hasNonNull("@opprettet"))
            assertDoesNotThrow { LocalDateTime.parse(it["@opprettet"].asText()) }
            assertEquals(
                listOf("Sykepengehistorikk", "Foreldrepenger"),
                it["@behov"].map(JsonNode::asText),
            )
            assertEquals("behov", it["@event_name"].asText())
            assertEquals("Arbeidsgiver 1", it["Arbeidsgiver"].asText())
            assertEquals("Vedtaksperiode 1", it["Vedtaksperiode"].asText())
            assertEquals(
                LocalDate.now().toString(),
                it[Sykepengehistorikk.name]["historikkFom"].asText(),
            )
        }
        objectMapper.readTree(messages[1].second).also {
            assertEquals("behov", it["@event_name"].asText())
            assertTrue(it.hasNonNull("@id"))
            assertDoesNotThrow { UUID.fromString(it["@id"].asText()) }
            assertTrue(it.hasNonNull("@opprettet"))
            assertTrue(it.hasNonNull("@behovId"))
            assertDoesNotThrow { UUID.fromString(it["@behovId"].asText()) }
            assertDoesNotThrow { LocalDateTime.parse(it["@opprettet"].asText()) }
            assertEquals(listOf("Utbetaling"), it["@behov"].map(JsonNode::asText))
            assertEquals("behov", it["@event_name"].asText())
            assertEquals("Arbeidsgiver 2", it["Arbeidsgiver"].asText())
            assertEquals("Vedtaksperiode 2", it["Vedtaksperiode"].asText())
        }
    }

    @Test
    fun `duplikatnøkler er ok når verdi er lik`() {
        val aktivitetslogg = Aktivitetslogg()
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        aktivitetslogg.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        aktivitetslogg.kontekst(vedtaksperiode1)
        aktivitetslogg.behov(
            Sykepengehistorikk,
            "Trenger sykepengehistorikk",
            mapOf("historikkFom" to LocalDate.now()),
        )
        aktivitetslogg.behov(
            Foreldrepenger,
            "Trenger foreldrepengeytelser",
            mapOf("historikkFom" to LocalDate.now()),
        )

        assertDoesNotThrow { behovMediator.håndter(testRapid, eksempelmelding, aktivitetslogg) }
    }

    @Test
    fun `kan produsere samme behov flere ganger så lenge detaljer er likt`() {
        val aktivitetslogg = Aktivitetslogg()
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        aktivitetslogg.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        aktivitetslogg.kontekst(vedtaksperiode1)
        aktivitetslogg.behov(
            Sykepengehistorikk,
            "Trenger sykepengehistorikk",
            mapOf("historikkFom" to LocalDate.now().minusDays(1)),
        )
        aktivitetslogg.behov(
            Sykepengehistorikk,
            "Trenger sykepengehistorikk",
            mapOf("historikkFom" to LocalDate.now().minusDays(1)),
        )

        assertDoesNotThrow { behovMediator.håndter(testRapid, eksempelmelding, aktivitetslogg) }
    }

    @Test
    fun `kan ikke produsere samme behov flere ganger`() {
        val aktivitetslogg = Aktivitetslogg()
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        aktivitetslogg.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        aktivitetslogg.kontekst(vedtaksperiode1)
        aktivitetslogg.behov(
            Sykepengehistorikk,
            "Trenger sykepengehistorikk",
            mapOf("historikkFom" to LocalDate.now().minusDays(1)),
        )
        aktivitetslogg.behov(
            Sykepengehistorikk,
            "Trenger sykepengehistorikk",
            mapOf("historikkFom" to LocalDate.now().minusDays(2)),
        )

        assertThrows<IllegalArgumentException> {
            behovMediator.håndter(testRapid, eksempelmelding, aktivitetslogg)
        }
    }

    @Test
    fun `kan produsere samme behov med like detaljer`() {
        val aktivitetslogg = Aktivitetslogg()
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        aktivitetslogg.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        aktivitetslogg.kontekst(vedtaksperiode1)
        aktivitetslogg.behov(Sykepengehistorikk, "Trenger sykepengehistorikk")
        aktivitetslogg.behov(Sykepengehistorikk, "Trenger sykepengehistorikk")

        assertDoesNotThrow { behovMediator.håndter(testRapid, eksempelmelding, aktivitetslogg) }
    }

    @Test
    fun `kan ikke produsere samme behov med ulike detaljer`() {
        val aktivitetslogg = Aktivitetslogg()
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        aktivitetslogg.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        aktivitetslogg.kontekst(vedtaksperiode1)
        aktivitetslogg.behov(Sykepengehistorikk, "Trenger sykepengehistorikk", mapOf("a" to 1))
        aktivitetslogg.behov(Sykepengehistorikk, "Trenger sykepengehistorikk", mapOf("a" to 2))

        assertThrows<IllegalArgumentException> {
            behovMediator.håndter(testRapid, eksempelmelding, aktivitetslogg)
        }
    }

    private class TestKontekst(private val type: String, private val melding: String) :
        Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }
}
