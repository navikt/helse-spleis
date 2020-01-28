package no.nav.helse.serde.reflection

import no.nav.helse.fixtures.januar
import no.nav.helse.fixtures.juli
import no.nav.helse.hendelser.ModelForeldrepenger
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ForeldrepengerReflectTest {
    @Test
    internal fun `kontroller at alle felter er gjort rede for`() {
        assertMembers<ModelForeldrepenger, ForeldrepengerReflect>(
            listOf("foreldrepengeytelse", "svangerskapsytelse"),
            listOf("aktivitetslogger")
        )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    internal fun `mapper ModelForeldrepenger til map`() {
        val map = ForeldrepengerReflect(foreldrepenger).toMap()

        assertEquals(2, map.size)
        assertEquals(1.januar.minusYears(2), (map["foreldrepengeytelse"] as Map<String, LocalDate>)["fom"])
        assertEquals(31.januar.minusYears(2), (map["foreldrepengeytelse"] as Map<String, LocalDate>)["tom"])
        assertEquals(1.juli.minusYears(2), (map["svangerskapsytelse"] as Map<String, LocalDate>)["fom"])
        assertEquals(31.juli.minusYears(2), (map["svangerskapsytelse"] as Map<String, LocalDate>)["tom"])
    }

    internal val foreldrepenger = ModelForeldrepenger(
        foreldrepengeytelse = Periode(
            fom = 1.januar.minusYears(2),
            tom = 31.januar.minusYears(2)
        ),
        svangerskapsytelse = Periode(
            fom = 1.juli.minusYears(2),
            tom = 31.juli.minusYears(2)
        ),
        aktivitetslogger = Aktivitetslogger()
    )
}
