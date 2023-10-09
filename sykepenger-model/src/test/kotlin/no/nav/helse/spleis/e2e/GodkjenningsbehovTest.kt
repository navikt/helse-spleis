package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.etterspurtBehov
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_24
import no.nav.helse.september
import no.nav.helse.sisteBehov
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.IKKE_GODKJENT
import no.nav.helse.utbetalingslinjer.Utbetalingstatus.IKKE_UTBETALT
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GodkjenningsbehovTest : AbstractEndToEndTest() {

    @Test
    fun `godkjenningsbehov som ikke kan avvises blir forsøkt avvist`() {
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "Agp skal utbetales av NAV!!"
        )
        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertFalse(kanAvvises(1.vedtaksperiode))

        val utbetalingId = UUID.fromString(person.personLogg.sisteBehov(Aktivitet.Behov.Behovtype.Godkjenning).kontekst()["utbetalingId"])
        val utbetaling = inspektør.utbetalinger(1.vedtaksperiode).last()
        assertEquals(utbetalingId, utbetaling.inspektør.utbetalingId)

        assertEquals(IKKE_UTBETALT, utbetaling.inspektør.tilstand)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingId = utbetalingId, utbetalingGodkjent = false)
        assertEquals(IKKE_GODKJENT, utbetaling.inspektør.tilstand)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        assertVarsel(RV_UT_24, 1.vedtaksperiode.filter())
    }

    @Test
    fun `legger ved orgnummer for arbeidsgiver med kun sammenligningsgrunnlag i utbetalingsbehovet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                    sammenligningsgrunnlag(a2, 1.oktober(2017), 1000.månedlig.repeat(9))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, 1.oktober(2017), null, Arbeidsforholdtype.ORDINÆRT),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, 30.september(2017), Arbeidsforholdtype.ORDINÆRT)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val orgnummereMedRelevanteArbeidsforhold = hendelselogg.etterspurtBehov<Set<String>>(
            vedtaksperiodeId = 1.vedtaksperiode.id(a1),
            behov = Aktivitet.Behov.Behovtype.Godkjenning,
            felt = "orgnummereMedRelevanteArbeidsforhold"
        )
        assertEquals(setOf(a1, a2), orgnummereMedRelevanteArbeidsforhold)
    }

    @Test
    fun `førstegangsbehandling som kan avvises`() {
        tilGodkjenning(1.januar, 31.januar, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(kanAvvises(1.vedtaksperiode))
    }

    @Test
    fun `omgjøring som kan avvises`() {
        håndterSøknad(Sykdom(2.januar, 17.januar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(kanAvvises(1.vedtaksperiode))
    }

    @Test
    fun `omgjøring som _ikke_ kan avvises`() {
        håndterSøknad(Sykdom(2.januar, 17.januar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        håndterSøknad(Sykdom(18.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(2.januar til 17.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(kanAvvises(2.vedtaksperiode))
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        assertFalse(kanAvvises(1.vedtaksperiode))
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertFalse(kanAvvises(2.vedtaksperiode))
    }

    @Test
    fun `revurdering kan ikke avvises`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.05, "forklaring", null, emptyList())))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertFalse(kanAvvises(1.vedtaksperiode))
    }

    @Test
    fun `kan avvise en out of order rett i forkant av en utbetalt periode`() {
        nyttVedtak(1.februar, 28.februar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(kanAvvises(2.vedtaksperiode))
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)
        assertSisteForkastetPeriodeTilstand(a1, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertFalse(kanAvvises(1.vedtaksperiode))
    }

    @Test
    fun `kan avvise en out of order selv om noe er utbetalt senere på annen agp`() {
        nyttVedtak(1.mars, 31.mars)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        assertTrue(kanAvvises(2.vedtaksperiode))
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false)
        assertSisteForkastetPeriodeTilstand(a1, 2.vedtaksperiode, TIL_INFOTRYGD)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertFalse(kanAvvises(1.vedtaksperiode))
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
    }

    private fun kanAvvises(vedtaksperiode: IdInnhenter, orgnummer: String = a1) = hendelselogg.etterspurtBehov<Boolean>(
        vedtaksperiodeId = vedtaksperiode.id(orgnummer),
        behov = Aktivitet.Behov.Behovtype.Godkjenning,
        felt = "kanAvvises"
    )!!
}
