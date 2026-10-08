package no.nav.helse.hendelser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

internal class OverstyrTidslinjeTest {
    @Test
    fun `overstyring uten dager`() {
        assertThrows<RuntimeException> { OverstyrTidslinje(MeldingsreferanseId(UUID.randomUUID()), Behandlingsporing.Yrkesaktivitet.Arbeidstaker("orgnr"), emptyList(), LocalDateTime.now()) }
    }
}
