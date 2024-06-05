package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.spleis.meldinger.model.SimuleringMessage.Simuleringstatus.OK
import org.junit.jupiter.api.Test
import no.nav.inntektsmeldingkontrakt.Periode as IMPeriode

internal class InntektsmeldingReplayTest: AbstractEndToEndMediatorTest() {

    @Test
    fun `inntektsmelding strekker periode tilbake - uoverstemmelse av refusjonsopplysninger etter replay av inntektsmeldinger`() {
        nyPeriode(12.februar(2024) til 16.februar(2024), a1)
        nyPeriode(17.februar(2024) til 3.mars(2024), a2)
        nyPeriode(4.mars(2024) til 24.mars(2024), a1)
        sendInntektsmelding(listOf(
            IMPeriode(12.februar(2024), 27.februar(2024))
        ), 4.mars(2024), orgnummer = a1)
        sendVilkårsgrunnlag(2, 12.februar(2024), orgnummer = a1)
        sendInntektsmelding(listOf(
            IMPeriode(12.februar(2024), 27.februar(2024))
        ), 12.februar(2024), orgnummer = a1)
        assertTilstand(0, "AVSLUTTET_UTEN_UTBETALING")
        assertTilstand(1, "AVSLUTTET_UTEN_UTBETALING")
        assertTilstand(2, "AVVENTER_HISTORIKK")
    }

    @Test
    fun `Replay av inntektsmelding skal ikke gi varsler på tvers av arbeidsgivere`() {
        nyPeriode(29.mars til 13.april, a1)
        nyPeriode(29.mars til 13.april, a2)

        nyPeriode(14.april til 23.april, a1)
        nyPeriode(14.april til 23.april, a2)

        sendInntektsmelding(listOf(IMPeriode(29.mars, 13.april)), førsteFraværsdag = 29.mars, orgnummer = a1)

        nyPeriode(24.april til 30.april, a1)

        assertTilstand(0, "AVSLUTTET_UTEN_UTBETALING")
        assertTilstand(1, "AVSLUTTET_UTEN_UTBETALING")
        assertTilstand(2, "AVVENTER_BLOKKERENDE_PERIODE")
        assertTilstand(3, "AVVENTER_INNTEKTSMELDING")
        assertTilstand(4, "AVVENTER_BLOKKERENDE_PERIODE")
        assertIngenVarsler()

        // At a2 nå spør om replay gjør at vi også replayer inntektsmeldingen a1 har sendt og a1 får
        // varsel om flere inntektsmeldinger selv om det kun finnes én
        nyPeriode(24.april til 30.april, a2)

        assertTilstand(0, "AVSLUTTET_UTEN_UTBETALING")
        assertTilstand(1, "AVSLUTTET_UTEN_UTBETALING")
        assertTilstand(2, "AVVENTER_BLOKKERENDE_PERIODE")
        assertTilstand(3, "AVVENTER_INNTEKTSMELDING")
        assertTilstand(4, "AVVENTER_BLOKKERENDE_PERIODE")
        assertTilstand(5, "AVVENTER_INNTEKTSMELDING")
        assertIngenVarsler()
    }

    @Test
    fun `Får med oss informasjon fra inntektsmelding også når den kommer før søknad`() {
        nyPeriode(1.januar til 31.januar, ORGNUMMER)
        sendInntektsmelding(listOf(IMPeriode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        assertTilstand(0, "AVSLUTTET")

        sykmelding(1.mars til 31.mars, ORGNUMMER)
        sendInntektsmelding(emptyList(), førsteFraværsdag = 1.mars, orgnummer = ORGNUMMER, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
        søknad(1.mars til 31.mars, ORGNUMMER)
        assertTilstand(1, "AVVENTER_VILKÅRSPRØVING")
        assertVarsel(1, RV_IM_8)
    }

    private fun nyPeriode(periode: Periode, orgnr: String) {
        sykmelding(periode, orgnr)
        søknad(periode, orgnr)
    }

    private fun sykmelding(periode: Periode, orgnr: String) = sendNySøknad(SoknadsperiodeDTO(fom = periode.start, tom = periode.endInclusive, sykmeldingsgrad = 100), orgnummer = orgnr)
    private fun søknad(periode: Periode, orgnr: String) = sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = periode.start, tom = periode.endInclusive, sykmeldingsgrad = 100)), orgnummer = orgnr)

    private companion object {
        val a1 = "a1"
        val a2 = "a2"
    }
}