package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.lagStandardSammenligningsgrunnlag
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RE_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_8
import no.nav.helse.person.inntekt.IkkeRapportert
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsforhold
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.UtbetalingInntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.utbetalingslinjer.UtbetalingInntektskilde.FLERE_ARBEIDSGIVERE
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import no.nav.helse.person.inntekt.Inntektsmelding as InntektsmeldingInntekt

internal class FlereArbeidsgivereGhostTest : AbstractEndToEndTest() {

    @Test
    fun `blir syk fra ghost lagrer ikke refusjonsopplysninger tilbake til skjæringstidspunktet`() {
        utbetalPeriodeMedGhost(tilGodkjenning = true)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 18.januar, orgnummer = a2)
        assertVarsel(RV_RE_1, 1.vedtaksperiode.filter(a2))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
    }


    @Test
    fun `blir syk fra ghost`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(listOf(a1 to INNTEKT, a2 to INNTEKT), 1.januar),
            inntektsvurdering = lagStandardSammenligningsgrunnlag(listOf(a1 to INNTEKT, a2 to INNTEKT), 1.januar),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH),
            ), orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)

        assertVarsel(RV_IM_4, AktivitetsloggFilter.arbeidsgiver(a1))
        assertVarsel(RV_IM_4, AktivitetsloggFilter.arbeidsgiver(a2))
        assertVarsel(RV_RE_1, AktivitetsloggFilter.arbeidsgiver(a2))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, orgnummer = a1)
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    @Test
    fun `blir syk fra ghost og inntektsmeldingen kommer først`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(listOf(a1 to INNTEKT, a2 to INNTEKT), 1.januar),
            inntektsvurdering = lagStandardSammenligningsgrunnlag(listOf(a1 to INNTEKT, a2 to INNTEKT), 1.januar),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH),
            ), orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `Korrigerende refusjonsopplysninger på arbeidsgiver med skatteinntekt i sykepengegrunnlaget`() {
        utbetalPeriodeMedGhost()
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        val inntektsmelding = håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2, beregnetInntekt = 32000.månedlig)
        assertVarsel(RV_RE_1, 1.vedtaksperiode.filter(a2))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertInntektstype(1.januar, mapOf(a1 to InntektsmeldingInntekt::class, a2 to SkattSykepengegrunnlag::class))

        assertEquals(
            listOf(Refusjonsopplysning(inntektsmelding, 1.januar, 31.januar, beløp = 32000.månedlig), Refusjonsopplysning(inntektsmelding, 1.februar, null, beløp = 32000.månedlig)),
            inspektør(a2).refusjonsopplysningerFraVilkårsgrunnlag(1.januar).inspektør.refusjonsopplysninger
        )
        val korrigerendeInntektsmelding = håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.februar til 16.februar),
            orgnummer = a2,
            beregnetInntekt = 32000.månedlig,
            førsteFraværsdag = 20.februar,
            refusjon = Inntektsmelding.Refusjon(beløp = 30000.månedlig, null)
        )
        assertInntektstype(1.januar, mapOf(a1 to InntektsmeldingInntekt::class, a2 to SkattSykepengegrunnlag::class))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmelding, 1.januar, 31.januar, beløp = 32000.månedlig),
                Refusjonsopplysning(inntektsmelding, 1.februar, 19.februar, beløp = 32000.månedlig),
                Refusjonsopplysning(korrigerendeInntektsmelding, 20.februar, null, beløp = 30000.månedlig)
            ),
            inspektør(a2).refusjonsopplysningerFraVilkårsgrunnlag(1.januar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `ghost n stuff`() {
        utbetalPeriodeMedGhost()

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1063, a1Linje.beløp)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
    }

    @Test
    fun `ny ghost etter tidligere ghostperiode`() {
        utbetalPeriodeMedGhost()

        håndterSykmelding(Sykmeldingsperiode(26.mars, 10.april), orgnummer = a1)
        håndterSøknad(Sykdom(26.mars, 10.april, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar),
            førsteFraværsdag = 26.mars,
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 2.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 2.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )

        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 2.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 2.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val oppdrag = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag

        val a1Linje = oppdrag.first()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1063, a1Linje.beløp)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
        val a1Linje2 = oppdrag.last()
        assertEquals(26.mars, a1Linje2.fom)
        assertEquals(10.april, a1Linje2.tom)
        assertEquals(1063, a1Linje2.beløp)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(2.vedtaksperiode))
    }

    @Test
    fun `Førstegangsbehandling med ghost - skal få warning om flere arbeidsforhold med ulikt sykefravær`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
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
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
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

        assertVarsler()
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(a1))
        assertIngenVarsel(RV_VV_8, 1.vedtaksperiode.filter(a1))
        assertEquals(
            FLERE_ARBEIDSGIVERE,
            inspektør(a1).inntektskilde(1.vedtaksperiode)
        )
    }


    @Test
    fun `En førstegangsbehandling og et arbeidsforhold som starter etter skjæringstidspunktet - ghostn't (inaktive arbeidsforholdet) skal ikke påvirke beregningen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, 2.januar, null)
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12))
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
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1431, a1Linje.beløp)
        assertEquals(EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))

    }

    @Test
    fun `En førstegangsbehandling og et arbeidsforhold som slutter før skjæringstidspunktet - ghostn't (inaktive arbeidsforholdet) skal ikke påvirke beregningen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(1))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, 1.desember(2017), 31.desember(2017))
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(1))
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
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        assertEquals(17.januar, a1Linje.fom)
        assertEquals(15.mars, a1Linje.tom)
        assertEquals(1431, a1Linje.beløp)
        assertEquals(EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))

    }

    @Test
    fun `Ghosts har ikke ubetalinger, men er med i beregningen for utbetaling av arbeidsgiver med sykdom`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = LocalDate.EPOCH, ansattTom = null)
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
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
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.last()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(30.mars, a1Linje.tom)
        assertEquals(997, a1Linje.beløp)

        Assertions.assertTrue(inspektør(a2).utbetalinger.isEmpty())
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
    }

    @Test
    fun `spøkelse med varierende grad`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 50.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = LocalDate.EPOCH, ansattTom = null)
        )

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
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.last()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(30.mars, a1Linje.tom)
        assertEquals(499, a1Linje.beløp)

        Assertions.assertTrue(inspektør(a2).utbetalinger.isEmpty())
    }

    @Test
    fun `en forlengelse av et ghost tilfelle vil fortsatt bruke arbeidsdagene for forrige periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = LocalDate.EPOCH, ansattTom = null)
        )

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
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april), orgnummer = a1)
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val a1Linje = inspektør(a1).utbetalinger.last().inspektør.arbeidsgiverOppdrag.single()
        assertEquals(17.mars, a1Linje.fom)
        assertEquals(30.april, a1Linje.tom)
        assertEquals(997, a1Linje.beløp)

        Assertions.assertTrue(inspektør(a2).utbetalinger.isEmpty())
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
    }

    @Test
    fun `Tar ikke med arbeidsforhold dersom personen startet i jobb mer enn 2 måneder før skjæringstidspunktet og ikke har inntekt de 2 siste månedene`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3))
        )
        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = 31.desember(2017), ansattTom = null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(
                        a1,
                        finnSkjæringstidspunkt(a1, 1.vedtaksperiode),
                        10000.månedlig.repeat(12)
                    )
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

        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

        assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
        assertEquals(120000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(120000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(120000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(no.nav.helse.person.inntekt.Inntektsmelding::class, it.inntektsopplysning::class)
        }
        assertEquals(1, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(10000.månedlig, it.rapportertInntekt)
        }
    }

    @Test
    fun `Tar med arbeidsforhold dersom personen startet i jobb mindre enn 2 måneder før skjæringstidspunktet, selvom det mangler inntekt de 2 siste månedene`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3))
        )
        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = 2.januar, ansattTom = null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(
                        a1,
                        finnSkjæringstidspunkt(a1, 1.vedtaksperiode),
                        10000.månedlig.repeat(12)
                    )
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

        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

        assertEquals(120000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(120000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(120000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
        assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(no.nav.helse.person.inntekt.Inntektsmelding::class, it.inntektsopplysning::class)
        }
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(IkkeRapportert::class, it.inntektsopplysning::class)
        }

        assertEquals(1, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(10000.månedlig, it.rapportertInntekt)
        }
    }

    @Test
    fun `Tar ikke med arbeidsforhold dersom siste inntekt var 3 måneder før skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 10000.månedlig.repeat(3)),
            grunnlag(a2, 1.mars.minusMonths(2), listOf(500.månedlig)),
        )
        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = LocalDate.EPOCH, ansattTom = null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(
                        a1,
                        finnSkjæringstidspunkt(a1, 1.vedtaksperiode),
                        10000.månedlig.repeat(12)
                    )
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

        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

        assertEquals(120000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(120000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(120000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
        assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(no.nav.helse.person.inntekt.Inntektsmelding::class, it.inntektsopplysning::class)
        }
        assertEquals(1, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(10000.månedlig, it.rapportertInntekt)
        }
    }

    @Test
    fun `bruker har fyllt inn andre inntektskilder i søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)

        håndterSøknad(
            Sykdom(1.mars, 31.mars, 100.prosent),
            andreInntektskilder = true,
            orgnummer = a1
        )
        assertFunksjonellFeil(RV_SØ_10)
    }

    @Test
    fun `Forlengelse av en ghostsak skal ikke få warning - stoler på avgjørelsen som ble tatt i førstegangsbehandlingen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = LocalDate.EPOCH, ansattTom = null)
        )

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
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(a1))
        assertIngenVarsler(2.vedtaksperiode.filter(orgnummer = a1))
    }

    @Test
    fun `ettergølgende vedtaksperider av en vedtaksperiode med inntektskilde FLERE_ARBEIDSGIVERE blir også markert som flere arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.mars til 16.mars),
            førsteFraværsdag = 1.mars,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = LocalDate.EPOCH, ansattTom = null)
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 35000.månedlig.repeat(12))
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
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april), orgnummer = a1)
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)

        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(2.vedtaksperiode))
    }

    @Test
    fun `tar med arbeidsforhold i vilkårsgrunnlag som startet innen 2 mnd før skjæringstidspunkt, selvom vi ikke har inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = 1.november(2017), ansattTom = null)
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12))
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
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

        assertEquals(360000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(360000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(360000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
        assertEquals(FLERE_ARBEIDSGIVERE, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(FLERE_ARBEIDSGIVERE, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(no.nav.helse.person.inntekt.Inntektsmelding::class, it.inntektsopplysning::class)
        }
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a2).inspektør.also {
            assertEquals(IkkeRapportert::class, it.inntektsopplysning::class)
        }

        assertEquals(1, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(30000.månedlig, it.rapportertInntekt)
        }
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `overstyrer inntekt dersom det ikke er rapportert inn inntekt enda`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 30000.månedlig,
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Vilkårsgrunnlag.Arbeidsforhold(orgnummer = a2, ansattFom = 1.november(2017), ansattTom = null)
        )

        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 30000.månedlig.repeat(12))
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
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "Jeg, en saksbehandler, overstyrte pga 8-15")))
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

        assertEquals(360000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(360000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(360000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
        assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(no.nav.helse.person.inntekt.Inntektsmelding::class, it.inntektsopplysning::class)
        }

        assertEquals(1, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(30000.månedlig, it.rapportertInntekt)
        }
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `skal ikke gå til AvventerHistorikk uten IM fra alle arbeidsgivere om vi ikke overlapper med første vedtaksperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(18.januar, 10.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(18.januar, 10.februar), orgnummer = a2)
        håndterSøknad(Sykdom(18.januar, 10.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(18.januar, 10.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(18.januar til 2.februar), orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `forlengelse av ghost med IM som har første fraværsdag på annen måned enn skjæringstidspunkt skal ikke vente på IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 12.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 12.februar), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 12.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 12.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(13.februar, 28.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(13.februar, 28.februar), orgnummer = a2)
        håndterSøknad(Sykdom(13.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(13.februar, 28.februar, 100.prosent), orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `forlengelse av ghost med IM som har første fraværsdag på annen måned enn skjæringstidspunkt skal ikke vente på IM (uferdig)`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(21.februar, 28.februar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(21.februar, 28.februar), orgnummer = a2)
        håndterSøknad(Sykdom(21.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(21.februar, 28.februar, 100.prosent), orgnummer = a2)


        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `deaktivert arbeidsforhold blir med i vilkårsgrunnlag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.desember(2017), null)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
        assertEquals(listOf(a1, a2).toList(), person.relevanteArbeidsgivere(skjæringstidspunkt).toList())
        håndterOverstyrArbeidsforhold(skjæringstidspunkt, listOf(
            OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(
                a2,
                true,
                "forklaring"
            )
        ))

        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

        assertEquals(listOf(a1), person.relevanteArbeidsgivere(skjæringstidspunkt))
        assertEquals(listOf(a2), sykepengegrunnlagInspektør.deaktiverteArbeidsforhold)

        assertEquals(372000.årlig, sykepengegrunnlagInspektør.beregningsgrunnlag)
        assertEquals(372000.årlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
        assertEquals(372000.årlig, sammenligningsgrunnlagInspektør.sammenligningsgrunnlag)
        assertEquals(0, vilkårsgrunnlag.sykepengegrunnlag.inspektør.avviksprosent)
        assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(no.nav.helse.person.inntekt.Inntektsmelding::class, it.inntektsopplysning::class)
        }
        assertEquals(1, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(31000.månedlig, it.rapportertInntekt)
        }
    }

    @Test
    fun `arbeidsgiver går fra å være ghost mens først arbeidsgiver står til godkjenning`() {
        utbetalPeriodeMedGhost(tilGodkjenning = true)

        nyPeriode(16.mars til 31.mars, a1) // Forlengelse på a1
        nyPeriode(16.mars til 15.april, a2) // Går fra Ghost -> ikke ghost

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, a1)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, a2)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertInntektstype(1.januar, mapOf(a1 to InntektsmeldingInntekt::class, a2 to SkattSykepengegrunnlag::class))

        // IM for tidligere ghost a2 sparker igang revurdering på a1
        håndterInntektsmelding(
            listOf(16.mars til 31.mars),
            førsteFraværsdag = 16.mars,
            refusjon = Inntektsmelding.Refusjon(INNTEKT,null, emptyList()),
            orgnummer = a2
        )
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertInntektstype(1.januar, mapOf(a1 to InntektsmeldingInntekt::class, a2 to SkattSykepengegrunnlag::class))

        // Her står saken nå (*NÅ*)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)

        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, a1)

        // Vi har fortsatt ikke beregnet hva utbetalingen for a2 kommer til å bli, men saksbehandleren lurer på det allerede *NÅ*
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, a2)
    }

    @Test
    fun `deaktivere arbeidsgiver med kort søknad`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.januar, 50.prosent), orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )

        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        val utbetaling1 = inspektør(a1).utbetaling(0).inspektør
        assertEquals(75, utbetaling1.utbetalingstidslinje[1.januar].økonomi.inspektør.totalGrad.roundToInt())
        håndterOverstyrArbeidsforhold(1.januar, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, true, "forklaring")))

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        val utbetaling2 = inspektør(a1).utbetaling(1).inspektør
        assertEquals(100, utbetaling2.utbetalingstidslinje[1.januar].økonomi.inspektør.totalGrad.roundToInt())
    }

    private fun assertInntektstype(skjæringstidspunkt: LocalDate, forventet: Map<String, KClass<out Inntektsopplysning>>) {
        val arbeidsgiverInntektsopplysninger = inspektør.vilkårsgrunnlag(skjæringstidspunkt)!!.inspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger
        forventet.forEach { (organisasjonsnummer, forventetInntektstype) ->
            val inntektsopplysning = arbeidsgiverInntektsopplysninger.single { it.gjelder(organisasjonsnummer) }.inspektør.inntektsopplysning
            assertEquals(forventetInntektstype, inntektsopplysning::class)
        }
    }

    private fun utbetalPeriodeMedGhost(tilGodkjenning: Boolean = false) {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )

        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekter,
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        if (tilGodkjenning) return
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
    }
}