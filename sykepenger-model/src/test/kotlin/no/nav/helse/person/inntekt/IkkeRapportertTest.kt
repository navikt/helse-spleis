package no.nav.helse.person.inntekt

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class IkkeRapportertTest {

    @Test
    fun `overstyres av saksbehandler`() {
        val ikkeRapportert = IkkeRapportert(1.januar, UUID.randomUUID())
        val saksbehandler = Saksbehandler(UUID.randomUUID(), Inntektsdata(UUID.randomUUID(), 1.januar, 500.daglig, LocalDateTime.now()), ikkeRapportert)
        val result = ikkeRapportert.overstyresAv(saksbehandler)

        assertSame(ikkeRapportert, result.inspektør.forrigeInntekt)
    }
}
