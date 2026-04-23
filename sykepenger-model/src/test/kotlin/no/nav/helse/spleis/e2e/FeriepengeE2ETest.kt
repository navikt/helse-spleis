package no.nav.helse.spleis.e2e

import java.time.Year
import kotlin.math.roundToInt
import no.nav.helse.EnableFeriepenger
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Behovsoppsamler
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.feriepenger.Feriepengerendringskode
import no.nav.helse.feriepenger.Feriepengerklassekode
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Personutbetalingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør.Feriepengeoppdrag.Companion.utbetalingslinjer
import no.nav.helse.inspectors.TestArbeidsgiverInspektør.Feriepengeutbetalingslinje
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.september
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

@EnableFeriepenger
@Isolated
internal class FeriepengeE2ETest : AbstractDslTest() {

    @Test
    fun `person som har fått utbetalt direkte`() {
        a1 {
            nyttVedtak(1.januar(2022) til 31.januar(2022), refusjon = Inntektsmelding.Refusjon(INGEN, null))
            inspektør.utbetaling(0).let { utbetalingInspektør ->
                assertEquals(0, utbetalingInspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalingInspektør.personOppdrag.size)
            }
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2022),
                datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2023)
            )
            assertEquals(1605.5819999999999, inspektør.spleisFeriepengebeløpPerson.first())
            assertEquals(0.0, inspektør.spleisFeriepengebeløpArbeidsgiver.first())
        }
    }

    @Test
    fun `person som har fått revurdert en utbetalt periode med ferie`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrTidslinje(januar.map { ManuellOverskrivingDag(it, Dagtype.Feriedag) })
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            inspektør.utbetaling(1).let { utbetalingInspektør ->
                assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
                assertEquals(0, utbetalingInspektør.personOppdrag.size)
                utbetalingInspektør.arbeidsgiverOppdrag.single().also { linje ->
                    assertEquals(17.januar til 31.januar, linje.periode)
                    assertEquals(1431, linje.beløp)
                    assertEquals(17.januar, linje.datoStatusFom)
                }
            }
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2018),
                datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2019)
            )
            assertEquals(0.0, inspektør.spleisFeriepengebeløpPerson.first())
            assertEquals(0.0, inspektør.spleisFeriepengebeløpArbeidsgiver.first())
        }
    }

    @Test
    fun `person som har både refusjon og direkte utbetaling`() {
        a1 {
            nyttVedtak(1.januar(2022) til 31.januar(2022), refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null))
            inspektør.utbetaling(0).let { utbetalingInspektør ->
                assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalingInspektør.personOppdrag.size)
            }
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2022),
                datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2023)
            )
            assertEquals(802.2299999999999, inspektør.spleisFeriepengebeløpPerson.first())
            assertEquals(802.2299999999999, inspektør.spleisFeriepengebeløpArbeidsgiver.first())
            assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())
            assertEquals(0.0, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
        }
    }

    @Test
    fun `person som har både litt fra infotrygd og litt fra spleis`() {
        a1 {
            nyttVedtak(1.januar(2022) til 31.januar(2022), refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null))
            inspektør.utbetaling(0).let { utbetalingInspektør ->
                assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalingInspektør.personOppdrag.size)
            }
        }
        val dagsatsIT = (INNTEKT / 2).dagligInt
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2022),
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(a1, 17.mars(2022), 31.mars(2022), dagsatsIT, 31.mars(2022)),
                Personutbetalingsperiode(a1, 17.mars(2022), 31.mars(2022), dagsatsIT, 31.mars(2022))
            ),
            feriepengehistorikk = listOf(
                UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 802, 1.mai(2023), 31.mai(2023)),
                UtbetalingshistorikkForFeriepenger.Feriepenger("0", 802, 1.mai(2023), 31.mai(2023))
            ),
            datoForSisteFeriepengekjøringIInfotrygd = 31.mai(2023)
        )
        a1 {
            assertEquals(802.2299999999999, inspektør.spleisFeriepengebeløpPerson.first())
            assertEquals(802.2299999999999, inspektør.spleisFeriepengebeløpArbeidsgiver.first())
            assertEquals(802.2299999999999, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
            assertEquals(802.2299999999999, inspektør.infotrygdFeriepengebeløpPerson.first())
        }
    }

    @Test
    fun `person som har både litt fra infotrygd og litt fra spleis med forskjellig refusjon`() {
        a1 {
            nyttVedtak(1.januar(2022) til 31.januar(2022), refusjon = Inntektsmelding.Refusjon(INNTEKT / 3, null))
            inspektør.utbetaling(0).let { utbetalingInspektør ->
                assertEquals(1, utbetalingInspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalingInspektør.personOppdrag.size)
            }
        }
        val dagsatsIT = (INNTEKT / 2).dagligInt
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2022),
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(a1, 17.mars(2022), 31.desember(2022), dagsatsIT, 31.mars(2022)),
                Personutbetalingsperiode(a1, 17.mars(2022), 31.desember(2022), dagsatsIT, 31.mars(2022))
            ),
            datoForSisteFeriepengekjøringIInfotrygd = 1.april(2022)
        )
        a1 {
            assertEquals(1070.388, inspektør.spleisFeriepengebeløpPerson.first())
            assertEquals(535.194, inspektør.spleisFeriepengebeløpArbeidsgiver.first())
            assertEquals(2698.41, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
            assertEquals(2698.41, inspektør.infotrygdFeriepengebeløpPerson.first())

            val utbetalingslinjer = listOf(
                Feriepengeutbetalingslinje(
                    fom = 1.mai(2023), tom = 31.mai(2023), beløp = -267,
                    klassekode = Feriepengerklassekode.RefusjonFeriepengerIkkeOpplysningspliktig,
                    endringskode = Feriepengerendringskode.NY
                ), Feriepengeutbetalingslinje(
                    fom = 1.mai(2023), tom = 31.mai(2023), beløp = 268,
                    klassekode = Feriepengerklassekode.SykepengerArbeidstakerFeriepenger,
                    endringskode = Feriepengerendringskode.NY
                )
            )
            assertEquals(utbetalingslinjer, inspektør.feriepengeoppdrag.utbetalingslinjer)
        }
    }

    @Test
    fun `Infotrygd har betalt ut 48 dager til person - Spleis har utbetalt 48 i forkant`() {
        a1 {
            nyttVedtak(1.januar(2022) til 31.mars(2022), refusjon = Inntektsmelding.Refusjon(INGEN, null))
            val dagsatsIT = 1574
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2022),
                utbetalinger = listOf(Personutbetalingsperiode(a1, 1.august(2022), 31.oktober(2022), dagsatsIT, 31.mars(2022))),
                datoForSisteFeriepengekjøringIInfotrygd = 31.mars(2023)
            )
            assertEquals(0.0, inspektør.spleisFeriepengebeløpArbeidsgiver.first())
            assertEquals(0.0, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
            assertEquals(7006.1759999999995, inspektør.spleisFeriepengebeløpPerson.first())
            assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())

            val utbetalingslinje = listOf(
                Feriepengeutbetalingslinje(
                    fom = 1.mai(2023),
                    tom = 31.mai(2023),
                    beløp = -700,
                    klassekode = Feriepengerklassekode.SykepengerArbeidstakerFeriepenger,
                    endringskode = Feriepengerendringskode.NY
                )
            )
            assertEquals(utbetalingslinje, inspektør.feriepengeoppdrag.utbetalingslinjer)
        }
    }

    @Test
    fun `Spleis utbetaler feriepenger til person, blir annullert i Spleis mellom første og andre kjøring`() {
        a1 {
            nyttVedtak(1.januar(2022) til 31.mars(2022), refusjon = Inntektsmelding.Refusjon(INGEN, null))
            val dagsatsIT = 1574

            // Første kjøring
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2022),
                utbetalinger = listOf(Personutbetalingsperiode(a1, 1.august(2022), 31.oktober(2022), dagsatsIT, 31.mars(2022))),
                datoForSisteFeriepengekjøringIInfotrygd = 1.april(2022)
            )
            assertEquals(7006.1759999999995, inspektør.spleisFeriepengebeløpPerson.first())
            assertEquals(listOf(
                Feriepengeutbetalingslinje(1.mai(2023), 31.mai(2023), -700, Feriepengerklassekode.SykepengerArbeidstakerFeriepenger, Feriepengerendringskode.NY)
            ), inspektør.feriepengeoppdrag.utbetalingslinjer)

            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            // Andre kjøring ❤️
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2022),
                utbetalinger = listOf(
                    Personutbetalingsperiode(a1, 17.januar(2022), 31.mars(2022), dagsatsIT, 31.mars(2022)),
                    Personutbetalingsperiode(a1, 1.august(2022), 31.oktober(2022), dagsatsIT, 31.oktober(2022))
                ),
                datoForSisteFeriepengekjøringIInfotrygd = 1.november(2022)
            )
            assertEquals(listOf(
                Feriepengeutbetalingslinje(1.mai(2023), 31.mai(2023), -700, Feriepengerklassekode.SykepengerArbeidstakerFeriepenger, Feriepengerendringskode.ENDR, statuskode = "OPPH")
            ), inspektør.feriepengeoppdrag.utbetalingslinjer)
        }
    }

    @Test
    fun `serialiserer og deserialiserer Spleis feriepengebeløp for person`() {
        a1 {
            nyttVedtak(1.januar(2022) til 31.januar(2022), refusjon = Inntektsmelding.Refusjon(INGEN, null))
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2022),
                datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2023)
            )
            assertEquals(1605.5819999999999, inspektør.spleisFeriepengebeløpPerson.first())
        }
        assertGjenoppbygget(dto())
        a1 {
            assertEquals(1605.5819999999999, inspektør.spleisFeriepengebeløpPerson.first())
        }
    }

    private fun standardOppsett2020() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020)))
            håndterSøknad(1.juni(2020) til 30.juni(2020))
            håndterArbeidsgiveropplysninger(listOf(1.juni(2020) til 16.juni(2020)), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
    }

    @Test
    fun `Beregner feriepenger korrekt for enkel spleisperiode uten infotrygdhistorikk`() {
        standardOppsett2020()
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2021)
        )
        a1 {
            assertEquals(0.0, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
            assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())
            assertEquals(1431 * 10 * 0.102, inspektør.spleisFeriepengebeløpArbeidsgiver.first())

            assertEquals(
                Feriepengeutbetalingslinje(1.mai(2021), 31.mai(2021), 1460, Feriepengerklassekode.RefusjonFeriepengerIkkeOpplysningspliktig, Feriepengerendringskode.NY),
                inspektør.feriepengeoppdrag.first().feriepengeutbetalingslinjer.first()
            )
        }
    }

    @Test
    fun `Beregner feriepenger korrekt for enkel spleisperiode med en utbetaling i infotrygd`() {
        standardOppsett2020()
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 1.mars(2020), 31.mars(2020), 1431, 31.mars(2020))),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 3211, 1.mai(2021), 31.mai(2021))),
            datoForSisteFeriepengekjøringIInfotrygd = 1.april(2020)
        )
        a1 {
            assertEquals(1431 * 22 * 0.102, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
            assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())
            assertEquals(1431 * 10 * 0.102, inspektør.spleisFeriepengebeløpArbeidsgiver.first())

            assertEquals(
                Feriepengeutbetalingslinje(1.mai(2021), 31.mai(2021), 1460, Feriepengerklassekode.RefusjonFeriepengerIkkeOpplysningspliktig, Feriepengerendringskode.NY),
                inspektør.feriepengeoppdrag.first().feriepengeutbetalingslinjer.first()
            )
        }
    }

    @Test
    fun `Legger ikke infotrygdcache til grunn for feriepenger`() {
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar(2020), 31.januar(2020)))
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020)))
            håndterSøknad(1.juni(2020) til 30.juni(2020))
            håndterArbeidsgiveropplysninger(listOf(1.juni(2020) til 16.juni(2020)), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 1.januar(2020), 31.januar(2020), 1431, 31.januar(2020))),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 3357, 1.mai(2021), 31.mai(2021))),
            datoForSisteFeriepengekjøringIInfotrygd = 1.februar(2020)
        )
        a1 {
            assertEquals(1431 * 23 * 0.102, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
            assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())
            assertEquals(1431 * 10 * 0.102, inspektør.spleisFeriepengebeløpArbeidsgiver.first())

            assertEquals(
                Feriepengeutbetalingslinje(1.mai(2021), 31.mai(2021), 1460, Feriepengerklassekode.RefusjonFeriepengerIkkeOpplysningspliktig, Feriepengerendringskode.NY),
                inspektør.feriepengeoppdrag.first().feriepengeutbetalingslinjer.first()
            )
        }
    }

    @Test
    fun `Beregner ikke feriepenger for personer markert for manuell beregning av feriepenger`() {
        standardOppsett2020()
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2021),
            skalBeregnesManuelt = true
        )

        assertEquals(0, a1 { inspektør.feriepengeoppdrag.size })
        assertTrue(testperson.personlogg.toString().contains("Person er markert for manuell beregning av feriepenger"))
    }

    @Test
    fun `Sender ikke to utbetalingsbehov om feriepengereberegningen er lik den forrige`() {
        standardOppsett2020()
        fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020),
                utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 1.november(2020), 30.november(2020), 1000, 1.desember(2020))),
                feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 2142, 1.mai(2021), 31.mai(2021))),
                datoForSisteFeriepengekjøringIInfotrygd = 1.desember(2020)
            )
        }

        assertEquals(2, a1 { inspektør.feriepengeoppdrag.size })

        assertIngenFeriepengebehov {
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020),
                utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 1.november(2020), 30.november(2020), 1000, 1.desember(2020))),
                feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 2142, 1.mai(2021), 31.mai(2021))),
                datoForSisteFeriepengekjøringIInfotrygd = 1.desember(2020)
            )
        }

        assertEquals(4, a1 { inspektør.feriepengeoppdrag.size })
    }

    @Test
    fun `Korrigerer en ukjent arbeidsgiver hvis feriepengene er brukt opp i spleis`() {
        val ORGNUMMER2 = "978654321"
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.august(2020)))
            håndterSøknad(1.juni(2020) til 30.august(2020))
            håndterArbeidsgiveropplysninger(listOf(1.juni(2020) til 16.juni(2020)), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(ORGNUMMER2, 1.november(2020), 30.november(2020), 1000, 1.desember(2020))),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER2, 2142, 1.mai(2021), 31.mai(2021))),
            datoForSisteFeriepengekjøringIInfotrygd = 2.desember(2020)
        )

        assertEquals(2, a1 { inspektør.feriepengeoppdrag.size })
        assertEquals(2, ORGNUMMER2.inspektør.feriepengeoppdrag.size)
        assertEquals(7006, a1 { inspektør.feriepengeoppdrag.first().feriepengeutbetalingslinjer.first().beløp })
        assertEquals(-2142, ORGNUMMER2.inspektør.feriepengeoppdrag.first().feriepengeutbetalingslinjer.first().beløp)
    }

    @Test
    fun `Ghost arbeidsgiver fra feriepengeberegner påvirker ikke senere sykepengeberegning`() {
        val ORGNUMMER2 = "978654321"
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(ORGNUMMER2, 1.november(2020), 30.november(2020), 1000, 1.desember(2020))),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER2, 2142, 1.mai(2021), 31.mai(2021))),
            datoForSisteFeriepengekjøringIInfotrygd = 1.desember(2020)
        )

        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.august(2020)))
            håndterSøknad(1.juni(2020) til 30.august(2020))
            håndterArbeidsgiveropplysninger(listOf(1.juni(2020) til 16.juni(2020)), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        assertEquals(0, a1 { inspektør.feriepengeoppdrag.size })
        assertEquals(2, ORGNUMMER2.inspektør.feriepengeoppdrag.size)
        assertTrue(ORGNUMMER2.inspektør.feriepengeoppdrag.first().feriepengeutbetalingslinjer.isEmpty())
    }

    @Test
    fun `Validerer at beregnet feriepengebeløp for IT finnes i lista over utbetalte feriepenger`() {
        standardOppsett2020()
        fangLoggmeldinger("Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp") {
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020),
                utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 1.januar(2020), 31.januar(2020), 1431, 31.januar(2020))),
                feriepengehistorikk = listOf(
                    UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 3357, 1.mai(2021), 31.mai(2021)),
                    UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 4000, 1.mai(2021), 31.mai(2021))
                ),
                datoForSisteFeriepengekjøringIInfotrygd = 31.januar(2020)
            )
        }.also { assertTrue(it.isEmpty()) }
        assertEquals(2, a1 { inspektør.feriepengeoppdrag.size })
    }

    @Test
    fun `Validering feiler hvis beregnet feriepengebeløp for IT ikke finnes i lista over utbetalte feriepenger`() {
        standardOppsett2020()
        fangLoggmeldinger("Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp") {
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020),
                utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 1.januar(2020), 31.januar(2020), 1431, 31.januar(2020))),
                feriepengehistorikk = listOf(
                    UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 3356, 1.mai(2021), 31.mai(2021)),
                    UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 4000, 1.mai(2021), 31.mai(2021))
                ),
                datoForSisteFeriepengekjøringIInfotrygd = 1.februar(2020)
            )
        }.also { assertTrue(it.isNotEmpty()) }
        assertEquals(2, a1 { inspektør.feriepengeoppdrag.size })
    }

    @Test
    fun `Validerer ikke utbetalte feriepenger hvis beregnet feriepengebeløp for IT er 0`() {
        standardOppsett2020()
        fangLoggmeldinger("Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp") {
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020),
                feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 4000, 1.mai(2021), 31.mai(2021))),
                datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2022)
            )
        }.also { assertTrue(it.isEmpty()) }
        assertEquals(2, a1 { inspektør.feriepengeoppdrag.size })
    }

    @Test
    fun `Utbetaling av feriepenger sender behov til oppdrag`() {
        standardOppsett2020()
        val behov = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020),
                datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2021)
            )
        }

        assertTrue(testperson.personlogg.toString().contains("Trenger å sende utbetaling til Oppdrag"))

        with(behov) {
            assertEquals("SPLEIS", event.saksbehandler)
            assertEquals("ENG", event.linje.satstype)
            assertEquals("SPREFAGFER-IOP", event.linje.klassekode)
        }
    }

    @Test
    fun `Sender ut events etter mottak av kvittering fra oppdrag`() {
        standardOppsett2020()
        val feriepengebehov = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020),
                datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2021)
            )
        }

        val fagsystemIdFeriepenger = feriepengebehov.event.fagsystemId
        håndterFeriepengerUtbetalt(fagsystemId = fagsystemIdFeriepenger)

        assertTrue(testperson.personlogg.toString().contains("Data for feriepenger fra Oppdrag/UR"))
        assertTrue(testperson.personlogg.toString().contains("utbetalt ok: ja"))

        observatør.feriepengerUtbetaltEventer.first().let { event ->
            assertEquals(fagsystemIdFeriepenger, event.arbeidsgiverOppdrag.fagsystemId)
            assertEquals("2021-05-01", event.fom.toString())
            assertEquals("2021-05-31", event.tom.toString())
            assertEquals("1460", event.arbeidsgiverOppdrag.totalbeløp.toString())
        }
    }

    @Test
    fun `Sender ut events kun for oppdrag med relevant utbetalingId etter mottak av kvittering`() {
        standardOppsett2020()
        val fagsystemIdFeriepenger = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020),
                datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2021)
            )
        }.event.fagsystemId

        håndterFeriepengerUtbetalt(fagsystemId = fagsystemIdFeriepenger)

        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.juli(2020), 10.juli(2020)))
            håndterSøknad(1.juli(2020) til 10.juli(2020))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2021)
        )

        håndterFeriepengerUtbetalt(fagsystemId = fagsystemIdFeriepenger)

        assertTrue(testperson.personlogg.toString().contains("Data for feriepenger fra Oppdrag/UR"))
        assertTrue(testperson.personlogg.toString().contains("utbetalt ok: ja"))

        assertEquals(2, observatør.feriepengerUtbetaltEventer.size)

        observatør.feriepengerUtbetaltEventer.first().let { event ->
            assertEquals(fagsystemIdFeriepenger, event.arbeidsgiverOppdrag.fagsystemId)
            assertEquals("2021-05-01", event.fom.toString())
            assertEquals("2021-05-31", event.tom.toString())
            assertEquals("1460", event.arbeidsgiverOppdrag.totalbeløp.toString())
        }
        observatør.feriepengerUtbetaltEventer.last().let { event ->
            assertEquals(fagsystemIdFeriepenger, event.arbeidsgiverOppdrag.fagsystemId)
            assertEquals("2021-05-01", event.fom.toString())
            assertEquals("2021-05-31", event.tom.toString())
            assertEquals("2627", event.arbeidsgiverOppdrag.totalbeløp.toString())
        }
    }

    @Test
    fun `Sender ikke behov når det ikke er noen diff i IT og spleis sine beregninger av feriepenger`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(6.juni(2020), 7.juni(2020)))
            håndterSøknad(6.juni(2020) til 7.juni(2020))
            håndterInntektsmelding(listOf(6.juni(2020) til 7.juni(2020)))
        }
        assertIngenFeriepengebehov {
            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020),
                datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2021)
            )
        }
    }

    private fun standardOppsettLangPeriode2020() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 14.august(2020)))
            håndterSøknad(1.juni(2020) til 14.august(2020)) // 43 dager
            håndterArbeidsgiveropplysninger(listOf(1.juni(2020) til 16.juni(2020)), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
    }

    @Test
    fun `reberegning av feriepenger med endringer`() {
        standardOppsett2020()
        fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2021))
        }
        assertIngenFeriepengebehov {
            håndterUtbetalingshistorikkForFeriepenger(
                utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 20.januar(2020), 31.januar(2020), 690, 30.juni(2020))),
                feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
                opptjeningsår = Year.of(2020),
                datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020)
            )
        }
    }

    @Test
    fun `reberegning av feriepenger med endringer hvor totalt utbetalte dager går over 48`() {
        standardOppsettLangPeriode2020()

        val førsteBehovOmFeriepenger = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 10.mai(2021))
        }
        val fagsystemId = førsteBehovOmFeriepenger.event.fagsystemId

        val andreBehovOmFeriepenger = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(
                utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 20.januar(2020), 31.januar(2020), 690, 30.juni(2020))), // 10 dager
                feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
                opptjeningsår = Year.of(2020),
                datoForSisteFeriepengekjøringIInfotrygd = 1.juli(2020)
            )
        }

        assertEquals(2, setOf(førsteBehovOmFeriepenger, andreBehovOmFeriepenger).size)

        assertEquals((38 * DAGSINNTEKT * 0.102).roundToInt(), andreBehovOmFeriepenger.event.linje.sats)
        assertEquals(fagsystemId, andreBehovOmFeriepenger.event.fagsystemId)
        assertEquals("ENDR", andreBehovOmFeriepenger.event.endringskode)
        assertEquals("NY", andreBehovOmFeriepenger.event.linje.endringskode)
        assertEquals(førsteBehovOmFeriepenger.event.linje.delytelseId, andreBehovOmFeriepenger.event.linje.refDelytelseId)
        assertEquals(førsteBehovOmFeriepenger.event.linje.klassekode, andreBehovOmFeriepenger.event.linje.klassekode)
    }

    @Test
    fun `reberegning av feriepenger med endringer hvor første ikke blir sendt`() {
        standardOppsettLangPeriode2020()

        assertIngenFeriepengebehov {
            håndterUtbetalingshistorikkForFeriepenger(
                utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 1.januar(2020), 8.mars(2020), 690, 30.juni(2020))), // 10 dager
                feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
                opptjeningsår = Year.of(2020),
                datoForSisteFeriepengekjøringIInfotrygd = 1.juli(2020)
            )
        }

        val behov2 = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(
                utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 20.januar(2020), 31.januar(2020), 690, 30.juni(2020))), // 10 dager
                feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
                opptjeningsår = Year.of(2020),
                datoForSisteFeriepengekjøringIInfotrygd = 1.juli(2020)
            )
        }

        assertEquals((38 * DAGSINNTEKT * 0.102).roundToInt(), behov2.event.linje.sats)
        assertEquals("NY", behov2.event.endringskode)
        assertEquals("NY", behov2.event.linje.endringskode)
    }

    @Test
    fun `Kobler ny utbetaling til det forrige sendte oppdraget`() {
        standardOppsettLangPeriode2020()
        val behov1 = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        }
        val fagsystemId = behov1.event.fagsystemId

        assertIngenFeriepengebehov {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        }

        assertEquals(4, a1 { inspektør.feriepengeoppdrag.size })

        val behov3 = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(
                utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 20.januar(2020), 31.januar(2020), 690, 30.juni(2020))), // 10 dager
                feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
                opptjeningsår = Year.of(2020),
                datoForSisteFeriepengekjøringIInfotrygd = 1.juli(2020)
            )
        }

        assertEquals(6, a1 { inspektør.feriepengeoppdrag.size })
        assertEquals((38 * DAGSINNTEKT * 0.102).roundToInt(), behov3.event.linje.sats)
        assertEquals(fagsystemId, behov3.event.fagsystemId)
        assertEquals("ENDR", behov3.event.endringskode)
        assertEquals("NY", behov3.event.linje.endringskode)
        assertEquals(behov1.event.linje.delytelseId, behov3.event.linje.refDelytelseId)
    }

    @Test
    fun `toggle av føkekr ikke shit`() {
        standardOppsettLangPeriode2020()
        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(Arbeidsgiverutbetalingsperiode(a1, 20.januar(2020), 31.januar(2020), 690, 30.juni(2020))), // 10 dager
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
            opptjeningsår = Year.of(2020),
            datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020)
        )
    }

    @Test
    fun `Reberegning som ender med 0 i totalsum sender opphør`() {
        standardOppsettLangPeriode2020()
        val behov1 = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        }
        val fagsystemId = behov1.event.fagsystemId

        a1 {
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
        }

        val behov2 = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        }
        assertEquals(4, a1 { inspektør.feriepengeoppdrag.size })
        assertEquals((43 * DAGSINNTEKT * 0.102).roundToInt(), behov2.event.linje.sats)
        assertEquals(1.mai(2021), behov2.event.linje.datoStatusFom)
        assertEquals(fagsystemId, behov2.event.fagsystemId)
        assertEquals(behov1.event.linje.klassekode, behov2.event.linje.klassekode)
    }

    @Test
    fun `Rekjøring etter annullert oppdrag skal ikke sende feriepenger ved beløp 0`() {
        standardOppsettLangPeriode2020()
        val behov1 = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        }
        val fagsystemId1FraBehov = behov1.event.fagsystemId

        a1 {
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
        }

        val behov2 = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        }
        assertIngenFeriepengebehov {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        }

        a1 {
            assertEquals(6, inspektør.feriepengeoppdrag.size)

            val fagsystemId1 = inspektør.feriepengeoppdrag[0].fagsystemId
            val fagsystemId2 = inspektør.feriepengeoppdrag[2].fagsystemId
            val fagsystemId3 = inspektør.feriepengeoppdrag[4].fagsystemId
            assertEquals(fagsystemId1, fagsystemId2)
            assertEquals(fagsystemId1, fagsystemId3)

            assertEquals(fagsystemId1, fagsystemId1FraBehov)
            assertEquals(fagsystemId1FraBehov, behov2.event.fagsystemId)
        }
    }

    @Test
    fun `Rekjøring etter annullert oppdrag skal sende feriepenger med ny fagsystemId`() {
        standardOppsettLangPeriode2020()
        val behov1 = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        }
        val fagsystemId1FraBehov = behov1.event.fagsystemId

        a1 {
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
        }

        val behov2 = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        }

        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.oktober(2020), 14.desember(2020)))
            håndterSøknad(1.oktober(2020) til 14.desember(2020)) // 41 dager
            håndterArbeidsgiveropplysninger(listOf(1.oktober(2020) til 16.oktober(2020)), vedtaksperiodeId = 2.vedtaksperiode)
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }

        val behov3 = fangDetEnesteFeriepengebehovet {
            håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020), datoForSisteFeriepengekjøringIInfotrygd = 30.juni(2020))
        }

        a1 {
            assertEquals(6, inspektør.feriepengeoppdrag.size)

            val fagsystemId1 = inspektør.feriepengeoppdrag[0].fagsystemId
            val fagsystemId2 = inspektør.feriepengeoppdrag[2].fagsystemId
            val fagsystemId3 = inspektør.feriepengeoppdrag[4].fagsystemId

            assertEquals(fagsystemId1, fagsystemId2)
            assertEquals(fagsystemId1, fagsystemId3)

            val fagsystemId2FraBehov = behov2.event.fagsystemId
            assertEquals(fagsystemId1, fagsystemId1FraBehov)
            assertEquals(fagsystemId1FraBehov, fagsystemId2FraBehov)

            assertEquals((41 * DAGSINNTEKT * 0.102).roundToInt(), behov3.event.linje.sats)
            assertNull(behov3.event.linje.datoStatusFom)
            assertEquals(fagsystemId3, behov3.event.fagsystemId)
            assertEquals(behov1.event.linje.klassekode, behov3.event.linje.klassekode)
        }
    }

    @Test
    fun `Feriepengeutbetaling til person`() {
        standardOppsettLangPeriode2020()
        fangLoggmeldinger("Differanse mellom det IT har utbetalt og det spleis har beregnet at IT skulle betale") {
            val behovene = fangAlleFeriepengebehovene {
                håndterUtbetalingshistorikkForFeriepenger(
                    opptjeningsår = Year.of(2020),
                    utbetalinger = listOf(Personutbetalingsperiode(a1, 1.september(2020), 15.september(2020), 1172, 20.september(2020))),
                    feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 3211, 1.mai(2021), 31.mai(2021))),
                    datoForSisteFeriepengekjøringIInfotrygd = 21.september(2020)
                )
            }
            assertEquals(2, behovene.size)
            with(behovene.first()) {
                assertEquals("SPREF", event.fagområde)
                assertEquals("SPREFAGFER-IOP", event.linje.klassekode)
            }

            with(behovene.last()) {
                assertEquals(0 - (6 * 1172 * 0.102).roundToInt(), event.linje.sats)
                assertNull(event.linje.datoStatusFom)
                assertEquals("SP", event.fagområde)
                assertEquals("SPATFER", event.linje.klassekode)
            }
        }.also { assertTrue(it.isNotEmpty()) }

    }

    @Test
    fun `Personutbetaling 48 dager i IT, spleis skal ikke betale noe`() {
        standardOppsettLangPeriode2020()
        fangLoggmeldinger("Differanse mellom det IT har utbetalt og det spleis har beregnet at IT skulle betale") {
            assertIngenFeriepengebehov {
                håndterUtbetalingshistorikkForFeriepenger(
                    opptjeningsår = Year.of(2020),
                    utbetalinger = listOf(Personutbetalingsperiode(a1, 1.januar(2020), 6.mars(2020), 1172, 20.mars(2020))),
                    feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(a1, 5738, 1.mai(2021), 31.mai(2021))),
                    datoForSisteFeriepengekjøringIInfotrygd = 21.mars(2020)
                )
            }
        }.also { assertTrue(it.isEmpty()) }
    }

    @Test
    fun `Feriepengeutbetaling til orgnummer 0`() {
        standardOppsettLangPeriode2020()
        fangLoggmeldinger("Differanse mellom det IT har utbetalt og det spleis har beregnet at IT skulle betale") {
            val behovene = fangAlleFeriepengebehovene {
                håndterUtbetalingshistorikkForFeriepenger(
                    opptjeningsår = Year.of(2020),
                    utbetalinger = listOf(Personutbetalingsperiode("0", 1.september(2020), 15.september(2020), 1172, 20.september(2020))),
                    feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger("0", 3211, 1.mai(2021), 31.mai(2021))),
                    datoForSisteFeriepengekjøringIInfotrygd = 21.september(2020)
                )
            }
            assertEquals(2, behovene.size)
            assertEquals(a1, behovene.first().event.mottaker)
            val tilOrgnr0 = behovene.last()
            assertEquals("12029240045", tilOrgnr0.event.mottaker)

            assertEquals(0 - (6 * 1172 * 0.102).roundToInt(), tilOrgnr0.event.linje.sats)
            assertNull(tilOrgnr0.event.linje.datoStatusFom)
            assertEquals("SP", tilOrgnr0.event.fagområde)
            assertEquals("SPATFER", tilOrgnr0.event.linje.klassekode)


        }.also { assertTrue(it.isNotEmpty()) }
    }

    @Test
    fun `Test av sanity-logging`() {
        standardOppsettLangPeriode2020()
        fangLoggmeldinger("Forventer ikke arbeidsgiveroppdrag til orgnummer \"0\"") {
            val behovene = fangAlleFeriepengebehovene {
                håndterUtbetalingshistorikkForFeriepenger(
                    opptjeningsår = Year.of(2020),
                    utbetalinger = listOf(
                        // Ikke funksjonelt gyldig med refusjon til orgnr 0
                        Arbeidsgiverutbetalingsperiode("0", 1.september(2020), 15.september(2020), 1172, 20.september(2020)),
                    ),
                    feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger("0", 3211, 1.mai(2021), 31.mai(2021))),
                    datoForSisteFeriepengekjøringIInfotrygd = 21.september(2020)
                )
            }
            assertEquals(2, behovene.size)
            assertEquals(a1, behovene.first().event.mottaker)
            val ugyldigBehovTilOrgnr0 = behovene.last()
            assertEquals("0", ugyldigBehovTilOrgnr0.event.mottaker)

            assertEquals(0 - (6 * 1172 * 0.102).roundToInt(), ugyldigBehovTilOrgnr0.event.linje.sats)
            assertNull(ugyldigBehovTilOrgnr0.event.linje.datoStatusFom)
            assertEquals("SPREF", ugyldigBehovTilOrgnr0.event.fagområde)
        }.also { assertTrue(it.isNotEmpty()) }
    }

    private fun fangAlleFeriepengebehovene(block: () -> Unit): Set<Behovsoppsamler.Behovsdetaljer.Feriepengeutbetaling> = behovSomOppstårSomFølgeAv<Behovsoppsamler.Behovsdetaljer.Feriepengeutbetaling> { block() }

    private fun assertIngenFeriepengebehov(block: () -> Unit) = fangAlleFeriepengebehovene(block).let {
        assertEquals(0, it.size) { "Forventet ingen behov for feriepenger, var ${it.size}" }
    }

    private fun fangDetEnesteFeriepengebehovet(block: () -> Unit) = fangAlleFeriepengebehovene(block).let {
        assertEquals(1, it.size)  { "Forventet nøyaktig et behov for feriepenger, var ${it.size}" }
        it.single()
    }

    private fun fangLoggmeldinger(vararg filter: String, block: () -> Any): List<String> {
        block()
        val etter = testperson.personlogg.toString()

        val bareMeldingerSomMatcher = { event: String ->
            filter.isEmpty() || filter.any { filtertekst -> event.contains(filtertekst) }
        }
        return etter.lineSequence().filter(bareMeldingerSomMatcher).toList()
    }

    private companion object {
        val DAGSINNTEKT = INNTEKT.rundTilDaglig().dagligInt
    }
}
