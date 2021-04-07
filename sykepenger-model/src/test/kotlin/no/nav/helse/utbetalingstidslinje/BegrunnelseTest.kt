package no.nav.helse.utbetalingstidslinje

import no.nav.helse.testhelpers.januar
import no.nav.helse.utbetalingstidslinje.Begrunnelse.Companion.avvis
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class BegrunnelseTest {
    private val økonomi = Økonomi.ikkeBetalt()

    @Test
    fun `dødsdato avviser ukedager og helger`() {
        assertNotNull(Begrunnelse.EtterDødsdato.avvis(NavDag(1.januar, økonomi)))
        assertNotNull(Begrunnelse.EtterDødsdato.avvis(NavHelgDag(1.januar, økonomi)))
    }

    @Test
    fun `avviser med flere begrunnelser`() {
        val dag = NavHelgDag(1.januar, økonomi)
        assertNull(listOf(Begrunnelse.MinimumSykdomsgrad).avvis(dag))
        assertNotNull(listOf(Begrunnelse.MinimumSykdomsgrad, Begrunnelse.EtterDødsdato).avvis(dag))
    }
}
