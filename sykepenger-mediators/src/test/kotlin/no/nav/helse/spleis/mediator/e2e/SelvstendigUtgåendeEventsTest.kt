package no.nav.helse.spleis.mediator.e2e

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.SelvstendigForsikring
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SelvstendigUtgåendeEventsTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Sender event SelvstendigIngenDagerIgjenEvent når bruker går til maks`() {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 1.januar, tom = 1.februar(2019), sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 1.februar(2019), sykmeldingsgrad = 100)), sendtNav = 1.januar.atStartOfDay(), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()

        assertEquals(1, testRapid.inspektør.meldinger("selvstendig_ingen_dager_igjen").size)
    }

    @Test
    fun `Sender ikke event SelvstendigIngenDagerIgjenEvent når bruker har gått til maks i en tidligere periode`() {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 1.januar, tom = 1.februar(2019), sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 1.februar(2019), sykmeldingsgrad = 100)), sendtNav = 1.januar.atStartOfDay(), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()

        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 2.februar(2019), tom = 20.februar(2019), sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 2.februar(2019), tom = 20.februar(2019), sykmeldingsgrad = 100)), sendtNav = 1.januar.atStartOfDay(), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendYtelserSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()

        assertEquals(1, testRapid.inspektør.meldinger("selvstendig_ingen_dager_igjen").size)
    }

    @Test
    fun `Sender ikke event SelvstendigIngenDagerIgjenEvent når bruker ikke går til maks`() {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), sendtNav = 1.januar.atStartOfDay(), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()

        assertEquals(0, testRapid.inspektør.meldinger("selvstendig_ingen_dager_igjen").size)
    }

    @Test
    fun `Sender event SelvstendigIngenDagerIgjenEvent når bruker dør iløpet av perioden`() {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), sendtNav = 1.januar.atStartOfDay(), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendDødsmelding(25.januar)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()

        assertEquals(1, testRapid.inspektør.meldinger("selvstendig_ingen_dager_igjen").size)
    }


    @Test
    fun `Sender event SelvstendigIngenDagerIgjenEvent når bruker blir 70 år i løpet av perioden`() {
        val fødselsnummer = "20014812238"
        val fødselsdato = 20.januar(1948)
        val meldingsfabrikk = TestMessageFactory(fødselsnummer, "SELVSTENDIG", INNTEKT, fødselsdato)
        val (_, nySøknad) = meldingsfabrikk.lagNySøknadSelvstendig(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), opprettet = 1.januar.atStartOfDay(), fnr = fødselsnummer, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        testRapid.sendTestMessage(nySøknad)

        val selvstendigHovedspørsmål = mapOf(
            "NARINGSDRIVENDE_VIRKSOMHETEN_AVVIKLET" to false,
            "NARINGSDRIVENDE_NY_I_ARBEIDSLIVET" to false,
            "NARINGSDRIVENDE_VARIG_ENDRING" to false,
            "FRAVAR_FOR_SYKMELDINGEN_V2" to false
        )
        val (_, sendtSøknad) = meldingsfabrikk.lagSøknadSelvstendig(
            fnr = fødselsnummer,
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            sendtNav = 1.februar.atStartOfDay(),
            ventetid = 1.januar til 16.januar,
            arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
            selvstendigHovedspørsmål = selvstendigHovedspørsmål
        )

        testRapid.sendTestMessage(sendtSøknad)

        // If the mediator asks for sykepengehistorikk first, reply with utbetalingshistorikk so flow continues
        if (testRapid.inspektør.harEtterspurteBehov(0, no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk)) {
            val sykBehov = testRapid.inspektør.etterspurteBehov(0, no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk)
            val behandlingIdForSyk = sykBehov.path("behandlingId").asText().toUUID()
            val (_, utbetalingshistorikkMsg) = meldingsfabrikk.lagUtbetalingshistorikk(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(0),
                behandlingId = behandlingIdForSyk,
                yrkesaktivitetstype = "SELVSTENDIG",
                orgnummer = "SELVSTENDIG"
            )
            testRapid.sendTestMessage(utbetalingshistorikkMsg)
        }

        assertTrue(testRapid.inspektør.harEtterspurteBehov(0, no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Medlemskap))
        val medlemskapBehov = testRapid.inspektør.etterspurteBehov(0, no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Medlemskap)
        val skjæringstidspunkt = medlemskapBehov.path("Medlemskap").path("skjæringstidspunkt").asLocalDate()
        val yrkesaktivitetstype = medlemskapBehov.path("yrkesaktivitetstype").asText()
        val behandlingId = medlemskapBehov.path("behandlingId").asText().toUUID()
        val (_, vilkårsMsg) = meldingsfabrikk.lagVilkårsgrunnlag(
            vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(0),
            behandlingId = behandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            inntekterForSykepengegrunnlag = emptyList(),
            inntekterForOpptjeningsvurdering = emptyList(),
            arbeidsforhold = emptyList(),
            medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            orgnummer = "SELVSTENDIG",
            yrkesaktivitetstype = yrkesaktivitetstype
        )
        testRapid.sendTestMessage(vilkårsMsg)

        val (_, ytelserMsg) = meldingsfabrikk.lagYtelser(
            vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(0),
            behandlingId = behandlingId,
            orgnummer = "SELVSTENDIG",
            yrkesaktivitetstype = yrkesaktivitetstype
        )
        testRapid.sendTestMessage(ytelserMsg)

        val simuleringBehovListe = testRapid.inspektør.alleEtterspurteBehov(no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering)
        simuleringBehovListe.forEach { behov ->
            val simBehandlingId = behov.path("behandlingId").asText().toUUID()
            val utbetalingId = UUID.fromString(behov.path("utbetalingId").asText())
            val fagsystemId = behov.path("Simulering").path("fagsystemId").asText()
            val fagområde = behov.path("Simulering").path("fagområde").asText()
            val (_, simuleringMsg) = meldingsfabrikk.lagSimulering(
                vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(0),
                behandlingId = simBehandlingId,
                status = SimuleringMessage.Simuleringstatus.OK,
                utbetalingId = utbetalingId,
                fagsystemId = fagsystemId,
                fagområde = fagområde,
                orgnummer = "SELVSTENDIG",
                yrkesaktivitetstype = behov.path("yrkesaktivitetstype").asText()
            )
            testRapid.sendTestMessage(simuleringMsg)
        }

        assertTrue(testRapid.inspektør.harEtterspurteBehov(0, no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning))
        val godkjenningBehov = testRapid.inspektør.etterspurteBehov(0, no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning)
        val godkjenningBehandlingId = godkjenningBehov.path("behandlingId").asText().toUUID()
        val godkjenningUtbetalingId = UUID.fromString(godkjenningBehov.path("utbetalingId").asText())
        val (_, godkjennMsg) = meldingsfabrikk.lagUtbetalingsgodkjenning(
            vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(0),
            behandlingId = godkjenningBehandlingId,
            utbetalingId = godkjenningUtbetalingId,
            utbetalingGodkjent = true,
            saksbehandlerIdent = "O123456",
            saksbehandlerEpost = "jan@banan.no",
            automatiskBehandling = false,
            makstidOppnådd = false,
            godkjenttidspunkt = LocalDateTime.now(),
            orgnummer = "SELVSTENDIG",
            yrkesaktivitetstype = godkjenningBehov.path("yrkesaktivitetstype").asText()
        )
        testRapid.sendTestMessage(godkjennMsg)

        val utbetalingsBehovListe = testRapid.inspektør.alleEtterspurteBehov(no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling)
        utbetalingsBehovListe.forEach { behov ->
            val (_, utbetalingMsg) = meldingsfabrikk.lagUtbetaling(
                fagsystemId = behov.path("Utbetaling").path("fagsystemId").asText(),
                utbetalingId = behov.path("utbetalingId").asText(),
                vedtaksperiodeId = behov.path("vedtaksperiodeId").asText().toUUID(),
                behandlingId = behov.path("behandlingId").asText().toUUID(),
                utbetalingOK = true,
                yrkesaktivitetstype = behov.path("yrkesaktivitetstype").asText()
            )
            testRapid.sendTestMessage(utbetalingMsg)
        }

        assertEquals(1, testRapid.inspektør.meldinger("selvstendig_ingen_dager_igjen").size)
    }

    @Test
    fun `Sender event SelvstendigUtbetaltEtterVentetid for bruker med forsikring fra dag 1 når vi betaler utover ventetid`() = Toggle.SelvstendigForsikring.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 1.januar, tom = 1.februar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 1.februar, sykmeldingsgrad = 100)), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0, selvstendigForsikring = listOf(SelvstendigForsikring(1.november(2017), null, SelvstendigForsikring.Forsikringstype.ÅttiProsentFraDagEn, 450_000.årlig)))
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()
        assertEquals(1, testRapid.inspektør.meldinger("selvstendig_utbetalt_etter_ventetid").size)

        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 2.februar, tom = 1.mars, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 2.februar, tom = 1.mars, sykmeldingsgrad = 100)), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendYtelserSelvstendig(1, selvstendigForsikring = listOf(SelvstendigForsikring(1.november(2017), null, SelvstendigForsikring.Forsikringstype.ÅttiProsentFraDagEn, 450_000.årlig)))
        sendSimuleringSelvstendig(1)
        sendUtbetalingsgodkjenningSelvstendig(1)
        sendUtbetaling()
        assertEquals(1, testRapid.inspektør.meldinger("selvstendig_utbetalt_etter_ventetid").size)

    }

    @Test
    fun `Sender ikke event SelvstendigUtbetaltEtterVentetid når bruker ikke har forsikring fra dag 1 når vi betaler utover ventetid`() = Toggle.SelvstendigForsikring.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 1.januar, tom = 1.februar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 1.februar, sykmeldingsgrad = 100)), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()
        assertEquals(0, testRapid.inspektør.meldinger("selvstendig_utbetalt_etter_ventetid").size)


        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 2.februar, tom = 1.mars, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 2.februar, tom = 1.mars, sykmeldingsgrad = 100)), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendYtelserSelvstendig(1)
        sendSimuleringSelvstendig(1)
        sendUtbetalingsgodkjenningSelvstendig(1)
        sendUtbetaling()
        assertEquals(0, testRapid.inspektør.meldinger("selvstendig_utbetalt_etter_ventetid").size)

    }

    @Test
    fun `Sender event SelvstendigUtbetaltEtterVentetid etter periode med kun ventetid`() = Toggle.SelvstendigForsikring.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 1.januar, tom = 10.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 10.januar, sykmeldingsgrad = 100)), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0, selvstendigForsikring = listOf(SelvstendigForsikring(1.november(2017), null, SelvstendigForsikring.Forsikringstype.ÅttiProsentFraDagEn, 450_000.årlig)))
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()
        assertEquals(0, testRapid.inspektør.meldinger("selvstendig_utbetalt_etter_ventetid").size)

        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 11.januar, tom = 31.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 11.januar, tom = 31.januar, sykmeldingsgrad = 100)), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendYtelserSelvstendig(1, selvstendigForsikring = listOf(SelvstendigForsikring(1.november(2017), null, SelvstendigForsikring.Forsikringstype.ÅttiProsentFraDagEn, 450_000.årlig)))
        sendSimuleringSelvstendig(1)
        sendUtbetalingsgodkjenningSelvstendig(1)
        sendUtbetaling()
        assertEquals(1, testRapid.inspektør.meldinger("selvstendig_utbetalt_etter_ventetid").size)

    }

    @Test
    fun `Sender både SelvstendigIngenDagerIgjenEvent og SelvstendigUtbetaltEtterVentetidEvent når bruker dør iløpet av perioden, men etter ventetiden`() = Toggle.SelvstendigForsikring.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), sendtNav = 1.januar.atStartOfDay(), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendDødsmelding(25.januar)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0, selvstendigForsikring = listOf(SelvstendigForsikring(1.november(2017), null, SelvstendigForsikring.Forsikringstype.ÅttiProsentFraDagEn, 450_000.årlig)))
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()

        assertEquals(1, testRapid.inspektør.meldinger("selvstendig_ingen_dager_igjen").size)
        assertEquals(1, testRapid.inspektør.meldinger("selvstendig_utbetalt_etter_ventetid").size)
    }
}
