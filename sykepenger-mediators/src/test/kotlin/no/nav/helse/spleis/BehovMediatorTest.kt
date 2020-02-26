package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.behov.BehovType
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class BehovMediatorTest {

    private companion object {
        private lateinit var behovMediator: BehovMediator

        private val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
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

    @BeforeEach
    fun setup() {
        behovMediator = BehovMediator(testRapid, mockk(relaxed = true))
        messages.clear()
    }

    @Test
    fun `to forskjellige kontekster gir to meldinger på kafka`() {
        val hendelse = hendelse()
        val vedtaksperiode1 = UUID.randomUUID()
        val vedtaksperiode2 = UUID.randomUUID()

        behovMediator.onBehov(BehovType.Godkjenning("aktørId", "fnr", "orgnr", vedtaksperiode1))
        behovMediator.onBehov(BehovType.GjennomgåTidslinje("aktørId", "fnr", "orgnr", vedtaksperiode2))
        behovMediator.onBehov(BehovType.Godkjenning("aktørId", "fnr", "orgnr", vedtaksperiode2))
        behovMediator.finalize(hendelse)

        assertEquals(2, messages.size)
        assertTrue(messages.all { it.first == hendelse.fødselsnummer() })
        messages.map { objectMapper.readTree(it.second) }.let {
            assertTrue(it.all { it["@event_name"].asText() == "behov" })
            assertTrue(it.all { it.hasNonNull("@opprettet") })
            assertTrue(it.all { it.hasNonNull("@id") })
        }
    }

    private fun hendelse() = object : ArbeidstakerHendelse(Aktivitetslogger(), Aktivitetslogg()) {
        override fun aktørId() = "aktørId"
        override fun fødselsnummer() = "fnr"
        override fun organisasjonsnummer() = "orgnr"
    }
}
