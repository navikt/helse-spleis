//package no.nav.helse.hendelser
//
//import no.nav.helse.hendelser.Utbetalingshistorikk.Periode.*
//import no.nav.helse.juni
//import no.nav.helse.person.Aktivitetslogg
//import no.nav.helse.person.Aktivitetslogger
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.assertThrows
//
//internal class UtbetalingshistorikkTest {
//
//    @Test
//    fun `utbetalingslinjer med refusjon til arbeidsgiver`() {
//        val fom = 1.juni
//        val tom = 10.juni
//        val dagsats = 1200
//        val sykepengehistorikk = sykepengehistorikk(RefusjonTilArbeidsgiver(fom, tom, dagsats))
//        assertFalse(sykepengehistorikk.valider().hasErrorsOld())
//        val utbetalingstidslinje = sykepengehistorikk.utbetalingstidslinje()
//        assertEquals(1, utbetalingstidslinje.size)
//        assertEquals(dagsats, utbetalingstidslinje[0].dagsats)
//        assertEquals(fom, utbetalingstidslinje[0].fom)
//        assertEquals(tom, utbetalingstidslinje[0].tom)
//        assertEquals(tom, sykepengehistorikk.sisteFraværsdag())
//    }
//
//    @Test
//    fun `utbetalingslinjer med andre typer`() {
//        assertInvalid(ReduksjonMedlem(1.juni, 10.juni, 1200))
//        assertInvalid(Etterbetaling(1.juni, 10.juni, 1200))
//        assertInvalid(KontertRegnskap(1.juni, 10.juni, 1200))
//        assertInvalid(ReduksjonArbeidsgiverRefusjon(1.juni, 10.juni, 1200))
//        assertInvalid(Tilbakeført(1.juni, 10.juni, 1200))
//        assertInvalid(Konvertert(1.juni, 10.juni, 1200))
//        assertInvalid(Ferie(1.juni, 10.juni, 1200))
//        assertInvalid(Opphold(1.juni, 10.juni, 1200))
//        assertInvalid(Sanksjon(1.juni, 10.juni, 1200))
//    }
//
//    private fun assertInvalid(periode: Utbetalingshistorikk.Periode) {
//        val sykepengehistorikk = sykepengehistorikk(periode)
//        assertTrue(sykepengehistorikk.valider().hasErrorsOld())
//        assertThrows<Aktivitetslogger.AktivitetException> { sykepengehistorikk.utbetalingslinjer() }
//    }
//
//    private fun sykepengehistorikk(periode: Utbetalingshistorikk.Periode): Utbetalingshistorikk {
//        return Utbetalingshistorikk(
//            utbetalinger = listOf(periode),
//            ukjentePerioder = emptyList(),
//            inntektshistorikk = emptyList(),
//            aktivitetslogger = Aktivitetslogger(),
//            aktivitetslogg = Aktivitetslogg()
//        )
//    }
//
//}
