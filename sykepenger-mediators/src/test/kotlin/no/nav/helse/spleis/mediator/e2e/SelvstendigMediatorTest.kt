package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.Toggle
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Test

internal class SelvstendigMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun selvstendigsøknad() = Toggle.SelvstendigNæringsdrivende.enable {
        nyttVedtak(3.januar til 26.januar)
        assertTilstander(
            0,
            "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING",
            "SELVSTENDIG_TIL_UTBETALING",
            "SELVSTENDIG_AVSLUTTET"
        )
    }

    @Test
    fun `annullere utbetalt periode`() = Toggle.SelvstendigNæringsdrivende.enable {
        nyttVedtak(januar)
        val utbetalingId = testRapid.inspektør.siste("utbetaling_utbetalt").path("utbetalingId").asText()
        sendAnnulleringSelvstendig(utbetalingId)
        sendUtbetaling()
        assertTilstander(
            0,
            "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING",
            "SELVSTENDIG_TIL_UTBETALING",
            "SELVSTENDIG_AVSLUTTET",
            "AVVENTER_ANNULLERING",
            "TIL_ANNULLERING",
            "TIL_INFOTRYGD"
        )

    }

    @Test
    fun `kaster ut søknad når det er fravær før sykmelding`() = Toggle.SelvstendigNæringsdrivende.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)), ventetid = 1.januar til 16.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE, fraværFørSykmelding = true)
        assertTilstander(0, "TIL_INFOTRYGD")
    }

    @Test
    fun fisker() = Toggle.SelvstendigNæringsdrivende.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.FISKER)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.FISKER)
        assertTilstander(
            0,
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun jordbruker() = Toggle.SelvstendigNæringsdrivende.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.JORDBRUKER)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.JORDBRUKER)
        assertTilstander(
            0,
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun annet() = Toggle.SelvstendigNæringsdrivende.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.ANNET)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.ANNET)
        assertTilstander(
            0,
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `overstyrer tidslinje i avventer godkjenning`() = Toggle.SelvstendigNæringsdrivende.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0)
        assertTilstander(
            0,
            "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING"
        )

        sendOverstyringTidslinjeSelvstendig((3.januar til 26.januar).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 80) })
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0, true)
        sendUtbetaling()
        assertUtbetalingTilstander(1, "NY", "IKKE_UTBETALT", "OVERFØRT", "UTBETALT")
        assertUtbetalingTilstander(0, "NY", "IKKE_UTBETALT", "FORKASTET")
        assertTilstander(
            0,
            "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING",
            "SELVSTENDIG_TIL_UTBETALING",
            "SELVSTENDIG_AVSLUTTET"
        )
    }


    @Test
    fun selvstendigBarnepasserSøknad() = Toggle.SelvstendigNæringsdrivende.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.BARNEPASSER)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.BARNEPASSER)
        sendVilkårsgrunnlagSelvstendig(0, orgnummer = "SELVSTENDIG")
        sendYtelserSelvstendig(0, orgnummer = "SELVSTENDIG")
        sendSimuleringSelvstendig(0, orgnummer = "SELVSTENDIG")
        sendUtbetalingsgodkjenningSelvstendig(0, orgnummer = "SELVSTENDIG")
        sendUtbetaling()
        assertTilstander(
            0,
            "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING",
            "SELVSTENDIG_TIL_UTBETALING",
            "SELVSTENDIG_AVSLUTTET"
        )
    }

    @Test
    fun `overstyrer tidslinje i avventer godkjenning for Barnepasser`() = Toggle.SelvstendigNæringsdrivende.enable {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.BARNEPASSER)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.BARNEPASSER)
        sendVilkårsgrunnlagSelvstendig(0, orgnummer = "SELVSTENDIG")
        sendYtelserSelvstendig(0, orgnummer = "SELVSTENDIG")
        sendSimuleringSelvstendig(0, orgnummer = "SELVSTENDIG")
        assertTilstander(
            0,
            "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING"
        )

        sendOverstyringTidslinjeSelvstendig((3.januar til 26.januar).map { ManuellOverskrivingDag(it, Dagtype.Sykedag, grad = 80) }, orgnummer = "SELVSTENDIG")
        sendYtelserSelvstendig(0, orgnummer = "SELVSTENDIG")
        sendSimuleringSelvstendig(0, orgnummer = "SELVSTENDIG")
        sendUtbetalingsgodkjenningSelvstendig(0, true, orgnummer = "SELVSTENDIG")
        sendUtbetaling()
        assertUtbetalingTilstander(1, "NY", "IKKE_UTBETALT", "OVERFØRT", "UTBETALT")
        assertUtbetalingTilstander(0, "NY", "IKKE_UTBETALT", "FORKASTET")
        assertTilstander(
            0,
            "SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_VILKÅRSPRØVING",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING",
            "SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE",
            "SELVSTENDIG_AVVENTER_HISTORIKK",
            "SELVSTENDIG_AVVENTER_SIMULERING",
            "SELVSTENDIG_AVVENTER_GODKJENNING",
            "SELVSTENDIG_TIL_UTBETALING",
            "SELVSTENDIG_AVSLUTTET"
        )
    }

    @Test
    fun `Kaster ut selvstendig søknad med oppgitt avvikling av foretak`() {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE, harOppgittAvvikling = true)
        assertTilstander(
            0,
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `Kaster ut selvstendig søknad med oppgitt ny i arbeidslivet`() {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE, harOppgittNyIArbeidslivet = true)
        assertTilstander(
            0,
            "TIL_INFOTRYGD"
        )
    }

    @Test
    fun `Kaster ut selvstendig søknad med oppgitt varig endring i inntekter`() {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)), ventetid = 3.januar til 18.januar, arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE, harOppgittVarigEndring = true)
        assertTilstander(
            0,
            "TIL_INFOTRYGD"
        )
    }

    private fun nyttVedtak(periode: Periode) {
        sendNySøknadSelvstendig(SoknadsperiodeDTO(fom = periode.start, tom = periode.endInclusive, sykmeldingsgrad = 100), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendSelvstendigsøknad(perioder = listOf(SoknadsperiodeDTO(fom = periode.start, tom = periode.endInclusive, sykmeldingsgrad = 100)), ventetid = periode.start til periode.start.plusDays(15), arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE)
        sendVilkårsgrunnlagSelvstendig(0)
        sendYtelserSelvstendig(0)
        sendSimuleringSelvstendig(0)
        sendUtbetalingsgodkjenningSelvstendig(0)
        sendUtbetaling()
    }
}
