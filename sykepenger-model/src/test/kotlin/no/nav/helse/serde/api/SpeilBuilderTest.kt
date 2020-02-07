package no.nav.helse.serde.api

import no.nav.helse.person.TilstandType
import no.nav.helse.serde.JsonBuilderTest.Companion.lagPerson
import org.junit.jupiter.api.Test

class SpeilBuilderTest {

    @Test
    internal fun `print person i AVVENTER_GODKJENNING-state som SPEIL-json`() {
        val person = lagPerson(TilstandType.AVVENTER_GODKJENNING)
        println(serializePersonForSpeil(person))
    }

    @Test
    internal fun `print person i TIL_UTBETALING-state som SPEIL-json`() {
        val person = lagPerson(TilstandType.TIL_UTBETALING)
        println(serializePersonForSpeil(person))
    }

}
