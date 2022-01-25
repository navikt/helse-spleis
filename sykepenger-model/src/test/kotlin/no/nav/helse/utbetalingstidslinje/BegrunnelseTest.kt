package no.nav.helse.utbetalingstidslinje

import no.nav.helse.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BegrunnelseTest {
    private val økonomi = Økonomi.ikkeBetalt()

    @Test
    fun `dødsdato avviser ukedager`() {
        assertTrue(Begrunnelse.EtterDødsdato.skalAvvises(NavDag(1.januar, økonomi)))
        assertFalse(Begrunnelse.EtterDødsdato.skalAvvises(NavHelgDag(1.januar, økonomi)))
    }

    @Test
    fun `MinimumSykdomsgrad avviser ikke helg`() {
        assertTrue(Begrunnelse.MinimumSykdomsgrad.skalAvvises(NavDag(1.januar, økonomi)))
        assertFalse(Begrunnelse.MinimumSykdomsgrad.skalAvvises(NavHelgDag(1.januar, økonomi)))
    }

    @Test
    fun `over70 avviser ikke helg`() {
        assertTrue(Begrunnelse.Over70.skalAvvises(NavDag(1.januar, økonomi)))
        assertFalse(Begrunnelse.Over70.skalAvvises(NavHelgDag(1.januar, økonomi)))
    }

    @Test
    fun `avviser med flere begrunnelser`() {
        val dag = NavDag(1.januar, økonomi)
        assertEquals(2, dag.avvis(listOf(Begrunnelse.MinimumSykdomsgrad, Begrunnelse.MinimumInntekt))?.begrunnelser?.size ?: 0)
    }
}
