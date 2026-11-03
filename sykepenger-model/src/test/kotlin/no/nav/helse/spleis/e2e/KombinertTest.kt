package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.dsl.*
import no.nav.helse.februar
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori.ARBEIDSTAKER
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori.ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

internal class KombinertTest : AbstractDslTest() {
    @Test
    fun `arbeidstaker og selvstendig som overlapper skjæringstidspunktet forkastes med RV_RK_1 når løsningen kommer`() =
        Toggle.ValidereRettighetskategori.enable {
            testperson.behovshåndterer.besvarRettighetskategoriAutomatisk = false
            val søknadAt = UUID.randomUUID()
            val søknadSn = UUID.randomUUID()
            a1 { håndterSøknad(januar, søknadId = søknadAt) }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar, søknadId = søknadSn)
                assertIngenFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            }
            a1 {
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
                assertEquals(setOf(søknadAt, søknadSn), rettighetskategoribehov(1.vedtaksperiode).last().søknader.toSet())

                håndterRettighetskategori(1.vedtaksperiode, hovedkategori = ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)
                assertForkastetPeriodeTilstander(
                    1.vedtaksperiode,
                    START,
                    AVVENTER_INFOTRYGDHISTORIKK,
                    AVVENTER_INNTEKTSMELDING,
                    AVVENTER_BLOKKERENDE_PERIODE,
                    AVVENTER_RETTIGHETSKATEGORI,
                    TIL_INFOTRYGD,
                    varselkode = Varselkode.RV_RK_1,
                )
                assertNull(inspektør.rettighetskategorigrunnlag(1.januar))
            }
            selvstendig {
                assertForkastetPeriodeTilstander(
                    1.vedtaksperiode,
                    START,
                    AVVENTER_BLOKKERENDE_PERIODE,
                    TIL_INFOTRYGD,
                    varselkode = null,
                )
                assertIngenFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
            }
            val json = dto().tilPersonData().tilSerialisertPerson().json
            assertFalse(json.contains(ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE.name))
        }

    @Test
    fun `arbeidstaker januar og selvstendig februar med samme skjæringstidspunkt forkastes begge når løsningen kommer`() =
        Toggle.ValidereRettighetskategori.enable {
            testperson.behovshåndterer.besvarRettighetskategoriAutomatisk = false
            val søknadAt = UUID.randomUUID()
            val søknadSn = UUID.randomUUID()
            a1 { håndterSøknad(januar, søknadId = søknadAt) }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(februar, søknadId = søknadSn)
                assertIngenFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
                assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            }
            a1 {
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
                assertEquals(setOf(søknadAt, søknadSn), rettighetskategoribehov(1.vedtaksperiode).last().søknader.toSet())

                håndterRettighetskategori(1.vedtaksperiode, hovedkategori = ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
                assertFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
                assertIngenTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK)
            }
            selvstendig {
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
                assertIngenFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
                assertIngenTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK)
            }
        }

    @Test
    fun `søknad som forlenger et tilfelle forkastet med RV_RK_1 forkastes uten RV_RK_1`() =
        Toggle.ValidereRettighetskategori.enable {
            testperson.behovshåndterer.besvarRettighetskategoriAutomatisk = false
            a1 { håndterSøknad(januar) }
            selvstendig { håndterFørstegangssøknadSelvstendig(januar) }
            a1 {
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterRettighetskategori(1.vedtaksperiode, hovedkategori = ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)

                håndterSøknad(februar)
                assertSisteForkastetTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
                assertIngenFunksjonellFeil(Varselkode.RV_RK_1, 2.vedtaksperiode.filter())
                assertFunksjonellFeil(Varselkode.RV_SØ_37, 2.vedtaksperiode.filter())
                assertTrue(rettighetskategoribehov(2.vedtaksperiode).isEmpty())
            }
        }

    @Test
    fun `søknad som kommer mens perioden står i AvventerRettighetskategori`() =
        Toggle.ValidereRettighetskategori.enable {
            testperson.behovshåndterer.besvarRettighetskategoriAutomatisk = false
            val søknadAt = UUID.randomUUID()
            val søknadSn = UUID.randomUUID()
            a1 {
                håndterSøknad(januar, søknadId = søknadAt)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
                assertEquals(listOf(søknadAt), rettighetskategoribehov(1.vedtaksperiode).last().søknader)
            }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar, søknadId = søknadSn)
                assertIngenFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
            }
            a1 {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
                assertEquals(setOf(søknadAt, søknadSn), rettighetskategoribehov(1.vedtaksperiode).last().søknader.toSet())
                håndterRettighetskategori(1.vedtaksperiode, hovedkategori = ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
                assertFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
            }
            selvstendig {
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
                assertIngenFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
            }
        }

    @Test
    fun `søknad som overlapper skjæringstidspunktet mens perioden står i AvventerVilkårsprøving med minimalt vilkårsgrunnlag`() =
        Toggle.ValidereRettighetskategori.enable {
            testperson.behovshåndterer.besvarRettighetskategoriAutomatisk = false
            a1 {
                håndterSøknad(januar)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterRettighetskategori(1.vedtaksperiode)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
                assertNotNull(inspektør.rettighetskategorigrunnlag(1.januar))
            }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar)
                assertIngenFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
            }
            val periodeSomSpør =
                listOf(a1, selvstendig).single { yrkesaktivitet ->
                    yrkesaktivitet { inspektør.sisteTilstand(1.vedtaksperiode) == AVVENTER_RETTIGHETSKATEGORI }
                }
            assertEquals(a1, periodeSomSpør)
            a1 {
                håndterRettighetskategori(1.vedtaksperiode, hovedkategori = ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
                assertFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
                assertIngenTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
                assertNull(inspektør.rettighetskategorigrunnlag(1.januar))
                assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
            }
            selvstendig {
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
                assertIngenFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
            }
        }

    @Test
    fun `revurdering som flytter skjæringstidspunktet forkaster ikke på grunnlag av hovedkategorien`() =
        Toggle.ValidereRettighetskategori.enable {
            a1 {
                nyttVedtak(januar)
                testperson.behovshåndterer.besvarRettighetskategoriAutomatisk = false
                håndterOverstyrTidslinje(
                    listOf(
                        ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag, 100),
                        ManuellOverskrivingDag(4.januar, Dagtype.Arbeidsdag, 100),
                    ),
                )
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
                håndterRettighetskategori(1.vedtaksperiode, hovedkategori = ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)
                assertIngenFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
                håndterVilkårsgrunnlag(1.vedtaksperiode)
                håndterYtelser(1.vedtaksperiode, rettighetskategoriResultat = ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
                assertVarsler(listOf(Varselkode.RV_RK_2, Varselkode.RV_IV_7, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            }
        }

    @Test
    fun `løsning på et eldre behov tas imot etter at en søknad med en annen yrkesaktivitetstype har kommet inn`() =
        Toggle.ValidereRettighetskategori.enable {
            testperson.behovshåndterer.besvarRettighetskategoriAutomatisk = false
            a1 {
                håndterSøknad(januar)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_RETTIGHETSKATEGORI)
            }
            selvstendig { håndterFørstegangssøknadSelvstendig(januar) }
            a1 {
                håndterRettighetskategori(1.vedtaksperiode, hovedkategori = ARBEIDSTAKER)
                håndterRettighetskategori(1.vedtaksperiode, hovedkategori = ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)
                assertIngenFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
                håndterVilkårsgrunnlag(1.vedtaksperiode)
                assertFunksjonellFeil(Varselkode.RV_IV_13, 1.vedtaksperiode.filter())
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            }
        }

    @Test
    fun `hovedkategorien finnes ikke i person-json`() =
        Toggle.ValidereRettighetskategori.enable {
            testperson.behovshåndterer.besvarRettighetskategoriAutomatisk = false
            val rettighetskategoriId = UUID.randomUUID()
            a1 {
                håndterSøknad(januar)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterRettighetskategori(1.vedtaksperiode, rettighetskategoriId, ARBEIDSTAKER)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            }
            val json = dto().tilPersonData().tilSerialisertPerson().json
            assertTrue(json.contains(rettighetskategoriId.toString()))
            assertFalse(json.contains("hovedkategori", ignoreCase = true))
        }

    @Test
    fun `ny yrkesaktivitet etter utbetaling forkaster bare den nye perioden med RV_SØ_53`() =
        Toggle.ValidereRettighetskategori.enable {
            a1 { nyttVedtak(januar) }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar)
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
                assertFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
                assertIngenFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
                assertTrue(rettighetskategoribehov(1.vedtaksperiode).isEmpty())
            }
            a1 {
                assertTrue(inspektør.periodeErIkkeForkastet(1.vedtaksperiode))
                assertIngenFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
            }
        }

    @Test
    fun `selvstendig forlengelse etter utbetalt arbeidstaker forkastes med RV_SØ_53`() =
        Toggle.ValidereRettighetskategori.enable {
            a1 { nyttVedtak(januar) }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(februar)
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
                assertFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
            }
            a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        }

    @Test
    fun `med togglen av brukes ikke hovedkategorien i løsningen`() =
        Toggle.ValidereRettighetskategori.disable {
            testperson.behovshåndterer.besvarRettighetskategoriAutomatisk = false
            a1 {
                håndterSøknad(januar)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterRettighetskategori(1.vedtaksperiode, hovedkategori = ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
                assertIngenFunksjonellFeil(Varselkode.RV_RK_1, 1.vedtaksperiode.filter())
                assertNotNull(inspektør.rettighetskategorigrunnlag(1.januar))
            }
        }

    @Test
    fun `med togglen av forkastes selvstendig som overlapper arbeidstaker med RV_SØ_53 når søknaden kommer inn`() =
        Toggle.ValidereRettighetskategori.disable {
            a1 { håndterSøknad(januar) }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar)
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
                assertFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
            }
        }

    @Test
    fun `selvstendig som forlenger en senere arbeidstakerperiode forkastes med RV_SØ_53 når den første perioden har spurt`() =
        Toggle.ValidereRettighetskategori.enable {
            a1 {
                håndterSøknad(januar)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterSøknad(februar)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(mars)
                assertFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            }
            a1 {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
                assertTrue(inspektør.periodeErIkkeForkastet(2.vedtaksperiode))
            }
        }

    @Test
    fun `selvstendig som forlenger arbeidstaker i AvventerVilkårsprøving forkastes med RV_SØ_53 og arbeidstaker får RV_RK_3`() =
        Toggle.ValidereRettighetskategori.enable {
            a1 {
                håndterSøknad(januar)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
                assertNotNull(inspektør.rettighetskategorigrunnlag(1.januar))
            }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(februar)
                assertFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            }
            a1 {
                håndterVilkårsgrunnlag(1.vedtaksperiode)
                håndterYtelser(1.vedtaksperiode)
                assertVarsel(Varselkode.RV_RK_3, 1.vedtaksperiode.filter())
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
            }
        }

    @Test
    fun `med togglen av forkastes selvstendig som forlenger arbeidstaker i AvventerVilkårsprøving med RV_SØ_53`() =
        Toggle.ValidereRettighetskategori.disable {
            a1 {
                håndterSøknad(januar)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(februar)
                assertFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            }
            a1 {
                håndterVilkårsgrunnlag(1.vedtaksperiode)
                håndterYtelser(1.vedtaksperiode)
                assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
            }
        }

    @Test
    fun `arbeidstaker som forlenger selvstendig i AvventerVilkårsprøving forkastes med RV_SØ_53 og selvstendig får RV_RK_3`() =
        Toggle.ValidereRettighetskategori.enable {
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
                assertNotNull(inspektør.rettighetskategorigrunnlag(1.januar))
            }
            a1 {
                håndterSøknad(februar)
                assertFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
                assertVarsel(Varselkode.RV_SØ_54, 1.vedtaksperiode.filter())
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            }
            selvstendig {
                håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
                håndterYtelserSelvstendig(1.vedtaksperiode)
                assertVarsel(Varselkode.RV_RK_3, 1.vedtaksperiode.filter())
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
            }
        }

    @Test
    fun `selvstendig rett før utbetalt arbeidstaker forkastes med RV_SØ_53`() =
        Toggle.ValidereRettighetskategori.enable {
            a1 { nyttVedtak(februar) }
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar)
                assertFunksjonellFeil(Varselkode.RV_SØ_53, 1.vedtaksperiode.filter())
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
                assertTrue(rettighetskategoribehov(1.vedtaksperiode).isEmpty())
            }
            a1 {
                assertTrue(inspektør.periodeErIkkeForkastet(1.vedtaksperiode))
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
                håndterYtelser(
                    1.vedtaksperiode,
                    opptjeningsvurderingResultatOk = true,
                    rettighetskategoriResultat = ARBEIDSTAKER,
                )
                assertVarsel(Varselkode.RV_RK_3, 1.vedtaksperiode.filter())
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            }
        }

    @Test
    fun `selvstendig med ghost fra skatt forkastes fortsatt med RV_IV_13 med togglen på`() =
        Toggle.ValidereRettighetskategori.enable {
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar)
                håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode, listOf(a1 to INNTEKT))
                assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
                assertFunksjonellFeil(Varselkode.RV_IV_13, 1.vedtaksperiode.filter())
                assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            }
        }

    private fun TestPerson.TestArbeidsgiver.assertIngenTilstand(
        vedtaksperiodeId: UUID,
        vararg tilstander: TilstandType,
    ) {
        val faktiske = observatør.tilstandsendringer.getValue(vedtaksperiodeId)
        tilstander.forEach { assertFalse(it in faktiske) { "Perioden var i $it: $faktiske" } }
    }

    @Test
    fun `en selvstendigsøknad er en forlengelse av en arbeidstakersøknad`() {
        a1 {
            nyttVedtak(januar)
        }
        selvstendig {
            håndterFørstegangssøknadSelvstendig(februar)
            assertFunksjonellFeil(Varselkode.`RV_SØ_53`, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `en selvstendigsøknad er en forlengelse av en arbeidstakersøknad og infaller på en måndag`() {
        a1 {
            håndterSøknad(1.januar til 5.januar)
        }
        selvstendig {
            håndterFørstegangssøknadSelvstendig(8.januar til 31.januar)
            assertFunksjonellFeil(Varselkode.`RV_SØ_53`, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `en arbeidstakersøknad er en forlengelse av en selvstendigsøknad`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
        }
        a1 {
            håndterSøknad(1.februar til 28.februar)
            assertFunksjonellFeil(Varselkode.`RV_SØ_53`, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_SØ_54, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `en arbeidstakersøknad er en forlengelse av en selvstendigsøknad og infaller på en måndag`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(1.januar til 5.januar)
        }
        a1 {
            håndterSøknad(8.januar til 31.januar)
            assertFunksjonellFeil(Varselkode.`RV_SØ_53`, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_SØ_54, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `selvstendig løper videre til vilkårsprøving selv om vi har en arbeidstaker-periode som ikke har refusjonsopplysninger`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(14.januar til 31.januar)
        }

        a1 {
            håndterSøknad(1.januar til 14.januar)
            håndterSøknad(15.januar til 20.januar)
            håndterSøknad(25.januar til 31.januar)
            håndterArbeidsgiveropplysningerForForkastetPeriode(
                2.vedtaksperiode,
                Arbeidsgiveropplysning.OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                Arbeidsgiveropplysning.OppgittInntekt(INNTEKT),
                Arbeidsgiveropplysning.OppgittRefusjon(INNTEKT, emptyList(), refusjonskravGyldigFra = null),
            )
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
        }

        selvstendig {
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }
}
