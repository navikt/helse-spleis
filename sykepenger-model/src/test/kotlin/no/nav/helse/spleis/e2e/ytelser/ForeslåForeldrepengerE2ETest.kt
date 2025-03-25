package no.nav.helse.spleis.e2e.ytelser

import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_5
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_21
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Disabled("Har skrudd av å foreslå foreldrepenger ettersom det kan føre til feilaktige annulleringer")
internal class ForeslåForeldrepengerE2ETest : AbstractEndToEndTest() {
    @Test
    fun `Å foreslå foreldrepenger en hel periode kan medføre at vi feilaktig annullerer tidligere utebetalte perioder`() {
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
        nyttVedtak(mars)
        nyttVedtak(mai, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        val korrelasjonsIdMars = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId
        val korrelasjonsIdMai = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId

        håndterSøknad(juli)
        håndterArbeidsgiveropplysninger(listOf(1.juli til 16.juli), vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)

        assertEquals(listOf(1.juli til 16.juli), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.arbeidsgiverperiode)

        håndterYtelser(3.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(juli, 100)))

        assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.arbeidsgiverperiode)

        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()
        val korrelasjonsIdJuli = inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId

        assertEquals(korrelasjonsIdMars, korrelasjonsIdJuli) // Siden vi ikke har agp bygger vi videre på første utbetaling etter siste utbetalingsdag i Infotrygd
        assertTrue(inspektør.utbetalinger.last { it.korrelasjonsId == korrelasjonsIdMai }.erAnnullering) // Også annullerer vi alt mellom utbetalingen vi bygger videre på og perioden vi nå behandler
        assertVarsler(listOf(RV_AY_5, RV_UT_21), 3.vedtaksperiode.filter())
    }

    @Test
    fun `vilkårsgrunnlag forsvinner når foreldrepengene legges inn`() {
        nyttVedtak(7.januar til 23.januar, arbeidsgiverperiode = listOf(1.januar.somPeriode(), 8.januar til 22.januar))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(23.januar, Dagtype.Foreldrepengerdag)))

        assertForventetFeil(
            forklaring = "foreldrepengene legges inn på sykdomstidslinjen slik at vedtaksperioden blir stående uten et skjæringstidspunkt, og vilkårsgrunnlaget forsvinner",
            nå = {
                assertThrows<IllegalStateException> {
                    håndterYtelser(
                        1.vedtaksperiode, foreldrepenger = listOf(
                        GradertPeriode(8.januar til 23.januar, 100)
                    )
                    )
                }
            },
            ønsket = {
                håndterYtelser(
                    1.vedtaksperiode, foreldrepenger = listOf(
                    GradertPeriode(8.januar til 23.januar, 100)
                )
                )
                assertTilstand(1.vedtaksperiode, TilstandType.AVVENTER_SIMULERING_REVURDERING)
            }
        )
    }

    @Test
    fun `Fullstendig overlapp med foreldrepenger`() {
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(januar, 100)))
        assertVarsel(RV_AY_5, 1.vedtaksperiode.filter())
        assertEquals("YYYYYYY YYYYYYY YYYYYYY YYYYYYY YYY", inspektør.sykdomstidslinje.toShortString())
    }

    @Test
    fun `Overlapp med foreldrepenger i halen og utenfor perioden begrenses av perioden`() {
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(1.januar til 10.februar, 100)))
        assertVarsel(RV_AY_5, 1.vedtaksperiode.filter())
        assertEquals("YYYYYYY YYYYYYY YYYYYYY YYYYYYY YYY", inspektør.sykdomstidslinje.toShortString())
    }

    @Test
    fun `Overlapp med foreldrepenger i halen og før perioden begrenses av perioden`() {
        håndterSøknad(februar)
        håndterArbeidsgiveropplysninger(listOf(1.februar til 16.februar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(1.januar til 28.februar, 100)))
        assertVarsel(RV_AY_5, 1.vedtaksperiode.filter())
        assertEquals(1.februar, inspektør.sykdomstidslinje.førsteDag())
        assertEquals(28.februar, inspektør.sykdomstidslinje.sisteDag())
        assertEquals("YYYY YYYYYYY YYYYYYY YYYYYYY YYY", inspektør.sykdomstidslinje.toShortString())
    }
}
