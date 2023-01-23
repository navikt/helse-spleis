package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import no.nav.helse.FeilerMedHåndterInntektsmeldingOppdelt
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold.ArbeidsforholdOverstyrt
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.søppelbøtte
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.Varselkode
import no.nav.helse.person.Varselkode.RV_AY_3
import no.nav.helse.person.Varselkode.RV_AY_4
import no.nav.helse.person.Varselkode.RV_AY_5
import no.nav.helse.person.Varselkode.RV_AY_6
import no.nav.helse.person.Varselkode.RV_AY_7
import no.nav.helse.person.Varselkode.RV_AY_8
import no.nav.helse.person.Varselkode.RV_AY_9
import no.nav.helse.person.Varselkode.RV_IM_1
import no.nav.helse.person.Varselkode.RV_IM_2
import no.nav.helse.person.Varselkode.RV_IM_3
import no.nav.helse.person.Varselkode.RV_IM_4
import no.nav.helse.person.Varselkode.RV_IM_5
import no.nav.helse.person.Varselkode.RV_IT_1
import no.nav.helse.person.Varselkode.RV_IT_11
import no.nav.helse.person.Varselkode.RV_IT_12
import no.nav.helse.person.Varselkode.RV_IT_13
import no.nav.helse.person.Varselkode.RV_IT_14
import no.nav.helse.person.Varselkode.RV_IT_15
import no.nav.helse.person.Varselkode.RV_IT_33
import no.nav.helse.person.Varselkode.RV_IT_4
import no.nav.helse.person.Varselkode.RV_IV_1
import no.nav.helse.person.Varselkode.RV_IV_2
import no.nav.helse.person.Varselkode.RV_IV_3
import no.nav.helse.person.Varselkode.RV_MV_1
import no.nav.helse.person.Varselkode.RV_MV_2
import no.nav.helse.person.Varselkode.RV_OO_1
import no.nav.helse.person.Varselkode.RV_OO_2
import no.nav.helse.person.Varselkode.RV_OS_1
import no.nav.helse.person.Varselkode.RV_OS_2
import no.nav.helse.person.Varselkode.RV_OS_3
import no.nav.helse.person.Varselkode.RV_OV_1
import no.nav.helse.person.Varselkode.RV_RE_1
import no.nav.helse.person.Varselkode.RV_RV_1
import no.nav.helse.person.Varselkode.RV_RV_2
import no.nav.helse.person.Varselkode.RV_SI_1
import no.nav.helse.person.Varselkode.RV_SV_1
import no.nav.helse.person.Varselkode.RV_SV_2
import no.nav.helse.person.Varselkode.RV_SØ_1
import no.nav.helse.person.Varselkode.RV_SØ_10
import no.nav.helse.person.Varselkode.RV_SØ_2
import no.nav.helse.person.Varselkode.RV_SØ_3
import no.nav.helse.person.Varselkode.RV_SØ_4
import no.nav.helse.person.Varselkode.RV_SØ_5
import no.nav.helse.person.Varselkode.RV_SØ_7
import no.nav.helse.person.Varselkode.RV_SØ_8
import no.nav.helse.person.Varselkode.RV_UT_1
import no.nav.helse.person.Varselkode.RV_UT_2
import no.nav.helse.person.Varselkode.RV_VV_1
import no.nav.helse.person.Varselkode.RV_VV_2
import no.nav.helse.person.Varselkode.RV_VV_4
import no.nav.helse.person.Varselkode.RV_VV_8
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class VarselE2ETest: AbstractEndToEndTest() {

    @Test
    fun `varsel - Søknaden inneholder permittering, Vurder om permittering har konsekvens for rett til sykepenger`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            permittert = true
        )
        assertVarsel(RV_SØ_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Minst én dag er avslått på grunn av foreldelse, Vurder å sende vedtaksbrev fra Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        assertVarsel(RV_SØ_2)
    }

    @Test
    fun `varsel - Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            merknaderFraSykmelding = listOf(Søknad.Merknad("UGYLDIG_TILBAKEDATERING"))
        )
        assertVarsel(RV_SØ_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Utdanning oppgitt i perioden i søknaden`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            Søknad.Søknadsperiode.Utdanning(20.januar, 31.januar)
        )
        assertVarsel(RV_SØ_4, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Søknaden inneholder Permisjonsdager utenfor sykdomsvindu`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            Søknad.Søknadsperiode.Permisjon(1.desember(2017), 31.desember(2017)),
        )
        assertVarsel(RV_SØ_5, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Søknaden inneholder Arbeidsdager utenfor sykdomsvindu`(){
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent),
            Søknad.Søknadsperiode.Arbeid(20.januar, 31.januar)
        )
        assertVarsel(RV_SØ_7, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Utenlandsopphold oppgitt i perioden i søknaden`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent),
            Søknad.Søknadsperiode.Utlandsopphold(11.januar, 15.januar)
        )
        assertVarsel(RV_SØ_8, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Den sykmeldte har oppgitt å ha andre arbeidsforhold med sykmelding i søknaden`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            andreInntektskilder = true
        )
        assertFunksjonellFeil(RV_SØ_10.varseltekst, 1.vedtaksperiode.filter())
    }

    @Test
    fun `funksjonell feil - Periode som forlenger forkastet periode skal forkastes`() {
        nyPeriode(1.januar til 19.januar)
        person.søppelbøtte(hendelselogg, 1.januar til 19.januar)

        nyPeriode(22.januar til 31.januar)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, TilstandType.START, TilstandType.TIL_INFOTRYGD)
        assertFunksjonellFeil(Varselkode.RV_SØ_19)
        assertIngenFunksjonellFeil(Varselkode.RV_SØ_28)
    }

    @Test
    fun `funksjonell feil - Periode med mindre enn 20 dagers gap til forkastet periode skal forkastes`() {
        nyPeriode(1.januar til 19.januar)
        person.søppelbøtte(hendelselogg, 1.januar til 19.januar)

        nyPeriode(25.januar til 31.januar)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, TilstandType.START, TilstandType.TIL_INFOTRYGD)
        assertFunksjonellFeil(Varselkode.RV_SØ_28)
    }

    @Test
    @FeilerMedHåndterInntektsmeldingOppdelt("ufullstendig validering: La oss undersøke om vi kan fjerne RV_IM_1")
    fun `varsel - Vi har mottatt en inntektsmelding i en løpende sykmeldingsperiode med oppgitt første - bestemmende fraværsdag som er ulik tidligere fastsatt skjæringstidspunkt`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 23.januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(11.januar, 31.januar, 100.prosent))
        assertVarsel(RV_IM_1, 2.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet, Kontrollér at inntektsmeldingen er knyttet til riktig periode`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 23.januar)
        assertVarsel(RV_IM_2, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Inntektsmeldingen og vedtaksløsningen er uenige om beregningen av arbeidsgiverperioden, Undersøk hva som er riktig arbeidsgiverperiode`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 15.januar, 17.januar til 18.januar))
        assertVarsel(RV_IM_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn, Utbetal kun hvis det blir korrekt`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertVarsel(RV_IM_4, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige, kontroller at dagsatsen blir riktig`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, ansattFom = LocalDate.EPOCH, ansattTom = 31.desember(2017)),
                Arbeidsforhold(a2, ansattFom = 1.januar, ansattTom = null)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertVarsel(RV_VV_8, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Sykmeldte har oppgitt ferie første dag i arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            Søknad.Søknadsperiode.Ferie(1.januar, 20.januar),
            Søknad.Søknadsperiode.Ferie(25.januar, 31.januar)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertForventetFeil(
            nå = {
                assertIngenVarsel(RV_IM_5, 1.vedtaksperiode.filter(ORGNUMMER))
            },
            ønsket = {
                // TODO: https://trello.com/c/92DhehGa
                assertVarsel(RV_IM_5, 1.vedtaksperiode.filter(ORGNUMMER))
            }
        )
    }

    @Test
    fun `varsel - Arbeidsgiver er ikke registrert i Aa-registeret`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            arbeidsforhold = emptyList()
        )
        assertVarsel(RV_VV_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(4.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(4.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = 10000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(4.januar til 19.januar),
            førsteFraværsdag = 4.januar,
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
            Arbeidsforhold(orgnummer = a1, ansattFom = LocalDate.EPOCH, ansattTom = null),
            Arbeidsforhold(orgnummer = a2, ansattFom = LocalDate.EPOCH, ansattTom = null)
        )
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
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `varsel - Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 prosent, Vurder å sende vedtaksbrev fra Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 19.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 19.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_VV_4, 1.vedtaksperiode.filter(a1))
    }

    @Test
    @Disabled
    fun `varsel - Fant ikke refusjonsgrad for perioden - Undersøk oppgitt refusjon før du utbetaler`() {
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 25.februar)
        nyPeriode(1.januar til 10.januar)
        nyPeriode(11.januar til 31.januar)
        nyPeriode(1.februar til 28.februar)
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        assertVarsel(RV_RE_1, 2.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Det er utbetalt en periode i Infotrygd etter perioden du skal behandle nå - Undersøk at antall forbrukte dager og grunnlag i Infotrygd er riktig`() {
        nyttVedtak(1.januar, 31.januar)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Dagtype.Sykedag, 80)))
        håndterYtelser(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mars, 31.mars, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.mars, INNTEKT, true))
        )
        assertVarsel(RV_IT_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Perioden er avslått på grunn av manglende opptjening`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER, ansattFom = 31.desember(2017), ansattTom = null)))
        assertVarsel(RV_OV_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel revurdering - Perioden er avslått på grunn av manglende opptjening`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(
            arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER, 31.desember(2017), null), Arbeidsforhold(a2, LocalDate.EPOCH, 5.januar)),
            inntektsvurdering = Inntektsvurdering(listOf(sammenligningsgrunnlag(a2, 1.januar, INNTEKT.repeat(12)))),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(grunnlag(a2, 1.januar, 1000.månedlig.repeat(3))),
                arbeidsforhold = emptyList()
            ),
        )
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        assertIngenVarsel(RV_OV_1, 1.vedtaksperiode.filter())
        håndterOverstyrArbeidsforhold(1.januar, listOf(ArbeidsforholdOverstyrt(a2, true, "forklaring")))
        assertVarsel(RV_OV_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Vurder lovvalg og medlemskap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke)

        assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Perioden er avslått på grunn av at den sykmeldte ikke er medlem av Folketrygden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Nei)

        assertVarsel(RV_MV_2, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Bruker har flere inntektskilder de siste tre månedene enn arbeidsforhold som er oppdaget i Aa-registeret`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1)
        håndterVilkårsgrunnlag(orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 1000.månedlig
                }
            }
        ))
        håndterYtelser(inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(orgnummer = a1)
        håndterUtbetalingsgodkjenning(utbetalingGodkjent = true, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertVarsel(RV_IV_1, 1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `varsel - Utbetaling i Infotrygd overlapper med vedtaksperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag()
        håndterYtelser(besvart = LocalDateTime.now().minusYears(1))
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        håndterOverstyrTidslinje((20.januar til 26.januar).map { manuellFeriedag(it) })

        håndterYtelser(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
            )
        )
        assertVarsel(Varselkode.RV_IT_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Det er registrert utbetaling på nødnummer`() {
        val nødnummer = "973626108"
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag()
        håndterYtelser(besvart = LocalDateTime.now().minusYears(1))
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        håndterOverstyrTidslinje((20.januar til 26.januar).map { manuellFeriedag(it) })

        håndterYtelser(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(nødnummer, 1.februar, 15.februar, 100.prosent, INNTEKT))
        assertVarsel(RV_IT_4, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 100.månedlig)
        håndterVilkårsgrunnlag(inntekt = 100.månedlig)
        håndterYtelser()
        assertVarsel(RV_SV_1)
    }

    @Test
    fun `varsel - Minst en arbeidsgiver inngår ikke i sykepengegrunnlaget`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)), orgnummer = a1)
        assertForventetFeil(
            forklaring = "perioden sitter fast fordi vi ikke har nødvendig inntekt for vilkårsprøving." +
                    "litt usikker på hvordan vi vil håndtere dette. Vedtaksperioden hos a2 vil kastes ut i AvventerBlokkerende," +
                    "med RV_SV_2 som funksjonell feil.",
            nå = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
            },
            ønsket = {
                håndterYtelser(1.vedtaksperiode, orgnummer = a1)
                assertVarsel(RV_SV_2)
            }
        )
    }

    @Test
    fun `varsel - Feil under simulering`() {
        nyttVedtak(1.januar, 31.januar)

        håndterOverstyrTidslinje(listOf(manuellFeriedag(18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode, simuleringOK = false)
        assertVarsel(RV_SI_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Simulering av revurdert utbetaling feilet, Utbetalingen må annulleres`() {
        nyttVedtak(1.januar, 31.januar)

        håndterOverstyrTidslinje(listOf(manuellFeriedag(18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode, simuleringOK = false)
        assertVarsel(Varselkode.RV_SI_2, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Det er simulert et negativt beløp`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode, simuleringsresultat = standardSimuleringsresultat(ORGNUMMER, totalbeløp = -1))
        assertVarsel(Varselkode.RV_SI_3)
    }

    @Test
    fun `varsel - Har mer enn 25 prosent avvik`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(inntekt = INNTEKT * 2)
        håndterYtelser()
        assertIngenVarsel(RV_IV_2, 1.vedtaksperiode.filter())
        assertFunksjonellFeil("Har mer enn 25 % avvik", 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Har mer enn 25 prosent avvik - Dette støttes foreløpig ikke i Speil - Du må derfor annullere periodene`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrInntekt(inntekt = INNTEKT * 2, skjæringstidspunkt = 1.januar)
        håndterYtelser()
        assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
        assertVarsel(RV_IV_2, 1.vedtaksperiode.filter())
    }

    @Test
    fun `ikke lenger varsel - Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 16.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 16.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(31.januar til 15.februar))
        assertInfo(RV_RV_1.varseltekst)
    }

    @Test
    fun `varsel - Endrer tidligere oppdrag, Kontroller simuleringen`(){
        nyttVedtak(3.januar, 26.januar)
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        håndterOverstyrTidslinje((23.januar til 23.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_OS_3)
    }

    @Test
    fun `varsel - Utbetalingens fra og med-dato er endret, Kontroller simuleringen`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            Søknad.Søknadsperiode.Ferie(17.januar, 18.januar)
        )
        håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_OS_2)
    }

    @Test
    fun `varsel - Utbetalingen forlenger et tidligere oppdrag som opphørte alle utbetalte dager, Sjekk simuleringen`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent, beregnetInntekt = 5000.månedlig)
        nullstillTilstandsendringer()
        håndterOverstyrInntekt(inntekt = 3000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrInntekt(5000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_OS_1)
    }

    @Test
    fun `varsel - Bruker har mottatt AAP innenfor 6 måneder før skjæringstidspunktet - Kontroller at brukeren har rett til sykepenger`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1, arbeidsavklaringspenger = listOf(1.desember(2017) til 15.desember(2017)))

        assertVarsel(RV_AY_3)
    }

    @Test
    fun `varsel - Bruker har mottatt dagpenger innenfor 4 uker før skjæringstidspunktet - Kontroller om bruker er dagpengemottaker - Kombinerte ytelser støttes foreløpig ikke av systemet`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1, dagpenger = listOf(1.desember(2017) til 15.desember(2017)))

        assertVarsel(RV_AY_4)
    }

    @Test
    fun `varsel - Det er mottatt foreldrepenger i samme periode`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode, foreldrepenger = 1.januar til 31.januar)

        assertVarsel(RV_AY_5)
    }

    @Test
    fun `varsel - Det er utbetalt pleiepenger i samme periode`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(1.januar til 31.januar))

        assertVarsel(RV_AY_6)
    }

    @Test
    fun `varsel - Det er utbetalt omsorgspenger i samme periode`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode, omsorgspenger = listOf(1.januar til 31.januar))

        assertVarsel(RV_AY_7)
    }

    @Test
    fun `varsel - Det er utbetalt opplæringspenger i samme periode`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode, opplæringspenger = listOf(1.januar til 31.januar))

        assertVarsel(RV_AY_8)
    }

    @Test
    fun `varsel - Det er institusjonsopphold i perioden - Vurder retten til sykepenger`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode, institusjonsoppholdsperioder = listOf(Institusjonsopphold.Institusjonsoppholdsperiode(1.januar, 31.januar)))

        assertVarsel(RV_AY_9)
    }

    @Test
    fun `varsel - Utbetaling av revurdert periode ble avvist av saksbehandler - Utbetalingen må annulleres`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning(utbetalingGodkjent = false)

        assertVarsel(RV_UT_1)
    }

    @Test
    fun `varsel - Utbetalingen ble gjennomført, men med advarsel`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(status = Oppdragstatus.AKSEPTERT_MED_FEIL)
        assertVarsel(RV_UT_2)
    }

    @Test
    fun `varsel - Det er behandlet en søknad i Speil for en senere periode enn denne`() {
        nyttVedtak(1.mars, 31.mars)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        assertVarsel(RV_OO_1, 2.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Saken må revurderes fordi det har blitt behandlet en tidligere periode som kan ha betydning`() {
        nyttVedtak(1.mars, 31.mars)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        assertVarsel(RV_OO_2, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Fant frilanserinntekt på en arbeidsgiver de siste 3 månedene`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 16.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 16.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterInntektsmelding(listOf(31.januar til 15.februar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    ArbeidsgiverInntekt(ORGNUMMER, (10..12).map {
                        ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                            yearMonth = YearMonth.of(2017, it),
                            type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                            inntekt = INNTEKT,
                            fordel = "fordel",
                            beskrivelse = "beskrivelse"
                        )
                    })
                ),
                arbeidsforhold = listOf(
                    InntektForSykepengegrunnlag.Arbeidsforhold(
                        ORGNUMMER, listOf(
                            InntektForSykepengegrunnlag.MånedligArbeidsforhold(
                                yearMonth = YearMonth.of(2017, 10),
                                erFrilanser = true
                            )
                        )
                    )
                )
            )
        )
        assertVarsel(RV_IV_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Det er registrert bruk av på nødnummer`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(inntektshistorikk = listOf(Inntektsopplysning.ferdigInntektsopplysning("973626108", 1.januar, inntekt = INNTEKT, true, null, null)))
        assertIngenVarsel(RV_IT_11, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel revurdering - Det er registrert bruk av på nødnummer`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))

        håndterYtelser(inntektshistorikk = listOf(Inntektsopplysning.ferdigInntektsopplysning("973626108", 1.januar, inntekt = INNTEKT, true, null, null)))
        assertVarsel(RV_IT_11, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Organisasjonsnummer for inntektsopplysning fra Infotrygd mangler`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(inntektshistorikk = listOf(Inntektsopplysning.ferdigInntektsopplysning("", 1.januar, inntekt = INNTEKT, true, null, null)))
        assertIngenVarsel(RV_IT_12, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel revurdering - Organisasjonsnummer for inntektsopplysning fra Infotrygd mangler`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))

        håndterYtelser(inntektshistorikk = listOf(Inntektsopplysning.ferdigInntektsopplysning("", 1.januar, inntekt = INNTEKT, true, null, null)))
        assertVarsel(RV_IT_12, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel revurdering - Støtter ikke overgang fra infotrygd for flere arbeidsgivere`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))

        håndterYtelser(
            inntektshistorikk = listOf(
                Inntektsopplysning.ferdigInntektsopplysning(a2, 1.januar, inntekt = INNTEKT, true, null, null),
                Inntektsopplysning.ferdigInntektsopplysning(a3, 1.januar, inntekt = INNTEKT, true, null, null)
            )
        )
        assertVarsel(RV_IT_13, 1.vedtaksperiode.filter())
        assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel revurdering - Forlenger en Infotrygdperiode på tvers av arbeidsgivere`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar, besvart = LocalDateTime.now().minusWeeks(2))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.februar, Dagtype.Feriedag)))

        håndterYtelser(
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT)),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        )
        assertVarsel(RV_IT_14, 2.vedtaksperiode.filter())
        assertIngenFunksjonelleFeil(2.vedtaksperiode.filter())
    }

    @Test
    fun `varsel revurdering - Personen er ikke registrert som normal arbeidstaker i Infotrygd`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))

        håndterYtelser(
            arbeidskategorikoder = mapOf("05" to 1.januar)
        )

        assertVarsel(RV_IT_15, 1.vedtaksperiode.filter())
        assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel revurdering - Forkaster avvist revurdering ettersom vedtaksperioden ikke har tidligere utbetalte utbetalinger`() {
        nyPeriode(2.januar til 17.januar)
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        håndterVilkårsgrunnlag()
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning(utbetalingGodkjent = false)

        assertIngenVarsel(RV_RV_2, 1.vedtaksperiode.filter())
        assertFunksjonellFeil("Forkaster avvist revurdering ettersom vedtaksperioden ikke har tidligere utbetalte utbetalinger.", 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - skjæringstidspunkt endres som følge av historikk fra IT`() {
        nyPeriode(10.januar til 25.januar)
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.now().minusDays(2))
        nyPeriode(26.januar til 31.januar)
        håndterInntektsmelding(listOf(10.januar til 25.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 9.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.januar, INNTEKT, true)
        ))
        assertFunksjonellFeil(RV_IT_33, 2.vedtaksperiode.filter(ORGNUMMER))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `varsel - skjæringstidspunkt endres som følge av historikk fra IT - forlengelse`() {
        nyttVedtak(10.februar, 28.februar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 5.februar, 9.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 5.februar, INNTEKT, true)
        ))
        håndterYtelser(2.vedtaksperiode)
        assertFunksjonellFeil(RV_IT_33, 2.vedtaksperiode.filter(ORGNUMMER))
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
    }

}