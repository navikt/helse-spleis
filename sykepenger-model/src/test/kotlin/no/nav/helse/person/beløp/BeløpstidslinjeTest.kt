package no.nav.helse.person.beløp

import java.time.LocalDate
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
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class BeløpstidslinjeTest {

    @Test
    fun `beløpstidlinje lager en tidslinje med beløp og kilde`() {
        val beløpstidslinje = beløpstidslinjeMedAvsender(
            Triple(1.januar til 10.januar, INNTEKT, Avsender.ARBEIDSGIVER),
            Triple(11.januar til 31.januar, INNTEKT/2, Avsender.SAKSBEHANDLER)
        )

        assertEquals(10, beløpstidslinje.count { it.kilde.avsender == Avsender.ARBEIDSGIVER })
        assertEquals(21, beløpstidslinje.count { it.kilde.avsender == Avsender.SAKSBEHANDLER })
    }

    @Test
    fun `Hvis man slår opp på en dag som ikke finnes, da skal man få en ukjent dag`() {
        val beløpstidslinje = beløpstidslinjeMedAvsender(Triple(januar, INNTEKT, Avsender.ARBEIDSGIVER))
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
            beløpstidslinjeMedAvsender(
                Triple(januar, INNTEKT, Avsender.ARBEIDSGIVER),
                Triple(januar, INNTEKT, Avsender.ARBEIDSGIVER)
            )
        }
    }

    @Test
    fun `Du haver to stykk beløpstidslinje, som du ønsker forent`()  {
        val arbeidsgiverkilde = Kilde(UUID.randomUUID(), Avsender.ARBEIDSGIVER)
        val saksbehandlerkilde = Kilde(UUID.randomUUID(), Avsender.SAKSBEHANDLER)
        val gammelTidslinje = beløpstidslinjeMedKilde(
            Triple(januar, INNTEKT, arbeidsgiverkilde),
            Triple(mars, INGEN, arbeidsgiverkilde)
        )
        val nyTidslinje = beløpstidslinjeMedKilde(Triple(20.januar til 10.mars, INNTEKT, saksbehandlerkilde))

        val forventetTidslinje = beløpstidslinjeMedKilde(
            Triple(1.januar til 19.januar, INNTEKT, arbeidsgiverkilde),
            Triple(20.januar til 10.mars, INNTEKT, saksbehandlerkilde),
            Triple(11.mars til 31.mars, INGEN, arbeidsgiverkilde)
        )
        assertEquals(forventetTidslinje, gammelTidslinje + nyTidslinje)
    }


    @Test
    fun `Trekke fra dager på en beløpstidslinje`() {
        val tidslinje = (1.daglig fra 1.januar til 2.januar) + (2.daglig fra 4.januar til 5.januar)
        assertEquals(1.daglig, tidslinje[1.januar].beløp)
        assertEquals(UkjentDag, tidslinje[3.januar])
        assertEquals(2.daglig, tidslinje[4.januar].beløp)

        val forventet = (1.daglig kun 2.januar) + (2.daglig kun 5.januar)

        val fratrukket = tidslinje - 4.januar - 1.januar

        assertEquals(forventet, fratrukket)

        assertEquals(UkjentDag, fratrukket[1.januar])
        assertEquals(UkjentDag, fratrukket[4.januar])
    }

    private companion object {
        private fun beløpstidslinjeMedAvsender(vararg perioder: Triple<Periode, Inntekt, Avsender>) = Beløpstidslinje(
            perioder.flatMap { (periode, inntekt, avsender) -> periode.map { dato -> Beløpsdag(dato, inntekt, Kilde(UUID.randomUUID(), avsender)) } }
        )
        private fun beløpstidslinjeMedKilde(vararg perioder: Triple<Periode, Inntekt, Kilde>) = Beløpstidslinje(
            perioder.flatMap { (periode, inntekt, kilde) -> periode.map { dato -> Beløpsdag(dato, inntekt, kilde) } }
        )

        private val EnHardkodenKilde = Kilde(UUID.fromString("00000000-0000-0000-0000-000000000000"), Avsender.SYSTEM)
        infix fun Inntekt.fra(fra: LocalDate) = this to fra
        infix fun Inntekt.kun(fra: LocalDate) = this fra fra til fra
        infix fun Pair<Inntekt, LocalDate>.til(til: LocalDate) = Beløpstidslinje((second til til).map { Beløpsdag(it, first, EnHardkodenKilde) })
    }
}