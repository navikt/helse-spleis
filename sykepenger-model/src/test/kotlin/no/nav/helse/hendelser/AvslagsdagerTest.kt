package no.nav.helse.hendelser

import no.nav.helse.januar
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.Avslagstidslinje.Avslagsdag
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class AvslagsdagerTest {
    private val vurdering1 = UUID.randomUUID()
    private val vurdering2 = UUID.randomUUID()
    private val oppholdskrav = Avslagsdag(listOf(Begrunnelse.Oppholdskrav), Avslagsdager.KILDE)
    private val saksbehandler = Avslagsdag(listOf(Begrunnelse.AvslåttMeldingTilNavDag), "Saksbehandler")

    @Test
    fun `avslag fra sp-avslagsdager utenfor beregningsperioden står urørt`() {
        val eksisterende = Avslagstidslinje(1.januar til 12.januar to oppholdskrav)
        val løsning = Avslagsdager(setOf(vurdering2), listOf(Avslagsdager.Dag(vurdering2, 9.januar, Begrunnelse.Oppholdskrav)))

        val resultat = løsning.forPeriode(8.januar til 31.januar, 1.januar til 12.januar, eksisterende)

        assertEquals(Avslagstidslinje(1.januar til 7.januar to oppholdskrav, 9.januar til 9.januar to oppholdskrav), resultat.avslagstidslinje)
        assertEquals(setOf(vurdering2), resultat.vurderingIder)
    }

    @Test
    fun `dager fra løsningen utenfor perioden tas ikke med`() {
        val løsning =
            Avslagsdager(
                setOf(vurdering1),
                listOf(
                    Avslagsdager.Dag(vurdering1, 2.januar, Begrunnelse.Oppholdskrav),
                    Avslagsdager.Dag(vurdering1, 20.januar, Begrunnelse.Oppholdskrav),
                ),
            )

        val resultat = løsning.forPeriode(1.januar til 31.januar, 1.januar til 10.januar, Avslagstidslinje())

        assertEquals(Avslagstidslinje(2.januar, oppholdskrav), resultat.avslagstidslinje)
    }

    @Test
    fun `kaster før noe endres når en dag fra løsningen har avslag med annen kilde`() {
        val eksisterende = Avslagstidslinje(1.januar, saksbehandler)
        val løsning = Avslagsdager(setOf(vurdering1), listOf(Avslagsdager.Dag(vurdering1, 1.januar, Begrunnelse.Oppholdskrav)))

        assertThrows<IllegalStateException> { løsning.forPeriode(1.januar til 2.januar, 1.januar til 2.januar, eksisterende) }
    }

    @Test
    fun `oppfylt vurdering fjerner avslag fra sp-avslagsdager og lar avslag fra saksbehandler stå`() {
        val eksisterende = Avslagstidslinje(1.januar til 1.januar to saksbehandler, 2.januar til 2.januar to oppholdskrav)

        val resultat = Avslagsdager(setOf(vurdering1), emptyList()).forPeriode(1.januar til 2.januar, 1.januar til 2.januar, eksisterende)

        assertEquals(Avslagstidslinje(1.januar, saksbehandler), resultat.avslagstidslinje)
        assertEquals(setOf(vurdering1), resultat.vurderingIder)
    }
}
