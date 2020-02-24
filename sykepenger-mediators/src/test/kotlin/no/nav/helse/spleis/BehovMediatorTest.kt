package no.nav.helse.spleis

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.behov.BehovType
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Personkontekst
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.person.Vedtaksperiodekontekst
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

internal class BehovMediatorTest {

    companion object {
        private lateinit var kafkaProducer: KafkaProducer<String, String>
        private lateinit var behovMediator: BehovMediator
    }

    @BeforeEach
    fun setup() {
        kafkaProducer = mockk(relaxed = true)
        behovMediator = BehovMediator(kafkaProducer, Aktivitetslogg(), mockk(relaxed = true))
    }

    @Test
    fun `behov med forskjellig kontekst`() {
        val personkontekst1 = personkontekst()
        val personkontekst2 = personkontekst()

        behovMediator.onBehov(personkontekst1.kontekstId, BehovType.GjennomgåTidslinje(personkontekst1))
        behovMediator.onBehov(personkontekst2.kontekstId, BehovType.GjennomgåTidslinje(personkontekst2))
        behovMediator.finalize(mockk())
        verify(exactly = 2) { kafkaProducer.send(any()) }
    }

    @Test
    fun `grupperte behov med forskjellig kontekst`() {
        val vedtaksperiodekontekst1 = vedtaksperiodekontekst()
        val vedtaksperiodekontekst2 = vedtaksperiodekontekst()

        behovMediator.onBehov(vedtaksperiodekontekst1.kontekstId, BehovType.GjennomgåTidslinje(vedtaksperiodekontekst1))
        behovMediator.onBehov(vedtaksperiodekontekst1.kontekstId, BehovType.Godkjenning(vedtaksperiodekontekst1))
        behovMediator.onBehov(vedtaksperiodekontekst2.kontekstId, BehovType.GjennomgåTidslinje(vedtaksperiodekontekst2))
        behovMediator.onBehov(vedtaksperiodekontekst2.kontekstId, BehovType.Godkjenning(vedtaksperiodekontekst2))
        behovMediator.finalize(mockk())
        verify(exactly = 2) { kafkaProducer.send(any()) }
    }

    @Test
    fun `behov med samme kontekst`() {
        val vedtaksperiodekontekst1 = vedtaksperiodekontekst()
        val vedtaksperiodekontekst2 = vedtaksperiodekontekst()

        behovMediator.onBehov(vedtaksperiodekontekst1.kontekstId, BehovType.GjennomgåTidslinje(vedtaksperiodekontekst1))
        behovMediator.onBehov(vedtaksperiodekontekst1.kontekstId, BehovType.GjennomgåTidslinje(vedtaksperiodekontekst2))
        assertThrows<Aktivitetslogg.AktivitetException> {
            behovMediator.finalize(mockk())
        }
    }

    private fun vedtaksperiodekontekst() = object : Vedtaksperiodekontekst {
        override val vedtaksperiodeId = UUID.randomUUID()
        override val organisasjonsnummer = "orgnummer"
        override val aktørId = "aktørId"
        override val fødselsnummer = "fødselsnummer"
        override val kontekstId = UUID.randomUUID()

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    private fun personkontekst() = object : Personkontekst {
        override val aktørId = "aktørId"
        override val fødselsnummer = "fnr"

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override val kontekstId = UUID.randomUUID()
    }
}
