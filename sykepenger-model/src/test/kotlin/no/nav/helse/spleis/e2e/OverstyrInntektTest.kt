package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.person.TilstandType
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OverstyrInntektTest : AbstractEndToEndTest() {

    @BeforeEach
    fun setup() {
        Toggles.RevurderInntekt.enable()
    }

    @AfterEach
    fun tearDown() {
        Toggles.RevurderInntekt.pop()
    }

    @Test
    fun `skal kunne overstyre en inntekt i et enkelt case`() {
        val fom = 1.januar(2021)
        val overstyrtInntekt = 32000.månedlig
        tilGodkjenning(fom, 31.januar(2021), 100.prosent, fom)

        assertInntektForDato(INNTEKT, fom, inspektør)

        håndterOverstyring(inntekt = overstyrtInntekt, orgnummer = ORGNUMMER, skjæringstidspunkt = fom)

        assertTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING, // <-- Her står vi når vi overstyrer inntekt.
            TilstandType.AVVENTER_VILKÅRSPRØVING)

        // dra saken til AVVENTER_GODKJENNING
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        // assert at vi går gjennom restene av tilstandene som vanlig
        assertTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING, // <-- Her sto vi da vi overstyrte inntekt.
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING) // <-- og her skal den nye, overstyrte inntekten vært benyttet

        // assert at vi bruker den nye inntekten i beregning av penger til sjuk.
        assertInntektForDato(overstyrtInntekt, fom, inspektør)
    }

    @Test
    fun `overstyrt inntekt til mer enn 25 prosent avvik skal sendes til infotrygd`() {
        val fom = 1.januar(2021)
        val overstyrtInntekt = INNTEKT*1.40
        tilGodkjenning(fom, 31.januar(2021), 100.prosent, fom)

        håndterOverstyring(inntekt = overstyrtInntekt, orgnummer = ORGNUMMER, skjæringstidspunkt = fom)

        håndterVilkårsgrunnlag(1.vedtaksperiode)

        assertForkastetPeriodeTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
            TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.AVVENTER_HISTORIKK,
            TilstandType.AVVENTER_SIMULERING,
            TilstandType.AVVENTER_GODKJENNING,
            TilstandType.AVVENTER_VILKÅRSPRØVING,
            TilstandType.TIL_INFOTRYGD)
    }

    @Test
    fun `avviser revurdering av inntekt for saker med flere arbeidsgivere`() {
        val ag1 = "ag1"
        val ag2 = "ag2"
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = ag1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = ag2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = ag1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = ag2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = ag1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = ag2)

        val inntekter = listOf(
            grunnlag(ag1, finnSkjæringstidspunkt(ag1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(ag2, finnSkjæringstidspunkt(ag1, 1.vedtaksperiode), 31000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = ag1)

        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(ag1, finnSkjæringstidspunkt(ag1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(ag2, finnSkjæringstidspunkt(ag1, 1.vedtaksperiode), 31000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            orgnummer = ag1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = ag1)
        håndterSimulering(1.vedtaksperiode, orgnummer = ag1)

        håndterOverstyring(inntekt = 33000.månedlig, "ag1", 1.januar)
        Assertions.assertEquals(1, observatør.avvisteRevurderinger.size)
        assertErrorTekst(inspektør, "Forespurt overstyring av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
    }

    @Test
    fun `avviser revurdering av inntekt for saker med 1 arbeidsgiver og ghost`() {
        val ag1 = "ag1"
        val ag2 = "ag2"
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = ag1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = ag1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = ag1
        )

        val inntekter = listOf(
            grunnlag(ag1, finnSkjæringstidspunkt(ag1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(ag2, finnSkjæringstidspunkt(ag1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(ag1, LocalDate.EPOCH, null),
            Arbeidsforhold(ag2, LocalDate.EPOCH, null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = ag1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(ag1, finnSkjæringstidspunkt(ag1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(ag2, finnSkjæringstidspunkt(ag1, 1.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = ag1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = ag1)
        håndterSimulering(1.vedtaksperiode, orgnummer = ag1)

        håndterOverstyring(32000.månedlig, ag1, 1.januar)
        Assertions.assertEquals(1, observatør.avvisteRevurderinger.size)
        assertErrorTekst(inspektør, "Forespurt overstyring av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
    }
}
