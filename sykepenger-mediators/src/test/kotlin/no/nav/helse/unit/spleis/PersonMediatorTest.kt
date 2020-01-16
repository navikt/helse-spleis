package no.nav.helse.unit.spleis

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Topics
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.Problemer
import no.nav.helse.person.VedtaksperiodeObserver
import no.nav.helse.spleis.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Future

internal class PersonMediatorTest {

    private val probe = mockk<VedtaksperiodeProbe>(relaxed = true)
    private val lagrePersonDao = mockk<PersonObserver>(relaxed = true)
    private val repo = mockk<PersonRepository>()
    private val utbetalingsRepo = mockk<UtbetalingsreferanseRepository>(relaxed = true)
    private val lagreUtbetalingDao = mockk<LagreUtbetalingDao>(relaxed = true)
    private val producer = mockk<KafkaProducer<String, String>>(relaxed = true) {
        every { send(any()) } returns mockk<Future<RecordMetadata>> {
            every { get() } returns RecordMetadata(TopicPartition("topic", 1), 0, 1, 100L, 100L, 0, 0)
        }
    }

    private val personMediator = PersonMediator(
        vedtaksperiodeProbe = probe,
        personRepository = repo,
        lagrePersonDao = lagrePersonDao,
        utbetalingsreferanseRepository = utbetalingsRepo,
        lagreUtbetalingDao = lagreUtbetalingDao,
        producer = producer
    )

    private val problemer = Problemer()
    private val nySøknad = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = "fnr",
        aktørId = "aktørid",
        orgnummer = "orgnr",
        rapportertdato = LocalDateTime.now(),
        sykeperioder = listOf(Triple(LocalDate.now(), LocalDate.now(), 100)),
        problemer = problemer,
        originalJson = "{}"
    )

    @Test
    fun `sørger for at observers blir varslet om endring`() {
        every { repo.hentPerson(any()) } returns null

        personMediator.onNySøknad(nySøknad, problemer)

        verify(exactly = 1) {
            repo.hentPerson(any())
            lagrePersonDao.personEndret(any())
            probe.personEndret(any())
            lagreUtbetalingDao.personEndret(any())
            producer.send(match { it.topic() == Topics.vedtaksperiodeEventTopic })
        }
    }

    @Test
    fun `skal sende utbetalingsevent ved utbetaling`() {
        personMediator.vedtaksperiodeTilUtbetaling(
            VedtaksperiodeObserver.UtbetalingEvent(
                UUID.randomUUID(),
                "aktørId",
                "fødselsnummer",
                "organisasjonsnummer",
                "utbetalingsreferanse",
                listOf(),
                LocalDate.of(2019, 12, 12)
            )
        )

        verify(exactly = 1) {
            producer.send(match { it.topic() == Topics.utbetalingEventTopic })
        }
    }
}
