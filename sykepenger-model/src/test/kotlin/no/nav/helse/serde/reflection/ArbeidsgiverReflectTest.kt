package no.nav.helse.serde.reflection

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsgiverReflectTest {
    private companion object {
        private const val FØDSELSNUMMER = "fnr"
        private const val AKTØR = "aktørId"
        private const val ORGNR = "orgnr"
    }

    @Test
    fun `mapper Arbeidsgiver til map`() {
        val map = ArbeidsgiverReflect(arbeidsgiver).toMap()
        assertEquals(4, map.size)
        assertEquals(ORGNR, map["organisasjonsnummer"])
        assertTrue(map["beregnetUtbetalingstidslinjer"] is List<*>)
    }

    internal val arbeidsgiver = Arbeidsgiver(Person(AKTØR, FØDSELSNUMMER), ORGNR)
}
