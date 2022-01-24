package no.nav.helse.serde

import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MetrikkEndToEndTest : AbstractEndToEndTest() {
    @Test
    fun `kan generere metrikker uten exceptions`() {
        nyttVedtak(1.januar, 31.januar)
        assertTrue(person.serialize().metrikker().isNotEmpty())
    }
}
