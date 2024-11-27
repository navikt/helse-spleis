package no.nav.helse.hendelser

import no.nav.helse.februar
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.Arbeidstaker
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.ArbeidstakerSelvstendig
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.Inaktiv
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode.Tom
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingshistorikkForFeriepengerTest {
    companion object {
        private const val ORGNUMMER = "987654321"
    }

    @Test
    fun `Arbeidstakere med kode 01 har rett på feriepenger`() {
        val arbeidskategorikoder =
            UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
                listOf(KodePeriode(januar, Arbeidstaker)),
            )

        assertTrue(arbeidskategorikoder.harRettPåFeriepenger(1.januar, ORGNUMMER))
    }

    @Test
    fun `Arbeidstakere med kode 07 har ikke rett på feriepenger`() {
        val arbeidskategorikoder =
            UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
                listOf(KodePeriode(januar, Inaktiv)),
            )

        assertFalse(arbeidskategorikoder.harRettPåFeriepenger(1.januar, ORGNUMMER))
    }

    @Test
    fun `Arbeidstakere med kode 07 først og 01 etter har kun rett på feriepenger for periode med 01`() {
        val arbeidskategorikoder =
            UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
                listOf(
                    KodePeriode(januar, Inaktiv),
                    KodePeriode(februar, Arbeidstaker),
                ),
            )

        assertFalse(arbeidskategorikoder.harRettPåFeriepenger(31.januar, ORGNUMMER))
        assertTrue(arbeidskategorikoder.harRettPåFeriepenger(1.februar, ORGNUMMER))
    }

    @Test
    fun `Arbeidstakere med kode 'blank' først og 01 etter har kun rett på feriepenger for periode med 01`() {
        val arbeidskategorikoder =
            UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
                listOf(
                    KodePeriode(januar, Tom),
                    KodePeriode(februar, Arbeidstaker),
                ),
            )

        assertFalse(arbeidskategorikoder.harRettPåFeriepenger(31.januar, ORGNUMMER))
        assertTrue(arbeidskategorikoder.harRettPåFeriepenger(1.februar, ORGNUMMER))
    }

    @Test
    fun `Personer med kode 03 og orgnummer 0 har ikke rett på feriepenger`() {
        val arbeidskategorikoder =
            UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(
                listOf(
                    KodePeriode(februar, ArbeidstakerSelvstendig),
                ),
            )

        assertFalse(arbeidskategorikoder.harRettPåFeriepenger(1.februar, "0"))
    }
}
