package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.spleis.meldinger.model.SimuleringMessage.Simuleringstatus.OK
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksperiodeVenterTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `sender ut vedtaksperiode venter`(){
        assertAntallOgSisteÅrsak(0)
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        assertAntallOgSisteÅrsak(0)
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        assertAntallOgSisteÅrsak(1, "INNTEKTSMELDING")
        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        assertAntallOgSisteÅrsak(1)
        sendVilkårsgrunnlag(0)
        assertAntallOgSisteÅrsak(2, "BEREGNING")
        sendYtelser(0)
        assertAntallOgSisteÅrsak(3, "UTBETALING")
        sendSimulering(0, OK)
        assertAntallOgSisteÅrsak(4, "GODKJENNING")
        sendUtbetalingsgodkjenning(0)
        assertAntallOgSisteÅrsak(5, "UTBETALING")
        sendUtbetaling()
        assertAntallOgSisteÅrsak(5)
    }

    @Test
    fun `unngår å sende unødvendig vedtaksperiode venter når replay treffer`(){
        sendInntektsmelding(listOf(Periode(fom = 1.februar, tom = 16.februar)), førsteFraværsdag = 2.januar)
        sendInntektsmelding(listOf(Periode(fom = 1.mars, tom = 16.mars)), førsteFraværsdag = 3.januar)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100))
        assertAntallOgSisteÅrsak(0)
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100)))
        assertAntallOgSisteÅrsak(0) // Står i AVVENTER_VILKÅRSPRØVING som ikke implementerer venter
        sendVilkårsgrunnlag(0)
        assertAntallOgSisteÅrsak(1, "BEREGNING")
    }

    @Test
    fun `replay som bommer på person som allerede har Infotrygdhistorikk`(){
        nyttVedtakJanuar()
        val antallVedtaksperiodeVenter = vedtaksperiodeVenter.size
        sendInntektsmelding(listOf(Periode(fom = 1.februar, tom = 16.februar)), førsteFraværsdag = 1.februar)
        assertAntallOgSisteÅrsak(antallVedtaksperiodeVenter)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100))
        assertAntallOgSisteÅrsak(antallVedtaksperiodeVenter)
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100)))
        assertAntallOgSisteÅrsak(antallVedtaksperiodeVenter + 1, "INNTEKTSMELDING")
    }

    @Test
    fun `søknad treffer avsluttet periode`() {
        nyttVedtakJanuar()
        val antallVedtaksperiodeVenter = vedtaksperiodeVenter.size
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        assertAntallOgSisteÅrsak(antallVedtaksperiodeVenter)
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 50))) // Lavere grad
        assertAntallOgSisteÅrsak(antallVedtaksperiodeVenter + 1, "BEREGNING")
    }

    private fun nyttVedtakJanuar() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
    }
    private val vedtaksperiodeVenter get() = testRapid.inspektør.meldinger("vedtaksperiode_venter")
    private fun assertAntallOgSisteÅrsak(forventetAntall: Int, forventetÅrsak: String? = null) {
        val vedtaksperiodeVenter = vedtaksperiodeVenter
        assertEquals(forventetAntall, vedtaksperiodeVenter.size)
        forventetÅrsak?.let {
            assertEquals(it, vedtaksperiodeVenter.last().path("venterPå").path("venteårsak").path("hva").asText())
        }
    }

}