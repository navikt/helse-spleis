package no.nav.helse.person

import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode
import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.*
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class HistorieTest {

    private companion object {
        private const val FNR = "12345678910"
        private const val AKTØRID = "1234567891011"
        private const val AG1 = "1234"
        private const val AG2 = "2345"
    }

    @Test
    fun `sykedag på fredag og feriedag på fredag`() {
        val historie = historie(
            RefusjonTilArbeidsgiver(2.januar, 12.januar, 1000, 100, AG1),
            Ferie(15.januar, 19.januar),
            Utbetaling(22.januar, 31.januar, 1000, 100, FNR),
        )

        assertEquals(null, historie.skjæringstidspunkt(1.januar))
        assertEquals(2.januar, historie.skjæringstidspunkt(21.januar))
        assertEquals(2.januar, historie.skjæringstidspunkt(31.januar))
    }

    @Test
    fun `skjæringstidspunkt med flere arbeidsgivere`() {
        val historie = historie(
            RefusjonTilArbeidsgiver(2.januar, 12.januar, 1000, 100, AG1),
            Ferie(15.januar, 19.januar),
            RefusjonTilArbeidsgiver(22.januar, 31.januar, 1000, 100, AG2),
        )

        assertEquals(null, historie.skjæringstidspunkt(1.januar))
        assertEquals(2.januar, historie.skjæringstidspunkt(21.januar))
        assertEquals(2.januar, historie.skjæringstidspunkt(31.januar))
    }

    @Test
    fun `innledende ferie som avsluttes på fredag`() {
        val historie = historie(
            Ferie(1.januar, 5.januar),
            RefusjonTilArbeidsgiver(8.januar, 12.januar, 1000, 100, AG1)
        )

        assertEquals(8.januar, historie.skjæringstidspunkt(12.januar))
    }

    private fun historie(vararg perioder: Periode) = Historie(
        Utbetalingshistorikk(
            UUID.randomUUID(),
            AKTØRID,
            FNR,
            "ET ORGNR",
            UUID.randomUUID().toString(),
            perioder.toList(),
            emptyList()
        )
    )
}
