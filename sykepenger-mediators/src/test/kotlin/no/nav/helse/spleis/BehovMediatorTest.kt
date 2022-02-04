package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.mockk
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.somFødselsnummer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class BehovMediatorTest {

    private companion object {
        private const val aktørId = "aktørId"
        private const val fødselsnummer = "01010112345"

        private lateinit var behovMediator: BehovMediator

        private val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())

        private val message = TestHendelseMessage(fødselsnummer)
    }

    private val messages = mutableListOf<Pair<String?, String>>()
    private val testRapid = object : RapidsConnection() {
        override fun publish(message: String) {
            messages.add(null to message)
        }

        override fun publish(key: String, message: String) {
            messages.add(key to message)
        }

        override fun start() {}
        override fun stop() {}
    }

    private lateinit var aktivitetslogg: Aktivitetslogg
    private lateinit var person: Person

    @BeforeEach
    fun setup() {
        person = Person(aktørId, fødselsnummer.somFødselsnummer(), MaskinellJurist())
        aktivitetslogg = Aktivitetslogg()
        behovMediator = BehovMediator(testRapid, mockk(relaxed = true))
        messages.clear()
    }

    @Test
    fun `grupperer behov`() {
        val hendelse = TestHendelse("Hendelse1", aktivitetslogg.barn())
        hendelse.kontekst(person)
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        hendelse.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        hendelse.kontekst(vedtaksperiode1)
        hendelse.behov(
            Sykepengehistorikk, "Trenger sykepengehistorikk", mapOf(
                "historikkFom" to LocalDate.now()
            )
        )
        hendelse.behov(Foreldrepenger, "Trenger foreldrepengeytelser")
        val arbeidsgiver2 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 2")
        hendelse.kontekst(arbeidsgiver2)
        val vedtaksperiode2 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 2")
        hendelse.kontekst(vedtaksperiode2)
        hendelse.kontekst(TestKontekst("Tilstand", "Tilstand 1"))
        hendelse.kontekst(TestKontekst("Tilstand", "Tilstand 2"))
        hendelse.behov(Utbetaling, "Skal utbetale")

        behovMediator.håndter(message, hendelse)

        assertEquals(2, messages.size)
        assertEquals(fødselsnummer, messages[0].first)
        assertEquals(fødselsnummer, messages[1].first)

        objectMapper.readTree(messages[0].second).also {
            assertEquals("behov", it["@event_name"].asText())
            assertTrue(it.hasNonNull("@id"))
            assertDoesNotThrow { UUID.fromString(it["@id"].asText()) }
            assertTrue(it.hasNonNull("@opprettet"))
            assertDoesNotThrow { LocalDateTime.parse(it["@opprettet"].asText()) }
            assertEquals(listOf("Sykepengehistorikk", "Foreldrepenger"), it["@behov"].map(JsonNode::asText))
            assertEquals("behov", it["@event_name"].asText())
            assertEquals(aktørId, it["aktørId"].asText())
            assertEquals(fødselsnummer, it["fødselsnummer"].asText())
            assertEquals("Arbeidsgiver 1", it["Arbeidsgiver"].asText())
            assertEquals("Vedtaksperiode 1", it["Vedtaksperiode"].asText())
            assertEquals(LocalDate.now().toString(), it[Sykepengehistorikk.name]["historikkFom"].asText())
        }
        objectMapper.readTree(messages[1].second).also {
            assertEquals("behov", it["@event_name"].asText())
            assertTrue(it.hasNonNull("@id"))
            assertDoesNotThrow { UUID.fromString(it["@id"].asText()) }
            assertTrue(it.hasNonNull("@opprettet"))
            assertDoesNotThrow { LocalDateTime.parse(it["@opprettet"].asText()) }
            assertEquals(listOf("Utbetaling"), it["@behov"].map(JsonNode::asText))
            assertEquals("behov", it["@event_name"].asText())
            assertEquals(aktørId, it["aktørId"].asText())
            assertEquals(fødselsnummer, it["fødselsnummer"].asText())
            assertEquals("Arbeidsgiver 2", it["Arbeidsgiver"].asText())
            assertEquals("Vedtaksperiode 2", it["Vedtaksperiode"].asText())
        }
    }

    @Test
    fun `duplikatnøkler er ok når verdi er lik`() {
        val hendelse = TestHendelse("Hendelse1", aktivitetslogg.barn())
        hendelse.kontekst(person)
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        hendelse.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        hendelse.kontekst(vedtaksperiode1)
        hendelse.behov(
            Sykepengehistorikk, "Trenger sykepengehistorikk", mapOf(
                "historikkFom" to LocalDate.now()
            )
        )
        hendelse.behov(
            Foreldrepenger, "Trenger foreldrepengeytelser", mapOf(
                "historikkFom" to LocalDate.now()
            )
        )

        assertDoesNotThrow { behovMediator.håndter(message, hendelse) }
    }

    @Test
    fun `kan ikke produsere samme behov flere ganger`() {
        val hendelse = TestHendelse("Hendelse1", aktivitetslogg.barn())
        hendelse.kontekst(person)
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        hendelse.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        hendelse.kontekst(vedtaksperiode1)
        hendelse.behov(
            Sykepengehistorikk, "Trenger sykepengehistorikk", mapOf(
                "historikkFom" to LocalDate.now().minusDays(1)
            )
        )
        hendelse.behov(
            Sykepengehistorikk, "Trenger sykepengehistorikk", mapOf(
                "historikkFom" to LocalDate.now().minusDays(1)
            )
        )

        assertThrows<IllegalArgumentException> { behovMediator.håndter(message, hendelse) }
    }

    @Test
    fun `kan ikke produsere samme behov`() {
        val hendelse = TestHendelse("Hendelse1", aktivitetslogg.barn())
        hendelse.kontekst(person)
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        hendelse.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        hendelse.kontekst(vedtaksperiode1)
        hendelse.behov(Sykepengehistorikk, "Trenger sykepengehistorikk")
        hendelse.behov(Sykepengehistorikk, "Trenger sykepengehistorikk")

        assertThrows<IllegalArgumentException> { behovMediator.håndter(message, hendelse) }
    }

    private class TestKontekst(
        private val type: String,
        private val melding: String
    ) : Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }

    private class TestHendelse(
        private val melding: String,
        val logg: Aktivitetslogg
    ) : ArbeidstakerHendelse(UUID.randomUUID(), fødselsnummer, aktørId, "not_relevant", logg), Aktivitetskontekst {

        override fun kontekst(kontekst: Aktivitetskontekst) {
            logg.kontekst(kontekst)
        }
    }
}
