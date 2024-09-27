package no.nav.helse.person.beløp

import java.util.UUID
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class BeløpstidslinjeTest {

    @Test
    fun `beløpstidlinje lager en tidslinje med beløp og kilde`() {
        val beløpstidslinje = beløpstidslinje(
            Triple(1.januar til 10.januar, INNTEKT, Avsender.ARBEIDSGIVER),
            Triple(11.januar til 31.januar, INNTEKT/2, Avsender.SAKSBEHANDLER)
        )

        assertEquals(10, beløpstidslinje.count { it.kilde.avsender == Avsender.ARBEIDSGIVER })
        assertEquals(21, beløpstidslinje.count { it.kilde.avsender == Avsender.SAKSBEHANDLER })
    }

    @Test
    fun `Hvis man slår opp på en dag som ikke finnes, da skal man få en ukjent dag`() {
        val beløpstidslinje = beløpstidslinje(Triple(januar, INNTEKT, Avsender.ARBEIDSGIVER))
        assertDoesNotThrow { beløpstidslinje[1.februar] }
        assertEquals(UkjentDag, beløpstidslinje[1.februar])
    }

    @Test
    fun `Hvis du absolutt vil ha en tom tidslinje, så skal du få det`() {
        val beløpstidslinje = beløpstidslinje()
        assertEquals(0, beløpstidslinje.count())
    }

    @Test
    fun `Man skal ikke kunne opprette en ny tidslinje med overlappende dager`() {
        assertThrows<IllegalArgumentException> {
            beløpstidslinje(
                Triple(januar, INNTEKT, Avsender.ARBEIDSGIVER),
                Triple(januar, INNTEKT, Avsender.ARBEIDSGIVER)
            )
        }
    }

    private fun beløpstidslinje(vararg perioder: Triple<Periode, Inntekt, Avsender>) = Beløpstidslinje(
        perioder.flatMap { (periode, inntekt, avsender) -> periode.map { dato -> Beløpsdag(dato, inntekt, Kilde(UUID.randomUUID(), avsender)) } }
    )
}