package no.nav.helse.feriepenger

import java.time.LocalDate
import java.time.Year
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.UNG_PERSON_FØDSELSDATO
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FeriepengedatoerTest : AbstractDslTest() {
    private companion object {
        private val alder = UNG_PERSON_FØDSELSDATO.alder
    }

    private fun feriepengerFor(opptjeningsår: Year, historikk: UtbetalingshistorikkForFeriepenger, sisteInfotrygdkjøring: LocalDate = LocalDate.MAX) =
        Feriepengeberegner(alder, opptjeningsår, grunnlagFraInfotrygd = historikk.grunnlagForFeriepenger(sisteInfotrygdkjøring), grunnlagFraSpleis = testperson.person.grunnlagForFeriepenger())

    @Test
    fun `Finner datoer for feriepengeberegning med 48 sammenhengende utbetalingsdager i IT fra første januar`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(a1, 1.januar, 7.mars, 1000, 7.mars))
        )

        val beregner = feriepengerFor(Year.of(2018), historikk)
        assertEquals(48, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 49 sammenhengende utbetalingsdager i IT fra første januar`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(a1, 1.januar, 8.mars, 1000, 8.mars))
        )

        val beregner = feriepengerFor(Year.of(2018), historikk)
        assertEquals(48, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 47 sammenhengende utbetalingsdager i IT fra første januar`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(a1, 1.januar, 6.mars, 1000, 6.mars))
        )

        val beregner = feriepengerFor(Year.of(2018), historikk)
        assertEquals(47, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning kun for dager i aktuelt opptjeningsår`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    a1,
                    1.desember(2017),
                    31.januar(2018),
                    1000,
                    31.januar(2018)
                )
            )
        )

        val beregner = feriepengerFor(Year.of(2018), historikk)
        assertEquals(23, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Teller ikke med utbetalinger gjort etter feriepengekjøring i IT - oppdatert for første kjøring i 23 for regnskapåret i 21`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    a1,
                    1.desember(2017),
                    31.januar(2018),
                    1000,
                    24.august(2024)
                )
            )
        )

        val beregner = feriepengerFor(Year.of(2018), historikk, sisteInfotrygdkjøring = 24.august(2024))
        assertEquals(0, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Teller ikke med utbetalinger med inaktiv arbeidskategorikode i IT`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    a1,
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

        val beregner = feriepengerFor(Year.of(2018), historikk)
        assertEquals(0, beregner.beregnFeriepenger(a1).second.datoer.size)
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
                    a1,
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

        val beregner = feriepengerFor(Year.of(2018), historikk)
        assertEquals(
            Feriepengeberegningsresultat(
                orgnummer = a1,
                arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                    infotrygdFeriepengebeløp = 2346.0,
                    spleisFeriepengebeløp = 0.0,
                    totaltFeriepengebeløp = 2346.0,
                    differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                    hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2346.0
                ),
                person = Feriepengeberegningsresultat.Beregningsverdier(
                    infotrygdFeriepengebeløp = 0.0,
                    spleisFeriepengebeløp = 0.0,
                    totaltFeriepengebeløp = 0.0,
                    differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                    hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
                )
            ), beregner.beregnFeriepenger(a1).first
        )
    }

    @Test
    fun `Prioriterer ikke personutbetalinger med kombinert arbeidskategorikode og orgnummer lik 0 i IT`() {
        a1.byggPerson(
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
                    a1,
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

        val beregner = feriepengerFor(Year.of(2018), historikk)
        assertEquals((17.januar(2018) til 23.mars(2018)).filterNot { it.erHelg() }, beregner.beregnFeriepenger(a1).second.datoer)
        assertEquals(
            Feriepengeberegningsresultat(
                orgnummer = a1,
                arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                    infotrygdFeriepengebeløp = 0.0,
                    spleisFeriepengebeløp = 7006.1759999999995,
                    totaltFeriepengebeløp = 7006.1759999999995,
                    differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 4864,
                    hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2142.0
                ),
                person = Feriepengeberegningsresultat.Beregningsverdier(
                    infotrygdFeriepengebeløp = 0.0,
                    spleisFeriepengebeløp = 0.0,
                    totaltFeriepengebeløp = 0.0,
                    differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                    hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
                )
            ), beregner.beregnFeriepenger(a1).first
        )
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 48 sammenhengende utbetalingsdager i Oppdrag fra første januar`() {
        a1.byggPerson(
            arbeidsgiverperiode = 16.desember(2017) til 31.desember(2017),
            syktil = 7.mars(2018)
        )

        val beregner = feriepengerFor(Year.of(2018), utbetalingshistorikkForFeriepenger())

        assertEquals(48, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 49 sammenhengende utbetalingsdager i Oppdrag fra første januar`() {
        a1.byggPerson(
            arbeidsgiverperiode = 16.desember(2017) til 31.desember(2017),
            syktil = 8.mars(2018)
        )

        val beregner = feriepengerFor(Year.of(2018), utbetalingshistorikkForFeriepenger())

        assertEquals(48, beregner.beregnFeriepenger(a1).second.datoer.size)

    }

    @Test
    fun `Finner datoer for feriepengeberegning med 47 sammenhengende utbetalingsdager i Oppdrag fra første januar`() {
        a1.byggPerson(
            arbeidsgiverperiode = 16.desember(2017) til 31.desember(2017),
            syktil = 6.mars(2018)
        )

        val beregner = feriepengerFor(Year.of(2018), utbetalingshistorikkForFeriepenger())

        assertEquals(47, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 48 sammenhengende utbetalingsdager i Oppdrag fra niende mai`() {
        a1.byggPerson(
            arbeidsgiverperiode = 23.april(2018) til 8.mai(2018),
            syktil = 13.juli(2018)
        )

        val beregner = feriepengerFor(Year.of(2018), utbetalingshistorikkForFeriepenger())

        assertEquals(48, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 47 ikke-sammenhengende utbetalingsdager i Oppdrag`() {
        a1.byggPerson(
            arbeidsgiverperiode = 16.desember(2017) til 31.desember(2017),
            syktil = 22.januar(2018)
        )
        a1.byggPerson(
            arbeidsgiverperiode = 1.mars(2018) til 16.mars(2018),
            syktil = 28.mars(2018),
            vedtaksperiodeIndeks = 2
        )
        a1.byggPerson(
            arbeidsgiverperiode = 1.mai(2018) til 16.mai(2018),
            syktil = 12.juni(2018),
            vedtaksperiodeIndeks = 3
        )
        a1.byggPerson(
            arbeidsgiverperiode = 1.juli(2018) til 16.juli(2018),
            syktil = 21.juli(2018),
            vedtaksperiodeIndeks = 4
        )

        val beregner = feriepengerFor(Year.of(2018), utbetalingshistorikkForFeriepenger())

        assertEquals(47, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 48 ikke-sammenhengende utbetalingsdager i Oppdrag`() {
        a1.byggPerson(
            arbeidsgiverperiode = 16.desember(2017) til 31.desember(2017),
            syktil = 22.januar(2018)
        )
        a1.byggPerson(
            arbeidsgiverperiode = 1.mars(2018) til 16.mars(2018),
            syktil = 28.mars(2018),
            vedtaksperiodeIndeks = 2
        )
        a1.byggPerson(
            arbeidsgiverperiode = 1.mai(2018) til 16.mai(2018),
            syktil = 12.juni(2018),
            vedtaksperiodeIndeks = 3
        )
        a1.byggPerson(
            arbeidsgiverperiode = 1.juli(2018) til 16.juli(2018),
            syktil = 23.juli(2018),
            vedtaksperiodeIndeks = 4
        )

        val beregner = feriepengerFor(Year.of(2018), utbetalingshistorikkForFeriepenger())

        assertEquals(48, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med to helt overlappende Oppdrag`() {
        byggPersonToParallelle(
            arbeidsgiverperiode = 23.april(2018) til 8.mai(2018),
            syktil = 13.juli(2018)
        )

        val beregner = feriepengerFor(Year.of(2018), utbetalingshistorikkForFeriepenger())

        assertEquals(48, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med to ikke-overlappende utbetalingstidslinjer`() {
        a1.byggPerson(
            arbeidsgiverperiode = 1.januar(2018) til 16.januar(2018),
            syktil = 15.februar(2018),
            a1
        )
        a2.byggPerson(
            arbeidsgiverperiode = 1.juli(2018) til 16.juli(2018),
            syktil = 15.august(2018),
            a2
        )

        val beregner = feriepengerFor(Year.of(2018), utbetalingshistorikkForFeriepenger())

        assertEquals(44, beregner.beregnFeriepenger(a1).second.datoer.size)
    }

    @Test
    fun `Teller ikke med dager fra annullerte utbetalinger i feriepengeberegneren`() {
        byggPersonMedAnnullering()
        val beregner = feriepengerFor(Year.of(2018), utbetalingshistorikkForFeriepenger())
        assertEquals(0, beregner.beregnFeriepenger(a1).second.datoer.size)
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
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            utbetalinger = utbetalinger,
            feriepengehistorikk = emptyList(),
            arbeidskategorikoder = arbeidskategorikoder,
            opptjeningsår = Year.of(2020),
            skalBeregnesManuelt = skalBeregnesManuelt,
            datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2025)
        )

    private fun String.byggPerson(
        arbeidsgiverperiode: Periode = 1.januar til 16.januar,
        syktil: LocalDate = 31.januar,
        vararg orgnumre: String = arrayOf(a1),
        vedtaksperiodeIndeks: Int = 1
    ) {
        this {
            håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil))
            håndterSøknad(
                Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent),
            )
            håndterArbeidsgiveropplysninger(
                listOf(arbeidsgiverperiode),
                vedtaksperiodeId = vedtaksperiodeIndeks.vedtaksperiode
            )
            val sisteVedtaksperiode = sisteVedtaksperiode
            håndterVilkårsgrunnlagFlereArbeidsgivere(sisteVedtaksperiode, *orgnumre)
            håndterYtelser(sisteVedtaksperiode)
            håndterSimulering(sisteVedtaksperiode)
            håndterUtbetalingsgodkjenning(sisteVedtaksperiode)
            håndterUtbetalt()
        }
    }

    private fun byggPersonMedAnnullering(
        arbeidsgiverperiode: Periode = 1.januar til 16.januar,
        syktil: LocalDate = 31.januar,
        orgnummer: String = a1
    ) {
        orgnummer {
            håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil), orgnummer = orgnummer)
            håndterSøknad(
                Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent),
                orgnummer = orgnummer
            )
            håndterArbeidsgiveropplysninger(
                listOf(arbeidsgiverperiode),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            val sisteVedtaksperiode = sisteVedtaksperiode
            håndterVilkårsgrunnlag(sisteVedtaksperiode, orgnummer = orgnummer)
            håndterYtelser(sisteVedtaksperiode, orgnummer = orgnummer)
            håndterSimulering(sisteVedtaksperiode, orgnummer = orgnummer)
            håndterUtbetalingsgodkjenning(sisteVedtaksperiode, orgnummer = orgnummer)
            håndterUtbetalt(orgnummer = orgnummer)
            håndterOverstyrTidslinje(arbeidsgiverperiode.oppdaterTom(syktil).map {
                ManuellOverskrivingDag(it, Dagtype.Feriedag)
            })
            håndterYtelser(sisteVedtaksperiode, orgnummer = orgnummer)
            assertVarsel(Varselkode.RV_UT_23, sisteVedtaksperiode.filter())
            håndterSimulering(sisteVedtaksperiode, orgnummer = orgnummer)
            håndterUtbetalingsgodkjenning(sisteVedtaksperiode, orgnummer = orgnummer)
            håndterUtbetalt(orgnummer = orgnummer)

        }
    }

    private fun byggPersonToParallelle(
        arbeidsgiverperiode: Periode = 1.januar til 16.januar,
        syktil: LocalDate = 31.januar,
    ) {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil))

        }
        a2 {

            håndterSykmelding(Sykmeldingsperiode(arbeidsgiverperiode.start, syktil))
        }
        a1 {
            håndterSøknad(Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent))

        }
        a2 {
            håndterSøknad(Sykdom(arbeidsgiverperiode.start, syktil, 100.prosent))

        }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(arbeidsgiverperiode),
                vedtaksperiodeId = 1.vedtaksperiode
            )

        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(arbeidsgiverperiode),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

        }
    }
}
