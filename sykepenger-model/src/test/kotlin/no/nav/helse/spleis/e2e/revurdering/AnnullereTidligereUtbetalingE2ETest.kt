package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.ArbeidIkkeGjenopptattDag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_21
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AnnullereTidligereUtbetalingE2ETest : AbstractDslTest() {

    @Test
    fun `Velger første oppdrag å bygge videre på når to arbeidsgiverperioder blir til én og den første har hatt en revudering uten endring i utbetaling`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            // Inntektsmelding som fører til revurdering uten endring i utbetaling
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            val korrelasjonsIdFomJanuar = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId

            // <hull>
            nyttVedtak(april)
            val korrelasjonsIdFomApril = inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId

            // Så pølsestrekker saksbehandler periodene til å være kant-i-kant
            håndterOverstyrTidslinje(mars.map { ManuellOverskrivingDag(it, ArbeidIkkeGjenopptattDag) })
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            val korrelasjonsIdByggetViderePå = inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId

            assertVarsler(3.vedtaksperiode, RV_UT_21)
            assertEquals(korrelasjonsIdFomJanuar, korrelasjonsIdByggetViderePå)
            val aprilUtbetaling = inspektør.utbetalinger.last { it.korrelasjonsId == korrelasjonsIdFomApril }
            assertEquals(Utbetalingtype.ANNULLERING, aprilUtbetaling.type)
            val januarUtbetaling = inspektør.utbetalinger.last { it.korrelasjonsId == korrelasjonsIdFomJanuar }
            assertEquals(Utbetalingtype.REVURDERING, januarUtbetaling.type)
            assertEquals(17.januar, januarUtbetaling.arbeidsgiverOppdrag.linjer.minOf { it.fom })
        }
    }

    @Test
    fun `annullere tidligere utbetaling på samme arbeidsgiver`() {
        a1 {
            nyttVedtak(januar)
            val utbetalingId = inspektør.utbetaling(0).utbetalingId
            nyttVedtak(mars)
            håndterAnnullering(utbetalingId)
            håndterUtbetalt()
            assertVarsel(Varselkode.RV_RV_7, 2.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `annullere tidligere utbetaling på annen arbeidsgiver`() {
        a1 {
            nyttVedtak(januar)
        }
        val utbetalingId = inspektør.utbetaling(0).utbetalingId
        a2 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            håndterAnnullering(utbetalingId)
            assertIngenFunksjonelleFeil()
            assertEquals(Utbetalingtype.ANNULLERING, inspektør.sisteUtbetaling().type)
        }
        a2 {
            assertVarsel(Varselkode.RV_RV_7, 1.vedtaksperiode.filter())
        }
    }
}
