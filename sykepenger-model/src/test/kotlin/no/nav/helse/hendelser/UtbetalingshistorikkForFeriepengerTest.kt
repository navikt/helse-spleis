package no.nav.helse.hendelser

import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode.Companion.mapTilNoeFornuftig
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingshistorikkForFeriepengerTest {
    @Test
    fun `Arbeidstakere med kode 01 har rett på feriepenger`() {
        val arbeidskategorikoder = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
            listOf("01" to 31.januar).mapTilNoeFornuftig()
        )

        assertTrue(arbeidskategorikoder.harRettPåFeriepenger(1.januar))
    }

    @Test
    fun `Arbeidstakere med kode 07 har ikke rett på feriepenger`() {
        val arbeidskategorikoder = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
            listOf("07" to 31.januar).mapTilNoeFornuftig()
        )

        assertFalse(arbeidskategorikoder.harRettPåFeriepenger(1.januar))
    }

    @Test
    fun `Arbeidstakere med kode 07 først og 01 etter har kun rett på feriepenger for periode med 01`() {
        val arbeidskategorikoder = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
            listOf(
                "07" to 31.januar,
                "01" to 28.februar
            ).mapTilNoeFornuftig()
        )

        assertFalse(arbeidskategorikoder.harRettPåFeriepenger(31.januar))
        assertTrue(arbeidskategorikoder.harRettPåFeriepenger(1.februar))
    }

    @Test
    fun `Arbeidstakere med kode 'blank' først og 01 etter har kun rett på feriepenger for periode med 01`() {
        val arbeidskategorikoder = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
            listOf(
                "  " to 31.januar,
                "01" to 28.februar
            ).mapTilNoeFornuftig()
        )

        assertFalse(arbeidskategorikoder.harRettPåFeriepenger(31.januar))
        assertTrue(arbeidskategorikoder.harRettPåFeriepenger(1.februar))
    }
}
