package no.nav.helse.spleis

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import org.junit.jupiter.api.Test

internal class HendelseMediatorTest {

    private val lagreHendelseDao = mockk<HendelseConsumer.MessageListener>(relaxed = true)
    private val sakMediator = mockk<SakMediator>(relaxed = true)

    private val hendelseMediator = HendelseMediator(
        consumer = mockk(relaxed = true),
        lagreHendelseDao = lagreHendelseDao,
        sakMediator = sakMediator
    )

    @Test
    fun `hendelser lagres i db`() {
        hendelseMediator.onNySøknad(nySøknadHendelse())
        hendelseMediator.onSendtSøknad(sendtSøknadHendelse())
        hendelseMediator.onInntektsmelding(inntektsmeldingHendelse())

        verify(exactly = 1) {
            lagreHendelseDao.onNySøknad(any())
            lagreHendelseDao.onSendtSøknad(any())
            lagreHendelseDao.onInntektsmelding(any())

            sakMediator.onNySøknad(any())
            sakMediator.onSendtSøknad(any())
            sakMediator.onInntektsmelding(any())
        }
    }
}
