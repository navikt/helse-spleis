package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.reflection.castAsList
import no.nav.helse.serde.reflection.castAsMap
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        val grunnlagForSykepengegrunnlag = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 100.månedlig.repeat(3))
        )
        val sammenligningsgrunnlag = listOf(
            sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(12)),
            sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 100.månedlig.repeat(12))
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = 1.februar)
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(grunnlagForSykepengegrunnlag),
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )

        assertNoWarnings(a1.inspektør)
    }

    @Test
    fun `Filtrerer ut irrelevante arbeidsforhold per arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(3))
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )
        assertEquals(1, tellArbeidsforholdhistorikkinnslag(a1).size)
        assertEquals(1, tellArbeidsforholdhistorikkinnslag(a2).size)
    }

    @Test
    fun `Infotrygdforlengelse av arbeidsgiver som ikke finnes i aareg, kan utbetales uten warning`() {

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 28.februar, 100.prosent, 10000.månedlig))
        val inntektshistorikk = listOf(Inntektsopplysning(a1, 1.februar, 10000.månedlig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = utbetalinger, inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertTilstand(a1, TilstandType.AVSLUTTET)
        assertNoWarnings(inspektør(a1))
    }

    @Test
    fun `Tidligere periode fra gammel arbeidsgiver, deretter en infotrygdforlengelse fra nåværende arbeidsgiver, kan utbetales uten warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                listOf(sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(12)))
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3)))
            ),
            arbeidsforhold = listOf(Arbeidsforhold(a1, LocalDate.EPOCH, null))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a2, 14.februar, 28.februar, 100.prosent, 10000.månedlig))
        val inntektshistorikk = listOf(Inntektsopplysning(a2, 14.februar, 10000.månedlig, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = utbetalinger, inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        assertTilstand(a2, TilstandType.AVSLUTTET)
        assertNoWarnings(inspektør(a2))
    }

    @Test
    fun `Vanlige forlengelser av arbeidsgiver som ikke finnes i aareg, kan utbetales uten warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        val inntekter1 = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3))
        )
        val sammenligningsgrunnlag1 = listOf(
            sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(12))
        )
        val arbeidsforhold1 = emptyList<Arbeidsforhold>()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag1),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter1),
            arbeidsforhold = arbeidsforhold1,
            opptjening = Opptjeningvurdering(listOf(Arbeidsforhold(a1, LocalDate.EPOCH, null)))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)

        assertTilstand(a1, TilstandType.AVSLUTTET, 2)

        val sisteGodkjenningsbehov = inspektør(a1).sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning).detaljer()
        assertEquals(0, sisteGodkjenningsbehov["warnings"].castAsMap<String, Any>()["aktiviteter"].castAsList<Any>().size)

    }

    @Test
    fun `Flere aktive arbeidsforhold, men kun 1 med inntekt, skal ikke få warning for flere arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.juli(2021), 31.juli(2021), 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.juli(2021), 31.juli(2021), 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.juli(2021) til 16.juli(2021)),
            førsteFraværsdag = 1.juli(2021),
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3))
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = 2.januar(2020), tom = null),
            Arbeidsforhold(orgnummer = a2, fom = 1.mai(2019), tom = null)
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        assertNoWarnings(inspektør(a1))

    }

    @Test
    fun `arbeidsgivere med sammenligningsgrunnlag, men uten inntekt, skal ikke anses som ekstra arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1)

        val arbeidsforhold = listOf(Arbeidsforhold(a1, LocalDate.EPOCH))
        val inntekter = listOf(grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3)))

        val sammenligningsgrunnlag = inntektperioderForSammenligningsgrunnlag {
            1.januar(2017) til 1.desember(2017) inntekter {
                a1 inntekt INNTEKT
            }
            1.januar(2017) til 1.januar(2017) inntekter {
                a2 inntekt 10000
            }
        }

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        assertFalse(inspektør(a1).warnings.contains("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"))
        assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(2.vedtaksperiode))
    }

    @Test
    fun `Syk for a1, slutter i a1, syk for a2, a1 finnes ikke i Aa-reg lenger - ingen warning for manglende arbeidsforhold`() {
        /*
        * Siden vi ikke vet om arbeidsforhold for tidligere utbetalte perioder må vi passe på at ikke lar de periodene føre til advarsel på nye helt uavhengie vedtaksperioder
        * Sjekker kun arbeidsforhold for gjelende skjæringstidspunkt, derfor vil ikke mangel av arbeidsforhold for a1 skape problemer
        * */
        håndterSykmelding(Sykmeldingsperiode(1.mars(2017), 31.mars(2017), 50.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars(2017), 31.mars(2017), 50.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars(2017) til 16.mars(2017)),
            førsteFraværsdag = 1.mars(2017),
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        val inntekterA1 = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )
        val arbeidsforholdA1 = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null)
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1, inntektshistorikk = emptyList())
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12)))
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekterA1),
            arbeidsforhold = arbeidsforholdA1,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 50.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 50.prosent), orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a2
        )
        val inntekterA2 = listOf(
            grunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforholdA2 = listOf(
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekterA2),
            arbeidsforhold = arbeidsforholdA2,
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        val a2Linje = inspektør(a2).utbetalinger.last().arbeidsgiverOppdrag().last()
        assertEquals(17.mars, a2Linje.førstedato())
        assertEquals(31.mars, a2Linje.sistedato())
        assertEquals(692, a2Linje.beløp())

        assertNoWarnings(inspektør(a2))
    }

    @Test
    fun `lagrer kun arbeidsforhold som gjelder under skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        val inntekter1 = listOf(grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3)))
        val arbeidsforhold1 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, 1.januar),
            Arbeidsforhold(a1, 1.januar, null), // Skal gjelde
            Arbeidsforhold(a1, 28.februar, 1.mars), // Skal gjelde
            Arbeidsforhold(a1, 1.mars, 31.mars), // Skal gjelde
            Arbeidsforhold(a1, 1.februar, 28.februar),
            Arbeidsforhold(a1, 2.mars, 31.mars)
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter1),
            arbeidsforhold = arbeidsforhold1
        )
        assertEquals(3, tellArbeidsforholdINyesteHistorikkInnslag(a1))
    }

    @Test
    fun `opphold i arbeidsforhold skal ikke behandles som ghost`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 11400.månedlig,
            orgnummer = a1
        )

        val arbeidsforhold1 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH),
            Arbeidsforhold(a2, LocalDate.EPOCH)
        )
        val inntekter1 = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 11400.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 45000.månedlig.repeat(3))
        )
        val sammenligningsgrunnlag1 = listOf(
            sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 11400.månedlig.repeat(12)),
            sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 45000.månedlig.repeat(12))
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag1),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter1),
            arbeidsforhold = arbeidsforhold1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 11400.månedlig,
            orgnummer = a1
        )

        val arbeidsforhold2 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, 31.januar),
            Arbeidsforhold(a1, 1.mars, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null)

        )
        val inntekter2 = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 2.vedtaksperiode), 11400.månedlig.repeat(2)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 2.vedtaksperiode), 45000.månedlig.repeat(3))
        )
        val sammenligningsgrunnlag2 = listOf(
            sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 2.vedtaksperiode), 11400.månedlig.repeat(11)),
            sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 2.vedtaksperiode), 47000.månedlig.repeat(12))
        )

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            2.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag2),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter2),
            arbeidsforhold = arbeidsforhold2
        )
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            listOf(2.februar til 17.februar),
            førsteFraværsdag = 2.februar,
            beregnetInntekt = 45000.månedlig,
            orgnummer = a2
        )


        val inntekter3 = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), 11400.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), 45000.månedlig.repeat(3))
        )
        val sammenligningsgrunnlag3 = listOf(
            sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), 11400.månedlig.repeat(12)),
            sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), 45000.månedlig.repeat(12))
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a2,
            inntektsvurdering = Inntektsvurdering(sammenligningsgrunnlag3),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter3),
            arbeidsforhold = arbeidsforhold2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a2)

        val utbetaling = inspektør(a2).utbetalinger.single()
        val linje = utbetaling.arbeidsgiverOppdrag().linjerUtenOpphør().single()
        assertEquals(100.0, utbetaling.utbetalingstidslinje()[20.februar].økonomi.medData { _, _, _, _, totalGrad, _, _, _, _ -> totalGrad })
        assertEquals(2077, linje.beløp()) // Ikke cappet på 6G, siden personen ikke jobber hos a1 ved dette skjæringstidspunktet
        assertEquals(18.februar, linje.førstedato())
        assertEquals(20.februar, linje.sistedato())
    }

    @Test
    fun `Vedtaksperioder med flere arbeidsforhold fra Aa-reg skal ha inntektskilde FLERE_ARBEIDSGIVERE`() {
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
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3))
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold
        )

        assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
    }

    @Test
    fun `ignorerer arbeidsforhold med blanke orgnumre`() {
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
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3))
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold
        )
        assertEquals(listOf(a1, a2), arbeidsgivere())
    }

    @Test
    fun `arbeidsforhold fom skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
        )

        val arbeidsforhold = listOf(Arbeidsforhold(a1, 1.januar))
        val inntekter = listOf(grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(3)))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold
        )
        assertFalse(person.harVedtaksperiodeForArbeidsgiverMedUkjentArbeidsforhold(1.januar))
    }

    @Test
    fun `Forlengelser skal ikke få warning på at arbeidsgiver ikke finnes i Aa-reg`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                listOf(sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), INNTEKT.repeat(12)))
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
            arbeidsforhold = emptyList(),
            opptjening = Opptjeningvurdering(
                listOf(Arbeidsforhold(a1, 1.januar(2016), null))
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertEquals(1, inspektør(a1).warnings.count { it == "Arbeidsgiver er ikke registrert i Aa-registeret." })
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
