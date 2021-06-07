package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*

internal class FeriepengeberegnerVisitorTest : AbstractEndToEndTest() {
    private companion object {
        private val alder = Alder(UNG_PERSON_FNR_2018)
        private const val a1 = "456789123"
        private const val a2 = "789456213"
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 48 sammenhengende utbetalingsdager i IT fra første januar`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(ORGNUMMER, 1.januar, 7.mars, 1000, 7.mars))
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), historikk, person)
        assertEquals(48, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 49 sammenhengende utbetalingsdager i IT fra første januar`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(ORGNUMMER, 1.januar, 8.mars, 1000, 8.mars))
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), historikk, person)
        assertEquals(48, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 47 sammenhengende utbetalingsdager i IT fra første januar`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(ORGNUMMER, 1.januar, 6.mars, 1000, 6.mars))
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), historikk, person)
        assertEquals(47, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning kun for dager i aktuelt opptjeningsår`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.desember(2017),
                    31.januar(2018),
                    1000,
                    31.januar(2018)
                )
            )
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), historikk, person)
        assertEquals(23, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Teller ikke med utbetalinger gjort etter feriepengekjøring i IT`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.desember(2017),
                    31.januar(2018),
                    1000,
                    11.mai(2021)
                )
            )
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), historikk, person)
        assertEquals(0, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Teller ikke med utbetalinger med inaktiv arbeidskategorikode i IT`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.desember(2017),
                    31.januar(2018),
                    1000,
                    31.januar(2018)
                )
            ),
            arbeidskategorikoder = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
                listOf(
                    UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode(
                        periode = 1.desember(2017) til 31.januar(2018),
                        arbeidskategorikode = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.Inaktiv
                    )
                )
            )
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), historikk, person)
        assertEquals(0, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Teller ikke med utbetalinger med kombinert arbeidskategorikode og orgnummer lik 0 i IT`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Personutbetalingsperiode(
                    "0",
                    1.desember(2017),
                    31.januar(2018),
                    1000,
                    31.januar(2018)
                ),
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.desember(2017),
                    31.januar(2018),
                    1000,
                    31.januar(2018)
                )
            ),
            arbeidskategorikoder = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
                listOf(
                    UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode(
                        periode = 1.desember(2017) til 31.januar(2018),
                        arbeidskategorikode = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.ArbeidstakerSelvstendig
                    )
                )
            )
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), historikk, person)
        assertEquals(0.0, beregner.beregnFeriepengerForInfotrygdPerson())
    }

    @Test
    fun `Prioriterer ikke personutbetalinger med kombinert arbeidskategorikode og orgnummer lik 0 i IT`() {
        byggPerson(
            arbeidsgiverperiode = 1.januar(2018) til 16.januar(2018),
            syktil = 28.mars(2018)
        )

        val historikk = utbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Personutbetalingsperiode(
                    "0",
                    1.desember(2018),
                    31.desember(2018),
                    1000,
                    31.desember(2018)
                ),
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.desember(2018),
                    31.desember(2018),
                    1000,
                    31.desember(2018)
                )
            ),
            arbeidskategorikoder = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
                listOf(
                    UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode(
                        periode = 1.desember(2018) til 31.desember(2018),
                        arbeidskategorikode = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.ArbeidstakerSelvstendig
                    )
                )
            )
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), historikk, person)
        assertEquals((17.januar(2018) til 23.mars(2018)).filterNot { it.erHelg() }, beregner.feriepengedatoer())
        assertEquals(0.0, beregner.beregnFeriepengerForInfotrygdPerson())
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 48 sammenhengende utbetalingsdager i Oppdrag fra første januar`() {
        byggPerson(
            arbeidsgiverperiode = 16.desember(2017) til 31.desember(2017),
            syktil = 7.mars(2018)
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), utbetalingshistorikkForFeriepenger(), person)

        assertEquals(48, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 49 sammenhengende utbetalingsdager i Oppdrag fra første januar`() {
        byggPerson(
            arbeidsgiverperiode = 16.desember(2017) til 31.desember(2017),
            syktil = 8.mars(2018)
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), utbetalingshistorikkForFeriepenger(), person)

        assertEquals(48, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 47 sammenhengende utbetalingsdager i Oppdrag fra første januar`() {
        byggPerson(
            arbeidsgiverperiode = 16.desember(2017) til 31.desember(2017),
            syktil = 6.mars(2018)
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), utbetalingshistorikkForFeriepenger(), person)

        assertEquals(47, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 48 sammenhengende utbetalingsdager i Oppdrag fra niende mai`() {
        byggPerson(
            arbeidsgiverperiode = 23.april(2018) til 8.mai(2018),
            syktil = 13.juli(2018)
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), utbetalingshistorikkForFeriepenger(), person)

        assertEquals(48, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 47 ikke-sammenhengende utbetalingsdager i Oppdrag`() {
        byggPerson(
            arbeidsgiverperiode = 16.desember(2017) til 31.desember(2017),
            syktil = 22.januar(2018)
        )
        byggPerson(
            arbeidsgiverperiode = 1.mars(2018) til 16.mars(2018),
            syktil = 28.mars(2018)
        )
        byggPerson(
            arbeidsgiverperiode = 1.mai(2018) til 16.mai(2018),
            syktil = 12.juni(2018)
        )
        byggPerson(
            arbeidsgiverperiode = 1.juli(2018) til 16.juli(2018),
            syktil = 21.juli(2018)
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), utbetalingshistorikkForFeriepenger(), person)

        assertEquals(47, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 48 ikke-sammenhengende utbetalingsdager i Oppdrag`() {
        byggPerson(
            arbeidsgiverperiode = 16.desember(2017) til 31.desember(2017),
            syktil = 22.januar(2018)
        )
        byggPerson(
            arbeidsgiverperiode = 1.mars(2018) til 16.mars(2018),
            syktil = 28.mars(2018)
        )
        byggPerson(
            arbeidsgiverperiode = 1.mai(2018) til 16.mai(2018),
            syktil = 12.juni(2018)
        )
        byggPerson(
            arbeidsgiverperiode = 1.juli(2018) til 16.juli(2018),
            syktil = 23.juli(2018)
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), utbetalingshistorikkForFeriepenger(), person)

        assertEquals(48, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med to helt overlappende Oppdrag`() {
        byggPersonToParallelle(
            arbeidsgiverperiode = 23.april(2018) til 8.mai(2018),
            syktil = 13.juli(2018)
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), utbetalingshistorikkForFeriepenger(), person)

        assertEquals(48, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med to ikke-overlappende utbetalingstidslinjer`() {
        byggPerson(
            arbeidsgiverperiode = 1.januar(2018) til 16.januar(2018),
            syktil = 15.februar(2018),
            orgnummer = a1
        )
        byggPerson(
            arbeidsgiverperiode = 1.juli(2018) til 16.juli(2018),
            syktil = 15.august(2018),
            orgnummer = a2
        )

        val beregner = Feriepengeberegner(alder, Year.of(2018), utbetalingshistorikkForFeriepenger(), person)

        assertEquals(44, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Teller ikke med dager fra annullerte utbetalinger i feriepengeberegneren`() {
        byggPersonMedAnnullering()
        val beregner = Feriepengeberegner(alder, Year.of(2018), utbetalingshistorikkForFeriepenger(), person)
        assertEquals(0, beregner.feriepengedatoer().size)
    }

    @Test
    fun `Teller ikke med dager fra opphørte utbetalingslinjer`() {
        byggPersonMedOpphør()
        val beregner = Feriepengeberegner(alder, Year.of(2018), utbetalingshistorikkForFeriepenger(), person)
        assertEquals((16.januar til 28.februar).filterNot { it.erHelg() }, beregner.feriepengedatoer())
    }

    private fun utbetalingshistorikkForFeriepenger(
        utbetalinger: List<UtbetalingshistorikkForFeriepenger.Utbetalingsperiode> = emptyList(),
        arbeidskategorikoder: UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
            listOf(
                UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode(
                    periode = LocalDate.MIN til LocalDate.MAX,
                    arbeidskategorikode = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.Arbeidstaker
                )
            )
        ),
        skalBeregnesManuelt: Boolean = false
    ) =
        UtbetalingshistorikkForFeriepenger(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = ORGNUMMER,
            utbetalinger = utbetalinger,
            feriepengehistorikk = emptyList(),
            arbeidskategorikoder = arbeidskategorikoder,
            opptjeningsår = Year.of(2020),
            skalBeregnesManuelt = skalBeregnesManuelt,
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

    private fun byggPersonMedAnnullering(
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
        håndterAnnullerUtbetaling(orgnummer = orgnummer)
        håndterUtbetalt()
    }

    private fun byggPersonToParallelle(
        arbeidsgiverperiode: Periode = 1.januar til 16.januar,
        syktil: LocalDate = 31.januar
    ) {
        håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = a2)
        håndterSøknadMedValidering(1.vedtaksperiode(a1), Søknad.Søknadsperiode.Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = a1)
        håndterSøknadMedValidering(1.vedtaksperiode(a2), Søknad.Søknadsperiode.Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), orgnummer = a2)
        håndterInntektsmelding(listOf(arbeidsgiverperiode), orgnummer = a1)
        håndterInntektsmelding(listOf(arbeidsgiverperiode), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                arbeidsgiverperiode.start.minusYears(1) til arbeidsgiverperiode.start.withDayOfMonth(1).minusMonths(1) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }
        ), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)
    }

    private fun byggPersonMedOpphør(
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
        håndterUtbetalingshistorikk(
            observatør.sisteVedtaksperiode(),
            orgnummer = orgnummer,
            inntektshistorikk = listOf(Inntektsopplysning(orgnummer, 1.desember(2017), INNTEKT, true)),
            besvart = LocalDateTime.now().minusMonths(1),
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(orgnummer, 1.desember(2017), 31.desember(2017), 100.prosent, INNTEKT))
        )
        håndterYtelser(
            observatør.sisteVedtaksperiode(),
            orgnummer = orgnummer,
            inntektshistorikk = listOf(Inntektsopplysning(orgnummer, 1.desember(2017), INNTEKT, true)),
            besvart = LocalDateTime.now().minusMonths(1),
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(orgnummer, 1.desember(2017), 31.desember(2017), 100.prosent, INNTEKT))
        )

        håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalt(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 60.prosent), orgnummer = orgnummer)
        håndterSøknadMedValidering(
            observatør.sisteVedtaksperiode(),
            Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 60.prosent),
            orgnummer = orgnummer
        )
        håndterYtelser(
            observatør.sisteVedtaksperiode(),
            orgnummer = orgnummer,
            inntektshistorikk = listOf(Inntektsopplysning(orgnummer, 1.desember(2017), 40000.månedlig, true)),
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(orgnummer, 1.desember(2017), 15.januar, 50.prosent, 40000.månedlig))
        )

        håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalt(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
    }
}
