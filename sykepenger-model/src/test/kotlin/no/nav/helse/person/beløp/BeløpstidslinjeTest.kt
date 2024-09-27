package no.nav.helse.person.beløp

import java.util.UUID
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class BeløpstidslinjeTest {

    @Test
    fun `beløpstidlinje lager en tidslinje med beløp og kilde`() {
        val beløpstidslinje = beløpstidslinjeTull(
            Triple(1.januar til 10.januar, INNTEKT, Avsender.ARBEIDSGIVER),
            Triple(11.januar til 31.januar, INNTEKT/2, Avsender.SAKSBEHANDLER)
        )

        assertEquals(10, beløpstidslinje.count { it.kilde.avsender == Avsender.ARBEIDSGIVER })
        assertEquals(21, beløpstidslinje.count { it.kilde.avsender == Avsender.SAKSBEHANDLER })
    }

    @Test
    fun `Hvis man slår opp på en dag som ikke finnes, da skal man få en ukjent dag`() {
        val beløpstidslinje = beløpstidslinjeTull(Triple(januar, INNTEKT, Avsender.ARBEIDSGIVER))
        assertDoesNotThrow { beløpstidslinje[1.februar] }
        assertEquals(UkjentDag, beløpstidslinje[1.februar])
    }

    @Test
    fun `Hvis du absolutt vil ha en tom tidslinje, så skal du få det`() {
        val beløpstidslinje = Beløpstidslinje(emptyList())
        assertEquals(0, beløpstidslinje.count())
    }

    @Test
    fun `Man skal ikke kunne opprette en ny tidslinje med overlappende dager`() {
        assertThrows<IllegalArgumentException> {
            beløpstidslinjeTull(
                Triple(januar, INNTEKT, Avsender.ARBEIDSGIVER),
                Triple(januar, INNTEKT, Avsender.ARBEIDSGIVER)
            )
        }
    }

    @Test
    fun `Du haver to stykk beløpstidslinje, som du ønsker forent`()  {
        val arbeidsgiverkilde = Kilde(UUID.randomUUID(), Avsender.ARBEIDSGIVER)
        val saksbehandlerkilde = Kilde(UUID.randomUUID(), Avsender.SAKSBEHANDLER)
        val gammelTidslinje = beløpstidslinje(
            Triple(januar, INNTEKT, arbeidsgiverkilde),
            Triple(mars, INGEN, arbeidsgiverkilde)
        )
        val nyTidslinje = beløpstidslinje(Triple(20.januar til 10.mars, INNTEKT, saksbehandlerkilde))

        val forventetTidslinje = beløpstidslinje(
            Triple(1.januar til 19.januar, INNTEKT, arbeidsgiverkilde),
            Triple(20.januar til 10.mars, INNTEKT, saksbehandlerkilde),
            Triple(11.mars til 31.mars, INGEN, arbeidsgiverkilde)
        )
        assertEquals(forventetTidslinje, gammelTidslinje + nyTidslinje)
    }

    private fun beløpstidslinjeTull(vararg perioder: Triple<Periode, Inntekt, Avsender>) = Beløpstidslinje(
        perioder.flatMap { (periode, inntekt, avsender) -> periode.map { dato -> Beløpsdag(dato, inntekt, Kilde(UUID.randomUUID(), avsender)) } }
    )
    private fun beløpstidslinje(vararg perioder: Triple<Periode, Inntekt, Kilde>) = Beløpstidslinje(
        perioder.flatMap { (periode, inntekt, kilde) -> periode.map { dato -> Beløpsdag(dato, inntekt, kilde) } }
    )
}