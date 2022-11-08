package no.nav.helse.spleis.e2e.refusjon

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.LagreRefusjonsopplysningerIVilkårsgrunnlag::class)
internal class RefusjonsopplysningerE2ETest : AbstractDslTest() {

    @Test
    fun `første fraværsdag oppgitt til dagen etter arbeidsgiverperioden`(){
        a1 {
            nyttVedtak(førsteFraværsdag = 17.januar, arbeidsgiverperiode = listOf(1.januar til 16.januar), fom = 1.januar, tom = 31.januar)
            assertIngenInfoSomInneholder("Mangler refusjonsopplysninger på orgnummer")
        }
    }

    @Test
    fun `første fraværsdag oppgitt til dagen etter arbeidsgiverperioden over helg`(){
        a1 {
            nyttVedtak(førsteFraværsdag = 22.januar, arbeidsgiverperiode = listOf(4.januar til 19.januar), fom = 4.januar, tom = 31.januar)
            assertIngenInfoSomInneholder("Mangler refusjonsopplysninger på orgnummer")
        }
    }

    @Test
    fun `første fraværsdag oppgitt til dagen etter arbeidsgiverperioden over helg med en dags gap`(){
        a1 {
            håndterSykmelding(Sykmeldingsperiode(4.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(4.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(4.januar til 19.januar), førsteFraværsdag = 23.januar)
            assertInfo("Mangler refusjonsopplysninger på orgnummer $a1 for periodene [22-01-2018 til 22-01-2018]")
        }
    }

    @Test
    fun `første fraværsdag oppgitt med en dags gap til arbeidsgiverperioden`(){
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 18.januar)
            assertInfo("Mangler refusjonsopplysninger på orgnummer $a1 for periodene [17-01-2018 til 17-01-2018]")
        }
    }

    @Test
    fun `lager nytt innslag i vilkårsgrunnlaghistorikken med oppdaterte refusjonsopplysninger ved ny inntektsmelding`() {
        a1 {
            val arbeidsgiverperiode = listOf(1.januar til 16.januar)
            nyttVedtak(1.januar, 31.januar, arbeidsgiverperiode = arbeidsgiverperiode, førsteFraværsdag = 1.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, opphørsdato = null))
            inspektør.refusjonsopplysningerFraVilkårsgrunnlag().assertRefusjonsbeløp(1.januar til 31.januar, INNTEKT)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            håndterInntektsmelding(arbeidsgiverperioder = arbeidsgiverperiode, førsteFraværsdag = 22.januar, refusjon = Inntektsmelding.Refusjon(beløp = INGEN, opphørsdato = null))
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            inspektør.refusjonsopplysningerFraVilkårsgrunnlag().let { refusjonsopplysninger ->
                refusjonsopplysninger.assertRefusjonsbeløp(1.januar til 21.januar, INNTEKT)
                refusjonsopplysninger.assertRefusjonsbeløp(22.januar til 31.januar, INGEN)
            }
        }
    }

    @Test
    fun `Duplikat innhold i ny inntektsmelding`() {
        a1 {
            val arbeidsgiverperiode = listOf(1.januar til 16.januar)
            nyttVedtak(1.januar, 31.januar, arbeidsgiverperiode = arbeidsgiverperiode, førsteFraværsdag = 1.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, opphørsdato = null))
            inspektør.refusjonsopplysningerFraVilkårsgrunnlag().assertRefusjonsbeløp(1.januar til 31.januar, INNTEKT)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            håndterInntektsmelding(arbeidsgiverperioder = arbeidsgiverperiode, førsteFraværsdag = 1.januar, refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = null))
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            inspektør.refusjonsopplysningerFraVilkårsgrunnlag().assertRefusjonsbeløp(1.januar til 31.januar, INNTEKT)
        }
    }
    @Test
    fun `Duplikat inntektsmelding`() {
        a1 {
            val arbeidsgiverperiode = listOf(1.januar til 16.januar)
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            val inntektsmeldingId = håndterInntektsmelding(arbeidsgiverperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            inspektør.refusjonsopplysningerFraVilkårsgrunnlag().assertRefusjonsbeløp(1.januar til 31.januar, INNTEKT)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterInntektsmelding(
                id = inntektsmeldingId,
                arbeidsgiverperioder = arbeidsgiverperiode,
            )
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            inspektør.refusjonsopplysningerFraVilkårsgrunnlag().assertRefusjonsbeløp(1.januar til 31.januar, INNTEKT)
        }
    }

    @Test
    fun `forkastet periode som mangler refusjonsopplysninger`(){
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 20.januar)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
            assertFunksjonellFeil("Mangler refusjonsopplysninger")
        }
    }

    private fun Refusjonsopplysninger.assertRefusjonsbeløp(periode: Periode, beløp: Inntekt) {
        periode.forEach { dag ->
            assertEquals(beløp, refusjonsbeløp(dag))
        }
    }
}