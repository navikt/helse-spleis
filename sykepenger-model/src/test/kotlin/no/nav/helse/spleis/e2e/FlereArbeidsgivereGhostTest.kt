package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class FlereArbeidsgivereGhostTest : AbstractEndToEndTest() {
    private companion object {
        private const val a1 = "arbeidsgiver 1"
        private const val a2 = "arbeidsgiver 2"
    }

    @Test
    fun `ghost n stuff`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
            refusjon = Refusjon(null, 31000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1063, a1Linje.beløp)
        assertEquals(
            Inntektskilde.FLERE_ARBEIDSGIVERE,
            inspektør(a1).inntektskilde(1.vedtaksperiode)
        )
    }

    @Test
    fun `Førstegangsbehandling med ghost - skal få warning om flere arbeidsforhold med ulikt sykefravær`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 10000.månedlig, emptyList())
        )
        val inntekter = listOf(
            grunnlag(
                a1, finnSkjæringstidspunkt(
                    a1, 1.vedtaksperiode
                ), 10000.månedlig.repeat(3)
            ),
            grunnlag(
                a2, finnSkjæringstidspunkt(
                    a1, 1.vedtaksperiode
                ), 20000.månedlig.repeat(3)
            )
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertWarnings(inspektør(a1))
        assertTrue(inspektør(a1).warnings.contains("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"))
        assertFalse(inspektør(a1).warnings.contains("Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig."))
        assertEquals(
            Inntektskilde.FLERE_ARBEIDSGIVERE,
            inspektør(a1).inntektskilde(1.vedtaksperiode)
        )
    }


    @Test
    fun `En førstegangsbehandling og et arbeidsforhold som starter etter skjæringstidspunktet - ghostn't (inaktive arbeidsforholdet) skal ikke påvirke beregningen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
            refusjon = Refusjon(null, 31000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, 2.januar, null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12))
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

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1431, a1Linje.beløp)
        assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))

    }

    @Test
    fun `En førstegangsbehandling og et arbeidsforhold som slutter før skjæringstidspunktet - ghostn't (inaktive arbeidsforholdet) skal ikke påvirke beregningen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
            refusjon = Refusjon(null, 31000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(1))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, 1.desember(2017), 31.desember(2017))
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(1))
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

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1431, a1Linje.beløp)
        assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))

    }

    @Test
    fun `Ghosts har ikke ubetalinger, men er med i beregningen for utbetaling av arbeidsgiver med sykdom`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
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

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().last()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(31.mars, a1Linje.tom)
        assertEquals(997, a1Linje.beløp)

        assertTrue(inspektør(a2).utbetalinger.isEmpty())
        assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
    }

    @Test
    fun `spøkelse med varierende grad`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 50.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 50.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
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

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().last()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(31.mars, a1Linje.tom)
        assertEquals(499, a1Linje.beløp)

        assertTrue(inspektør(a2).utbetalinger.isEmpty())
    }

    @Test
    fun `en forlengelse av et ghost tilfelle vil fortsatt bruke arbeidsdagene for forrige periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
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

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag().single()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(30.april, a1Linje.tom)
        assertEquals(997, a1Linje.beløp)

        assertTrue(inspektør(a2).utbetalinger.isEmpty())
        assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
    }

    @Test
    fun `En ghost som er registrert i Aa-reg mer enn 3 måneder før skjæringstidspunkt og ikke har hatt inntekt de siste 3 månedene før skjæringstidspunktet skal ikke markeres som FLERE_ARBEIDSGIVERE`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            orgnummer = a1,
            refusjon = Refusjon(null, 10000.månedlig, emptyList())
        )
        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3))
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(12)))
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertEquals(Inntektskilde.EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))

    }

    @Test
    fun `bruker har fyllt inn ANDRE_ARBEIDSFORHOLD uten sykmelding i søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(false, "ANDRE_ARBEIDSFORHOLD")),
            orgnummer = a1
        )

        assertNoErrors(inspektør(a1))
    }

    @Test
    fun `bruker har fyllt inn ANDRE_ARBEIDSFORHOLD med sykmelding i søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD")),
            orgnummer = a1
        )

        assertErrors(inspektør(a1))
    }

    @Test
    fun `Ignorerer skatteinntekter hentet før vilkårsgrunnlag for infotrygd ble lagret`() {
        /*
        * Denne testen passer på at vi ikke tar med skatteinntekter ved overgang fra infotrygd.
        * Tidligere hentet vi skatteinntekter i tilstanden Utbetalingsgrunnlag som lå rett før AvventerHistorikk.
        * For de vedtaksperiodene som allerede hadde hentet skatteinntekter via denne tilstanden,
        * ble skatteinntektene lagret og tatt med i sykepengegrunnlaget for infotrygd.
        * Sykepengegrunnlaget for infotrygd skal utelukkende være basert på inntekter fra infotrygd
        * */

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 31.januar, 100.prosent, INNTEKT))
        val inntektshistorikk = listOf(Inntektsopplysning(a1, 17.januar, INNTEKT, true))

        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1, utbetalinger = utbetalinger, inntektshistorikk = inntektshistorikk)

        person.lagreArbeidsforhold(a1, listOf(Arbeidsforhold(a1, LocalDate.EPOCH)), person.aktivitetslogg, 17.januar)
        person.lagreArbeidsforhold(a2, listOf(Arbeidsforhold(a2, LocalDate.EPOCH)), person.aktivitetslogg, 17.januar)

        inspektør(a2).inntektInspektør.inntektshistorikk {
            val hendelseId = UUID.randomUUID()
            addSkattSykepengegrunnlag(17.januar, hendelseId, INNTEKT, YearMonth.of(2017, 12), LØNNSINNTEKT, "fordel", "beskrivelse")
            addSkattSykepengegrunnlag(17.januar, hendelseId, INNTEKT, YearMonth.of(2017, 11), LØNNSINNTEKT, "fordel", "beskrivelse")
            addSkattSykepengegrunnlag(17.januar, hendelseId, INNTEKT, YearMonth.of(2017, 10), LØNNSINNTEKT, "fordel", "beskrivelse")
        }

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertEquals(1431, inspektør(a1).arbeidsgiverOppdrag.last().first().beløp)
    }

    @Test
    fun `Overgang fra infotrygd skal ikke få ghost warning selv om vi har lagret skatteinntekter i en gammel versjon av spleis`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 31.januar, 100.prosent, INNTEKT))
        val inntektshistorikk = listOf(Inntektsopplysning(a1, 17.januar, INNTEKT, true))

        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1, utbetalinger = utbetalinger, inntektshistorikk = inntektshistorikk)

        person.lagreArbeidsforhold(a1, listOf(Arbeidsforhold(a1, LocalDate.EPOCH)), person.aktivitetslogg, 17.januar)
        person.lagreArbeidsforhold(a2, listOf(Arbeidsforhold(a2, LocalDate.EPOCH)), person.aktivitetslogg, 17.januar)

        inspektør(a2).inntektInspektør.inntektshistorikk {
            val hendelseId = UUID.randomUUID()
            addSkattSykepengegrunnlag(17.januar, hendelseId, INNTEKT, YearMonth.of(2017, 12), LØNNSINNTEKT, "fordel", "beskrivelse")
            addSkattSykepengegrunnlag(17.januar, hendelseId, INNTEKT, YearMonth.of(2017, 11), LØNNSINNTEKT, "fordel", "beskrivelse")
            addSkattSykepengegrunnlag(17.januar, hendelseId, INNTEKT, YearMonth.of(2017, 10), LØNNSINNTEKT, "fordel", "beskrivelse")
        }

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertNoWarnings(a1.inspektør)
    }

    @Test
    fun `Forlengelse etter ping-pong (Spleis-IT-Spleis) skal få ghost warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
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

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        val utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(a1, 1.februar, 28.februar, 100.prosent, 30000.månedlig))
        val inntektshistorikk = listOf(Inntektsopplysning(a1, 1.februar, 30000.månedlig, true))

        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1, utbetalinger = utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertWarningTekst(
            a1.inspektør,
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold",
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"
        )
    }

    @Test
    fun `Spleisforlengelse skal få ghost warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            orgnummer = a1,
            refusjon = Refusjon(null, 30000.månedlig, emptyList())
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(orgnummer = a1, fom = LocalDate.EPOCH, tom = null),
            Arbeidsforhold(orgnummer = a2, fom = LocalDate.EPOCH, tom = null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
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

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertWarningTekst(
            a1.inspektør,
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold",
            "Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"
        )
    }
}
