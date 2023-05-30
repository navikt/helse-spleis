package no.nav.helse.spleis.e2e

import java.time.Year
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.januar
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.SendFeriepengeOppdrag::class)
internal class RekjørerFeriepengerTest: AbstractDslTest() {

    @Test
    fun `rekjøring av feriepenger for arbeidsgiver hvor det skulle vært utbetalt feriepenger`() {
        a1 {
            nyttVedtak(1.januar(2022), 31.januar(2022))
            håndterPersonPåminnelse()
            assertEquals(0, antallFeriepengeutbetalingerTilArbeidsgiver())
            håndterUtbetalingshistorikkForFeriepenger(Year.of(2022))
            assertEquals(1, antallFeriepengeutbetalingerTilArbeidsgiver())
            håndterPersonPåminnelse()
            assertInfo("Overfører arbeidsigiveroppdrag for feriepenger 2022 på nytt", person())
            assertEquals(2, antallFeriepengeutbetalingerTilArbeidsgiver())
        }
    }

    @Test
    fun `rekjører ikke feriepenger for arbeidsgiver hvor det ikke skulle vært utbetalt feriepenger`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            håndterPersonPåminnelse()
            assertEquals(0, antallFeriepengeutbetalingerTilArbeidsgiver())
            håndterUtbetalingshistorikkForFeriepenger(Year.of(2022))
            assertEquals(0, antallFeriepengeutbetalingerTilArbeidsgiver())
            håndterPersonPåminnelse()
            assertIngenInfo("Overfører arbeidsigiveroppdrag for feriepenger 2022 på nytt", person())
            assertEquals(0, antallFeriepengeutbetalingerTilArbeidsgiver())
        }
    }
}