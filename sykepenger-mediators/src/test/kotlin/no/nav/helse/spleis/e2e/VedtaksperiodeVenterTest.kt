package no.nav.helse.spleis.e2e

import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksperiodeVenterTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `sender ut vedtaksperiode venter`(){
        assertEquals(0, antallVedtaksperiodeVenter())
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        assertEquals(0, antallVedtaksperiodeVenter())
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        assertEquals(1, antallVedtaksperiodeVenter())

        sendInntektsmelding(listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        assertEquals(1, antallVedtaksperiodeVenter())
        sendVilkårsgrunnlag(0)
        assertEquals(2, antallVedtaksperiodeVenter())
        sendYtelser(0)
        assertEquals(3, antallVedtaksperiodeVenter())
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        assertEquals(4, antallVedtaksperiodeVenter())
        sendUtbetalingsgodkjenning(0)
        assertEquals(5, antallVedtaksperiodeVenter())
        sendUtbetaling()
        assertEquals(5, antallVedtaksperiodeVenter())
    }

    @Test
    fun `unngår å sende unødvendig vedtaksperiode venter ved replay av inntektsmelding`(){
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendInntektsmelding(listOf(Periode(fom = 1.februar, tom = 16.februar)), førsteFraværsdag = 2.januar)
        sendInntektsmelding(listOf(Periode(fom = 1.mars, tom = 16.mars)), førsteFraværsdag = 3.januar)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100))
        assertEquals(0, antallVedtaksperiodeVenter())
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100)))
        assertEquals(1, antallVedtaksperiodeVenter())
        sendVilkårsgrunnlag(0)
        assertEquals(2, antallVedtaksperiodeVenter())
    }

    @Test
    fun `unngår å sende unødvendig vedtaksperiode venter ved replay av inntektsmelding2`(){
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        val antallVedtaksperiodeVenter = antallVedtaksperiodeVenter()
        sendInntektsmelding(listOf(Periode(fom = 1.februar, tom = 16.februar)), førsteFraværsdag = 1.februar)
        sendNySøknad(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100))
        assertEquals(antallVedtaksperiodeVenter, antallVedtaksperiodeVenter())
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100)))
        assertEquals(antallVedtaksperiodeVenter + 1, antallVedtaksperiodeVenter())
    }

    private fun antallVedtaksperiodeVenter() = testRapid.inspektør.meldinger("vedtaksperiode_venter").size

}