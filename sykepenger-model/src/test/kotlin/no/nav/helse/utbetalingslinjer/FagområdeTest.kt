package no.nav.helse.utbetalingslinjer

import no.nav.helse.Toggle
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FagområdeTest {

    @Test
    fun `setter riktig behovtype for arbeidsgiverutbetaling`() {
        Toggle.NyeBehovForUtbetaling.disable {
            val aktivitetslogg = Aktivitetslogg()
            Fagområde.SykepengerRefusjon.overfør(aktivitetslogg, emptyMap())
            assertEquals(1, aktivitetslogg.behov().size)
            assertEquals(Utbetaling, aktivitetslogg.behov().first().type)
        }
        Toggle.NyeBehovForUtbetaling.enable {
            val aktivitetslogg = Aktivitetslogg()
            Fagområde.SykepengerRefusjon.overfør(aktivitetslogg, emptyMap())
            assertEquals(1, aktivitetslogg.behov().size)
            assertEquals(UtbetalingArbeidsgiver, aktivitetslogg.behov().first().type)
        }
    }

    @Test
    fun `setter riktig behovtype for personutbetaling`() {
        Toggle.NyeBehovForUtbetaling.disable {
            val aktivitetslogg = Aktivitetslogg()
            Fagområde.Sykepenger.overfør(aktivitetslogg, emptyMap())
            assertEquals(1, aktivitetslogg.behov().size)
            assertEquals(Utbetaling, aktivitetslogg.behov().first().type)
        }
        Toggle.NyeBehovForUtbetaling.enable {
            val aktivitetslogg = Aktivitetslogg()
            Fagområde.Sykepenger.overfør(aktivitetslogg, emptyMap())
            assertEquals(1, aktivitetslogg.behov().size)
            assertEquals(UtbetalingPerson, aktivitetslogg.behov().first().type)
        }
    }

    @Test
    fun `setter riktig behovtype for arbeidsgiversimulering`() {
        Toggle.NyeBehovForUtbetaling.disable {
            val aktivitetslogg = Aktivitetslogg()
            Fagområde.SykepengerRefusjon.simuler(aktivitetslogg, emptyMap())
            assertEquals(1, aktivitetslogg.behov().size)
            assertEquals(Simulering, aktivitetslogg.behov().first().type)
        }
        Toggle.NyeBehovForUtbetaling.enable {
            val aktivitetslogg = Aktivitetslogg()
            Fagområde.SykepengerRefusjon.simuler(aktivitetslogg, emptyMap())
            assertEquals(1, aktivitetslogg.behov().size)
            assertEquals(SimuleringArbeidsgiver, aktivitetslogg.behov().first().type)
        }
    }

    @Test
    fun `setter riktig behovtype for personsimulering`() {
        Toggle.NyeBehovForUtbetaling.disable {
            val aktivitetslogg = Aktivitetslogg()
            Fagområde.Sykepenger.simuler(aktivitetslogg, emptyMap())
            assertEquals(1, aktivitetslogg.behov().size)
            assertEquals(Simulering, aktivitetslogg.behov().first().type)
        }
        Toggle.NyeBehovForUtbetaling.enable {
            val aktivitetslogg = Aktivitetslogg()
            Fagområde.Sykepenger.simuler(aktivitetslogg, emptyMap())
            assertEquals(1, aktivitetslogg.behov().size)
            assertEquals(SimuleringPerson, aktivitetslogg.behov().first().type)
        }
    }
}
