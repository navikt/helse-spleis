package no.nav.helse.spleis.mediator

import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.mediator.e2e.AbstractEndToEndMediatorTest
import no.nav.helse.spleis.mediator.meldinger.TestRapid
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.spleis.utboks.InMemoryUtboksDao
import no.nav.helse.spleis.utboks.TestUtsender
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `NavNo inntektsmeldinger`() {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagNavNoInntektsmelding(
                arbeidsgiverperiode = listOf(Periode(LocalDate.now(), LocalDate.now())),
                vedtaksperiodeId = UUID.randomUUID()
            )
        )
        assertTrue(hendelseMediator.lestNavNoInntektsmelding)
    }

    @Test
    fun `korrigert NavNo inntektsmeldinger`() {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagKorrigertNavNoInntektsmelding(
                arbeidsgiverperiode = listOf(Periode(LocalDate.now(), LocalDate.now())),
                vedtaksperiodeId = UUID.randomUUID()
            )
        )
        assertTrue(hendelseMediator.lestKorrigertNavNoInntektsmelding)
    }

    @Test
    fun `NavNo selvbestemt inntektsmeldinger`() {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagNavNoSelvbestemtInntektsmelding(
                arbeidsgiverperiode = listOf(Periode(LocalDate.now(), LocalDate.now())),
                vedtaksperiodeId = UUID.randomUUID()
            )
        )
        assertTrue(hendelseMediator.lestNavNoSelvbestemtInntektsmelding)
    }

    @Test
    fun `annullerer utbetaling`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAnnullering(UUID.randomUUID()))
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
    fun simuleringer() {
        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(UUID.randomUUID(), UUID.randomUUID(), SimuleringMessage.Simuleringstatus.OK, UUID.randomUUID()))
        assertTrue(hendelseMediator.lestSimulering) { "Skal lese OK simulering" }
        hendelseMediator.reset()

        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(UUID.randomUUID(), UUID.randomUUID(), SimuleringMessage.Simuleringstatus.FUNKSJONELL_FEIL, UUID.randomUUID()))
        assertTrue(hendelseMediator.lestSimulering) { "Skal lese simulering med feil" }
        hendelseMediator.reset()

        testRapid.sendTestMessage(meldingsfabrikk.lagSimulering(UUID.randomUUID(), UUID.randomUUID(), SimuleringMessage.Simuleringstatus.OPPDRAG_UR_ER_STENGT, UUID.randomUUID()))
        assertTrue(hendelseMediator.lestSimulering) { "Kan lese simuleringhendelse når Oppdrag/UR er stengt" }
        hendelseMediator.reset()
    }

    @Test
    fun utbetalingshistorikk() {
        testRapid.sendTestMessage(meldingsfabrikk.lagUtbetalingshistorikk(UUID.randomUUID()))
        assertTrue(hendelseMediator.lestUtbetalingshistorikk)
    }

    @Test
    fun `ignorerer gammel utbetalingshistorikk`() {
        val message = meldingsfabrikk.lagUtbetalingshistorikk(UUID.randomUUID(), besvart = LocalDateTime.now().minusHours(2))
        testRapid.sendTestMessage(message)
        assertFalse(hendelseMediator.lestUtbetalingshistorikk)
    }

    @Test
    fun vilkårsgrunnlag() {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagVilkårsgrunnlag(
                vedtaksperiodeId = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                skjæringstidspunkt = 1.januar,
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
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
                forsikringsvurderingId = null,
            )
        )
        assertTrue(hendelseMediator.lestVilkårsgrunnlag)
    }

    @Test
    fun ytelser() {
        testRapid.sendTestMessage(meldingsfabrikk.lagYtelser(UUID.randomUUID(), UUID.randomUUID()))
        assertTrue(hendelseMediator.lestYtelser)
    }

    @Test
    fun utbetalingsgodkjenning() {
        testRapid.sendTestMessage(
            meldingsfabrikk.lagUtbetalingsgodkjenning(
                vedtaksperiodeId = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
                utbetalingId = UUID.randomUUID(),
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
                vedtaksperiodeId = UUID.randomUUID(),
                behandlingId = UUID.randomUUID(),
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
        assertTrue(hendelseMediator.lestForkastSykmeldingsperioder)
    }

    @Test
    fun `graderte andre ytelser endret`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagGraderteAndreYtelserEndret(5.januar))
        val forventet = TestHendelseMediator.EndretGrunnlagForBeregningData(
            type = "GraderteAndreYtelser",
            fom = 5.januar,
        )
        assertEquals(forventet, hendelseMediator.lestEndretGrunnlagForBeregning)
    }

    @Test
    fun inntektsendringer() {
        testRapid.sendTestMessage(meldingsfabrikk.lagInntektsendringer(7.januar))
        val forventet = TestHendelseMediator.EndretGrunnlagForBeregningData(
            type = "Inntektsendringer",
            fom = 7.januar,
        )
        assertEquals(forventet, hendelseMediator.lestEndretGrunnlagForBeregning)
    }

    @Test
    fun `endret forsikringsvurdering`() {
        val forsikringsvurderingId = UUID.randomUUID()

        testRapid.sendTestMessage(meldingsfabrikk.lagEndretForsikringsvurdering(
            skjæringstidspunkt = 1.januar,
            forsikringsvurderingId = forsikringsvurderingId,
        ))
        val forventet = TestHendelseMediator.EndretVurderingPåSkjæringstidspunktData(
            skjæringstidspunkt = 1.januar,
            vurderingId = forsikringsvurderingId,
            type = "Forsikringsvurdering",
            manuellVurdering = false
        )
        assertEquals(forventet, hendelseMediator.lestEndretVurderingPåSkjæringstidspunkt)
    }

    @Test
    fun `endret opptjeningsvurdering`() {
        val opptjeningsvurderingId = UUID.randomUUID()

        testRapid.sendTestMessage(meldingsfabrikk.lagEndretOpptjeningsvurdering(
            skjæringstidspunkt = 2.januar,
            opptjeningsvurderingId = opptjeningsvurderingId,
            manuellVurdering = true
        ))
        val forventet = TestHendelseMediator.EndretVurderingPåSkjæringstidspunktData(
            skjæringstidspunkt = 2.januar,
            vurderingId = opptjeningsvurderingId,
            type = "Opptjeningsvurdering",
            manuellVurdering = true
        )
        assertEquals(forventet, hendelseMediator.lestEndretVurderingPåSkjæringstidspunkt)
    }

    @Test
    fun `avbrutt fisker søknad`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAvbruttFiskerSøknad(1.januar,  31.januar))
        val forventet = TestHendelseMediator.AvbruttSøknadData(1.januar, 31.januar, Behandlingsporing.Yrkesaktivitet.Selvstendig)
        assertEquals(forventet, hendelseMediator.lestAvbruttSøknad)
    }

    @Test
    fun `avbrutt frilanser søknad`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAvbruttFrilanserSøknad(1.februar,  28.februar))
        val forventet = TestHendelseMediator.AvbruttSøknadData(1.februar, 28.februar, Behandlingsporing.Yrkesaktivitet.Frilans)
        assertEquals(forventet, hendelseMediator.lestAvbruttSøknad)
    }

    @Test
    fun `avbrutt jordbruker søknad`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAvbruttJordbrukerSøknad(1.mars,  31.mars))
        val forventet = TestHendelseMediator.AvbruttSøknadData(1.mars, 31.mars, Behandlingsporing.Yrkesaktivitet.Selvstendig)
        assertEquals(forventet, hendelseMediator.lestAvbruttSøknad)
    }

    @Test
    fun `avbrutt selvstendig søknad`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAvbruttSelvstendigSøknad(1.april,  30.april))
        val forventet = TestHendelseMediator.AvbruttSøknadData(1.april, 30.april, Behandlingsporing.Yrkesaktivitet.Selvstendig)
        assertEquals(forventet, hendelseMediator.lestAvbruttSøknad)
    }

    @Test
    fun `avbrutt arbeidstaker søknad`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAvbruttArbeidstakerSøknad(1.mai,  31.mai, "testOrgNr"))
        val forventet = TestHendelseMediator.AvbruttSøknadData(1.mai, 31.mai, Behandlingsporing.Yrkesaktivitet.Arbeidstaker("testOrgNr"))
        assertEquals(forventet, hendelseMediator.lestAvbruttSøknad)
    }

    @Test
    fun `avbrutt arbeidsledig søknad uten tidligere arbeidsgiver`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAvbruttArbeidsledigSøknad(1.juni,  30.juni, null))
        val forventet = TestHendelseMediator.AvbruttSøknadData(1.juni, 30.juni, Behandlingsporing.Yrkesaktivitet.Arbeidsledig)
        assertEquals(forventet, hendelseMediator.lestAvbruttSøknad)
    }

    @Test
    fun `avbrutt arbeidsledig søknad med tidligere arbeidsgiver`() {
        testRapid.sendTestMessage(meldingsfabrikk.lagAvbruttArbeidsledigSøknad(1.juli,  31.juli, "tidligereOrgNr"))
        val forventet = TestHendelseMediator.AvbruttSøknadData(1.juli, 31.juli, Behandlingsporing.Yrkesaktivitet.Arbeidstaker("tidligereOrgNr"))
        assertEquals(forventet, hendelseMediator.lestAvbruttSøknad)
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
                hendelseMediator = hendelseMediator,
                utsender = TestUtsender(),
                utboksDao = InMemoryUtboksDao()
            )
        }
    }
}
