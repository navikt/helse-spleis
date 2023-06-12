package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SKJØNNSMESSIG_FASTSETTELSE
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail

internal class SkjønnsmessigFastsettelseTest: AbstractDslTest() {

    @Test
    fun `skjønnsmessig fastsatt inntekt skal ikke ha avviksvurdering`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        val sykepengegrunnlag = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.sykepengegrunnlag.inspektør
        val inntektsopplysning = inspektør.inntektsopplysningISykepengegrunnlaget(1.januar)
        assertTrue(inntektsopplysning is SkjønnsmessigFastsatt)
        assertEquals(0, sykepengegrunnlag.avviksprosent)
        assertEquals(INNTEKT * 2, sykepengegrunnlag.beregningsgrunnlag)
        assertEquals(INNTEKT, sykepengegrunnlag.omregnetÅrsinntekt)
    }

    @Test
    fun `alle inntektene må skjønnsfastsettes ved overstyring`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        a1 {
            assertThrows<IllegalStateException> {
                håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
            }
        }
    }

    @Test
    fun `saksbehandler-inntekt overstyres av en skjønnsmessig med samme beløp`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2, forklaring = "forklaring")))
        assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is Saksbehandler)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        assertEquals(3, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is SkjønnsmessigFastsatt)
    }

    @Test
    fun `skjønnsmessig fastsettelse overstyres av en skjønnsmessig med samme beløp`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
    }

    @Test
    fun `skjønnsmessig fastsettelse overstyres av en inntektmelding med samme beløp`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT * 2)
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is SkjønnsmessigFastsatt)
    }

    @Test
    fun `korrigert IM etter skjønnsfastsettelse på flere AG`() {
        (a1 og a2 og a3).nyeVedtak(1.januar til 31.januar)
        håndterOverstyrArbeidsgiveropplysninger(
            1.januar,
            listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a3, inntekt = INNTEKT * 3, forklaring = "ogga bogga"))
        )
        a3 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a3) is Saksbehandler) }
        håndterSkjønnsmessigFastsettelse(
            1.januar,
            listOf(
                OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2),
                OverstyrtArbeidsgiveropplysning(orgnummer = a2, inntekt = INNTEKT * 2),
                OverstyrtArbeidsgiveropplysning(orgnummer = a3, inntekt = INNTEKT * 2)
            )
        )
        a1 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a1) is SkjønnsmessigFastsatt) }
        a2 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a2) is SkjønnsmessigFastsatt) }
        a3 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a3) is SkjønnsmessigFastsatt) }

        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT) }

        a1 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a1) is Inntektsmelding) }
        a2 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a2) is Inntektsmelding) }
        a3 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a3) is Saksbehandler) }
    }

    @Test
    fun `skjønnsmessig fastsettelse overstyres av en inntektmelding med ulikt beløp`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSkjønnsmessigFastsettelse(
            1.januar,
            listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2))
        )
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT * 3)
        assertEquals(3, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is Inntektsmelding)
    }

    @Test
    fun `førstegangsbehandling med mer enn 25% avvik`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            nyPeriode(1.januar til 31.januar, a1)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 2)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt = INNTEKT)
        }
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE)

    }

    @Test
    fun `Tidligere perioder revurderes mens nyere skjønnsmessig fastsettes `() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            nyPeriode(1.mars til 31.mars, a1)
            håndterInntektsmelding(listOf(1.mars til 16.mars), beregnetInntekt = INNTEKT * 2)
            håndterVilkårsgrunnlag(2.vedtaksperiode, inntekt = INNTEKT)
            nullstillTilstandsendringer()
            håndterOverstyrTidslinje(
                listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag, 100))
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertForventetFeil(
                forklaring = "Vi må finne ut av hva vi skal gjøre",
                nå = {
                    assertTilstander(2.vedtaksperiode, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
                },
                ønsket = {
                    fail { "Vi må finne ut av hva vi skal gjøre"}
                }
            )
        }

    }

    private fun TestArbeidsgiverInspektør.inntektsopplysningISykepengegrunnlaget(skjæringstidspunkt: LocalDate, orgnr: String = a1) =
        vilkårsgrunnlag(skjæringstidspunkt)!!.inspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.single { it.gjelder(orgnr) }.inspektør.inntektsopplysning
}