package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.reflection.castAsList
import no.nav.helse.serde.reflection.castAsMap
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class FlereArbeidsgivereArbeidsforholdTest : AbstractEndToEndTest() {
    private companion object {
        private const val a1 = "arbeidsgiver 1"
        private const val a2 = "arbeidsgiver 2"
    }

    @Test
    fun `Førstegangsbehandling med ekstra arbeidsforhold som ikke er aktivt - skal ikke få warning hvis det er flere arbeidsforhold`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                orgnummer = a1,
                refusjon = Refusjon(null, 10000.månedlig, emptyList())
            )
            val inntekter = listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(3))
            )
            val arbeidsforhold = listOf(
                Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
                Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = 1.februar)
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)

            assertNoWarnings(inspektør(a1))
        }
    }

    @Test
    fun `Filtrerer ut irrelevante arbeidsforhold per arbeidsgiver`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                orgnummer = a1,
                refusjon = Refusjon(null, 10000.månedlig, emptyList())
            )
            val inntekter = listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 20000.månedlig.repeat(3))
            )
            val arbeidsforhold = listOf(
                Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
                Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)

            assertEquals(1, tellArbeidsforholdhistorikkinnslag(a1).size)
            assertEquals(1, tellArbeidsforholdhistorikkinnslag(a2).size)
        }
    }

    @Test
    fun `Infotrygdforlengelse av arbeidsgiver som ikke finnes i aareg, kan utbetales uten warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(3))
        )
        val arbeidsforhold = emptyList<Arbeidsforhold>()

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 28.februar, 100.prosent, 10000.månedlig))
        val inntektshistorikk = listOf(Inntektsopplysning(a1, 1.februar, 10000.månedlig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), utbetalinger = utbetalinger, inntektshistorikk, orgnummer = a1)
        håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), a1, inntekter, arbeidsforhold)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        assertTilstand(a1, TilstandType.AVSLUTTET)
        assertNoWarnings(inspektør(a1))
    }

    @Test
    fun `Vanlige forlengelser av arbeidsgiver som ikke finnes i aareg, kan utbetales uten warning`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                orgnummer = a1,
                refusjon = Refusjon(null, 10000.månedlig, emptyList())
            )
            val inntekter1 = listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(3))
            )
            val sammenligningsgrunnlag1 = listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(12))
            )
            val arbeidsforhold1 = emptyList<Arbeidsforhold>()
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), a1, inntekter1, arbeidsforhold1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag1))
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)
            val inntekter2 = listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 2.vedtaksperiode(a1)), 10000.månedlig.repeat(3))
            )
            val arbeidsforhold2 = emptyList<Arbeidsforhold>()
            håndterUtbetalingsgrunnlag(2.vedtaksperiode(a1), a1, inntekter2, arbeidsforhold2)
            håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)

            assertTilstand(a1, TilstandType.AVSLUTTET, 2)

            val sisteGodkjenningsbehov = inspektør(a1).sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning).detaljer()
            assertEquals(0, sisteGodkjenningsbehov["warnings"].castAsMap<String, Any>()["aktiviteter"].castAsList<Any>().size)
        }
    }

    @Test
    fun `Flere aktive arbeidsforhold, men kun 1 med inntekt, skal ikke få warning for flere arbeidsgivere`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.juli(2021), 31.juli(2021), 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.juli(2021), 31.juli(2021), 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.juli(2021) til 16.juli(2021)),
                førsteFraværsdag = 1.juli(2021),
                orgnummer = a1,
                refusjon = Refusjon(null, 30000.månedlig, emptyList())
            )
            val inntekter = listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(3))
            )
            val arbeidsforhold = listOf(
                Arbeidsforhold(orgnummer = a1, fom = 2.januar(2020), tom = null),
                Arbeidsforhold(orgnummer = a2, fom = 1.mai(2019), tom = null)
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekter, orgnummer = a1, arbeidsforhold = arbeidsforhold)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 30000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
            assertNoWarnings(inspektør(a1))
        }
    }

    @Test
    fun `arbeidsgivere med sammenligningsgrunnlag, men uten inntekt, skal ikke anses som ekstra arbeidsgiver`() = Toggles.FlereArbeidsgivereUlikFom.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1)

        val arbeidsforhold = listOf(Arbeidsforhold(a1, LocalDate.EPOCH))
        val inntekter = listOf(grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), INNTEKT.repeat(3)))

        val sammenligningsgrunnlag = inntektperioderForSammenligningsgrunnlag {
            1.januar(2017) til 1.desember(2017) inntekter {
                a1 inntekt INNTEKT
            }
            1.januar(2017) til 1.januar(2017) inntekter {
                a2 inntekt 10000
            }
        }

        håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntekter = inntekter, arbeidsforhold = arbeidsforhold)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag))
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode(a1)))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        håndterUtbetalingsgrunnlag(2.vedtaksperiode(a1), orgnummer = a1, inntekter = inntekter, arbeidsforhold = arbeidsforhold)

        assertFalse(inspektør(a1).warnings.contains("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"))
        assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(2.vedtaksperiode(a1)))
    }

    @Test
    fun `Syk for a1, slutter i a1, syk for a2, a1 finnes ikke i Aa-reg lenger - ingen warning for manglende arbeidsforhold`() {
        /*
        * Siden vi ikke vet om arbeidsforhold for tidligere utbetalte perioder må vi passe på at ikke lar de periodene føre til advarsel på nye helt uavhengie vedtaksperioder
        * Sjekker kun arbeidsforhold for gjelende skjæringstidspunkt, derfor vil ikke mangel av arbeidsforhold for a1 skape problemer
        * */
        Toggles.FlereArbeidsgivereUlikFom.enable {
            håndterSykmelding(Sykmeldingsperiode(1.mars(2017), 31.mars(2017), 50.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars(2017), 31.mars(2017), 50.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.mars(2017) til 16.mars(2017)),
                førsteFraværsdag = 1.mars(2017),
                orgnummer = a1,
                refusjon = Refusjon(null, 30000.månedlig, emptyList())
            )
            val inntekterA1 = listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(3))
            )
            val arbeidsforholdA1 = listOf(
                Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null)
            )
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), inntekter = inntekterA1, orgnummer = a1, arbeidsforhold = arbeidsforholdA1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1, inntektshistorikk = emptyList())
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a1), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 35000.månedlig.repeat(12))
                    )
                ), orgnummer = a1
            )
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 50.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 50.prosent), orgnummer = a2)
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                orgnummer = a2,
                refusjon = Refusjon(null, 30000.månedlig, emptyList())
            )
            val inntekterA2 = listOf(
                grunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode(a2)), 35000.månedlig.repeat(3))
            )

            val arbeidsforholdA2 = listOf(
                Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
            )

            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), inntekter = inntekterA2, orgnummer = a2, arbeidsforhold = arbeidsforholdA2)
            håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode(a2), inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode(a2)), 35000.månedlig.repeat(12))
                    )
                ), orgnummer = a2
            )
            håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

            val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().last()
            assertEquals(17.mars, a2Linje.fom)
            assertEquals(31.mars, a2Linje.tom)
            assertEquals(692, a2Linje.beløp)

            assertNoWarnings(inspektør(a2))
        }
    }

    @Test
    fun `Warning om manglende arbeidsforhold dukker ikke opp når FlereArbeidsgivereUlikFom-toggle er false`() {
        Toggles.FlereArbeidsgivereUlikFom.disable {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                førsteFraværsdag = 1.mars,
                orgnummer = a1,
                refusjon = Refusjon(null, 10000.månedlig, emptyList())
            )
            val inntekter1 = listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(3))
            )
            val sammenligningsgrunnlag1 = listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(12))
            )
            val arbeidsforhold1 = emptyList<Arbeidsforhold>()
            håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), a1, inntekter1, arbeidsforhold1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterVilkårsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag1))
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

            assertNoWarnings(inspektør(a1))
        }
    }

    @Test
    fun `lagrer kun arbeidsforhold som gjelder under skjæringstidspunkt`() = Toggles.FlereArbeidsgivereUlikFom.enable {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 10000.månedlig, emptyList())
        )
        val inntekter1 = listOf(grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 10000.månedlig.repeat(3)))
        val arbeidsforhold1 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, 1.januar),
            Arbeidsforhold(a1, 1.januar, null), // Skal gjelde
            Arbeidsforhold(a1, 28.februar, 1.mars), // Skal gjelde
            Arbeidsforhold(a1, 1.mars, 31.mars), // Skal gjelde
            Arbeidsforhold(a1, 1.februar, 28.februar),
            Arbeidsforhold(a1, 2.mars, 31.mars)
        )
        håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), a1, inntekter1, arbeidsforhold1)

        assertEquals(3, tellArbeidsforholdINyesteHistorikkInnslag(a1))
    }

    @Test
    fun `opphold i arbeidsforhold skal ikke behandles som ghost`() = Toggles.FlereArbeidsgivereUlikFom.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
            refusjon = Refusjon(null, 11400.månedlig)
        )

        val arbeidsforhold1 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH),
            Arbeidsforhold(a2, LocalDate.EPOCH)
        )
        val inntekter1 = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 11400.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 45000.månedlig.repeat(3))
        )
        val sammenligningsgrunnlag1 = listOf(
            sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 11400.månedlig.repeat(12)),
            sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), 45000.månedlig.repeat(12))
        )

        håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntekter = inntekter1, arbeidsforhold = arbeidsforhold1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag1))
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 11400.månedlig)
        )

        val arbeidsforhold2 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, 31.januar),
            Arbeidsforhold(a1, 1.mars, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null)

        )
        val inntekter2 = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 2.vedtaksperiode(a1)), 11400.månedlig.repeat(2)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 2.vedtaksperiode(a1)), 45000.månedlig.repeat(3))
        )
        val sammenligningsgrunnlag2 = listOf(
            sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 2.vedtaksperiode(a1)), 11400.månedlig.repeat(11)),
            sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 2.vedtaksperiode(a1)), 47000.månedlig.repeat(12))
        )

        håndterUtbetalingsgrunnlag(2.vedtaksperiode(a1), orgnummer = a1, inntekter = inntekter2, arbeidsforhold = arbeidsforhold2)
        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(2.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag2))
        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            listOf(2.februar til 17.februar),
            førsteFraværsdag = 2.februar,
            orgnummer = a2,
            refusjon = Refusjon(null, 45000.månedlig)
        )


        val inntekter3 = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode(a2)), 11400.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode(a2)), 45000.månedlig.repeat(3))
        )
        val sammenligningsgrunnlag3 = listOf(
            sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode(a2)), 11400.månedlig.repeat(12)),
            sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode(a2)), 45000.månedlig.repeat(12))
        )

        håndterUtbetalingsgrunnlag(1.vedtaksperiode(a2), orgnummer = a2, inntekter = inntekter3, arbeidsforhold = arbeidsforhold2)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterVilkårsgrunnlag(1.vedtaksperiode(a2), orgnummer = a2, inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag3))
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        val utbetaling = inspektør(a2).utbetalinger.single()
        val linje = utbetaling.arbeidsgiverOppdrag().linjerUtenOpphør().single()
        assertEquals(100.0, utbetaling.utbetalingstidslinje()[20.februar].økonomi.medData { _, _, _, _, totalGrad, _, _, _, _ -> totalGrad })
        assertEquals(2077, linje.beløp) // Ikke cappet på 6G, siden personen ikke jobber hos a1 ved dette skjæringstidspunktet
        assertEquals(18.februar, linje.fom)
        assertEquals(20.februar, linje.tom)
    }

    @Test
    fun `Vedtaksperioder med flere arbeidsforhold fra Aa-reg skal ha inntektskilde FLERE_ARBEIDSGIVERE`() = Toggles.FlereArbeidsgivereUlikFom.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH),
            Arbeidsforhold(a2, LocalDate.EPOCH)
        )
        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), INNTEKT.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), INNTEKT.repeat(3))
        )

        håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntekter = inntekter, arbeidsforhold = arbeidsforhold)

        assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode(a1)))
    }

    @Test
    fun `ignorerer arbeidsforhold med blanke orgnumre`() = Toggles.FlereArbeidsgivereUlikFom.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH),
            Arbeidsforhold("", LocalDate.EPOCH, 31.januar),
            Arbeidsforhold("", LocalDate.EPOCH),
            Arbeidsforhold(a2, LocalDate.EPOCH)
        )
        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), INNTEKT.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), INNTEKT.repeat(3))
        )

        håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntekter = inntekter, arbeidsforhold = arbeidsforhold)
        assertEquals(listOf(a1, a2), arbeidsgivere())
    }

    @Test
    fun `arbeidsforhold fom skjæringstidspunkt`() = Toggles.FlereArbeidsgivereUlikFom.enable {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
        )

        val arbeidsforhold = listOf(Arbeidsforhold(a1, 1.januar))
        val inntekter = listOf(grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), INNTEKT.repeat(3)))

        håndterUtbetalingsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntekter = inntekter, arbeidsforhold = arbeidsforhold)
        assertFalse(person.harVedtaksperiodeForArbeidsgiverMedUkjentArbeidsforhold(1.januar))
    }

    @Test
    fun `Person som bytter jobb i løpet av behandlingen skal blir markert med riktig warning`() = Toggles.FlereArbeidsgivereUlikFom.enable {
        val inntektshistorikk = listOf(Inntektsopplysning(a1, 9.september(2020), INNTEKT, true))
        val utbetalinger = ArbeidsgiverUtbetalingsperiode(a1, 9.september(2020), 30.september(2020), 100.prosent, INNTEKT)

        håndterSykmelding(Sykmeldingsperiode(1.oktober(2020), 31.oktober(2020), 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.oktober(2020), 31.oktober(2020), 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode(a1),
            utbetalinger,
            inntektshistorikk = inntektshistorikk,
            orgnummer = a1,
            besvart = LocalDate.EPOCH.atStartOfDay()
        )
        håndterUtbetalingsgrunnlag(
            1.vedtaksperiode(a1),
            a1,
            listOf(grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode(a1)), INNTEKT.repeat(3))),
            listOf(Arbeidsforhold(a1, 16.september(2020), null), Arbeidsforhold(a2, LocalDate.EPOCH, 15.september(2020)))
        )
        håndterYtelser(1.vedtaksperiode(a1), utbetalinger, orgnummer = a1, inntektshistorikk = inntektshistorikk, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 30.november(2020), 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.november(2020), 30.november(2020), 100.prosent), orgnummer = a1)
        håndterUtbetalingsgrunnlag(
            2.vedtaksperiode(a1),
            a1,
            listOf(grunnlag(a1, finnSkjæringstidspunkt(a1, 2.vedtaksperiode(a1)), INNTEKT.repeat(3))),
            listOf(Arbeidsforhold(a1, 16.september(2020), null), Arbeidsforhold(a2, LocalDate.EPOCH, 15.september(2020)))
        )
        håndterYtelser(2.vedtaksperiode(a1), utbetalinger, orgnummer = a1, inntektshistorikk = inntektshistorikk, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
        assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(2.vedtaksperiode(a1)))
        assertTrue(inspektør(a1).warnings.contains("Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig."))
    }

    fun arbeidsgivere(): List<String> {
        val arbeidsforhold = mutableListOf<String>()
        person.accept(object : PersonVisitor {
            override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
                arbeidsforhold.add(organisasjonsnummer)
            }
        })
        return arbeidsforhold
    }
}
