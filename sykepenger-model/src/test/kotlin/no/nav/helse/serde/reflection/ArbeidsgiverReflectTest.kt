package no.nav.helse.serde.reflection

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ArbeidsgiverReflectTest {
    @Test
    internal fun `kontroller at alle felter er gjort rede for`() {
        assertMembers<Arbeidsgiver, ArbeidsgiverReflect>(
            skalMappes = listOf("organisasjonsnummer", "id"),
            skalIkkeMappes = listOf(
                "inntekthistorikk",
                "perioder",
                "tidslinjer",
                "person",
                "aktivitetslogger"
            )
        )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    internal fun `mapper Arbeidsgiver til map`() {
        val map = ArbeidsgiverReflect(arbeidsgiver).toMap()

        assertEquals(2 , map.size)
        assertEquals(orgnummer, map["organisasjonsnummer"])
    }

    internal val arbeidsgiver = Arbeidsgiver(Person("1", "2"), orgnummer)
}
