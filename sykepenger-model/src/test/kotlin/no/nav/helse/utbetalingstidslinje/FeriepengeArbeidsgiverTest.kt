package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.*
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingslinjer.Feriepengeutbetaling
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate
import java.time.Year
import java.util.*

internal class FeriepengeArbeidsgiverTest() : AbstractEndToEndTest() {
    private companion object {
        private val alder = Alder(UNG_PERSON_FNR_2018)
    }

    private val feriepengeinspektør: Feriepengeinspektør get() = Feriepengeinspektør().apply { inspektør.arbeidsgiver.accept(this) }

//    @Test
//    fun `Finner datoer for feriepengeberegning med 48 sammenhengende utbetalingsdager i IT fra første januar`() {
//        val historikk = utbetalingshistorikkForFeriepenger(
//            listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 7.mars, 100.prosent, 1000.månedlig))
//        )
//
//        val beregner = FeriepengeberegnerArbeidsgiver(
//            (1.januar til 7.mars).filterNot { it.erHelg() }.toList(),
//            historikk,
//            inspektør.arbeidsgiver,
//            alder
//        )
//
//        assertEquals(48, beregner.beregn())
//    }
//
//    @Test
//    fun `dfg`() {
//        byggPerson(
//            arbeidsgiverperiode = 1.januar til 16.januar,
//            syktil = 31.januar,
//        )
//
//        FeriepengeberegnerArbeidsgiver(
//            (17.januar til 31.januar).filterNot { it.erHelg() }.toList(),
//            utbetalingshistorikkForFeriepenger(emptyList()),
//            inspektør.arbeidsgiver,
//            alder
//        ).beregn()
//
//        assertEquals(0.0, feriepengeinspektør.infotrygdFeriepengebeløpPerson)
//        assertEquals(0.0, feriepengeinspektør.infotrygdFeriepengebeløpArbeidsgiver)
//        assertEquals(1605.5819999999999, feriepengeinspektør.spleisFeriepengebeløpArbeidsgiver)
//    }

    private class Feriepengeinspektør : ArbeidsgiverVisitor {
        var infotrygdFeriepengebeløpPerson: Double? = null
        var infotrygdFeriepengebeløpArbeidsgiver: Double? = null
        var spleisFeriepengebeløpArbeidsgiver: Double? = null

        override fun preVisitFeriepengeutbetaling(
            feriepengeutbetaling: Feriepengeutbetaling,
            infotrygdFeriepengebeløpPerson: Double,
            infotrygdFeriepengebeløpArbeidsgiver: Double,
            spleisFeriepengebeløpArbeidsgiver: Double
        ) {
            this.infotrygdFeriepengebeløpPerson = infotrygdFeriepengebeløpPerson
            this.infotrygdFeriepengebeløpArbeidsgiver = infotrygdFeriepengebeløpArbeidsgiver
            this.spleisFeriepengebeløpArbeidsgiver = spleisFeriepengebeløpArbeidsgiver
        }
    }

    private fun utbetalingshistorikkForFeriepenger(utbetalinger: List<Infotrygdperiode> = emptyList()) =
        UtbetalingshistorikkForFeriepenger(
            UUID.randomUUID(),
            AKTØRID,
            ORGNUMMER,
            utbetalinger,
            emptyList(),
            emptyList(),
            false,
            emptyMap(),
            Year.of(2020)
        )

    private fun byggPerson(
        arbeidsgiverperiode: Periode = 1.januar til 16.januar,
        syktil: LocalDate = 31.januar,
        orgnummer: String = ORGNUMMER
    ) {
        håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = orgnummer)
        håndterSøknadMedValidering(
            observatør.sisteVedtaksperiode(),
            Søknad.Søknadsperiode.Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent),
            orgnummer = orgnummer
        )
        håndterUtbetalingshistorikk(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterInntektsmelding(listOf(arbeidsgiverperiode), orgnummer = orgnummer)
        håndterYtelser(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterVilkårsgrunnlag(observatør.sisteVedtaksperiode(), inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                arbeidsgiverperiode.start.minusYears(1) til arbeidsgiverperiode.start.withDayOfMonth(1).minusMonths(1) inntekter {
                    orgnummer inntekt INNTEKT
                }
            }
        ), orgnummer = orgnummer)
        håndterYtelser(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalt(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
    }
}
