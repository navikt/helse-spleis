package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.*
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RK_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_53
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class AvventerRettighetskategoriTest : AbstractDslTest() {
    @BeforeEach
    fun `besvar rettighetskategori manuelt`() {
        testperson.behovshåndterer.besvarRettighetskategoriAutomatisk = false
    }

    @Test
    fun `bare arbeidstaker går gjennom AvventerRettighetskategori og får rettighetskategoriId fra løsningen`() {
        a1 {
            val søknadId = UUID.randomUUID()
            håndterSøknad(januar, søknadId = søknadId)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
            assertEquals(
                "RETTIGHETSKATEGORI",
                observatør.vedtaksperiodeVenter
                    .last()
                    .venterPå.venteårsak.hva,
            )

            val behov = rettighetskategoribehov(1.vedtaksperiode).single()
            assertEquals(1.januar, behov.skjæringstidspunkt)
            assertEquals(listOf(søknadId), behov.søknader)

            val rettighetskategoriId = UUID.randomUUID()
            håndterRettighetskategori(1.vedtaksperiode, rettighetskategoriId)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertEquals(rettighetskategoriId, inspektør.rettighetskategorigrunnlag(1.januar)?.rettighetskategoriId)
            assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

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
            )
            assertEquals(rettighetskategoriId, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.rettighetskategoriId)
        }
    }

    @Test
    fun `bare selvstendig næringsdrivende går gjennom AvventerRettighetskategori`() {
        selvstendig {
            val søknadId = UUID.randomUUID()
            håndterFørstegangssøknadSelvstendig(januar, søknadId = søknadId)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
            val behov = rettighetskategoribehov(1.vedtaksperiode).single()
            assertEquals(1.januar, behov.skjæringstidspunkt)
            assertEquals(listOf(søknadId), behov.søknader)

            val rettighetskategoriId = UUID.randomUUID()
            håndterRettighetskategori(1.vedtaksperiode, rettighetskategoriId, Hovedkategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            assertEquals(rettighetskategoriId, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.rettighetskategoriId)
        }
    }

    @Test
    fun `forlengelse går ikke gjennom AvventerRettighetskategori`() {
        a1 {
            nyttVedtakMedRettighetskategori(januar)
            nyPeriode(februar)
            håndterYtelser(2.vedtaksperiode)
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
            )
            assertTrue(rettighetskategoribehov(2.vedtaksperiode).isEmpty())
        }
    }

    @Test
    fun `bare den siste søknaden for den første perioden er med i behovet`() {
        a1 {
            val førsteSøknad = UUID.randomUUID()
            val korrigerendeSøknad = UUID.randomUUID()
            val andreSøknad = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent), søknadId = førsteSøknad)
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent), søknadId = korrigerendeSøknad, korrigerer = førsteSøknad)
            håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent), søknadId = andreSøknad)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 2.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)

            val behov = rettighetskategoribehov(2.vedtaksperiode).single()
            assertEquals(1.januar, behov.skjæringstidspunkt)
            assertEquals(listOf(korrigerendeSøknad), behov.søknader)
        }
    }

    @Test
    fun `to arbeidsgivere der den andre starter etter skjæringstidspunktet`() {
        val søknadA1 = UUID.randomUUID()
        val søknadA2 = UUID.randomUUID()
        a1 { håndterSøknad(januar, søknadId = søknadA1) }
        a2 { håndterSøknad(10.januar til 31.januar, søknadId = søknadA2) }
        a1 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a2 { håndterArbeidsgiveropplysninger(listOf(10.januar til 25.januar)) }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
            val behov = rettighetskategoribehov(1.vedtaksperiode).single()
            assertEquals(1.januar, behov.skjæringstidspunkt)
            assertEquals(setOf(søknadA1, søknadA2), behov.søknader.toSet())
            assertEquals(2, behov.søknader.size)
        }
    }

    @Test
    fun `korrigert søknad for den første perioden gir nytt behov med den korrigerende søknaden`() {
        a1 {
            val søknadId = UUID.randomUUID()
            val korrigerendeSøknad = UUID.randomUUID()
            håndterSøknad(januar, søknadId = søknadId)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)

            håndterSøknad(Sykdom(1.januar, 31.januar, 80.prosent), søknadId = korrigerendeSøknad, korrigerer = søknadId)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
            assertEquals(listOf(korrigerendeSøknad), rettighetskategoribehov(1.vedtaksperiode).last().søknader)

            håndterRettighetskategori(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `revurdering som flytter skjæringstidspunktet går gjennom AvventerRettighetskategori`() {
        a1 {
            val rettighetskategoriFør = UUID.randomUUID()
            nyttVedtakMedRettighetskategori(januar, rettighetskategoriFør)

            håndterOverstyrTidslinje(
                listOf(
                    ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag, 100),
                    ManuellOverskrivingDag(4.januar, Dagtype.Arbeidsdag, 100),
                ),
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
            assertEquals(5.januar, rettighetskategoribehov(1.vedtaksperiode).last().skjæringstidspunkt)
            observatør.vedtaksperiodeVenter.last().venterPå.venteårsak.also {
                assertEquals("RETTIGHETSKATEGORI", it.hva)
                assertEquals("OVERSTYRING_IGANGSATT", it.hvorfor)
            }

            val rettighetskategoriEtter = UUID.randomUUID()
            håndterRettighetskategori(1.vedtaksperiode, rettighetskategoriEtter)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(rettighetskategoriEtter, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.rettighetskategoriId)
        }
    }

    @Test
    fun `fullt vilkårsgrunnlag erstatter det minimale med samme rettighetskategoriId`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            val rettighetskategoriId = UUID.randomUUID()
            håndterRettighetskategori(1.vedtaksperiode, rettighetskategoriId)

            val minimalt =
                dto()
                    .vilkårsgrunnlagHistorikk.historikk
                    .first()
                    .vilkårsgrunnlag
                    .single()
            assertInstanceOf(VilkårsgrunnlagUtDto.Rettighetskategori::class.java, minimalt)
            assertEquals(rettighetskategoriId, minimalt.rettighetskategoriId)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            val fullt =
                dto()
                    .vilkårsgrunnlagHistorikk.historikk
                    .first()
                    .vilkårsgrunnlag
                    .single()
            assertInstanceOf(VilkårsgrunnlagUtDto.Spleis::class.java, fullt)
            assertEquals(1.januar, fullt.skjæringstidspunkt)
            assertEquals(rettighetskategoriId, fullt.rettighetskategoriId)
            assertNotEquals(minimalt.vilkårsgrunnlagId, fullt.vilkårsgrunnlagId)
            assertNull(inspektør.rettighetskategorigrunnlag(1.januar))
        }
        assertGjenoppbygget(dto())
    }

    @Test
    fun `minimalt vilkårsgrunnlag forsvinner når sykefraværstilfellet forkastes før vilkårsprøving`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterRettighetskategori(1.vedtaksperiode)
            assertNotNull(inspektør.rettighetskategorigrunnlag(1.januar))

            håndterAnmodningOmForkasting(1.vedtaksperiode, force = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertNull(inspektør.rettighetskategorigrunnlag(1.januar))
            assertTrue(
                dto()
                    .vilkårsgrunnlagHistorikk.historikk
                    .first()
                    .vilkårsgrunnlag
                    .isEmpty(),
            )
        }
    }

    @Test
    fun `ubesvart behov sendes på nytt ved påminnelse`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertEquals(1, rettighetskategoribehov(1.vedtaksperiode).size)

            håndterPåminnelse(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
            assertEquals(2, rettighetskategoribehov(1.vedtaksperiode).size)
        }
    }

    @Test
    fun `løsning for feil vedtaksperiode eller i feil tilstand ignoreres`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterRettighetskategori(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            val nyId = UUID.randomUUID()
            håndterRettighetskategori(1.vedtaksperiode, nyId, skjæringstidspunkt = 1.januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertNotEquals(nyId, inspektør.rettighetskategorigrunnlag(1.januar)?.rettighetskategoriId)
            assertInfo("Forventet ikke rettighetskategori i AVVENTER_VILKÅRSPRØVING", 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `arbeidstaker går videre før selvstendig tilbakedatert kommer`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterRettighetskategori(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertFunksjonellFeil(RV_SØ_53, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `selvstendig tilbakedatert kommer før arbeidstaker har spurt om rettighetskategori`() {
        a1 {
            håndterSøknad(januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertIngenFunksjonellFeil(RV_SØ_53, 1.vedtaksperiode.filter())
            assertTrue(rettighetskategoribehov(1.vedtaksperiode).isEmpty())
        }
        a1 {
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
            assertEquals(2, rettighetskategoribehov(1.vedtaksperiode).single().søknader.size)
            håndterRettighetskategori(1.vedtaksperiode, hovedkategori = Hovedkategori.ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertFunksjonellFeil(RV_RK_1, 1.vedtaksperiode.filter())
        }
        selvstendig {
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertTrue(rettighetskategoribehov(1.vedtaksperiode).isEmpty())
        }
    }

    @Test
    fun `ny arbeidstaker kommer etter at den første arbeidstakeren har gått til godkjenning`() {
        val søknadA1 = UUID.randomUUID()
        val rettighetskategoriId = UUID.randomUUID()
        a1 {
            håndterSøknad(januar, søknadId = søknadA1)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertEquals(listOf(søknadA1), rettighetskategoribehov(1.vedtaksperiode).single().søknader)
            håndterRettighetskategori(1.vedtaksperiode, rettighetskategoriId)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 {
            håndterSøknad(januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE) }
        a2 {
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTrue(rettighetskategoribehov(1.vedtaksperiode).isEmpty())
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            assertTrue(rettighetskategoribehov(1.vedtaksperiode).isEmpty())
            assertEquals(rettighetskategoriId, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.rettighetskategoriId)
        }
    }

    private fun TestPerson.TestArbeidsgiver.nyttVedtakMedRettighetskategori(
        periode: Periode,
        rettighetskategoriId: UUID = UUID.randomUUID(),
    ) {
        nyPeriode(periode)
        håndterArbeidsgiveropplysninger(listOf(periode.start til periode.start.plusDays(15)), beregnetInntekt = INNTEKT)
        håndterRettighetskategori(1.vedtaksperiode, rettighetskategoriId)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
    }
}
