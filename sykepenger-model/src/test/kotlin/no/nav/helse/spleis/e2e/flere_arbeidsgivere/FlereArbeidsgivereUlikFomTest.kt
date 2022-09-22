package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Varselkode.RV_VV_8
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereUlikFomTest : AbstractEndToEndTest() {

    @Test
    fun `To førstegangsbehandlinger med ulik fom i forskjellige måneder - skal bruke skatteinntekter for arbeidsgiver med senest fom`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(28.februar til 15.mars), førsteFraværsdag = 28.februar, orgnummer = a1) // ff 1 mars
        håndterInntektsmelding(listOf(5.mars til 20.mars), førsteFraværsdag = 5.mars, beregnetInntekt = INNTEKT, orgnummer = a2)

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)

        Assertions.assertEquals(
            31000.månedlig,
            inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a1)?.omregnetÅrsinntekt()
        )
        Assertions.assertEquals(
            20000.månedlig,
            inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a2)?.omregnetÅrsinntekt()
        )
    }

    @Test
    fun `To førstegangsbehandlinger med lik fom - skal bruke inntektsmelding for begge arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 18000.månedlig,
            orgnummer = a2
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)


        Assertions.assertEquals(
            30000.månedlig,
            inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a1)?.omregnetÅrsinntekt()
        )
        Assertions.assertEquals(
            18000.månedlig,
            inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a2)?.omregnetÅrsinntekt()
        )

    }

    @Test
    fun `Bruker gjennomsnitt av skatteinntekter ved ulik fom i forskjellige måneder`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(28.februar til 15.mars), førsteFraværsdag = 28.februar, orgnummer = a1)
        håndterInntektsmelding(listOf(5.mars til 20.mars), førsteFraværsdag = 5.mars, beregnetInntekt = INNTEKT, orgnummer = a2)

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(2) + 23000.månedlig)
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(
                        a2,
                        finnSkjæringstidspunkt(a1, 1.vedtaksperiode),
                        20000.månedlig.repeat(11) + 23000.månedlig
                    )
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)


        Assertions.assertEquals(
            31000.månedlig,
            inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a1)?.omregnetÅrsinntekt()
        )
        Assertions.assertEquals(
            21000.månedlig,
            inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a2)?.omregnetÅrsinntekt()
        )
    }

    @Test
    fun `Ulik fom og ikke 6G-begrenset, utbetalinger beregnes riktig`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            beregnetInntekt = 20000.månedlig,
            orgnummer = a2
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(16.mars, a1Linje.fom)
        Assertions.assertEquals(30.mars, a1Linje.tom)
        Assertions.assertEquals(10000.månedlig.reflection { _, _, _, dagligInt -> dagligInt }, a1Linje.beløp)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(21.mars, a2Linje.fom)
        Assertions.assertEquals(30.mars, a2Linje.tom)
        Assertions.assertEquals(20000.månedlig.reflection { _, _, _, dagligInt -> dagligInt }, a2Linje.beløp)
    }

    @Test
    fun `Ulik fom og 6G-begrenset, skal beregne utbetaling ut fra skatteinntekter for a2`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val arbeidsgiverOppdrag = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag
        Assertions.assertEquals(2, arbeidsgiverOppdrag.size)
        val a1Linje1 = arbeidsgiverOppdrag[0]
        Assertions.assertEquals(16.mars, a1Linje1.fom)
        Assertions.assertEquals(20.mars, a1Linje1.tom)
        Assertions.assertEquals(997, a1Linje1.beløp)
        val a1Linje2 = arbeidsgiverOppdrag[1]
        Assertions.assertEquals(21.mars, a1Linje2.fom)
        Assertions.assertEquals(30.mars, a1Linje2.tom)
        Assertions.assertEquals(998, a1Linje2.beløp)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(21.mars, a2Linje.fom)
        Assertions.assertEquals(30.mars, a2Linje.tom)
        Assertions.assertEquals(1163, a2Linje.beløp)

    }

    @Test
    fun `Førstegangsbehandling med ulik fom og siste arbeidsgiver er 50 prosent sykmeldt`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 50.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 50.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)


        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(16.mars, a1Linje.fom)
        Assertions.assertEquals(30.mars, a1Linje.tom)
        Assertions.assertEquals(997, a1Linje.beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(21.mars, a2Linje.fom)
        Assertions.assertEquals(30.mars, a2Linje.tom)
        Assertions.assertEquals(582, a2Linje.beløp)

    }

    @Test
    fun `Førstegangsbehandling med ulik fom og første arbeidsgiver er 50 prosent sykmeldt`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars, 50.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.februar, 31.mars, 50.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(5.mars til 20.mars),
            førsteFraværsdag = 5.mars,
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)


        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(16.mars, a1Linje.fom)
        Assertions.assertEquals(30.mars, a1Linje.tom)
        Assertions.assertEquals(499, a1Linje.beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(21.mars, a2Linje.fom)
        Assertions.assertEquals(30.mars, a2Linje.tom)
        Assertions.assertEquals(1163, a2Linje.beløp)

    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom`() {

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 28.februar, 100.prosent), orgnummer = a3)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 28.februar, 100.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.februar, 100.prosent), orgnummer = a4)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 15.februar, 100.prosent), orgnummer = a4)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            førsteFraværsdag = 2.januar,
            beregnetInntekt = 32000.månedlig,
            orgnummer = a2
        )
        håndterInntektsmelding(
            listOf(3.januar til 18.januar),
            førsteFraværsdag = 3.januar,
            beregnetInntekt = 33000.månedlig,
            orgnummer = a3
        )
        håndterInntektsmelding(
            listOf(4.januar til 19.januar),
            førsteFraværsdag = 4.januar,
            beregnetInntekt = 34000.månedlig,
            orgnummer = a4
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(3)),
            grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 33000.månedlig.repeat(3)),
            grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 34000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 33000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 34000.månedlig.repeat(12))
                ),
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        håndterYtelser(1.vedtaksperiode, orgnummer = a4)
        håndterSimulering(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalt(orgnummer = a4)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(17.januar, a1Linje.fom)
        Assertions.assertEquals(31.januar, a1Linje.tom)
        Assertions.assertEquals(515, a1Linje.beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(18.januar, a2Linje.fom)
        Assertions.assertEquals(15.mars, a2Linje.tom)
        Assertions.assertEquals(532, a2Linje.beløp)

        val a3Linje1 = inspektør(a3).utbetalinger.last().inspektør.arbeidsgiverOppdrag[0]
        Assertions.assertEquals(19.januar, a3Linje1.fom)
        Assertions.assertEquals(15.februar, a3Linje1.tom)
        Assertions.assertEquals(549, a3Linje1.beløp)
        val a3Linje2 = inspektør(a3).utbetalinger.last().inspektør.arbeidsgiverOppdrag[1]
        Assertions.assertEquals(16.februar, a3Linje2.fom)
        Assertions.assertEquals(28.februar, a3Linje2.tom)
        Assertions.assertEquals(548, a3Linje2.beløp)

        val a4Linje = inspektør(a4).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(20.januar, a4Linje.fom)
        Assertions.assertEquals(15.februar, a4Linje.tom)
        Assertions.assertEquals(565, a4Linje.beløp)

    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom men lik slutt`() {

        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars, 100.prosent), orgnummer = a3)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 15.mars, 100.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars, 100.prosent), orgnummer = a4)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 15.mars, 100.prosent), orgnummer = a4)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            førsteFraværsdag = 2.januar,
            beregnetInntekt = 32000.månedlig,
            orgnummer = a2
        )
        håndterInntektsmelding(
            listOf(3.januar til 18.januar),
            førsteFraværsdag = 3.januar,
            beregnetInntekt = 33000.månedlig,
            orgnummer = a3
        )
        håndterInntektsmelding(
            listOf(4.januar til 19.januar),
            førsteFraværsdag = 4.januar,
            beregnetInntekt = 34000.månedlig,
            orgnummer = a4
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31500.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32500.månedlig.repeat(3)),
            grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 33500.månedlig.repeat(3)),
            grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 34500.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 33000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 34000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        håndterYtelser(1.vedtaksperiode, orgnummer = a4)
        håndterSimulering(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalt(orgnummer = a4)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(17.januar, a1Linje.fom)
        Assertions.assertEquals(15.mars, a1Linje.tom)
        Assertions.assertEquals(515, a1Linje.beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(18.januar, a2Linje.fom)
        Assertions.assertEquals(15.mars, a2Linje.tom)
        Assertions.assertEquals(532, a2Linje.beløp)

        val a3Linje = inspektør(a3).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(19.januar, a3Linje.fom)
        Assertions.assertEquals(15.mars, a3Linje.tom)
        Assertions.assertEquals(549, a3Linje.beløp)

        val a4Linje = inspektør(a4).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(20.januar, a4Linje.fom)
        Assertions.assertEquals(15.mars, a4Linje.tom)
        Assertions.assertEquals(565, a4Linje.beløp)

    }

    @Test
    fun `Fire arbeidsgivere - overlappende perioder med ulik fom men lik slutt, nå med gradert sykmelding!`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 15.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars, 50.prosent), orgnummer = a3)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 15.mars, 50.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars, 100.prosent), orgnummer = a4)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 15.mars, 100.prosent), orgnummer = a4)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            førsteFraværsdag = 2.januar,
            beregnetInntekt = 32000.månedlig,
            orgnummer = a2
        )
        håndterInntektsmelding(
            listOf(3.januar til 18.januar),
            førsteFraværsdag = 3.januar,
            beregnetInntekt = 33000.månedlig,
            orgnummer = a3
        )
        håndterInntektsmelding(
            listOf(4.januar til 19.januar),
            førsteFraværsdag = 4.januar,
            beregnetInntekt = 34000.månedlig,
            orgnummer = a4
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31500.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32500.månedlig.repeat(3)),
            grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 33500.månedlig.repeat(3)),
            grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 34500.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 33000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 34000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        håndterYtelser(1.vedtaksperiode, orgnummer = a4)
        håndterSimulering(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalt(orgnummer = a4)

        val a1Linjer = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag
        Assertions.assertEquals(1, a1Linjer.size)
        Assertions.assertEquals(17.januar, a1Linjer[0].fom)
        Assertions.assertEquals(15.mars, a1Linjer[0].tom)
        Assertions.assertEquals(515, a1Linjer[0].beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(18.januar, a2Linje.fom)
        Assertions.assertEquals(15.mars, a2Linje.tom)
        Assertions.assertEquals(532, a2Linje.beløp)

        // Siden maksbeløpet blir større når vi får inn flere arbeidsgivere vil vi få en ekstra krone som blir med i kronerulleringen(avrunning), det fører
        // til at vi fordeler den til den arbeidsgiveren som tapte mest på avrunningen
        val a3Linje = inspektør(a3).utbetalinger.last().inspektør.arbeidsgiverOppdrag[0]
        Assertions.assertEquals(19.januar, a3Linje.fom)
        Assertions.assertEquals(19.januar, a3Linje.tom)
        Assertions.assertEquals(274, a3Linje.beløp)
        val a3Linje2 = inspektør(a3).utbetalinger.last().inspektør.arbeidsgiverOppdrag[1]
        Assertions.assertEquals(20.januar, a3Linje2.fom)
        Assertions.assertEquals(15.mars, a3Linje2.tom)
        Assertions.assertEquals(275, a3Linje2.beløp)

        val a4Linje = inspektør(a4).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(20.januar, a4Linje.fom)
        Assertions.assertEquals(15.mars, a4Linje.tom)
        Assertions.assertEquals(565, a4Linje.beløp)

    }

    @Test
    fun `Wow! Her var det mye greier - ulik fom, lik tom, forskjellig gradering for alle arbeidsgivere`() {

        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 22.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 15.mars, 22.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 15.mars, 69.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 15.mars, 69.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.mars, 42.prosent), orgnummer = a3)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(3.januar, 15.mars, 42.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.januar, 15.mars, 37.prosent), orgnummer = a4)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 15.mars, 37.prosent), orgnummer = a4)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(2.januar til 17.januar),
            førsteFraværsdag = 2.januar,
            beregnetInntekt = 32000.månedlig,
            orgnummer = a2
        )
        håndterInntektsmelding(
            listOf(3.januar til 18.januar),
            førsteFraværsdag = 3.januar,
            beregnetInntekt = 33000.månedlig,
            orgnummer = a3
        )
        håndterInntektsmelding(
            listOf(4.januar til 19.januar),
            førsteFraværsdag = 4.januar,
            beregnetInntekt = 34000.månedlig,
            orgnummer = a4
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31500.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32500.månedlig.repeat(3)),
            grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 33500.månedlig.repeat(3)),
            grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 34500.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 33000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 34000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        håndterYtelser(1.vedtaksperiode, orgnummer = a4)
        håndterSimulering(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalt(orgnummer = a4)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(18.januar, a1Linje.fom)
        Assertions.assertEquals(15.mars, a1Linje.tom)
        Assertions.assertEquals(113, a1Linje.beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(18.januar, a2Linje.fom)
        Assertions.assertEquals(15.mars, a2Linje.tom)
        Assertions.assertEquals(367, a2Linje.beløp)

        val a3Linje = inspektør(a3).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(19.januar, a3Linje.fom)
        Assertions.assertEquals(15.mars, a3Linje.tom)
        Assertions.assertEquals(231, a3Linje.beløp)

        val a4Linje = inspektør(a4).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        Assertions.assertEquals(20.januar, a4Linje.fom)
        Assertions.assertEquals(15.mars, a4Linje.tom)
        Assertions.assertEquals(209, a4Linje.beløp)

    }

    @Test
    fun `Flere arbeidsgivere med ulik fom - skal få warning om flere arbeidsforhold med ulikt sykefravær`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(4.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )

        håndterInntektsmelding(
            listOf(4.mars til 19.mars),
            førsteFraværsdag = 4.mars,
            beregnetInntekt = 19000.månedlig,
            orgnummer = a2
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
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = LocalDate.EPOCH, ansattTom = null)
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertVarsel("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold", 1.vedtaksperiode.filter(a1))
        assertIngenVarsel(RV_VV_8, 1.vedtaksperiode.filter(a1))

    }

    @Test
    fun `Flere arbeidsgivere med lik fom - skal ikke få warning om flere arbeidsforhold med ulikt sykefravær`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 19000.månedlig,
            orgnummer = a2
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
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = LocalDate.EPOCH, ansattTom = null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            arbeidsforhold = arbeidsforhold,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            )
        )

        assertIngenVarsler()

    }

    @Test
    fun `Ulik fom og 6G-begrenset, to dager med utbetaling hos første arbeidsgiver før andre arbeidsgiver blir syk skal fortsatt 6G-cappe mht begge AG`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.februar, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(20.mars, 25.april, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(20.mars, 25.april, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(20.mars til 4.april),
            førsteFraværsdag = 20.mars,
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.last()
        Assertions.assertEquals(16.mars, a1Linje.fom)
        Assertions.assertEquals(30.mars, a1Linje.tom)
        Assertions.assertEquals(997, a1Linje.beløp)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.last()
        Assertions.assertEquals(5.april, a2Linje.fom)
        Assertions.assertEquals(25.april, a2Linje.tom)
        Assertions.assertEquals(1163, a2Linje.beløp)

    }

    @Test
    fun `Ulik fom og 6G-begrenset, to dager med utbetaling hos første arbeidsgiver før andre arbeidsgiver blir syk skal fortsatt 6G-cappe mht begge AG, nå med gradert sykmelding!`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 31.mars, 50.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.februar, 31.mars, 50.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(20.mars, 25.april, 70.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(20.mars, 25.april, 70.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(28.februar til 15.mars),
            førsteFraværsdag = 28.februar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(20.mars til 4.april),
            førsteFraværsdag = 20.mars,
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.last()
        Assertions.assertEquals(16.mars, a1Linje.fom)
        Assertions.assertEquals(30.mars, a1Linje.tom)
        Assertions.assertEquals(499, a1Linje.beløp)

        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a2)

        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.last()
        Assertions.assertEquals(5.april, a2Linje.fom)
        Assertions.assertEquals(25.april, a2Linje.tom)
        Assertions.assertEquals(814, a2Linje.beløp)
    }

    @Test
    fun `skjæringstidspunkt i samme måned betyr at begge arbeidsgivere bruker inntekt fra inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars, beregnetInntekt = 31000.månedlig, orgnummer = a1)
        håndterInntektsmelding(listOf(5.mars til 20.mars), førsteFraværsdag = 5.mars, beregnetInntekt = 21000.månedlig, orgnummer = a2)

        val inntekterFraSkatt = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekterFraSkatt,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )

        Assertions.assertEquals(
            31000.månedlig,
            inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a1)?.omregnetÅrsinntekt()
        )
        Assertions.assertInstanceOf(
            Inntektshistorikk.Inntektsmelding::class.java,
            inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a1)
        )

        Assertions.assertEquals(
            21000.månedlig,
            inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a2)?.omregnetÅrsinntekt()
        )
        Assertions.assertInstanceOf(
            Inntektshistorikk.Inntektsmelding::class.java,
            inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a2)
        )
    }

    @Test
    fun `skjæringstidspunkt i forskjellige måneder betyr at senere arbeidsgiver bruker skatteinntekt`() {
        håndterSykmelding(Sykmeldingsperiode(28.februar, 30.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 30.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.februar, 30.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 30.mars, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(28.februar til 15.mars), beregnetInntekt = 31000.månedlig, orgnummer = a1)
        håndterInntektsmelding(listOf(1.mars til 16.mars), beregnetInntekt = 21000.månedlig, orgnummer = a2)

        val inntekterFraSkatt = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekterFraSkatt,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )

        Assertions.assertEquals(
            31000.månedlig,
            inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a1)?.omregnetÅrsinntekt()
        )
        Assertions.assertInstanceOf(
            Inntektshistorikk.Inntektsmelding::class.java,
            inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a1)
        )

        Assertions.assertEquals(
            20000.månedlig,
            inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a2)?.omregnetÅrsinntekt()
        )
        Assertions.assertInstanceOf(
            Inntektshistorikk.SkattComposite::class.java,
            inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a2)
        )
    }

    @Test
    fun `To arbeidsgivere med ulik fom i samme måned - med en tidligere periode i samme måned - andre vedtaksperiode velger IM for egen første fraværsdag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 18.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 18.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        val sammenligningsgrunnlag = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                sammenligningsgrunnlag(a2, 1.januar, 32000.månedlig.repeat(12))
            )
        )
        val sykepengegrunnlag = InntektForSykepengegrunnlag(
            inntekter = listOf(
                grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                grunnlag(a2, 1.januar, INNTEKT.repeat(3))
            ), arbeidsforhold = emptyList()
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(22.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(21.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(22.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 21.januar, orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 22.januar, beregnetInntekt = 32000.månedlig, orgnummer = a2)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            2.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag,
            orgnummer = a1
        )

        val inntektsopplysning = inspektør(a1).vilkårsgrunnlag(2.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()?.get(a2)
        Assertions.assertInstanceOf(Inntektshistorikk.Inntektsmelding::class.java, inntektsopplysning)
        Assertions.assertEquals(32000.månedlig, inntektsopplysning?.omregnetÅrsinntekt())
    }

    @Disabled("TODO: Utbetalingstidslinjen gir ikke mening")
    @Test
    fun `Fire arbeidsgivere, to med fom i januar og to med fom i februar `() {
        // Arbeidsgiverne med fom i januar skal bruke inntektsmelding
        // Arbeidsgiverne med fom i februar skal bruke inntekt fra skatt
        håndterSykmelding(Sykmeldingsperiode(28.januar, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(28.januar, 31.mars, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(30.januar, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(30.januar, 31.mars, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 31.mars, 100.prosent), orgnummer = a3)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 31.mars, 100.prosent), orgnummer = a3)

        håndterSykmelding(Sykmeldingsperiode(4.februar, 31.mars, 100.prosent), orgnummer = a4)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.februar, 31.mars, 100.prosent), orgnummer = a4)

        håndterInntektsmelding(
            listOf(28.januar til 12.februar),
            førsteFraværsdag = 28.januar,
            beregnetInntekt = 31000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(30.januar til 14.februar),
            førsteFraværsdag = 30.januar,
            beregnetInntekt = 32000.månedlig,
            orgnummer = a2
        )
        håndterInntektsmelding(
            listOf(1.februar til 16.februar),
            førsteFraværsdag = 1.februar,
            beregnetInntekt = 33000.månedlig,
            orgnummer = a3
        )
        håndterInntektsmelding(
            listOf(4.februar til 19.februar),
            førsteFraværsdag = 4.februar,
            beregnetInntekt = 34000.månedlig,
            orgnummer = a4
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31500.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32500.månedlig.repeat(3)),
            grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 33500.månedlig.repeat(3)),
            grunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 34500.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 33000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a4, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 34000.månedlig.repeat(12))
                ),
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        håndterYtelser(1.vedtaksperiode, orgnummer = a4)
        håndterSimulering(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a4)
        håndterUtbetalt(orgnummer = a4)

        Assertions.assertInstanceOf(
            Inntektshistorikk.Inntektsmelding::class.java,
            inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a1)
        )
        Assertions.assertInstanceOf(
            Inntektshistorikk.Inntektsmelding::class.java,
            inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a2)
        )
        Assertions.assertInstanceOf(
            Inntektshistorikk.SkattComposite::class.java,
            inspektør(a3).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a3)
        )
        Assertions.assertInstanceOf(
            Inntektshistorikk.SkattComposite::class.java,
            inspektør(a4).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a4)
        )

        Assertions.assertEquals(
            31000.månedlig,
            inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a1)?.omregnetÅrsinntekt()
        )
        Assertions.assertEquals(
            32000.månedlig,
            inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a2)?.omregnetÅrsinntekt()
        )
        Assertions.assertEquals(
            33500.månedlig,
            inspektør(a3).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a3)?.omregnetÅrsinntekt()
        )
        Assertions.assertEquals(
            34500.månedlig,
            inspektør(a4).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()
                ?.get(a4)?.omregnetÅrsinntekt()
        )

        val a1Linjer = inspektør(a1).utbetalinger.last().arbeidsgiverOppdrag()
        Assertions.assertEquals(511, a1Linjer[0].beløp)

        val a2Linje = inspektør(a2).utbetalinger.last().inspektør.arbeidsgiverOppdrag.last()
        Assertions.assertEquals(528, a2Linje.beløp)

        val a3Linje = inspektør(a3).utbetalinger.last().inspektør.arbeidsgiverOppdrag.last()
        Assertions.assertEquals(553, a3Linje.beløp)

        val a4Linje = inspektør(a4).utbetalinger.last().inspektør.arbeidsgiverOppdrag.last()
        Assertions.assertEquals(569, a4Linje.beløp)
    }

    @Test
    fun `alle arbeidsgivere burde hoppe inn i AVVENTER_BLOKKERENDE_PERIODE dersom de har samme skjæringstidspunkt men ikke overlapper`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a3)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a3)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a3)

        assertTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            TilstandType.AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a3)
    }
}