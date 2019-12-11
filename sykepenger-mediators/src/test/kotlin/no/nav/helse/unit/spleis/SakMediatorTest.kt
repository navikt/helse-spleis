package no.nav.helse.unit.spleis

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.behov.BehovProducer
import no.nav.helse.sak.SakObserver
import no.nav.helse.spleis.*
import no.nav.helse.spleis.oppgave.GosysOppgaveProducer
import org.junit.jupiter.api.Test

internal class SakMediatorTest {

    private val probe = mockk<VedtaksperiodeProbe>(relaxed = true)
    private val oppgaveProducer = mockk<GosysOppgaveProducer>(relaxed = true)
    private val vedtaksperiodeEventProducer = mockk<VedtaksperiodeEventProducer>(relaxed = true)
    private val behovProducer = mockk<BehovProducer>(relaxed = true)
    private val lagreSakDao = mockk<SakObserver>(relaxed = true)
    private val repo = mockk<SakRepository>()
    private val utbetalingsRepo = mockk<UtbetalingsreferanseRepository>(relaxed = true)
    private val lagreUtbetalingDao = mockk<LagreUtbetalingDao>(relaxed = true)

    private val sakMediator = SakMediator(
        vedtaksperiodeProbe = probe,
        sakRepository = repo,
        lagreSakDao = lagreSakDao,
        utbetalingsreferanseRepository = utbetalingsRepo,
        lagreUtbetalingDao = lagreUtbetalingDao,
        behovProducer = behovProducer,
        gosysOppgaveProducer = oppgaveProducer,
        vedtaksperiodeEventProducer = vedtaksperiodeEventProducer
    )

    private val nySøknadHendelse = nySøknadHendelse()

    @Test
    fun `sørger for at observers blir varslet om endring`() {
        every {
            repo.hentSak(any(), any())
        } returns null

        sakMediator.håndter(nySøknadHendelse)

        verify(exactly = 1) {
            repo.hentSak(any(), any())
            lagreSakDao.sakEndret(any())
            probe.sakEndret(any())
            lagreUtbetalingDao.sakEndret(any())
            vedtaksperiodeEventProducer.sendEndringEvent(any())
        }
    }
}
