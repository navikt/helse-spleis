package no.nav.helse

import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.filter.BrukerutbetalingfilterTest.Companion.brukerutbetalingfilter
import no.nav.helse.person.filter.Featurefilter
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
import java.time.LocalDateTime
import java.util.*

internal class LageBrukerutbetalingToggleTest {

    private lateinit var aktivitetslogg: IAktivitetslogg
    private val ikkeTillatBrukerutbetalingfilter = object : Featurefilter {
        override fun filtrer(aktivitetslogg: IAktivitetslogg) = false
    }
    private val feilendeBrukerutbetalingfilter = object : Featurefilter {
        override fun filtrer(aktivitetslogg: IAktivitetslogg) = throw IllegalStateException("Skal ikke skje.")
    }

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `slår ikke ut dersom toggle er av`() {
        Toggle.LageBrukerutbetaling.disable {
            assertFalse(Toggle.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, lagUtbetaling(), false, ikkeTillatBrukerutbetalingfilter))
            assertTrue(Toggle.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, lagUtbetaling(), true, ikkeTillatBrukerutbetalingfilter))
        }
    }

    @Test
    fun `slår ikke ut ved full refusjon`() {
        Toggle.LageBrukerutbetaling.enable {
            val utbetaling = lagUtbetaling()
            assertFalse(Toggle.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, utbetaling, false, ikkeTillatBrukerutbetalingfilter))
            assertFalse(Toggle.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, utbetaling, true, ikkeTillatBrukerutbetalingfilter))
            assertFalse(utbetaling.harDelvisRefusjon())
        }
    }

    @Test
    fun `slår ikke ut ved null refusjon`() {
        Toggle.LageBrukerutbetaling.enable {
            val utbetaling = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 0)))
            assertFalse(Toggle.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, utbetaling, true, ikkeTillatBrukerutbetalingfilter))
            assertFalse(utbetaling.harDelvisRefusjon())
        }
    }

    @Test
    fun `slår ikke ut ved null refusjon og passerer filteret`() {
        Toggle.BrukerutbetalingFilter.enable {
            val utbetaling = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 0)))
            val filter = brukerutbetalingfilter(utbetaling = utbetaling)
            assertFalse(Toggle.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, utbetaling, true, filter))
            assertFalse(utbetaling.harDelvisRefusjon())
            assertTrue(aktivitetslogg.hentInfo().contains("Plukket ut for brukerutbetaling")) { "$aktivitetslogg" }
        }
    }

    @Test
    fun `slår ut ved null refusjon og passerer ikke filteret`() {
        Toggle.BrukerutbetalingFilter.enable {
            val utbetaling = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 0)))
            val filter = brukerutbetalingfilter(utbetaling = utbetaling, fnr = "30108512345")
            assertTrue(Toggle.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, utbetaling, true, filter))
            assertFalse(utbetaling.harDelvisRefusjon())
            assertTrue(aktivitetslogg.hentInfo().contains("Ikke kandidat til brukerutbetaling fordi: Fødselsdag passer ikke")) { "$aktivitetslogg" }
        }
    }

    @Test
    fun `slår alltid ut ved delvis refusjon`() {
        Toggle.LageBrukerutbetaling.enable {
            val utbetaling = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 600)))
            assertTrue(Toggle.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, utbetaling, false, feilendeBrukerutbetalingfilter))
            assertTrue(utbetaling.harDelvisRefusjon())
        }

        Toggle.BrukerutbetalingFilter.enable {
            val utbetaling = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV.copyWith(arbeidsgiverbeløp = 600)))
            assertTrue(Toggle.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, utbetaling, false, feilendeBrukerutbetalingfilter))
            assertTrue(utbetaling.harDelvisRefusjon())
        }
    }


    @Test
    fun `slår ikke ut ved overgang i refusjon`() {
        Toggle.LageBrukerutbetaling.enable {
            val første = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV))
            val andre = lagUtbetaling(tidslinjeOf(16.AP, 15.NAV, 15.NAV(dekningsgrunnlag = 1200, refusjonsbeløp = 0)), forrige = første)
            assertFalse(Toggle.LageBrukerutbetaling.kanIkkeFortsette(aktivitetslogg, andre, true, ikkeTillatBrukerutbetalingfilter))
            assertFalse(andre.harDelvisRefusjon())
        }
    }

    private fun lagUtbetaling(tidslinje: Utbetalingstidslinje = tidslinjeOf(16.AP, 15.NAV), forrige: Utbetaling? = null): Utbetaling {
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            tidslinje.first().dato
        ).betal()

        return Utbetaling.lagUtbetaling(
            forrige?.let { listOf(forrige) } ?: emptyList(),
            "fnr",
            UUID.randomUUID(),
            "orgnr",
            tidslinje,
            tidslinje.last().dato,
            aktivitetslogg,
            LocalDate.MAX,
            0,
            0,
            forrige
        ).also { utbetaling ->
            godkjenn(utbetaling)
        }
    }

    private fun godkjenn(utbetaling: Utbetaling) =
        Utbetalingsgodkjenning(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "ignore",
            fødselsnummer = "ignore",
            organisasjonsnummer = "ignore",
            utbetalingId = utbetaling.inspektør.utbetalingId,
            vedtaksperiodeId = "ignore",
            saksbehandler = "Z999999",
            saksbehandlerEpost = "mille.mellomleder@nav.no",
            utbetalingGodkjent = true,
            godkjenttidspunkt = LocalDateTime.now(),
            automatiskBehandling = false,
        ).also {
            utbetaling.håndter(it)
        }
}
