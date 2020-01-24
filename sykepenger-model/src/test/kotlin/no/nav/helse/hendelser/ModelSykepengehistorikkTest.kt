package no.nav.helse.hendelser

import no.nav.helse.hendelser.ModelSykepengehistorikk.Periode.*
import no.nav.helse.juni
import no.nav.helse.person.Aktivitetslogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ModelSykepengehistorikkTest {

    @Test
    fun `utbetalingslinjer med refusjon til arbeidsgiver`() {
        val fom = 1.juni
        val tom = 10.juni
        val dagsats = 1200
        val sykepengehistorikk = sykepengehistorikk(RefusjonTilArbeidsgiver(fom, tom, dagsats))
        assertFalse(sykepengehistorikk.valider().hasErrors())
        val utbetalingslinjer = sykepengehistorikk.utbetalingslinjer()
        assertEquals(1, utbetalingslinjer.size)
        assertEquals(dagsats, utbetalingslinjer[0].dagsats)
        assertEquals(fom, utbetalingslinjer[0].fom)
        assertEquals(tom, utbetalingslinjer[0].tom)
        assertEquals(tom, sykepengehistorikk.sisteFraværsdag())
    }

    @Test
    fun `utbetalingslinjer med andre typer`() {
        assertInvalid(ReduksjonMedlem(1.juni, 10.juni, 1200))
        assertInvalid(Etterbetaling(1.juni, 10.juni, 1200))
        assertInvalid(KontertRegnskap(1.juni, 10.juni, 1200))
        assertInvalid(ReduksjonArbRef(1.juni, 10.juni, 1200))
        assertInvalid(Tilbakeført(1.juni, 10.juni, 1200))
        assertInvalid(Konvertert(1.juni, 10.juni, 1200))
        assertInvalid(Ferie(1.juni, 10.juni, 1200))
        assertInvalid(Opphold(1.juni, 10.juni, 1200))
        assertInvalid(Sanksjon(1.juni, 10.juni, 1200))
        assertInvalid(Ukjent(1.juni, 10.juni, 1200))

    }

    private fun assertInvalid(periode: ModelSykepengehistorikk.Periode) {
        val sykepengehistorikk = sykepengehistorikk(periode)
        assertTrue(sykepengehistorikk.valider().hasErrors())
        assertThrows<Aktivitetslogger.AktivitetException> { sykepengehistorikk.utbetalingslinjer() }
    }

    private fun sykepengehistorikk(periode: ModelSykepengehistorikk.Periode): ModelSykepengehistorikk {
        return ModelSykepengehistorikk(
            utbetalinger = listOf(periode),
            inntektshistorikk = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        )
    }

}
