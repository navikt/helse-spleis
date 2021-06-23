package no.nav.helse.spleis.e2e

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.reflection.castAsList
import no.nav.helse.serde.serdeObjectMapper
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Year
import kotlin.math.roundToInt

internal class FeriepengeE2ETest : AbstractEndToEndTest() {
    private val logCollector = ListAppender<ILoggingEvent>()

    init {
        (LoggerFactory.getLogger("tjenestekall") as Logger).addAppender(logCollector)
        logCollector.start()
    }

    @BeforeEach
    fun setUp() {
        logCollector.list.clear()
        Toggles.SendFeriepengeOppdrag.enable()
    }

    @AfterEach
    fun tearDown() {
        Toggles.SendFeriepengeOppdrag.pop()
    }

    @Test
    fun `Beregner feriepenger korrekt for enkel spleisperiode uten infotrygdhistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
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
            inntekter = inntektperioderForSammenligningsgrunnlag {
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
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
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
            inntekter = inntektperioderForSammenligningsgrunnlag {
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
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
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
            inntekter = inntektperioderForSammenligningsgrunnlag {
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

    @Test
    fun `Sender ikke to utbetalingsbehov om feriepengereberegningen er lik den forrige`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
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
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.november(2020),
                    30.november(2020),
                    1000,
                    1.desember(2020)
                )
            ),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 2142, 1.mai(2021), 31.mai(2021)))
        )
        assertEquals(1, inspektør.feriepengeutbetalingslinjer.size)
        assertEquals(1, engangsutbetalinger().size)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER,
                    1.november(2020),
                    30.november(2020),
                    1000,
                    1.desember(2020)
                )
            ),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 2142, 1.mai(2021), 31.mai(2021)))
        )

        assertEquals(2, inspektør.feriepengeutbetalingslinjer.size)
        assertEquals(1, engangsutbetalinger().size)
    }

    @Test
    fun `Korrigerer en ukjent arbeidsgiver hvis feriepengene er brukt opp i spleis`() {
        val ORGNUMMER2 = "978654321"
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.august(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.august(2020), 100.prosent))
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
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                    ORGNUMMER2,
                    1.november(2020),
                    30.november(2020),
                    1000,
                    1.desember(2020)
                )
            ),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER2, 2142, 1.mai(2021), 31.mai(2021)))
        )

        assertEquals(1, inspektør.feriepengeutbetalingslinjer.size)
        assertEquals(1, inspektør(ORGNUMMER2).feriepengeutbetalingslinjer.size)
        assertEquals(7006, inspektør.feriepengeutbetalingslinjer.first().beløp)
        assertEquals(-2142, inspektør(ORGNUMMER2).feriepengeutbetalingslinjer.first().beløp)
    }

    @Test
    fun `Ghost arbeidsgiver fra feriepengeberegner påvirker ikke senere sykepengeberegning`() {
        val ORGNUMMER2 = "978654321"
        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
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
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.august(2020), 100.prosent))
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
        håndterUtbetalt(1.vedtaksperiode)


        assertEquals(0, inspektør.feriepengeutbetalingslinjer.size)
        assertEquals(1, inspektør(ORGNUMMER2).feriepengeutbetalingslinjer.size)
        assertEquals(0, inspektør(ORGNUMMER2).feriepengeutbetalingslinjer.first().beløp)
    }

    @Test
    fun `Validerer at beregnet feriepengebeløp for IT finnes i lista over utbetalte feriepenger`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
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
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
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

        assertEquals(1, inspektør.feriepengeutbetalingslinjer.size)
        assertFalse(logCollector.list.any { it.message.startsWith("Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp") })
    }

    @Test
    fun `Validering feiler hvis beregnet feriepengebeløp for IT ikke finnes i lista over utbetalte feriepenger`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
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
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            utbetalinger = listOf(
                UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
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

        assertEquals(1, inspektør.feriepengeutbetalingslinjer.size)
        assertTrue(logCollector.list.any { it.message.startsWith("Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp") })
    }

    @Test
    fun `Validerer ikke utbetalte feriepenger hvis beregnet feriepengebeløp for IT er 0`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
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
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020),
            feriepengehistorikk = listOf(
                UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, 4000, 1.mai(2021), 31.mai(2021))
            )
        )

        assertEquals(1, inspektør.feriepengeutbetalingslinjer.size)
        assertFalse(logCollector.list.any { it.message.startsWith("Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp") })
    }

    @Test
    fun `Utbetaling av feriepenger sender behov til oppdrag`() {
        Toggles.SendFeriepengeOppdrag.enable {
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
            håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
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
            håndterUtbetalt(1.vedtaksperiode)

            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020)
            )

            assertTrue(inspektør.personLogg.toString().contains("Trenger å sende utbetaling til Oppdrag"))
            assertEquals(inspektør.personLogg.behov().last().detaljer()["saksbehandler"], "SPLEIS")

            val linje = (inspektør.personLogg.behov().last().detaljer()["linjer"] as ArrayList<LinkedHashMap<String, String>>).first()
            assertEquals(linje["satstype"], "ENG")
            assertEquals(linje["klassekode"], "SPREFAGFER-IOP")
            assertEquals(linje["grad"], null)
        }
    }

    @Test
    fun `Sender ut events etter mottak av kvittering fra oppdrag`() {
        Toggles.SendFeriepengeOppdrag.enable {
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
            håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
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
            håndterUtbetalt(1.vedtaksperiode)

            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020)
            )

            val fagsystemIdFeriepenger = inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling).detaljer().get("fagsystemId") as String
            håndterFeriepengerUtbetalt(fagsystemId = fagsystemIdFeriepenger)

            assertTrue(inspektør.personLogg.toString().contains("Data for feriepenger fra Oppdrag/UR"))
            assertTrue(inspektør.personLogg.toString().contains("utbetalt ok: ja"))
            observatør.feriepengerUtbetaltEventer.first().let { event ->
                println(serdeObjectMapper.writeValueAsString(event))
                assertEquals(fagsystemIdFeriepenger, event.arbeidsgiverOppdrag["fagsystemId"])
                val linje = (event.arbeidsgiverOppdrag["linjer"] as ArrayList<LinkedHashMap<String, String>>).first()
                assertEquals("2021-05-01", linje["fom"])
                assertEquals("2021-05-31", linje["tom"])
                assertEquals(1460, linje["totalbeløp"])
            }
            assertTrue(observatør.feriepengerUtbetaltEndretEventer.any { it.arbeidsgiverOppdrag["linjer"].castAsList<Map<String, Any>>().single()["satstype"] == "ENG" })
        }
    }

    @Test
    fun `Sender ikke behov når det ikke er noen diff i IT og spleis sine beregninger av feriepenger`() {
        Toggles.SendFeriepengeOppdrag.enable {
            håndterSykmelding(Sykmeldingsperiode(6.juni(2020), 7.juni(2020), 100.prosent))
            håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(6.juni(2020), 7.juni(2020), 100.prosent))
            håndterUtbetalingshistorikk(1.vedtaksperiode)
            håndterInntektsmelding(listOf(6.juni(2020) til 7.juni(2020)))
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.juni(2019) til 1.mai(2020) inntekter {
                        ORGNUMMER inntekt INNTEKT
                    }
                }
            ))
            håndterYtelser(1.vedtaksperiode)

            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020)
            )
            assertFalse(inspektør.personLogg.toString().contains("Trenger å sende utbetaling til Oppdrag"))
        }
    }

    @Test
    fun `Totalbeløp settes til sats for utbetaling av feriepenger`() {
        Toggles.SendFeriepengeOppdrag.enable {
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
            håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
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
            håndterUtbetalt(1.vedtaksperiode)

            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020)
            )

            val linje = (inspektør.personLogg.behov().last().detaljer()["linjer"] as ArrayList<LinkedHashMap<String, String>>).first()

            assertEquals(1460, linje["sats"])
            assertEquals(1460, linje["totalbeløp"])
        }
    }

    @Test
    fun `reberegning av feriepenger med endringer`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 30.juni(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 30.juni(2020), 100.prosent))
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
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )
        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(ORGNUMMER, 20.januar(2020), 31.januar(2020), 690, 30.juni(2020))),
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10*690*0.102).roundToInt(), 1.mai, 31.mai)),
            opptjeningsår = Year.of(2020)
        )

        assertEquals(1, engangsutbetalinger().size)
    }

    @Test
    fun `reberegning av feriepenger med endringer hvor totalt utbetalte dager går over 48`() {
        håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 14.august(2020), 100.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
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
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )
        val førsteUtbetaling = engangsutbetalinger().last()
        val fagsystemId = førsteUtbetaling.detaljer()["fagsystemId"]
        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(ORGNUMMER, 20.januar(2020), 31.januar(2020), 690, 30.juni(2020))), // 10 dager
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10*690*0.102).roundToInt(), 1.mai, 31.mai)),
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
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
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
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(ORGNUMMER, 1.januar(2020), 8.mars(2020), 690, 30.juni(2020))), // 10 dager
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10*690*0.102).roundToInt(), 1.mai, 31.mai)),
            opptjeningsår = Year.of(2020)
        )
        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(ORGNUMMER, 20.januar(2020), 31.januar(2020), 690, 30.juni(2020))), // 10 dager
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10*690*0.102).roundToInt(), 1.mai, 31.mai)),
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
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
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
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )
        val førsteUtbetaling = engangsutbetalinger().last()
        val fagsystemId = førsteUtbetaling.detaljer()["fagsystemId"]

        håndterUtbetalingshistorikkForFeriepenger(
            opptjeningsår = Year.of(2020)
        )
        assertEquals(2, inspektør.feriepengeutbetalingslinjer.size)
        assertEquals(1, engangsutbetalinger().size)

        håndterUtbetalingshistorikkForFeriepenger(
            utbetalinger = listOf(UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(ORGNUMMER, 20.januar(2020), 31.januar(2020), 690, 30.juni(2020))), // 10 dager
            feriepengehistorikk = listOf(UtbetalingshistorikkForFeriepenger.Feriepenger(ORGNUMMER, (10*690*0.102).roundToInt(), 1.mai, 31.mai)),
            opptjeningsår = Year.of(2020)
        )

        assertEquals(3, inspektør.feriepengeutbetalingslinjer.size)
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
        Toggles.SendFeriepengeOppdrag.enable {
            håndterSykmelding(Sykmeldingsperiode(1.juni(2020), 14.august(2020), 100.prosent))
            håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
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
            håndterUtbetalt(1.vedtaksperiode)

            håndterUtbetalingshistorikkForFeriepenger(
                opptjeningsår = Year.of(2020)
            )
            val førsteUtbetaling = engangsutbetalinger()

            Toggles.SendFeriepengeOppdrag.disable {
                håndterUtbetalingshistorikkForFeriepenger(
                    utbetalinger = listOf(
                        UtbetalingshistorikkForFeriepenger.Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
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
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.juni(2020), 14.august(2020), 100.prosent)) // 43 dager
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
        håndterUtbetalt(1.vedtaksperiode)

        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020))

        val førsteUtbetaling = engangsutbetalinger().last()
        val fagsystemId = førsteUtbetaling.detaljer()["fagsystemId"]

        håndterAnnullerUtbetaling()
        håndterUtbetalt()

        håndterUtbetalingshistorikkForFeriepenger(opptjeningsår = Year.of(2020))

        assertEquals(2, inspektør.feriepengeutbetalingslinjer.size)
        assertEquals(2, engangsutbetalinger().size)
        val utbetaling = engangsutbetalinger().last()
        assertEquals((43 * DAGSINNTEKT * 0.102).roundToInt(), utbetaling.linje()["sats"])
        assertEquals(1.mai(2021).toString(), utbetaling.linje()["datoStatusFom"])
        assertEquals(fagsystemId, utbetaling.detaljer()["fagsystemId"])
        assertEquals(førsteUtbetaling.linje()["klassekode"], utbetaling.linje()["klassekode"])
    }

    private fun engangsutbetalinger() = inspektør.personLogg.behov()
        .filter { it.type == Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling }
        .filter { utbetaling -> utbetaling.detaljer()["linjer"].castAsList<Map<String, Any>>().any { linje -> linje["satstype"] == "ENG" }}

    private fun Aktivitetslogg.Aktivitet.Behov.linje() = this
        .detaljer()["linjer"].castAsList<Map<String, Any?>>()
        .single()
}
