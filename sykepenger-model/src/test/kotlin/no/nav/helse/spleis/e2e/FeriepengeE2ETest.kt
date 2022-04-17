package no.nav.helse.spleis.e2e

import ch.qos.logback.classic.Logger
import java.time.Year
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.september
import no.nav.helse.serde.reflection.castAsList
import no.nav.helse.sisteBehov
import no.nav.helse.testhelpers.LogCollector
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

@EnableToggle(Toggle.SendFeriepengeOppdrag::class)
internal class FeriepengeE2ETest : AbstractEndToEndTest() {
    private val logCollector = LogCollector()

    init {
        (LoggerFactory.getLogger("tjenestekall") as Logger).addAppender(logCollector)
        logCollector.start()
    }

    @Test
    fun `Beregner feriepenger korrekt for enkel spleisperiode uten infotrygdhistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )

        assertEquals(0.0, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
        assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())
        assertEquals(1431 * 10 * 0.102, inspektør.spleisFeriepengebeløpArbeidsgiver.first())

        val utbetalingslinje = TestArbeidsgiverInspektør.Feriepengeutbetalingslinje(
            1.mai(2021),
            31.mai(2021),
            Satstype.Engang,
            1460,
            null,
            Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig,
            Endringskode.NY
        )
        assertEquals(utbetalingslinje, inspektør.feriepengeoppdrag.first().feriepengeutbetalingslinjer.first())
    }

    @Test
    fun `Beregner feriepenger korrekt for enkel spleisperiode med en utbetaling i infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.mars(2020),
                    31.mars(2020),
                    1431,
                    31.mars(2020)
                )
            ),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 3211, 1.mai(2021), 31.mai(2021)))
        )

        assertEquals(1431 * 22 * 0.102, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
        assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())
        assertEquals(1431 * 10 * 0.102, inspektør.spleisFeriepengebeløpArbeidsgiver.first())

        val utbetalingslinje = TestArbeidsgiverInspektør.Feriepengeutbetalingslinje(
            1.mai(2021),
            31.mai(2021),
            Satstype.Engang,
            1460,
            null,
            Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig,
            Endringskode.NY
        )
        assertEquals(utbetalingslinje, inspektør.feriepengeoppdrag.first().feriepengeutbetalingslinjer.first())
    }

    @Test
    fun `Legger ikke infotrygdcache til grunn for feriepenger 8)`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
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
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.januar(2020),
                    31.januar(2020),
                    1431,
                    31.januar(2020)
                )
            ),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 3357, 1.mai(2021), 31.mai(2021)))
        )

        assertEquals(1431 * 23 * 0.102, inspektør.infotrygdFeriepengebeløpArbeidsgiver.first())
        assertEquals(0.0, inspektør.infotrygdFeriepengebeløpPerson.first())
        assertEquals(1431 * 10 * 0.102, inspektør.spleisFeriepengebeløpArbeidsgiver.first())

        val utbetalingslinje = TestArbeidsgiverInspektør.Feriepengeutbetalingslinje(
            1.mai(2021),
            31.mai(2021),
            Satstype.Engang,
            1460,
            null,
            Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig,
            Endringskode.NY
        )
        assertEquals(utbetalingslinje, inspektør.feriepengeoppdrag.first().feriepengeutbetalingslinjer.first())
    }

    @Test
    fun `Beregner ikke feriepenger for personer markert for manuell beregning av feriepenger`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            skalBeregnesManuelt = true
        )

        assertEquals(0, inspektør.feriepengeoppdrag.size)
        assertTrue(person.personLogg.toString().contains("Person er markert for manuell beregning av feriepenger"))
    }

    @Test
    fun `Sender ikke to utbetalingsbehov om feriepengereberegningen er lik den forrige`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.november(2020),
                    30.november(2020),
                    1000,
                    1.desember(2020)
                )
            ),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 2142, 1.mai(2021), 31.mai(2021)))
        )
        assertEquals(1, inspektør.feriepengeoppdrag.size)
        assertEquals(1, engangsutbetalinger().size)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.november(2020),
                    30.november(2020),
                    1000,
                    1.desember(2020)
                )
            ),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 2142, 1.mai(2021), 31.mai(2021)))
        )

        assertEquals(2, inspektør.feriepengeoppdrag.size)
        assertEquals(1, engangsutbetalinger().size)
    }

    @Test
    fun `Korrigerer en ukjent arbeidsgiver hvis feriepengene er brukt opp i spleis`() {
        val ORGNUMMER2 = "978654321"
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.august(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.august(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER2,
                    1.november(2020),
                    30.november(2020),
                    1000,
                    1.desember(2020)
                )
            ),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER2, 2142, 1.mai(2021), 31.mai(2021)))
        )

        assertEquals(1, inspektør.feriepengeoppdrag.size)
        assertEquals(1, inspektør(ORGNUMMER2).feriepengeoppdrag.size)
        assertEquals(7006, inspektør.feriepengeoppdrag.first().feriepengeutbetalingslinjer.first().beløp)
        assertEquals(-2142, inspektør(ORGNUMMER2).feriepengeoppdrag.first().feriepengeutbetalingslinjer.first().beløp)
    }

    @Test
    fun `Ghost arbeidsgiver fra feriepengeberegner påvirker ikke senere sykepengeberegning`() {
        val ORGNUMMER2 = "978654321"
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER2,
                    1.november(2020),
                    30.november(2020),
                    1000,
                    1.desember(2020)
                )
            ),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER2, 2142, 1.mai(2021), 31.mai(2021)))
        )
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.august(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.august(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()


        assertEquals(0, inspektør.feriepengeoppdrag.size)
        assertEquals(1, inspektør(ORGNUMMER2).feriepengeoppdrag.size)
        assertEquals(0, inspektør(ORGNUMMER2).feriepengeoppdrag.first().feriepengeutbetalingslinjer.first().beløp)
    }

    @Test
    fun `Validerer at beregnet feriepengebeløp for IT finnes i lista over utbetalte feriepenger`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.januar(2020),
                    31.januar(2020),
                    1431,
                    31.januar(2020)
                )
            ),
            feriepengehistorikk = listOf(
                UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 3357, 1.mai(2021), 31.mai(2021)),
                UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 4000, 1.mai(2021), 31.mai(2021))
            )
        )

        assertEquals(1, inspektør.feriepengeoppdrag.size)
        assertFalse(logCollector.any { it.message.startsWith("Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp") })
    }

    @Test
    fun `Validering feiler hvis beregnet feriepengebeløp for IT ikke finnes i lista over utbetalte feriepenger`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.januar(2020),
                    31.januar(2020),
                    1431,
                    31.januar(2020)
                )
            ),
            feriepengehistorikk = listOf(
                UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 3356, 1.mai(2021), 31.mai(2021)),
                UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 4000, 1.mai(2021), 31.mai(2021))
            )
        )

        assertEquals(1, inspektør.feriepengeoppdrag.size)
        assertTrue(logCollector.any { it.message.startsWith("Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp") })
    }

    @Test
    fun `Validerer ikke utbetalte feriepenger hvis beregnet feriepengebeløp for IT er 0`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            feriepengehistorikk = listOf(
                UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 4000, 1.mai(2021), 31.mai(2021))
            )
        )

        assertEquals(1, inspektør.feriepengeoppdrag.size)
        assertFalse(logCollector.any { it.message.startsWith("Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp") })
    }

    @Test
    fun `Utbetaling av feriepenger sender behov til oppdrag`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )

        assertTrue(person.personLogg.toString().contains("Trenger å sende utbetaling til Oppdrag"))
        assertEquals(person.personLogg.behov().last().detaljer()["saksbehandler"], "SPLEIS")

        val linje = (person.personLogg.behov().last().detaljer()["linjer"] as ArrayList<LinkedHashMap<String, String>>).first()
        assertEquals(linje["satstype"], "ENG")
        assertEquals(linje["klassekode"], "SPREFAGFER-IOP")
        assertEquals(linje["grad"], null)
    }

    @Test
    fun `Sender ut events etter mottak av kvittering fra oppdrag`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )

        val fagsystemIdFeriepenger = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling).detaljer()["fagsystemId"] as String
        håndterFeriepengerUtbetalt(fagsystemId = fagsystemIdFeriepenger)

        assertTrue(person.personLogg.toString().contains("Data for feriepenger fra Oppdrag/UR"))
        assertTrue(person.personLogg.toString().contains("utbetalt ok: ja"))
        observatør.feriepengerUtbetaltEventer.first().let { event ->
            assertEquals(fagsystemIdFeriepenger, event.arbeidsgiverOppdrag["fagsystemId"])
            val linje = (event.arbeidsgiverOppdrag["linjer"] as ArrayList<LinkedHashMap<String, String>>).first()
            assertEquals("2021-05-01", linje["fom"])
            assertEquals("2021-05-31", linje["tom"])
            assertEquals(1460, linje["totalbeløp"])
        }
        assertTrue(observatør.utbetaltEndretEventer.any {
            it.event.arbeidsgiverOppdrag["linjer"].castAsList<Map<String, Any>>().single()["satstype"] == "ENG"
        })
    }

    @Test
    fun `Sender ut events kun for oppdrag med relevant utbetalingId etter mottak av kvittering`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )

        val fagsystemIdFeriepenger = person.personLogg.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling).detaljer()["fagsystemId"] as String
        håndterFeriepengerUtbetalt(fagsystemId = fagsystemIdFeriepenger)

        håndterSykmelding(Sykmeldingsperiode(1.juli(2020), 10.juli(2020), 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.juli(2020), 10.juli(2020), 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )

        håndterFeriepengerUtbetalt(fagsystemId = fagsystemIdFeriepenger)

        assertTrue(person.personLogg.toString().contains("Data for feriepenger fra Oppdrag/UR"))
        assertTrue(person.personLogg.toString().contains("utbetalt ok: ja"))

        assertEquals(2, observatør.feriepengerUtbetaltEventer.size)

        observatør.feriepengerUtbetaltEventer.first().let { event ->
            assertEquals(fagsystemIdFeriepenger, event.arbeidsgiverOppdrag["fagsystemId"])
            val linje = (event.arbeidsgiverOppdrag["linjer"] as ArrayList<LinkedHashMap<String, String>>).first()
            assertEquals("2021-05-01", linje["fom"])
            assertEquals("2021-05-31", linje["tom"])
            assertEquals(1460, linje["totalbeløp"])
        }
        observatør.feriepengerUtbetaltEventer.last().let { event ->
            assertEquals(fagsystemIdFeriepenger, event.arbeidsgiverOppdrag["fagsystemId"])
            val linje = (event.arbeidsgiverOppdrag["linjer"] as ArrayList<LinkedHashMap<String, String>>).first()
            assertEquals("2021-05-01", linje["fom"])
            assertEquals("2021-05-31", linje["tom"])
            assertEquals(2627, linje["totalbeløp"])
        }

        val feriepengerUtbetaltEndretEventer = observatør.utbetaltEndretEventer.filter { it.event.type == Utbetalingtype.FERIEPENGER.name }
        assertEquals(2, feriepengerUtbetaltEndretEventer.size)
        assertTrue(feriepengerUtbetaltEndretEventer.any {
            it.event.arbeidsgiverOppdrag["linjer"].castAsList<Map<String, Any>>().single()["satstype"] == "ENG"
        })
    }

    @Test
    fun `Sender ikke behov når det ikke er noen diff i IT og spleis sine beregninger av feriepenger`() {
        Toggle.SendFeriepengeOppdrag.enable {
            håndterSykmelding(Sykmeldingsperiode(6.juni(2020), 7.juni(2020), 100.prosent))
            håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(6.juni(2020), 7.juni(2020), 100.prosent))
            håndterUtbetalingshistorikk(1.vedtaksperiode)
            håndterInntektsmelding(listOf(6.juni(2020) til 7.juni(2020)))

            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020)
            )
            assertFalse(person.personLogg.toString().contains("Trenger å sende utbetaling til Oppdrag"))
        }
    }

    @Test
    fun `Totalbeløp settes til sats for utbetaling av feriepenger`() {
        Toggle.SendFeriepengeOppdrag.enable {
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
            håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
            håndterUtbetalingshistorikk(1.vedtaksperiode)
            håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.juni(2019) til 1.mai(2020) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                }
            ))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020)
            )

            val linje = (person.personLogg.behov().last().detaljer()["linjer"] as ArrayList<LinkedHashMap<String, String>>).first()

            assertEquals(1460, linje["sats"])
            assertEquals(1460, linje["totalbeløp"])
        }
    }

    @Test
    fun `reberegning av feriepenger med endringer`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )
        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    20.januar(2020),
                    31.januar(2020),
                    690,
                    30.juni(2020)
                )
            ),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
            opptjeningsår = Year.of(2020)
        )

        assertEquals(1, engangsutbetalinger().size)
    }

    @Test
    fun `reberegning av feriepenger med endringer hvor totalt utbetalte dager går over 48`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 14.august(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )
        val førsteUtbetaling = engangsutbetalinger().last()
        val fagsystemId = førsteUtbetaling.detaljer()["fagsystemId"]
        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    20.januar(2020),
                    31.januar(2020),
                    690,
                    30.juni(2020)
                )
            ), // 10 dager
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
            opptjeningsår = Year.of(2020)
        )

        assertEquals(2, engangsutbetalinger().size)
        val utbetaling = engangsutbetalinger().last()
        assertEquals((38 * DAGSINNTEKT * 0.102).roundToInt(), utbetaling.linje()["sats"])
        assertEquals(fagsystemId, utbetaling.detaljer()["fagsystemId"])
        assertEquals("ENDR", utbetaling.detaljer()["endringskode"])
        assertEquals("NY", utbetaling.linje()["endringskode"])
        assertEquals(førsteUtbetaling.linje()["delytelseId"], utbetaling.linje()["refDelytelseId"])
        assertEquals(førsteUtbetaling.linje()["klassekode"], utbetaling.linje()["klassekode"])
    }

    @Test
    fun `reberegning av feriepenger med endringer hvor første ikke blir sendt`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 14.august(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.januar(2020),
                    8.mars(2020),
                    690,
                    30.juni(2020)
                )
            ), // 10 dager
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
            opptjeningsår = Year.of(2020)
        )
        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    20.januar(2020),
                    31.januar(2020),
                    690,
                    30.juni(2020)
                )
            ), // 10 dager
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
            opptjeningsår = Year.of(2020)
        )

        assertEquals(1, engangsutbetalinger().size)
        val utbetaling = engangsutbetalinger().last()
        assertEquals((38 * DAGSINNTEKT * 0.102).roundToInt(), utbetaling.linje()["sats"])
        assertEquals("NY", utbetaling.detaljer()["endringskode"])
        assertEquals("NY", utbetaling.linje()["endringskode"])
    }

    @Test
    fun `Kobler ny utbetaling til det forrige sendte oppdraget`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 14.august(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )
        val førsteUtbetaling = engangsutbetalinger().last()
        val fagsystemId = førsteUtbetaling.detaljer()["fagsystemId"]

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )
        assertEquals(2, inspektør.feriepengeoppdrag.size)
        assertEquals(1, engangsutbetalinger().size)

        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(
                Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    20.januar(2020),
                    31.januar(2020),
                    690,
                    30.juni(2020)
                )
            ), // 10 dager
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
            opptjeningsår = Year.of(2020)
        )

        assertEquals(3, inspektør.feriepengeoppdrag.size)
        assertEquals(2, engangsutbetalinger().size)
        val utbetaling = engangsutbetalinger().last()
        assertEquals((38 * DAGSINNTEKT * 0.102).roundToInt(), utbetaling.linje()["sats"])
        assertEquals(fagsystemId, utbetaling.detaljer()["fagsystemId"])
        assertEquals("ENDR", utbetaling.detaljer()["endringskode"])
        assertEquals("NY", utbetaling.linje()["endringskode"])
        assertEquals(førsteUtbetaling.linje()["delytelseId"], utbetaling.linje()["refDelytelseId"])
    }

    @Test
    fun `toggle av føkekr ikke shit`() {
        Toggle.SendFeriepengeOppdrag.enable {
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 14.august(2020), 100.prosent))
            håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
            håndterUtbetalingshistorikk(1.vedtaksperiode)
            håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.juni(2019) til 1.mai(2020) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                }
            ))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020)
            )
            val førsteUtbetaling = engangsutbetalinger()

            Toggle.SendFeriepengeOppdrag.disable {
                håndterUtbetalingshistorikkForFeriepenger(
                    utbetalinger = listOf(
                        Arbeidsgiverutbetalingsperiode(
                            ORGNUMMER,
                            20.januar(2020),
                            31.januar(2020),
                            690,
                            30.juni(2020)
                        )
                    ), // 10 dager
                    feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10 * 690 * 0.102).roundToInt(), 1.mai, 31.mai)),
                    opptjeningsår = Year.of(2020)
                )
            }

            assertEquals(1, engangsutbetalinger().size)
            val utbetaling = engangsutbetalinger()
            assertEquals(førsteUtbetaling, utbetaling)
        }
    }

    @Test
    fun `Reberegning som ender med 0 i totalsum sender opphør`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 14.august(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020))

        val førsteUtbetaling = engangsutbetalinger().last()
        val fagsystemId = førsteUtbetaling.detaljer()["fagsystemId"]

        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020))

        assertEquals(2, inspektør.feriepengeoppdrag.size)
        assertEquals(2, engangsutbetalinger().size)
        val utbetaling = engangsutbetalinger().last()
        assertEquals((43 * DAGSINNTEKT * 0.102).roundToInt(), utbetaling.linje()["sats"])
        assertEquals(1.mai(2021).toString(), utbetaling.linje()["datoStatusFom"])
        assertEquals(fagsystemId, utbetaling.detaljer()["fagsystemId"])
        assertEquals(førsteUtbetaling.linje()["klassekode"], utbetaling.linje()["klassekode"])
    }

    @Test
    fun `Rekjøring etter annullert oppdrag skal ikke sende feriepenger ved beløp 0`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 14.august(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020))

        val førsteUtbetaling = engangsutbetalinger().last()
        val fagsystemId1FraBehov = førsteUtbetaling.detaljer()["fagsystemId"]

        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020))

        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020))

        assertEquals(3, inspektør.feriepengeoppdrag.size)
        assertEquals(2, engangsutbetalinger().size)

        val fagsystemId1 = inspektør.feriepengeoppdrag[0].fagsystemId
        val fagsystemId2 = inspektør.feriepengeoppdrag[1].fagsystemId
        val fagsystemId3 = inspektør.feriepengeoppdrag[2].fagsystemId

        assertEquals(fagsystemId1, fagsystemId2)
        assertNotEquals(fagsystemId1, fagsystemId3)

        val fagsystemId2FraBehov = engangsutbetalinger().last().detaljer()["fagsystemId"]
        assertEquals(fagsystemId1, fagsystemId1FraBehov)
        assertEquals(fagsystemId1FraBehov, fagsystemId2FraBehov)
    }

    @Test
    fun `Rekjøring etter annullert oppdrag skal sende feriepenger med ny fagsystemId`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 14.august(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.juni(2020) til 16.juni(2020)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.juni(2019) til 1.mai(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020))

        val førsteUtbetaling = engangsutbetalinger().last()
        val fagsystemId1FraBehov = førsteUtbetaling.detaljer()["fagsystemId"]

        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020))

        håndterSykmelding(Sykmeldingsperiode(1.oktober(2020), 14.desember(2020), 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.oktober(2020), 14.desember(2020), 100.prosent)) // 41 dager
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterInntektsmelding(listOf(1.oktober(2020) til 16.oktober(2020)))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.oktober(2019) til 1.september(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020))

        assertEquals(3, inspektør.feriepengeoppdrag.size)
        assertEquals(3, engangsutbetalinger().size)

        val fagsystemId1 = inspektør.feriepengeoppdrag[0].fagsystemId
        val fagsystemId2 = inspektør.feriepengeoppdrag[1].fagsystemId
        val fagsystemId3 = inspektør.feriepengeoppdrag[2].fagsystemId

        assertEquals(fagsystemId1, fagsystemId2)
        assertNotEquals(fagsystemId1, fagsystemId3)

        val fagsystemId2FraBehov = engangsutbetalinger()[1].detaljer()["fagsystemId"]
        assertEquals(fagsystemId1, fagsystemId1FraBehov)
        assertEquals(fagsystemId1FraBehov, fagsystemId2FraBehov)

        val utbetaling = engangsutbetalinger().last()
        assertEquals((41 * DAGSINNTEKT * 0.102).roundToInt(), utbetaling.linje()["sats"])
        assertNull(utbetaling.linje()["datoStatusFom"])
        assertEquals(fagsystemId3, utbetaling.detaljer()["fagsystemId"])
        assertEquals(førsteUtbetaling.linje()["klassekode"], utbetaling.linje()["klassekode"])
    }

    private fun engangsutbetalinger() = person.personLogg.behov()
        .filter { it.type == Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling }
        .filter { utbetaling -> utbetaling.detaljer()["linjer"].castAsList<Map<String, Any>>().any { linje -> linje["satstype"] == "ENG" } }

    private fun Aktivitetslogg.Aktivitet.Behov.linje() = this
        .detaljer()["linjer"].castAsList<Map<String, Any?>>()
        .single()
}
