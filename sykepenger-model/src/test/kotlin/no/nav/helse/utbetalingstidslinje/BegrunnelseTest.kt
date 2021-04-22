package no.nav.helse.utbetalingstidslinje

import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BegrunnelseTest {
    private val økonomi = Økonomi.ikkeBetalt()

    @Test
    fun `dødsdato avviser ukedager og helger`() {
        assertTrue(Begrunnelse.EtterDødsdato.avvis(NavDag(1.januar, økonomi)))
        assertTrue(Begrunnelse.EtterDødsdato.avvis(NavHelgDag(1.januar, økonomi)))
    }

    @Test
    fun `MinimumSykdomsgrad avviser ikke helg`() {
        assertTrue(Begrunnelse.MinimumSykdomsgrad.avvis(NavDag(1.januar, økonomi)))
        assertFalse(Begrunnelse.MinimumSykdomsgrad.avvis(NavHelgDag(1.januar, økonomi)))
    }

    @Test
    fun `avviser med flere begrunnelser`() {
        val dag = NavHelgDag(1.januar, økonomi)
        assertNull(dag.avvis(listOf(Begrunnelse.MinimumSykdomsgrad)))
        assertEquals(1, dag.avvis(listOf(Begrunnelse.MinimumSykdomsgrad, Begrunnelse.EtterDødsdato))?.begrunnelser?.size)
    }
}
