package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.selvstendig
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RK_1
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

internal class RettighetskategoriEndretTest : AbstractDslTest() {
    @Test
    fun `periode med vedtak revurderes med ny rettighetskategoriId uten nytt behov om rettighetskategori`() {
        a1 {
            nyttVedtak(januar)
            val antallRettighetskategoribehov = rettighetskategoribehov(1.vedtaksperiode).size
            val nyRettighetskategoriId = UUID.randomUUID()

            håndterRettighetskategoriEndret(1.januar, nyRettighetskategoriId)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
            assertEquals(antallRettighetskategoribehov, rettighetskategoribehov(1.vedtaksperiode).size)
            assertEquals(nyRettighetskategoriId, observatør.trengerInformasjonTilVilkårsprøvingEventer.last().rettighetskategoriId)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertEquals(nyRettighetskategoriId, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.rettighetskategoriId)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_RETTIGHETSKATEGORI,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_VILKÅRSPRØVING_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
            )
        }
    }

    @Test
    fun `periode til godkjenning går tilbake til vilkårsprøving med ny rettighetskategoriId`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            val antallRettighetskategoribehov = rettighetskategoribehov(1.vedtaksperiode).size
            val nyRettighetskategoriId = UUID.randomUUID()

            håndterRettighetskategoriEndret(1.januar, nyRettighetskategoriId)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertEquals(antallRettighetskategoribehov, rettighetskategoribehov(1.vedtaksperiode).size)
            assertEquals(nyRettighetskategoriId, observatør.trengerInformasjonTilVilkårsprøvingEventer.last().rettighetskategoriId)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            assertEquals(nyRettighetskategoriId, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.rettighetskategoriId)
        }
    }

    @Test
    fun `selvstendig til godkjenning går tilbake til vilkårsprøving med ny rettighetskategoriId`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            val antallRettighetskategoribehov = rettighetskategoribehov(1.vedtaksperiode).size
            val nyRettighetskategoriId = UUID.randomUUID()

            håndterRettighetskategoriEndret(1.januar, nyRettighetskategoriId, Hovedkategori.SELVSTENDIG_NÆRINGSDRIVENDE)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertEquals(antallRettighetskategoribehov, rettighetskategoribehov(1.vedtaksperiode).size)
            assertEquals(nyRettighetskategoriId, observatør.trengerInformasjonTilVilkårsprøvingEventer.last().rettighetskategoriId)
        }
    }

    @Test
    fun `selvstendig med vedtak revurderes med ny rettighetskategoriId`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            val antallRettighetskategoribehov = rettighetskategoribehov(1.vedtaksperiode).size
            val nyRettighetskategoriId = UUID.randomUUID()

            håndterRettighetskategoriEndret(1.januar, nyRettighetskategoriId, Hovedkategori.SELVSTENDIG_NÆRINGSDRIVENDE)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
            assertEquals(antallRettighetskategoribehov, rettighetskategoribehov(1.vedtaksperiode).size)
            assertEquals(nyRettighetskategoriId, observatør.trengerInformasjonTilVilkårsprøvingEventer.last().rettighetskategoriId)
        }
    }

    @Test
    fun `berører alle perioder som ikke slutter før skjæringstidspunktet, på tvers av yrkesaktiviteter`() {
        listOf(a1, a2).nyeVedtak(januar)
        listOf(a1, a2).forlengVedtak(februar)
        a1 { nyttVedtak(1.april til 30.april) }

        a1 { håndterRettighetskategoriEndret(1.januar, UUID.randomUUID()) }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `periode uten vedtak forkastes når hovedkategorien ikke støttes`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            håndterRettighetskategoriEndret(1.januar, UUID.randomUUID(), Hovedkategori.ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)

            assertFunksjonellFeil(RV_RK_1, 1.vedtaksperiode.filter())
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `revurdering fortsetter når hovedkategorien ikke støttes, mens forlengelsen uten vedtak forkastes`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
            val nyRettighetskategoriId = UUID.randomUUID()

            håndterRettighetskategoriEndret(1.januar, nyRettighetskategoriId, Hovedkategori.ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)

            assertIngenFunksjonellFeil(RV_RK_1, 1.vedtaksperiode.filter())
            assertFunksjonellFeil(RV_RK_1, 2.vedtaksperiode.filter())
            assertSisteForkastetTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
            assertEquals(nyRettighetskategoriId, observatør.trengerInformasjonTilVilkårsprøvingEventer.last().rettighetskategoriId)
        }
    }

    @Test
    fun `bytter id i minimalt vilkårsgrunnlag`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            val antallRettighetskategoribehov = rettighetskategoribehov(1.vedtaksperiode).size
            val nyRettighetskategoriId = UUID.randomUUID()

            håndterRettighetskategoriEndret(1.januar, nyRettighetskategoriId)

            assertEquals(nyRettighetskategoriId, inspektør.rettighetskategorigrunnlag(1.januar)?.rettighetskategoriId)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertEquals(antallRettighetskategoribehov, rettighetskategoribehov(1.vedtaksperiode).size)
            assertEquals(nyRettighetskategoriId, observatør.trengerInformasjonTilVilkårsprøvingEventer.last().rettighetskategoriId)
        }
    }

    @Test
    fun `korrigert søknad etter endret rettighetskategori gir nytt behov om rettighetskategori`() {
        a1 {
            val søknadId = UUID.randomUUID()
            håndterSøknad(januar, søknadId = søknadId)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            val endretRettighetskategoriId = UUID.randomUUID()
            håndterRettighetskategoriEndret(1.januar, endretRettighetskategoriId)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            nullstillTilstandsendringer()

            håndterSøknad(Sykdom(1.januar, 31.januar, 80.prosent), korrigerer = søknadId)

            assertTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_RETTIGHETSKATEGORI, AVVENTER_VILKÅRSPRØVING)
            assertNotEquals(endretRettighetskategoriId, inspektør.rettighetskategorigrunnlag(1.januar)?.rettighetskategoriId)
        }
    }

    @Test
    fun `ignorerer meldingen når skjæringstidspunktet ikke har vilkårsgrunnlag`() {
        a1 {
            håndterSøknad(januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterRettighetskategoriEndret(1.januar, UUID.randomUUID())

            assertInfo("Fant ikke vilkårsgrunnlag på ${1.januar}")
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertNull(inspektør.rettighetskategorigrunnlag(1.januar))
        }
    }

    @Test
    fun `ignorerer meldingen når vilkårsgrunnlaget er fra Infotrygd`() {
        medJSONPerson("/personer/infotrygdforlengelse.json", 334)
        val infotrygdgrunnlag =
            dto()
                .vilkårsgrunnlagHistorikk.historikk
                .first()
                .vilkårsgrunnlag
                .filterIsInstance<VilkårsgrunnlagUtDto.Infotrygd>()
                .first()

        a1 {
            håndterRettighetskategoriEndret(infotrygdgrunnlag.skjæringstidspunkt, UUID.randomUUID())
            assertInfo("Kan ikke endre rettighetskategori på Infotrygdvilkårsgrunnlag")
            assertNull(inspektør.rettighetskategorigrunnlag(infotrygdgrunnlag.skjæringstidspunkt))
        }
    }

    @Test
    fun `samme rettighetskategoriId som før gjør ingenting`() {
        a1 {
            nyttVedtak(januar)
            val rettighetskategoriId = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.rettighetskategoriId
            nullstillTilstandsendringer()

            håndterRettighetskategoriEndret(1.januar, rettighetskategoriId)

            assertInfo("Rettighetskategorien er allerede lagt til grunn")
            assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }
    }
}
