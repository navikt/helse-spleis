package no.nav.helse.utbetalingstidslinje

import no.nav.helse.januar
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class BegrunnelseDtoTest {
    private val økonomi = Økonomi.Companion.ikkeBetalt()

    @Test
    fun `dødsdato avviser ukedager`() {
        Assertions.assertTrue(Begrunnelse.EtterDødsdato.skalAvvises(Utbetalingsdag.NavDag(1.januar, økonomi)))
        Assertions.assertFalse(Begrunnelse.EtterDødsdato.skalAvvises(Utbetalingsdag.NavHelgDag(1.januar, økonomi)))
    }

    @Test
    fun `MinimumSykdomsgrad avviser ikke helg`() {
        Assertions.assertTrue(Begrunnelse.MinimumSykdomsgrad.skalAvvises(Utbetalingsdag.NavDag(1.januar, økonomi)))
        Assertions.assertTrue(
            Begrunnelse.MinimumSykdomsgrad.skalAvvises(
                Utbetalingsdag.ArbeidsgiverperiodedagNav(
                    1.januar,
                    økonomi
                )
            )
        )
        Assertions.assertFalse(Begrunnelse.MinimumSykdomsgrad.skalAvvises(Utbetalingsdag.NavHelgDag(1.januar, økonomi)))
    }

    @Test
    fun `over70 avviser ikke helg`() {
        Assertions.assertTrue(Begrunnelse.Over70.skalAvvises(Utbetalingsdag.NavDag(1.januar, økonomi)))
        Assertions.assertFalse(Begrunnelse.Over70.skalAvvises(Utbetalingsdag.NavHelgDag(1.januar, økonomi)))
    }
}
