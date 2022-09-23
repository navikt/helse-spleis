package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold.ArbeidsforholdOverstyrt
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Varselkode.RV_IM_1
import no.nav.helse.person.Varselkode.RV_IM_2
import no.nav.helse.person.Varselkode.RV_IM_3
import no.nav.helse.person.Varselkode.RV_IM_4
import no.nav.helse.person.Varselkode.RV_IM_5
import no.nav.helse.person.Varselkode.RV_IT_1
import no.nav.helse.person.Varselkode.RV_MV_1
import no.nav.helse.person.Varselkode.RV_OV_1
import no.nav.helse.person.Varselkode.RV_RE_1
import no.nav.helse.person.Varselkode.RV_SØ_1
import no.nav.helse.person.Varselkode.RV_SØ_10
import no.nav.helse.person.Varselkode.RV_SØ_2
import no.nav.helse.person.Varselkode.RV_SØ_3
import no.nav.helse.person.Varselkode.RV_SØ_4
import no.nav.helse.person.Varselkode.RV_SØ_5
import no.nav.helse.person.Varselkode.RV_SØ_7
import no.nav.helse.person.Varselkode.RV_SØ_8
import no.nav.helse.person.Varselkode.RV_SØ_9
import no.nav.helse.person.Varselkode.RV_VV_1
import no.nav.helse.person.Varselkode.RV_VV_2
import no.nav.helse.person.Varselkode.RV_VV_4
import no.nav.helse.person.Varselkode.RV_VV_8
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
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
    fun `søknad med arbeidsdager mellom to perioder bridger ikke de to periodene`(){
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent),
            Søknad.Søknadsperiode.Arbeid(20.januar, 31.januar)
        )
        assertVarsel(RV_SØ_7, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Søknad med utenlandsopphold og studieopphold gir warning`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent),
            Søknad.Søknadsperiode.Utlandsopphold(11.januar, 15.januar)
        )
        assertVarsel(RV_SØ_8, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Det er oppgitt annen inntektskilde i søknaden, Vurder inntekt`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANNET")),
        )
        assertVarsel(RV_SØ_9, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Den sykmeldte har oppgitt å ha andre arbeidsforhold med sykmelding i søknaden`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD"))
        )
        assertVarsel(RV_SØ_10, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Vi har mottatt en inntektsmelding i en løpende sykmeldingsperiode med oppgitt første - bestemmende fraværsdag som er ulik tidligere fastsatt skjæringstidspunkt`() {
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 10.januar, 100.prosent))
        val imId = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 23.januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(11.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(imId, 2.vedtaksperiode.id(ORGNUMMER))
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
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
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
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
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
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `varsel - Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %, Vurder å sende vedtaksbrev fra Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 19.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 19.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertVarsel(RV_VV_4, 1.vedtaksperiode.filter(a1))
    }

    @Test
    fun `varsel - Fant ikke refusjonsgrad for perioden - Undersøk oppgitt refusjon før du utbetaler`() {
        val imId = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 25.februar)
        nyPeriode(1.januar til 10.januar)
        nyPeriode(11.januar til 31.januar)
        nyPeriode(1.februar til 28.februar)
        håndterInntektsmeldingReplay(imId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterInntektsmeldingReplay(imId, 2.vedtaksperiode.id(ORGNUMMER))
        håndterInntektsmeldingReplay(imId, 3.vedtaksperiode.id(ORGNUMMER))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
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
        håndterYtelser()
        håndterVilkårsgrunnlag(arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER, ansattFom = 31.desember(2017), ansattTom = null)))
        assertVarsel(RV_OV_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel revurdering - Perioden er avslått på grunn av manglende opptjening`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
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
        håndterYtelser()
        håndterVilkårsgrunnlag(medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke)

        assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())
    }
}