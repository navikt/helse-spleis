package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Aktivitetskontekst
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Personopplysninger
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.somFødselsnummer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

        override fun rapidName(): String {
            return "Testrapid"
        }

        override fun start() {}
        override fun stop() {}
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
        behovMediator = BehovMediator(mockk(relaxed = true))
        messages.clear()
    }

    @Test
    fun `grupperer behov`() {
        val hendelse = TestHendelse(aktivitetslogg.barn())

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

        behovMediator.håndter(testRapid, hendelse)

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
            assertTrue(it.hasNonNull("@behovId"))
            assertDoesNotThrow { UUID.fromString(it["@behovId"].asText()) }
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
        val hendelse = TestHendelse(aktivitetslogg.barn())
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

        assertDoesNotThrow { behovMediator.håndter(testRapid, hendelse) }
    }

    @Test
    fun `kan ikke produsere samme behov flere ganger`() {
        val hendelse = TestHendelse(aktivitetslogg.barn())
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

        assertThrows<IllegalArgumentException> { behovMediator.håndter(testRapid, hendelse) }
    }

    @Test
    fun `kan ikke produsere samme behov`() {
        val hendelse = TestHendelse(aktivitetslogg.barn())
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        hendelse.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        hendelse.kontekst(vedtaksperiode1)
        hendelse.behov(Sykepengehistorikk, "Trenger sykepengehistorikk")
        hendelse.behov(Sykepengehistorikk, "Trenger sykepengehistorikk")

        assertThrows<IllegalArgumentException> { behovMediator.håndter(testRapid, hendelse) }
    }

    private class TestKontekst(
        private val type: String,
        private val melding: String
    ) : Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }

    private class TestHendelse(
        val logg: Aktivitetslogg
    ) : ArbeidstakerHendelse(UUID.randomUUID(), fødselsnummer, aktørId, "not_relevant", logg), Aktivitetskontekst {
        private val person = person(MaskinellJurist())
        init {
            kontekst(person)
        }

        override fun kontekst(kontekst: Aktivitetskontekst) {
            logg.kontekst(kontekst)
        }

        override fun personopplysninger() = Personopplysninger(
            fødselsnummer = fødselsnummer.somFødselsnummer(),
            aktørId = aktørId
        )
    }
}
