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
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class BehovMediatorTest {

    private companion object {
        private lateinit var kafkaProducer: KafkaProducer<String, String>
        private lateinit var behovMediator: BehovMediator

        private val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
    }

    @BeforeEach
    fun setup() {
        kafkaProducer = mockk(relaxed = true)
        behovMediator = BehovMediator(kafkaProducer, mockk(relaxed = true))
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

        verify(exactly = 2) {
            kafkaProducer.send(match {
                it.key() == hendelse.fødselsnummer()
                    && objectMapper.readTree(it.value()).let {
                    it["@event_name"].asText() == "behov"
                        && it.has("@opprettet")
                        && it.has("@id")
                }
            })
        }
    }

    private fun hendelse() = object : ArbeidstakerHendelse(Aktivitetslogger(), Aktivitetslogg()) {
        override fun aktørId() = "aktørId"
        override fun fødselsnummer() = "fnr"
        override fun organisasjonsnummer() = "orgnr"
    }
}
