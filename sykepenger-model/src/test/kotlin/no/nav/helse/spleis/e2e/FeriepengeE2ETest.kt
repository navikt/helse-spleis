package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Feriepenger
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Year

internal class FeriepengeE2ETest : AbstractEndToEndTest() {
    @Test
    fun `Beregner feriepenger korrekt for enkel spleisperiode uten infotrygdhistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )

        assertEquals(0.0, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
        assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())
        assertEquals(1431 * 10 * 0.102, inspektør.spleisFeriepengebeløpArbeidsgiver.first())

        val utbetalingslinje = TestArbeidsgiverInspektør.Feriepengeutbetalingslinje(
            1.mai(2021),
            31.mai(2021),
            Satstype.ENG,
            1460,
            null,
            Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig,
            Endringskode.NY
        )
        assertEquals(utbetalingslinje, inspektør.feriepengeutbetalingslinjer.first())
    }

    @Test
    fun `Beregner feriepenger korrekt for enkel spleisperiode med en utbetaling i infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mars(2020), 31.mars(2020), 100.prosent, INNTEKT)),
            feriepengehistorikk = listOf(Feriepenger(ORGNUMMER, 3211, 1.mai(2021), 31.mai(2021)))
        )

        assertEquals(1431 * 22 * 0.102, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
        assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())
        assertEquals(1431 * 10 * 0.102, inspektør.spleisFeriepengebeløpArbeidsgiver.first())

        val utbetalingslinje = TestArbeidsgiverInspektør.Feriepengeutbetalingslinje(
            1.mai(2021),
            31.mai(2021),
            Satstype.ENG,
            1460,
            null,
            Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig,
            Endringskode.NY
        )
        assertEquals(utbetalingslinje, inspektør.feriepengeutbetalingslinjer.first())
    }

    @Test
    fun `Legger ikke infotrygdcache til grunn for feriepenger 8)`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar(2020), 31.januar(2020), 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar(2020), INNTEKT, true)
            )
        )
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar(2020), 31.januar(2020), 100.prosent, INNTEKT)),
            feriepengehistorikk = listOf(Feriepenger(ORGNUMMER, 3211, 1.mai(2021), 31.mai(2021)))
        )

        assertEquals(1431 * 23 * 0.102, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
        assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())
        assertEquals(1431 * 10 * 0.102, inspektør.spleisFeriepengebeløpArbeidsgiver.first())

        val utbetalingslinje = TestArbeidsgiverInspektør.Feriepengeutbetalingslinje(
            1.mai(2021),
            31.mai(2021),
            Satstype.ENG,
            1460,
            null,
            Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig,
            Endringskode.NY
        )
        assertEquals(utbetalingslinje, inspektør.feriepengeutbetalingslinjer.first())
    }

    @Test
    fun `Beregner ikke feriepenger for personer markert for manuell beregning av feriepenger`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            skalBeregnesManuelt = true
        )

        assertEquals(0, inspektør.feriepengeutbetalingslinjer.size)
        assertTrue(inspektør.personLogg.toString().contains("Person er markert for manuell beregning av feriepenger"))

    }
}
