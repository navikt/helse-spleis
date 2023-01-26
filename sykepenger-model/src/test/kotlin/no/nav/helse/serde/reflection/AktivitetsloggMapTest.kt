package no.nav.helse.serde.reflection

import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AktivitetsloggMapTest {
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun beforeEach() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `serialisering av aktiviteter med varselkode`() {
        aktivitetslogg.varsel(RV_SØ_1)
        val serialisert = AktivitetsloggMap().map(aktivitetslogg)

        val varsel = serialisert["aktiviteter"]!![0]
        assertEquals("RV_SØ_1", varsel["kode"].toString())
    }
}