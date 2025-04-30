package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class OverstyrTidslinjeTest {

    @Test
    fun `overstyring uten dager`() {
        assertThrows<RuntimeException> { OverstyrTidslinje(MeldingsreferanseId(UUID.randomUUID()), Behandlingsporing.Yrkesaktivitet.Arbeidstaker("orgnr"), emptyList(), LocalDateTime.now()) }
    }
}
