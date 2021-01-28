package no.nav.helse.spleis

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.mockk
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.spleis.meldinger.TestRapid
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class MessageMediatorTest {

    @Test
    fun søknader() {
        testRapid.sendTestMessage(meldingsfabrikk.lagNySøknad(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100)))
        assertTrue(hendelseMediator.lestNySøknad)

        testRapid.sendTestMessage(meldingsfabrikk.lagSøknadArbeidsgiver(listOf(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100))))
        assertTrue(hendelseMediator.lestSendtSøknadArbeidsgiver)

        testRapid.sendTestMessage(meldingsfabrikk.lagSøknadNav(listOf(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100))))
        assertTrue(hendelseMediator.lestSendtSøknad)
    }

    @Test
    fun inntektsmeldinger() {
        testRapid.sendTestMessage(meldingsfabrikk.lagInnteksmelding(listOf(Periode(LocalDate.now(), LocalDate.now())), LocalDate.now()))
        assertTrue(hendelseMediator.lestInntektsmelding)
    }

    @Test
    fun `annullerer utbetaling`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAnnullering("123456789"))
        assertTrue(hendelseMediator.lestAnnullerUtbetaling)
    }

    @Test
    fun påminnelser() {
        testRapid.sendTestMessage(meldingsfabrikk.lagPåminnelse(UUID.randomUUID(), TilstandType.START))
        assertTrue(hendelseMediator.lestPåminnelse)
    }

    @Test
    fun personpåminnelse() {
        testRapid.sendTestMessage(meldingsfabrikk.lagPersonPåminnelse())
        assertTrue(hendelseMediator.lestPersonpåminnelse)
    }

    @Test
    fun utbetalingpåminnelse() {
        testRapid.sendTestMessage(meldingsfabrikk.lagUtbetalingpåminnelse(UUID.randomUUID(), Utbetalingstatus.IKKE_UTBETALT))
        assertTrue(hendelseMediator.lestutbetalingpåminnelse)
    }

    @Test
    fun simuleringer() {
        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(UUID.randomUUID(), TilstandType.START, SimuleringMessage.Simuleringstatus.OK))
        assertTrue(hendelseMediator.lestSimulering) { "Skal lese OK simulering" }
        hendelseMediator.reset()

        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(UUID.randomUUID(), TilstandType.START, SimuleringMessage.Simuleringstatus.FUNKSJONELL_FEIL))
        assertTrue(hendelseMediator.lestSimulering) { "Skal lese simulering med feil" }
        hendelseMediator.reset()

        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(UUID.randomUUID(), TilstandType.START, SimuleringMessage.Simuleringstatus.OPPDRAG_UR_ER_STENGT))
        assertFalse(hendelseMediator.lestSimulering) { "Skal ikke lese simuleringhendelse når Oppdrag/UR er stengt" }
        hendelseMediator.reset()
    }

    @Test
    fun utbetalingshistorikk() {
        testRapid.sendTestMessage(meldingsfabrikk.lagUtbetalingshistorikk(UUID.randomUUID(), TilstandType.START))
        assertTrue(hendelseMediator.lestUtbetalingshistorikk)
    }

    @Test
    fun `ignorerer gammel utbetalingshistorikk`() {
        val message = meldingsfabrikk.lagUtbetalingshistorikk(UUID.randomUUID(), TilstandType.START).let {
            jacksonObjectMapper()
                .readValue<ObjectNode>(it)
                .put("@besvart", "${LocalDateTime.now().minusHours(2)}")
                .toString()
        }
        testRapid.sendTestMessage(message)
        assertFalse(hendelseMediator.lestUtbetalingshistorikk)
    }

    @Test
    fun vilkårsgrunnlag() {
        testRapid.sendTestMessage(meldingsfabrikk.lagVilkårsgrunnlag(UUID.randomUUID(), TilstandType.START, emptyList(), emptyList(), Medlemskapsvurdering.Medlemskapstatus.Ja))
        assertTrue(hendelseMediator.lestVilkårsgrunnlag)
    }

    @Test
    fun ytelser() {
        testRapid.sendTestMessage(meldingsfabrikk.lagYtelser(UUID.randomUUID(), TilstandType.START))
        assertTrue(hendelseMediator.lestYtelser)
    }

    @Test
    fun utbetalingsgodkjenning() {
        testRapid.sendTestMessage(meldingsfabrikk.lagUtbetalingsgodkjenning(
            vedtaksperiodeId = UUID.randomUUID(),
            utbetalingId = UUID.randomUUID(),
            tilstand = TilstandType.START,
            utbetalingGodkjent = true,
            saksbehandlerIdent = "en_saksbehandler",
            saksbehandlerEpost = "en_saksbehandler@ikke.no",
            automatiskBehandling = false,
            makstidOppnådd = false
        ))
        assertTrue(hendelseMediator.lestUtbetalingsgodkjenning)
    }

    @Test
    fun utbetaling() {
        testRapid.sendTestMessage(meldingsfabrikk.lagUtbetaling(
            fagsystemId = "qwer1234",
            utbetalingId = "asdf",
            utbetalingOK = true
        ))
        assertTrue(hendelseMediator.lestUtbetaling)
    }

    @Test
    fun avstemming() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAvstemming())
        assertTrue(hendelseMediator.lestAvstemming)
    }

    @BeforeEach
    internal fun reset() {
        testRapid.reset()
        hendelseMediator.reset()
    }

    private companion object {
        private val meldingsfabrikk = TestMessageFactory("12345678911", "1234567891234", "orgnr", 31000.0)
        private val testRapid = TestRapid()
        private val hendelseMediator = TestHendelseMediator()

        init {
            MessageMediator(
                rapidsConnection = testRapid,
                hendelseRepository = mockk(relaxed = true),
                hendelseMediator = hendelseMediator
            )
        }
    }
}
