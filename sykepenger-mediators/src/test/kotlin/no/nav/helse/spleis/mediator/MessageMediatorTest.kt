package no.nav.helse.spleis.mediator

import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest
import no.nav.helse.spleis.mediator.meldinger.TestRapid
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MessageMediatorTest {

    @Test
    fun søknader() {
        testRapid.sendTestMessage(meldingsfabrikk.lagNySøknad(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100)))
        assertTrue(hendelseMediator.lestNySøknad)

        testRapid.sendTestMessage(meldingsfabrikk.lagSøknadArbeidsgiver(listOf(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100))))
        assertTrue(hendelseMediator.lestSendtSøknadArbeidsgiver)

        testRapid.sendTestMessage(meldingsfabrikk.lagSøknadNav(perioder = listOf(SoknadsperiodeDTO(LocalDate.now(), LocalDate.now(), 100))))
        assertTrue(hendelseMediator.lestSendtSøknad)
    }

    @Test
    fun inntektsmeldinger() {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagInntektsmelding(
                listOf(Periode(LocalDate.now(), LocalDate.now())),
                LocalDate.now()
            )
        )
        assertTrue(hendelseMediator.lestInntektsmelding)
    }

    @Test
    fun `annullerer utbetaling`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAnnullering(UUID.randomUUID().toString()))
        assertTrue(hendelseMediator.lestAnnullerUtbetaling)
    }

    @Test
    fun påminnelser() {
        testRapid.sendTestMessage(meldingsfabrikk.lagPåminnelse(UUID.randomUUID(), TilstandType.START))
        assertTrue(hendelseMediator.lestPåminnelse)
    }

    @Test
    fun dødsmelding() {
        testRapid.sendTestMessage(meldingsfabrikk.lagDødsmelding(1.januar))
        assertTrue(hendelseMediator.lestDødsmelding)
    }

    @Test
    fun personpåminnelse() {
        testRapid.sendTestMessage(meldingsfabrikk.lagPersonPåminnelse())
        assertTrue(hendelseMediator.lestPersonpåminnelse)
    }

    @Test
    fun `anmodning om forkasting`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAnmodningOmForkasting())
        assertTrue(hendelseMediator.lestAnmodningOmForkasting)
    }

    @Test
    fun utbetalingpåminnelse() {
        testRapid.sendTestMessage(meldingsfabrikk.lagUtbetalingpåminnelse(UUID.randomUUID(), Utbetalingstatus.IKKE_UTBETALT))
        assertTrue(hendelseMediator.lestutbetalingpåminnelse)
    }

    @Test
    fun simuleringer() {
        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(UUID.randomUUID(), TilstandType.START, SimuleringMessage.Simuleringstatus.OK, UUID.randomUUID()))
        assertTrue(hendelseMediator.lestSimulering) { "Skal lese OK simulering" }
        hendelseMediator.reset()

        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(UUID.randomUUID(), TilstandType.START, SimuleringMessage.Simuleringstatus.FUNKSJONELL_FEIL, UUID.randomUUID()))
        assertTrue(hendelseMediator.lestSimulering) { "Skal lese simulering med feil" }
        hendelseMediator.reset()

        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(UUID.randomUUID(), TilstandType.START, SimuleringMessage.Simuleringstatus.OPPDRAG_UR_ER_STENGT, UUID.randomUUID()))
        assertTrue(hendelseMediator.lestSimulering) { "Kan lese simuleringhendelse når Oppdrag/UR er stengt" }
        hendelseMediator.reset()
    }

    @Test
    fun utbetalingshistorikk() {
        testRapid.sendTestMessage(meldingsfabrikk.lagUtbetalingshistorikk(UUID.randomUUID(), TilstandType.START))
        assertTrue(hendelseMediator.lestUtbetalingshistorikk)
    }

    @Test
    fun `ignorerer gammel utbetalingshistorikk`() {
        val message = meldingsfabrikk.lagUtbetalingshistorikk(UUID.randomUUID(), TilstandType.START, besvart = LocalDateTime.now().minusHours(2))
        testRapid.sendTestMessage(message)
        assertFalse(hendelseMediator.lestUtbetalingshistorikk)
    }

    @Test
    fun vilkårsgrunnlag() {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVilkårsgrunnlag(
                vedtaksperiodeId = UUID.randomUUID(),
                skjæringstidspunkt = 1.januar,
                tilstand = TilstandType.START,
                inntekterForSykepengegrunnlag = emptyList(),
                inntekterForOpptjeningsvurdering = listOf(
                    TestMessageFactory.InntekterForOpptjeningsvurderingFraLøsning(
                        måned = YearMonth.of(2017, 12),
                        inntekter = listOf(
                            TestMessageFactory.InntekterForOpptjeningsvurderingFraLøsning.Inntekt(
                                32000.0,
                                AbstractEndToEndMediatorTest.ORGNUMMER
                            )
                        )
                    )
                ),
                arbeidsforhold = emptyList(),
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja
            )
        )
        assertTrue(hendelseMediator.lestVilkårsgrunnlag)
    }

    @Test
    fun ytelser() {
        testRapid.sendTestMessage(meldingsfabrikk.lagYtelser(UUID.randomUUID(), TilstandType.START))
        assertTrue(hendelseMediator.lestYtelser)
    }

    @Test
    fun utbetalingsgodkjenning() {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetalingsgodkjenning(
                vedtaksperiodeId = UUID.randomUUID(),
                utbetalingId = UUID.randomUUID(),
                tilstand = TilstandType.START,
                utbetalingGodkjent = true,
                saksbehandlerIdent = "en_saksbehandler",
                saksbehandlerEpost = "en_saksbehandler@ikke.no",
                automatiskBehandling = false,
                makstidOppnådd = false,
                godkjenttidspunkt = LocalDateTime.now()
            )
        )
        assertTrue(hendelseMediator.lestUtbetalingsgodkjenning)
    }

    @Test
    fun utbetaling() {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetaling(
                fagsystemId = "qwer1234",
                utbetalingId = UUID.randomUUID().toString(),
                utbetalingOK = true
            )
        )
        assertTrue(hendelseMediator.lestUtbetaling)
    }

    @Test
    fun avstemming() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAvstemming())
        assertTrue(hendelseMediator.lestAvstemming)
    }

    @Test
    fun migrate() {
        testRapid.sendTestMessage(meldingsfabrikk.lagMigrate())
        assertTrue(hendelseMediator.lestMigrate)
    }

    @Test
    fun `forkast sykmeldingsperioder`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagForkastSykmeldingsperioder())
        assertTrue(hendelseMediator.lestForkastSykmeldingsperioderMessage)
    }

    @BeforeEach
    internal fun reset() {
        testRapid.reset()
        hendelseMediator.reset()
    }

    private companion object {
        private val meldingsfabrikk = TestMessageFactory("12121278911", "orgnr", 31000.0, 12.desember(1912))
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
