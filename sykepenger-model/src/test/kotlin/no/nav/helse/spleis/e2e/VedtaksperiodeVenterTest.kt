package no.nav.helse.spleis.e2e

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.Venteårsak.Companion.INNTEKTSMELDING
import no.nav.helse.person.Venteårsak.Companion.SØKNAD
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VedtaksperiodeVenterTest : AbstractDslTest() {

    @Test
    fun `Korrigerte søknader kommer i vedtaksperiode_venter`() {
        a1 {
            tilGodkjenning(januar, 100.prosent)
            val søknadId1 = observatør.behandlingOpprettetEventer.single().søknadIder.single()
            val behandlingId = observatør.behandlingOpprettetEventer.single().behandlingId
            val søknadId2 = UUID.randomUUID()
            val søknadId3 = UUID.randomUUID()

            håndterSøknad(Sykdom(1.januar, 31.januar, 80.prosent), søknadId = søknadId2)
            val hendelseIderEtterSøknad2 = observatør.vedtaksperiodeVenter.last { it.behandlingId == behandlingId }.hendelser
            assertTrue(hendelseIderEtterSøknad2.containsAll(setOf(søknadId1, søknadId2)))

            håndterSøknad(Sykdom(1.januar, 31.januar, 70.prosent), søknadId = søknadId3)
            val hendelseIderEtterSøknad3 = observatør.vedtaksperiodeVenter.last { it.behandlingId == behandlingId }.hendelser
            assertTrue(hendelseIderEtterSøknad3.containsAll(setOf(søknadId1, søknadId2, søknadId3)))
        }
    }

    @Test
    fun `Vedtaksperioden vi venter på skal ikke være en auu`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        val a1Vedtaksperiode2 = a1 { 2.vedtaksperiode }
        a2 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            val vedtaksperiodeVenter = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == 1.vedtaksperiode }
            assertEquals(a1Vedtaksperiode2, vedtaksperiodeVenter.venterPå.vedtaksperiodeId)
            assertEquals("a1", vedtaksperiodeVenter.venterPå.yrkesaktivitetssporing.somOrganisasjonsnummer)
            assertEquals("INNTEKTSMELDING", vedtaksperiodeVenter.venterPå.venteårsak.hva)
            assertNull(vedtaksperiodeVenter.venterPå.venteårsak.hvorfor)
        }
    }

    @Test
    fun `Vedtaksperiode som revurderes som følge av søknad fra ghost skal peke på at den venter på perioden til ghosten`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            val vedtaksperiodeVenter = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == 1.vedtaksperiode }
            assertEquals(1.vedtaksperiode, vedtaksperiodeVenter.venterPå.vedtaksperiodeId)
            assertEquals("a2", vedtaksperiodeVenter.venterPå.yrkesaktivitetssporing.somOrganisasjonsnummer)
            assertEquals("INNTEKTSMELDING", vedtaksperiodeVenter.venterPå.venteårsak.hva)
            assertNull(vedtaksperiodeVenter.venterPå.venteårsak.hvorfor)
        }
        val a2VedtaksperiodeId = a2 { 1.vedtaksperiode }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            val vedtaksperiodeVenter = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == 1.vedtaksperiode }
            assertEquals("INNTEKTSMELDING", vedtaksperiodeVenter.venterPå.venteårsak.hva)
            assertEquals("a2", vedtaksperiodeVenter.venterPå.yrkesaktivitetssporing.somOrganisasjonsnummer)
            assertEquals(a2VedtaksperiodeId, vedtaksperiodeVenter.venterPå.vedtaksperiodeId)
            assertNull(vedtaksperiodeVenter.venterPå.venteårsak.hvorfor)
        }
    }

    @Test
    fun `Vedtaksperioden vi venter på kan være en annen enn den som er nestemann til behandling`() {
        a1 { håndterSøknad(januar) }
        a2 {
            håndterSøknad(januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        val a2VedtaksperiodeId = a2 { 1.vedtaksperiode }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            val vedtaksperiodeVenter = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == 1.vedtaksperiode }
            assertEquals(a2VedtaksperiodeId, vedtaksperiodeVenter.venterPå.vedtaksperiodeId)
            assertEquals("a2", vedtaksperiodeVenter.venterPå.yrkesaktivitetssporing.somOrganisasjonsnummer)
            assertEquals("INNTEKTSMELDING", vedtaksperiodeVenter.venterPå.venteårsak.hva)
            assertNull(vedtaksperiodeVenter.venterPå.venteårsak.hvorfor)
        }
    }

    @Test
    fun `Venter på tidligere periode som ikke har fått inntektsmelding`() {
        a1 {
            val søknadIdJanuar = UUID.randomUUID()
            nyPeriode(januar, søknadId = søknadIdJanuar)

            assertVenterPå(
                listOf(
                    1.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to INNTEKTSMELDING
                )
            )
            val søknadIdMars = UUID.randomUUID()
            nyPeriode(mars, søknadId = søknadIdMars)
            assertVenterPå(
                listOf(
                    1.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to INNTEKTSMELDING,
                    2.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to INNTEKTSMELDING,
                    2.vedtaksperiode to INNTEKTSMELDING
                )
            )

            val inntektsmeldingIdMars = håndterInntektsmelding(listOf(1.mars til 16.mars))
            assertVenterPå(
                listOf(
                    1.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to INNTEKTSMELDING,
                    2.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to INNTEKTSMELDING,
                    2.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to INNTEKTSMELDING,
                    2.vedtaksperiode to INNTEKTSMELDING
                )
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            val venterTil = inspektør(1.vedtaksperiode).oppdatert.plusDays(180)
            val forventetVedtaksperiode1 = PersonObserver.VedtaksperiodeVenterEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = inspektør(1.vedtaksperiode).behandlinger.last().id,
                skjæringstidspunkt = inspektør(1.vedtaksperiode).skjæringstidspunkt,
                hendelser = setOf(søknadIdJanuar),
                ventetSiden = inspektør(1.vedtaksperiode).oppdatert,
                venterTil = venterTil,
                venterPå = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
                    vedtaksperiodeId = 1.vedtaksperiode,
                    skjæringstidspunkt = inspektør(1.vedtaksperiode).skjæringstidspunkt,
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    venteårsak = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
                        hva = "INNTEKTSMELDING",
                        hvorfor = null
                    )
                )
            )
            val forventetVedtaksperiode2 = PersonObserver.VedtaksperiodeVenterEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 2.vedtaksperiode,
                behandlingId = inspektør(2.vedtaksperiode).behandlinger.last().id,
                skjæringstidspunkt = inspektør(2.vedtaksperiode).skjæringstidspunkt,
                hendelser = setOf(søknadIdMars, inntektsmeldingIdMars),
                ventetSiden = inspektør(2.vedtaksperiode).oppdatert,
                venterTil = venterTil,
                venterPå = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
                    vedtaksperiodeId = 1.vedtaksperiode,
                    skjæringstidspunkt = inspektør(1.vedtaksperiode).skjæringstidspunkt,
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    venteårsak = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
                        hva = "INNTEKTSMELDING",
                        hvorfor = null
                    )
                )
            )
            assertEquals(forventetVedtaksperiode1, observatør.vedtaksperiodeVenter.last {
                it.vedtaksperiodeId == 1.vedtaksperiode
            })
            assertEquals(forventetVedtaksperiode2, observatør.vedtaksperiodeVenter.last {
                it.vedtaksperiodeId == 2.vedtaksperiode
            })
        }
    }

    @Test
    fun `Venter på søknad på annen arbeidsgiver`() {
        a1 {
            håndterSykmelding(januar)
        }
        a2 {
            val søknadId = UUID.randomUUID()
            nyPeriode(januar, søknadId = søknadId)
            assertVenterPå(
                listOf(
                    1.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to INNTEKTSMELDING
                )
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertVenterPå(
                listOf(
                    1.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to INNTEKTSMELDING,
                    1.vedtaksperiode to SØKNAD
                )
            )

            val forventet = PersonObserver.VedtaksperiodeVenterEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = inspektør(1.vedtaksperiode).behandlinger.last().id,
                skjæringstidspunkt = inspektør(1.vedtaksperiode).skjæringstidspunkt,
                hendelser = setOf(søknadId, inntektsmeldingId),
                ventetSiden = inspektør(1.vedtaksperiode).oppdatert,
                venterTil = LocalDateTime.MAX,
                venterPå = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
                    vedtaksperiodeId = 1.vedtaksperiode,
                    skjæringstidspunkt = inspektør(1.vedtaksperiode).skjæringstidspunkt,
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2),
                    venteårsak = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
                        hva = "SØKNAD",
                        hvorfor = null
                    )
                )
            )
            assertEquals(forventet, observatør.vedtaksperiodeVenter.last())
        }
    }

    @Test
    fun `Periode i avventer inntektsmelding`() {
        a1 {
            val søknadId = UUID.randomUUID()
            nyPeriode(januar, søknadId = søknadId)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            val forventet = PersonObserver.VedtaksperiodeVenterEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = inspektør(1.vedtaksperiode).behandlinger.last().id,
                skjæringstidspunkt = inspektør(1.vedtaksperiode).skjæringstidspunkt,
                hendelser = setOf(søknadId),
                ventetSiden = inspektør(1.vedtaksperiode).oppdatert,
                venterTil = inspektør(1.vedtaksperiode).oppdatert.plusDays(180),
                venterPå = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
                    vedtaksperiodeId = 1.vedtaksperiode,
                    skjæringstidspunkt = inspektør(1.vedtaksperiode).skjæringstidspunkt,
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    venteårsak = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
                        hva = "INNTEKTSMELDING",
                        hvorfor = null
                    )
                )
            )
            assertEquals(forventet, observatør.vedtaksperiodeVenter.last())
        }
    }

    @Test
    fun `En periode i Avsluttet Uten Utbetaling som eneste periode som fortsatt ikke skal utbetales skriker ikke om hjelp`() {
        a1 {
            nyPeriode(16.januar til 31.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            val venteHendelseFør = observatør.vedtaksperiodeVenter.toList()
            håndterPåminnelse(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertEquals(venteHendelseFør, observatør.vedtaksperiodeVenter)
        }
    }

    @Test
    fun `Om perioden man venter på har en timeout bør den brukes som venter til`() {
        a1 {
            håndterSøknad(januar)
            håndterSøknad(mars)
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

            val januarVenterTil = inspektør(1.vedtaksperiode).oppdatert.plusDays(180)
            val januarVenter = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == 1.vedtaksperiode }
            assertEquals(1.vedtaksperiode, januarVenter.venterPå.vedtaksperiodeId)
            assertEquals(januarVenterTil, januarVenter.venterTil)
            val marsVenter = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == 2.vedtaksperiode }
            assertEquals(1.vedtaksperiode, marsVenter.venterPå.vedtaksperiodeId)
            assertEquals(januarVenterTil, marsVenter.venterTil)
        }
    }

    private fun assertVenterPå(expected: List<Pair<UUID, Venteårsak>>) {
        val actual = observatør.vedtaksperiodeVenter.map { it.vedtaksperiodeId to it.venterPå.venteårsak.hva }
        assertEquals(expected.map { it.first to it.second.toString() }, actual)
        assertEquals(expected.size, observatør.vedtaksperiodeVenter.size)
    }

    internal companion object {
        internal fun TestObservatør.assertVenter(venterVedtaksperiodeId: UUID, venterPåVedtaksperiodeId: UUID = venterVedtaksperiodeId, venterPåOrgnr: String? = null, venterPåHva: Venteårsak) {
            vedtaksperiodeVenter.last { it.vedtaksperiodeId == venterVedtaksperiodeId }.venterPå.assertVenterPå(venterPåVedtaksperiodeId, venterPåOrgnr, venterPåHva)
            if (venterVedtaksperiodeId == venterPåVedtaksperiodeId) return
            // Om periode A venter på en annen periode B så burde også B vente på B (vente på seg selv)
            vedtaksperiodeVenter.last { it.vedtaksperiodeId == venterPåVedtaksperiodeId }.venterPå.assertVenterPå(venterPåVedtaksperiodeId, venterPåOrgnr, venterPåHva)
        }

        private fun PersonObserver.VedtaksperiodeVenterEvent.VenterPå.assertVenterPå(venterPåVedtaksperiodeId: UUID, venterPåOrgnr: String?, venterPåHva: Venteårsak) {
            venterPåOrgnr?.let { assertEquals(it, this.yrkesaktivitetssporing.somOrganisasjonsnummer) }
            assertEquals(venterPåVedtaksperiodeId, this.vedtaksperiodeId)
            assertEquals(venterPåHva.event(), this.venteårsak)
        }
    }
}
