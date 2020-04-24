package no.nav.helse.spleis

import io.mockk.mockk
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.meldinger.TestRapid
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class MessageMediatorTest {

    @Test
    internal fun `leser søknader`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagNySøknad(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100)))
        assertTrue(hendelseMediator.lestNySøknad)

        testRapid.sendTestMessage(meldingsfabrikk.lagSøknadArbeidsgiver(listOf(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100)), emptyList()))
        assertTrue(hendelseMediator.lestSendtSøknadArbeidsgiver)

        testRapid.sendTestMessage(meldingsfabrikk.lagSøknadNav(listOf(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100)), emptyList()))
        assertTrue(hendelseMediator.lestSendtSøknad)
    }

    @Test
    internal fun `leser inntektsmeldinger`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagInnteksmelding(listOf(Periode(LocalDate.now(), LocalDate.now())), LocalDate.now()))
        assertTrue(hendelseMediator.lestInntektsmelding)
    }

    @Test
    internal fun `leser kanseller utbetaling`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagKansellerUtbetaling("123456789"))
        assertTrue(hendelseMediator.lestKansellerUtbetaling)
    }

    @Test
    internal fun `leser påminnelser`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagPåminnelse(UUID.randomUUID(), TilstandType.START))
        assertTrue(hendelseMediator.lestPåminnelse)
    }

    @Test
    internal fun `leser behov`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagVilkårsgrunnlag(UUID.randomUUID(), TilstandType.START, true, emptyList(), emptyList(), Medlemskapsvurdering.Medlemskapstatus.Ja))
        assertTrue(hendelseMediator.lestVilkårsgrunnlag)

        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(UUID.randomUUID(), TilstandType.START, true))
        assertTrue(hendelseMediator.lestSimulering)

        testRapid.sendTestMessage(meldingsfabrikk.lagYtelser(UUID.randomUUID(), TilstandType.START))
        assertTrue(hendelseMediator.lestYtelser)

        testRapid.sendTestMessage(meldingsfabrikk.lagManuellSaksbehandling(UUID.randomUUID(), TilstandType.START, true))
        assertTrue(hendelseMediator.lestManuellSaksbehandling)

        testRapid.sendTestMessage(meldingsfabrikk.lagUtbetaling(UUID.randomUUID(), TilstandType.START, true))
        assertTrue(hendelseMediator.lestUtbetaling)
    }

    @Test
    internal fun `leser gammel løsning på godkjenning behov`(){
        testRapid.sendTestMessage(meldingsfabrikk.lagGammelManuellSaksbehandling(UUID.randomUUID(), TilstandType.START, true))
        assertTrue(hendelseMediator.lestManuellSaksbehandling)
    }

    @BeforeEach
    internal fun reset() {
        testRapid.reset()
        hendelseMediator.reset()
    }

    private companion object {
        private val meldingsfabrikk = TestMessageFactory("fnr", "aktør", "orgnr", 31000.0)
        private val testRapid = TestRapid()
        private val hendelseMediator = TestHendelseMediator()

        init {
            MessageMediator(
                rapidsConnection = testRapid,
                hendelseRecorder = mockk(relaxed = true),
                hendelseMediator = hendelseMediator
            )
        }
    }
}
