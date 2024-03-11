package no.nav.helse.serde.api

import no.nav.helse.serde.api.dto.BegrunnelseDTO
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BegrunnelseMementoDTOTest {

    @Test
    fun `likt innhold i enumene`() {
        assertEquals(
            Begrunnelse::class.sealedSubclasses.map { it.simpleName }.filterNot { it == "NyVilkårsprøvingNødvendig" }.toSet(),
            BegrunnelseDTO.values().map(BegrunnelseDTO::toString).toSet()
        )
    }
}
