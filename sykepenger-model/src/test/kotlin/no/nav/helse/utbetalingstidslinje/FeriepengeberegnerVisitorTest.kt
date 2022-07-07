package no.nav.helse.utbetalingstidslinje


import no.nav.helse.*
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.*
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
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
        private val alder = UNG_PERSON_FNR_2018.alder()
        private val a1 = "456789123"
        private val a2 = "789456213"
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
    fun `Teller ikke med utbetalinger gjort etter feriepengekjøring i IT - oppdatert for første kjøring i 2022`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.desember(2017),
                    31.januar(2018),
                    1000,
                    15.mai(2022)
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
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
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
            Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent),
            orgnummer = orgnummer
        )
        håndterUtbetalingshistorikk(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterInntektsmelding(listOf(arbeidsgiverperiode), orgnummer = orgnummer)
        håndterYtelser(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterVilkårsgrunnlag(observatør.sisteVedtaksperiode(), inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                arbeidsgiverperiode.start.minusYears(1) til arbeidsgiverperiode.start.withDayOfMonth(1).minusMonths(1) inntekter {
                    orgnummer inntekt INNTEKT
                }
            }
        ), orgnummer = orgnummer)
        håndterYtelser(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalt(orgnummer = orgnummer)
    }

    private fun byggPersonMedAnnullering(
        arbeidsgiverperiode: Periode = 1.januar til 16.januar,
        syktil: LocalDate = 31.januar,
        orgnummer: String = ORGNUMMER
    ) {
        håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = orgnummer)
        håndterSøknadMedValidering(
            observatør.sisteVedtaksperiode(),
            Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent),
            orgnummer = orgnummer
        )
        håndterUtbetalingshistorikk(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterInntektsmelding(listOf(arbeidsgiverperiode), orgnummer = orgnummer)
        håndterYtelser(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterVilkårsgrunnlag(observatør.sisteVedtaksperiode(), inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                arbeidsgiverperiode.start.minusYears(1) til arbeidsgiverperiode.start.withDayOfMonth(1).minusMonths(1) inntekter {
                    orgnummer inntekt INNTEKT
                }
            }
        ), orgnummer = orgnummer)
        håndterYtelser(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterSimulering(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(observatør.sisteVedtaksperiode(), orgnummer = orgnummer)
        håndterUtbetalt(orgnummer = orgnummer)
        håndterAnnullerUtbetaling(orgnummer = orgnummer)
        håndterUtbetalt()
    }

    private fun byggPersonToParallelle(
        arbeidsgiverperiode: Periode = 1.januar til 16.januar,
        syktil: LocalDate = 31.januar
    ) {
        håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = a2)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = a1)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)
        håndterInntektsmelding(listOf(arbeidsgiverperiode), orgnummer = a1)
        håndterInntektsmelding(listOf(arbeidsgiverperiode), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                arbeidsgiverperiode.start.minusYears(1) til arbeidsgiverperiode.start.withDayOfMonth(1).minusMonths(1) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }
        ), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
    }

}
