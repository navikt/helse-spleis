package no.nav.helse.spleis.e2e

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurdereInntektMedFlereArbeidsgivere::class)
internal class OverstyrInntektFlereArbeidsgivereTest: AbstractEndToEndTest() {

    val grunnlagsdataInspektør get() = GrunnlagsdataInspektør(inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!)

    @Test
    fun `overstyr inntekt med flere AG -- happy case`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()[a1]?.omregnetÅrsinntekt())
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()[a2]?.omregnetÅrsinntekt())

        håndterOverstyrInntekt(19000.månedlig, a1, 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        assertInntektForDato(19000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))
        assertEquals(19000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()[a1]?.omregnetÅrsinntekt())
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()[a2]?.omregnetÅrsinntekt())
    }

    @Test
    fun `overstyr inntekt med flere AG -- kan ikke overstyre perioden i AvventerBlokkerende`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterOverstyrInntekt(19000.månedlig, a2, 1.januar)
        assertNoErrors()
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()[a1]?.omregnetÅrsinntekt())
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()[a2]?.omregnetÅrsinntekt())

    }

    @Test
    fun `Overstyrer a2 når a1 er utbetalt`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        håndterOverstyrInntekt(19000.månedlig, a2, 1.januar)
        assertForventetFeil(
            forklaring = "Må blokkere dette fordi da er vi i en revurderingssituasjon for a1",
            nå = {
                assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
                assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
            },
            ønsket = {
                assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
                assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a2)
            }
        )
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()[a1]?.omregnetÅrsinntekt())
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()[a2]?.omregnetÅrsinntekt())
    }

}