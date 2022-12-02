package no.nav.helse.spleis.e2e.overstyring

import java.util.UUID
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.utbetalingslinjer.Endringskode.UEND
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class OverstyrArbeidsgiveropplysningerTest : AbstractEndToEndTest() {

    @Test
    fun `overstyrer inntekt og refusjon`() {
        nyttVedtak(1.januar, 31.januar)
        nullstillTilstandsendringer()
        val nyInntekt = INNTEKT * 2
        val overstyringId = UUID.randomUUID()
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(ORGNUMMER, nyInntekt, "Det var jo alt for lite!", null, listOf(
                Triple(1.januar, null, nyInntekt)
            ))
        ), meldingsreferanseId = overstyringId)
        håndterYtelser(1.vedtaksperiode)
        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        val revurdering = inspektør.utbetaling(1).inspektør
        assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
        assertEquals(0, revurdering.personOppdrag.size)
        revurdering.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(17.januar, oppdrag[0].inspektør.fom)
            assertEquals(31.januar, oppdrag[0].inspektør.tom)
            assertEquals(2161, oppdrag[0].inspektør.beløp)
        }
        inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.also { vilkårsgrunnlag ->
            vilkårsgrunnlag.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.let { arbeidsgiverInntektsopplysninger ->
                assertEquals(1, arbeidsgiverInntektsopplysninger.size)
                arbeidsgiverInntektsopplysninger.single().inspektør.also { overstyring ->
                    assertEquals(nyInntekt, overstyring.inntektsopplysning.omregnetÅrsinntekt())
                    assertEquals(listOf(Refusjonsopplysning(overstyringId, 1.januar, null, nyInntekt)), overstyring.refusjonsopplysninger.inspektør.refusjonsopplysninger)
                }
            }
        } ?: fail { "Forventet vilkårsgrunnlag" }
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `overstyrer inntekt og refusjon til samme som før`() {
        nyttVedtak(1.januar, 31.januar)
        nullstillTilstandsendringer()
        val nyInntekt = INNTEKT * 2
        val overstyringId = UUID.randomUUID()
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(ORGNUMMER, nyInntekt, "Det var jo alt for lite!", null, listOf(
                Triple(1.januar, null, nyInntekt)
            ))
        ), meldingsreferanseId = overstyringId)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        nullstillTilstandsendringer()
        val overstyring2Id = UUID.randomUUID()
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(ORGNUMMER, nyInntekt, "Det var jo alt for lite!", null, listOf(
                Triple(1.januar, null, nyInntekt)
            ))
        ), meldingsreferanseId = overstyring2Id)
        håndterYtelser(1.vedtaksperiode)

        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        val revurdering = inspektør.utbetaling(2).inspektør
        assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
        assertEquals(0, revurdering.personOppdrag.size)
        revurdering.arbeidsgiverOppdrag.also { oppdrag ->
            assertEquals(17.januar, oppdrag[0].inspektør.fom)
            assertEquals(31.januar, oppdrag[0].inspektør.tom)
            assertEquals(2161, oppdrag[0].inspektør.beløp)
        }
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.also { vilkårsgrunnlag ->
            vilkårsgrunnlag.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.let { arbeidsgiverInntektsopplysninger ->
                assertEquals(1, arbeidsgiverInntektsopplysninger.size)
                arbeidsgiverInntektsopplysninger.single().inspektør.also { overstyring ->
                    assertEquals(nyInntekt, overstyring.inntektsopplysning.omregnetÅrsinntekt())
                    assertEquals(listOf(Refusjonsopplysning(overstyringId, 1.januar, null, nyInntekt)), overstyring.refusjonsopplysninger.inspektør.refusjonsopplysninger)
                }
            }
        } ?: fail { "Forventet vilkårsgrunnlag" }
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
    }

    @Test
    fun `overstyrer inntekt på Infotrygdvilkårsgrunnlag`() {
        createOvergangFraInfotrygdPerson()
        assertTrue(inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.infotrygd)
        val antallHistorikkInnslagFør = inspektør.vilkårsgrunnlagHistorikkInnslag().size
        val gammelInntekt = inspektør.inntektISykepengegrunnlaget(1.vedtaksperiode)
        val nyInntekt = INNTEKT*2
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(ORGNUMMER, nyInntekt, "Prøver å overstyre Infotrygd-inntekt", null, emptyList())
        ))
        håndterYtelser(1.vedtaksperiode)
        assertEquals(antallHistorikkInnslagFør, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        assertEquals(UEND, inspektør.utbetaling(1).inspektør.arbeidsgiverOppdrag.inspektør.endringskode)
        assertEquals(gammelInntekt, inspektør.inntektISykepengegrunnlaget(1.vedtaksperiode))
    }

    @Test
    fun `overstyrer refusjon på Infotrygdvilkårsgrunnlag`() {
        createOvergangFraInfotrygdPerson()
        assertTrue(inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.infotrygd)
        val antallHistorikkInnslag = inspektør.vilkårsgrunnlagHistorikkInnslag().size
        val gammelInntekt = inspektør.inntektISykepengegrunnlaget(1.vedtaksperiode)
        val overstyringId = UUID.randomUUID()
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(
            OverstyrtArbeidsgiveropplysning(ORGNUMMER, gammelInntekt, "Bare nye refusjonsopplysninger her", null, listOf(
                Triple(1.januar, null, gammelInntekt/2)
            ))
        ), meldingsreferanseId = overstyringId)
        håndterYtelser(1.vedtaksperiode)
        assertEquals(antallHistorikkInnslag + 1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        assertEquals(gammelInntekt, inspektør.inntektISykepengegrunnlaget(1.vedtaksperiode))
        assertEquals(listOf(Refusjonsopplysning(overstyringId, 1.januar, null, gammelInntekt/2)), inspektør.refusjonsopplysningerISykepengegrunnlaget(1.vedtaksperiode))

        val førsteUtbetaling = inspektør.utbetaling(0).inspektør
        val revurdering = inspektør.utbetaling(1).inspektør
        assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)

        assertEquals(1431, førsteUtbetaling.arbeidsgiverOppdrag.single().inspektør.beløp)
        assertEquals(0, førsteUtbetaling.personOppdrag.size)

        assertEquals(715, revurdering.arbeidsgiverOppdrag.single().inspektør.beløp)
        assertEquals(716, revurdering.personOppdrag.single().inspektør.beløp)
    }

    @Test
    fun `skal være idempotente greier`() {
        nyttVedtak(1.januar, 31.januar)

        val overstyr: () -> Unit = {
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                meldingsreferanseId = UUID.randomUUID(),
                arbeidsgiveropplysninger = listOf(OverstyrtArbeidsgiveropplysning(ORGNUMMER, INNTEKT/2, "noe", null, refusjonsopplysninger = listOf(
                    Triple(1.januar, null, INNTEKT/2)
                )))
            )
            håndterYtelser(1.vedtaksperiode)
        }

        assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        overstyr()
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)

        repeat(10) { overstyr() }
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
    }

    private fun TestArbeidsgiverInspektør.inntektISykepengegrunnlaget(vedtaksperiodeInnhenter: IdInnhenter, orgnr: String = ORGNUMMER) =
        vilkårsgrunnlag(vedtaksperiodeInnhenter)!!.inspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.single { it.gjelder(orgnr) }.inspektør.inntektsopplysning.omregnetÅrsinntekt()

    private fun TestArbeidsgiverInspektør.refusjonsopplysningerISykepengegrunnlaget(vedtaksperiodeInnhenter: IdInnhenter, orgnr: String = ORGNUMMER) =
        vilkårsgrunnlag(vedtaksperiodeInnhenter)!!.inspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.single { it.gjelder(orgnr) }.inspektør.refusjonsopplysninger.inspektør.refusjonsopplysninger
}
