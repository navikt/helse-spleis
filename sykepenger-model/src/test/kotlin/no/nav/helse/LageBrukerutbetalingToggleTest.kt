package no.nav.helse

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class LageBrukerutbetalingToggleTest {

    private lateinit var aktivitetslogg: IAktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `slår ikke ut dersom toggle er av`() {
        Toggles.LageBrukerutbetaling.disable {
            assertFalse(Toggles.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, null))
            assertFalse(Toggles.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, lagUtbetaling()))
        }
    }

    @Test
    fun `slår ikke ut ved full refusjon`() {
        Toggles.LageBrukerutbetaling.enable {
            val utbetaling = lagUtbetaling()
            assertFalse(Toggles.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, utbetaling))
            assertFalse(utbetaling.harDelvisRefusjon())
        }
    }

    @Test
    fun `slår ikke ut ved null refusjon`() {
        Toggles.LageBrukerutbetaling.enable {
            val utbetaling = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 0)))
            assertFalse(Toggles.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, utbetaling))
            assertFalse(utbetaling.harDelvisRefusjon())
        }
    }

    @Test
    fun `slår ut ved delvis refusjon`() {
        Toggles.LageBrukerutbetaling.enable {
            val utbetaling = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 600)))
            assertTrue(Toggles.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, utbetaling))
            assertTrue(utbetaling.harDelvisRefusjon())
        }
    }

    private fun lagUtbetaling(tidslinje: Utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV)): Utbetaling {
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            tidslinje.first().dato
        ).betal()

        return Utbetaling.lagUtbetaling(
            emptyList(),
            "fnr",
            UUID.randomUUID(),
            "orgnr",
            tidslinje,
            tidslinje.last().dato,
            aktivitetslogg,
            LocalDate.MAX,
            0,
            0
        )
    }
}
