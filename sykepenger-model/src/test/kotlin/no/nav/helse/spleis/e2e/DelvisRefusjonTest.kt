package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DelvisRefusjonTest : AbstractEndToEndTest()  {


    @Test
    fun `Full refusjon til en arbeidsgiver med RefusjonPerDag på`() = Toggles.RefusjonPerDag.enable {
        nyttVedtak(1.januar, 31.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))

        assertTrue(inspektør.utbetalinger.last().arbeidsgiverOppdrag().isNotEmpty())
        inspektør.utbetalinger.last().arbeidsgiverOppdrag().forEach {
            assertEquals(1431, it.beløp)
        }
        assertTrue(inspektør.utbetalinger.last().personOppdrag().isEmpty())
    }

    @Test
    fun `Full refusjon til en arbeidsgiver med RefusjonPerDag av`() = Toggles.RefusjonPerDag.disable {
        nyttVedtak(1.januar, 31.januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))

        assertTrue(inspektør.utbetalinger.last().arbeidsgiverOppdrag().isNotEmpty())
        inspektør.utbetalinger.last().arbeidsgiverOppdrag().forEach {
            assertEquals(1431, it.beløp)
        }
        assertTrue(inspektør.utbetalinger.last().personOppdrag().isEmpty())
    }
}
