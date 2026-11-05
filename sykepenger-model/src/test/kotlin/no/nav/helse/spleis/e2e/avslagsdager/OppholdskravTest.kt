package no.nav.helse.spleis.e2e.avslagsdager

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.selvstendig
import no.nav.helse.februar
import no.nav.helse.hendelser.Avslagsdager
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.MeldingTilNavDager
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

internal class OppholdskravTest : AbstractDslTest() {
    private val avslåttePerioden = 22.januar til 26.januar

    @Test
    fun `avslag for oppholdskrav før første beregning`() {
        val vurdering = UUID.randomUUID()
        a1 {
            tilYtelser(januar)
            håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, avslåttePerioden))
            håndterSimulering(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertAvslåttForOppholdskrav(1.vedtaksperiode, avslåttePerioden)
            assertEquals(setOf(vurdering), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagsdagerVurderingIder)
            assertGjenoppbygget(dto())
        }
    }

    @Test
    fun `omgjøring med ny vurdering fjerner avslaget`() {
        val vurdering1 = UUID.randomUUID()
        val vurdering2 = UUID.randomUUID()
        a1 {
            tilYtelser(januar)
            håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering1, avslåttePerioden))
            håndterSimulering(1.vedtaksperiode)
            assertAvslåttForOppholdskrav(1.vedtaksperiode, avslåttePerioden)

            håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING, flagg = setOf("ønskerReberegning"))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterYtelser(1.vedtaksperiode, avslagsdager = Avslagsdager(setOf(vurdering2), emptyList()))
            håndterSimulering(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertEquals(Avslagstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagstidslinje)
            assertEquals(emptyList<LocalDate>(), inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedatoer)
            assertEquals(setOf(vurdering2), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagsdagerVurderingIder)
        }
    }

    @Test
    fun `uten løsning på avslagsdager blir avslaget fra forrige beregning stående`() {
        val vurdering = UUID.randomUUID()
        a1 {
            tilYtelser(januar)
            håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, avslåttePerioden))
            håndterSimulering(1.vedtaksperiode)

            håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING, flagg = setOf("ønskerReberegning"))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertAvslåttForOppholdskrav(1.vedtaksperiode, avslåttePerioden)
            assertEquals(setOf(vurdering), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagsdagerVurderingIder)
        }
    }

    @Test
    fun `to arbeidsgivere får begge dagene avslått`() {
        val vurdering = UUID.randomUUID()
        a1 { håndterSøknad(januar) }
        a2 { håndterSøknad(januar) }
        a1 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a2 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, avslåttePerioden))
            håndterSimulering(1.vedtaksperiode)
            assertAvslåttForOppholdskrav(1.vedtaksperiode, avslåttePerioden)
        }
        a2 {
            assertAvslåttForOppholdskrav(1.vedtaksperiode, avslåttePerioden)
            assertEquals(setOf(vurdering), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagsdagerVurderingIder)
        }
    }

    @Test
    fun `ny periode får avslaget gjennom behovet`() {
        val vurdering = UUID.randomUUID()
        val avslåttIFebruar = 5.februar til 9.februar
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, avslåttIFebruar))
            håndterSimulering(2.vedtaksperiode)
            assertAvslåttForOppholdskrav(2.vedtaksperiode, avslåttIFebruar)
            assertEquals(Avslagstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagstidslinje)
        }
    }

    @Test
    fun `overstyring av en dag som er avslått for oppholdskrav feiler`() {
        val vurdering = UUID.randomUUID()
        a1 {
            tilYtelser(januar)
            håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, avslåttePerioden))
            håndterSimulering(1.vedtaksperiode)

            assertThrows<IllegalStateException> {
                håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(23.januar, Dagtype.Sykedag, 100)))
            }
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertAvslåttForOppholdskrav(1.vedtaksperiode, avslåttePerioden)

            assertDoesNotThrow {
                håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(30.januar, Dagtype.Feriedag)))
            }
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `avslag for oppholdskrav på en dag saksbehandler har avslått feiler`() {
        val vurdering = UUID.randomUUID()
        selvstendig {
            saksbehandlerAvslårMeldingTilNav()
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            assertThrows<IllegalStateException> {
                håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, 1.januar.somPeriode()))
            }
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            assertEquals(avslåttAvSaksbehandler, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagstidslinje)
            assertEquals(emptySet<UUID>(), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagsdagerVurderingIder)
        }
    }

    @Test
    fun `oppfylt vurdering lar saksbehandlers avslag stå`() {
        val vurdering = UUID.randomUUID()
        selvstendig {
            saksbehandlerAvslårMeldingTilNav()
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode, avslagsdager = Avslagsdager(setOf(vurdering), emptyList()))
            håndterSimulering(1.vedtaksperiode)

            assertEquals(avslåttAvSaksbehandler, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagstidslinje)
            assertEquals(listOf(Begrunnelse.AvslåttMeldingTilNavDag), inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.begrunnelse(1.januar))
            assertEquals(setOf(vurdering), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagsdagerVurderingIder)
        }
    }

    @Test
    fun `uten løsning på avslagsdager er avslagstidslinjen urørt`() {
        a1 {
            tilYtelser(januar)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertEquals(Avslagstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagstidslinje)
            assertEquals(emptySet<UUID>(), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagsdagerVurderingIder)
        }
    }

    private val avslåttAvSaksbehandler =
        Avslagstidslinje(1.januar.somPeriode() to Avslagstidslinje.Avslagsdag(listOf(Begrunnelse.AvslåttMeldingTilNavDag), "Saksbehandler"))

    private fun no.nav.helse.dsl.TestPerson.TestArbeidsgiver.tilYtelser(periode: Periode) {
        håndterSøknad(periode)
        håndterArbeidsgiveropplysninger(listOf(periode.start til periode.start.plusDays(15)))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    private fun no.nav.helse.dsl.TestPerson.TestArbeidsgiver.saksbehandlerAvslårMeldingTilNav() {
        håndterFørstegangssøknadSelvstendig(MeldingTilNavDager(1.januar, 1.januar), Sykdom(2.januar, 31.januar, 100.prosent))
        håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.januar, Dagtype.AvslattMeldingTilNavdag)))
        assertEquals(avslåttAvSaksbehandler, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagstidslinje)
    }

    private fun no.nav.helse.dsl.TestPerson.TestArbeidsgiver.assertAvslåttForOppholdskrav(
        vedtaksperiodeId: UUID,
        periode: Periode,
    ) {
        assertEquals(
            Avslagstidslinje(periode to Avslagstidslinje.Avslagsdag(listOf(Begrunnelse.Oppholdskrav), Avslagsdager.KILDE)),
            inspektør.vedtaksperioder(vedtaksperiodeId).inspektør.avslagstidslinje,
        )
        with(inspektør.utbetalingstidslinjer(vedtaksperiodeId).inspektør) {
            assertEquals(periode.toList(), avvistedatoer)
            periode.forEach { assertEquals(listOf(Begrunnelse.Oppholdskrav), begrunnelse(it)) }
        }
    }

    private fun oppholdskravIkkeOppfylt(
        vurderingId: UUID,
        periode: Periode,
    ) = Avslagsdager(setOf(vurderingId), periode.map { Avslagsdager.Dag(vurderingId, it, Begrunnelse.Oppholdskrav) })
}
