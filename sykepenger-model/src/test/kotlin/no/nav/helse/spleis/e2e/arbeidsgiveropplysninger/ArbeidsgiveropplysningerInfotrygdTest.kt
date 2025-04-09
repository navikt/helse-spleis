package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class ForespørselOmArbeidsgiveropplysningerInfotrygdTest : AbstractDslTest() {

    @Test
    fun `sender ikke flagget trengerArbeidsgiveropplysninger i tilstander som har rukket å sende ut egne forespørsler`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            assertFalse(observatør.forkastet(1.vedtaksperiode).trengerArbeidsgiveropplysninger)
        }
    }
}
