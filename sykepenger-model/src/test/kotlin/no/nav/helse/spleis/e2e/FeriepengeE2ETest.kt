package no.nav.helse.spleis.e2e

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.serdeObjectMapper
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Year

internal class FeriepengeE2ETest : AbstractEndToEndTest() {
    private val logCollector = ListAppender<ILoggingEvent>()

    init {
        (LoggerFactory.getLogger("tjenestekall") as Logger).addAppender(logCollector)
        logCollector.start()
    }

    @BeforeEach
    fun setUp() {
        logCollector.list.clear()
    }

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

    @Test
    fun `Validerer at beregnet feriepengebeløp for IT finnes i lista over utbetalte feriepenger`() {
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

            assertTrue(inspektør.personLogg.toString().contains("Trenger å sende utbetaling til Oppdrag"))
            assertEquals(inspektør.personLogg.behov().last().detaljer()["saksbehandler"], "SPLEIS")

            val linje = (inspektør.personLogg.behov().last().detaljer()["linjer"] as ArrayList<LinkedHashMap<String, String>>).first()
            assertEquals(linje["satstype"], "ENG")
            assertEquals(linje["klassekode"], "SPREFAGFER-IOP")
            assertEquals(linje["grad"], null)
        }
    }

    @Test
    fun `Sender ut event etter mottak av kvittering fra oppdrag`() {
        Toggles.SendFeriepengeOppdrag.enable {
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
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
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
    fun `Totalbeløp settes til sats for utbetaling av feriepenger`(){
        Toggles.SendFeriepengeOppdrag.enable {
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

            val linje = ( inspektør.personLogg.behov().last().detaljer()["linjer"] as ArrayList<LinkedHashMap<String, String>>).first()

            assertEquals(1460, linje["sats"])
            assertEquals(1460, linje["totalbeløp"])
        }
    }
}
