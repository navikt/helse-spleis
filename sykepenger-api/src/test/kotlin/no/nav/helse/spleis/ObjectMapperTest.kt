package no.nav.helse.spleis

import no.nav.helse.spleis.speil.dto.SkjønnsmessigFastsattDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ObjectMapperTest {
    @Test
    fun `properties som starter med æ, ø eller å serialiseres`() {
        val json = objectMapper.readTree(objectMapper.writeValueAsString(SkjønnsmessigFastsattDTO(årlig = 12.0, månedlig = 1.0)))
        assertEquals(setOf("årlig", "månedlig"), json.propertyNames().toSet())
    }
}
