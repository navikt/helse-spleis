package no.nav.helse.hendelser

import no.nav.helse.januar
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.Avslagstidslinje.Avslagsdag
import no.nav.helse.person.LøpendeVilkår
import no.nav.helse.person.UtfallAvLøpendeVilkår
import no.nav.helse.person.UtfallAvLøpendeVilkår.IkkeOppfylt
import no.nav.helse.person.UtfallAvLøpendeVilkår.Oppfylt
import no.nav.helse.person.VurderingAvLøpendeVilkår
import no.nav.helse.person.VurderingerAvLøpendeVilkår
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

internal class AvslagsdagerTest {
    private val vurdering1 = UUID.randomUUID()
    private val vurdering2 = UUID.randomUUID()
    private val oppholdskrav = Avslagsdag(listOf(Begrunnelse.Oppholdskrav), Avslagsdager.KILDE)
    private val saksbehandler = Avslagsdag(listOf(Begrunnelse.AvslåttMeldingTilNavDag), "Saksbehandler")

    @Test
    fun `avslag fra sp-avslagsdager utenfor beregningsperioden står urørt`() {
        val eksisterende = Avslagstidslinje(1.januar til 12.januar to oppholdskrav)
        val løsning = Avslagsdager(listOf(vurdertDag(vurdering2, 9.januar, IkkeOppfylt)))

        val resultat = løsning.forPeriode(8.januar til 31.januar, 1.januar til 12.januar, eksisterende, VurderingerAvLøpendeVilkår())

        assertEquals(Avslagstidslinje(1.januar til 7.januar to oppholdskrav, 9.januar til 9.januar to oppholdskrav), resultat.avslagstidslinje)
    }

    @Test
    fun `dager fra løsningen utenfor perioden tas ikke med`() {
        val løsning =
            Avslagsdager(
                listOf(vurdertDag(vurdering1, 2.januar, IkkeOppfylt), vurdertDag(vurdering1, 20.januar, IkkeOppfylt)),
            )

        val resultat = løsning.forPeriode(1.januar til 31.januar, 1.januar til 10.januar, Avslagstidslinje(), VurderingerAvLøpendeVilkår())

        assertEquals(Avslagstidslinje(2.januar, oppholdskrav), resultat.avslagstidslinje)
        assertEquals(VurderingerAvLøpendeVilkår(2.januar, listOf(vurdering(vurdering1, IkkeOppfylt))), resultat.vurderinger)
    }

    @Test
    fun `kaster før noe endres når en dag fra løsningen har avslag med annen kilde`() {
        val eksisterende = Avslagstidslinje(1.januar, saksbehandler)
        val løsning = Avslagsdager(listOf(vurdertDag(vurdering1, 1.januar, IkkeOppfylt)))

        assertThrows<IllegalStateException> {
            løsning.forPeriode(1.januar til 2.januar, 1.januar til 2.januar, eksisterende, VurderingerAvLøpendeVilkår())
        }
    }

    @Test
    fun `oppfylt vurdering fjerner avslag fra sp-avslagsdager og lar avslag fra saksbehandler stå`() {
        val eksisterende = Avslagstidslinje(1.januar til 1.januar to saksbehandler, 2.januar til 2.januar to oppholdskrav)
        val løsning = Avslagsdager(listOf(vurdertDag(vurdering1, 2.januar, Oppfylt)))

        val resultat = løsning.forPeriode(1.januar til 2.januar, 1.januar til 2.januar, eksisterende, VurderingerAvLøpendeVilkår())

        assertEquals(Avslagstidslinje(1.januar, saksbehandler), resultat.avslagstidslinje)
    }

    @Test
    fun `oppfylt vurdering av en dag med avslag fra saksbehandler er ingen konflikt`() {
        val eksisterende = Avslagstidslinje(1.januar, saksbehandler)
        val løsning = Avslagsdager(listOf(vurdertDag(vurdering1, 1.januar, Oppfylt)))

        val resultat = løsning.forPeriode(1.januar til 2.januar, 1.januar til 2.januar, eksisterende, VurderingerAvLøpendeVilkår())

        assertEquals(Avslagstidslinje(1.januar, saksbehandler), resultat.avslagstidslinje)
        assertEquals(VurderingerAvLøpendeVilkår(1.januar, listOf(vurdering(vurdering1, Oppfylt))), resultat.vurderinger)
    }

    @Test
    fun `vurderingene i beregningsperioden erstattes, og vurderingene utenfor står urørt`() {
        val eksisterende =
            VurderingerAvLøpendeVilkår(
                1.januar til 12.januar to listOf(vurdering(vurdering1, IkkeOppfylt)),
            )
        val løsning =
            Avslagsdager(
                listOf(vurdertDag(vurdering2, 9.januar, Oppfylt), vurdertDag(vurdering2, 10.januar, IkkeOppfylt)),
            )

        val resultat = løsning.forPeriode(8.januar til 31.januar, 1.januar til 12.januar, Avslagstidslinje(), eksisterende)

        assertEquals(
            VurderingerAvLøpendeVilkår(
                1.januar til 7.januar to listOf(vurdering(vurdering1, IkkeOppfylt)),
                9.januar til 9.januar to listOf(vurdering(vurdering2, Oppfylt)),
                10.januar til 10.januar to listOf(vurdering(vurdering2, IkkeOppfylt)),
            ),
            resultat.vurderinger,
        )
    }

    private fun vurdertDag(
        vurderingId: UUID,
        dato: LocalDate,
        utfall: UtfallAvLøpendeVilkår,
    ) = Avslagsdager.VurdertDag(dato, vurdering(vurderingId, utfall))

    private fun vurdering(
        vurderingId: UUID,
        utfall: UtfallAvLøpendeVilkår,
    ) = VurderingAvLøpendeVilkår(LøpendeVilkår.Oppholdskrav, vurderingId, utfall)
}
