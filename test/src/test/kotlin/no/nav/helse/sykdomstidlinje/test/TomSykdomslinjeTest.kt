package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class TomSykdomslinjeTest {
    val tomTidslinje = Sykdomstidslinje.tomTidslinje()
    val sykdom = Sykdomstidslinje.sykedag(
        LocalDate.now(),
        Testhendelse()
    )

    @Test
    fun `pluss skal kunne håndtere tomme tidslinjer`() {
        val composittA = tomTidslinje + sykdom
        val composittB = sykdom + tomTidslinje

        assertEquals(sykdom.startdato(), composittA.startdato())
        assertEquals(sykdom.sluttdato(), composittA.sluttdato())
        assertEquals(sykdom.length(), composittA.length())

        assertEquals(sykdom.startdato(), composittB.startdato())
        assertEquals(sykdom.sluttdato(), composittB.sluttdato())
        assertEquals(sykdom.length(), composittB.length())
    }

    @Test
    fun `en tom tidslinje har ikke konsept om antall dager mellom`() {
        assertThrows<IllegalStateException> {
            tomTidslinje.antallDagerMellom(sykdom)
        }
        assertThrows<IllegalStateException> {
            sykdom.antallDagerMellom(tomTidslinje)
        }
        assertThrows<IllegalStateException> {
            tomTidslinje.antallDagerMellom(tomTidslinje)
        }
    }

    @Test
    fun `en tom tidslinje overlapper ikke med noe, inklusive seg selv`() {
        assertFalse(tomTidslinje.overlapperMed(tomTidslinje))
        assertFalse(tomTidslinje.overlapperMed(sykdom))
        assertFalse(sykdom.overlapperMed(tomTidslinje))
    }
}
