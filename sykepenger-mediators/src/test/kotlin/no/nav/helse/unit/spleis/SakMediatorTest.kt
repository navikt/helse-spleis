package no.nav.helse.unit.spleis

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.Topics
import no.nav.helse.sak.SakObserver
import no.nav.helse.sak.VedtaksperiodeObserver
import no.nav.helse.spleis.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SakMediatorTest {

    private val probe = mockk<VedtaksperiodeProbe>(relaxed = true)
    private val lagreSakDao = mockk<SakObserver>(relaxed = true)
    private val repo = mockk<SakRepository>()
    private val utbetalingsRepo = mockk<UtbetalingsreferanseRepository>(relaxed = true)
    private val lagreUtbetalingDao = mockk<LagreUtbetalingDao>(relaxed = true)
    private val producer = mockk<KafkaProducer<String, String>>(relaxed = true)

    private val sakMediator = SakMediator(
        vedtaksperiodeProbe = probe,
        sakRepository = repo,
        lagreSakDao = lagreSakDao,
        utbetalingsreferanseRepository = utbetalingsRepo,
        lagreUtbetalingDao = lagreUtbetalingDao,
        producer = producer
    )

    private val nySøknad = nySøknadHendelse()

    @Test
    fun `sørger for at observers blir varslet om endring`() {
        every { repo.hentSak(any()) } returns null

        sakMediator.onNySøknad(nySøknad)

        verify(exactly = 1) {
            repo.hentSak(any())
            lagreSakDao.sakEndret(any())
            probe.sakEndret(any())
            lagreUtbetalingDao.sakEndret(any())
            producer.send(match { it.topic() == Topics.vedtaksperiodeEventTopic })
        }
    }

    @Test
    fun `skal sende utbetalingsevent ved utbetaling`() {
        sakMediator.vedtaksperiodeTilUtbetaling(
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
