package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OldtidsutbetalingerFlereArbeidsgivereTest {
    private companion object {
        private const val ORGNUMMER1 = "12345678"
        private const val ORGNUMMER2 = "87654321"
        private const val ORGNUMMER3 = "13572468"
        private val PERSON = Person("aktørId", "fnr")
        private val ARBEIDSGIVER1 = Arbeidsgiver(PERSON, ORGNUMMER1)
        private val ARBEIDSGIVER2 = Arbeidsgiver(PERSON, ORGNUMMER2)
    }

    @Test
    fun `Historisk tidlinje for person`() {
        val oldtid = Oldtidsutbetalinger()

        oldtid.add(ORGNUMMER1, tidslinjeOf(5.UTELATE, 19.FRI))
        oldtid.add(ORGNUMMER3, tidslinjeOf(7.UTELATE, 13.NAV))
        oldtid.add(ORGNUMMER2, tidslinjeOf(3.NAV))
        oldtid.add(ORGNUMMER2, tidslinjeOf(13.UTELATE, 2.NAV))

        UtbetalingstidslinjeInspektør(oldtid.personTidslinje(Periode(7.januar, 18.januar))).also { inspektør ->
            assertEquals(18, inspektør.size)
            assertEquals(14, inspektør.navDagTeller)
            assertEquals(2, inspektør.fridagTeller)
            assertEquals(2, inspektør.ukjentDagTeller)
        }
    }
}
