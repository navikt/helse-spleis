package no.nav.helse.økonomi

import no.nav.helse.serde.reflection.createØkonomi
import no.nav.helse.serde.ØkonomiData
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CreateØkonomiTest {

    @Test
    fun `opprette bare prosenter`() {
        val data = ØkonomiData(79.5, 66.67, null, null, null)
        createØkonomi(data).also { økonomi ->
            økonomi.toMap().also { map ->
                assertEquals(79.5, map["grad"])
                assertEquals(66.67, map["arbeidsgiverBetalingProsent"])
                assertNull(map["lønn"])
            }
            // Indirect test of Økonomi state is KunGrad
            assertThrows<IllegalStateException> { listOf(økonomi).betale(1.januar) }
            assertDoesNotThrow { økonomi.lønn(1200) }
        }
    }

    @Test
    fun `opprette med bare lønn`() {
        val data = ØkonomiData(79.5, 66.67, 1199.6, null, null)
        createØkonomi(data).also { økonomi ->
            økonomi.toMap().also { map ->
                assertEquals(79.5, map["grad"])
                assertEquals(66.67, map["arbeidsgiverBetalingProsent"])
                assertEquals(1199.6, map["lønn"])
                assertNull(map["arbeidsgiversutbetaling"])
                assertNull(map["personUtbetaling"])
            }
            // Indirect test of Økonomi state is HarLønn
            assertThrows<IllegalStateException> { økonomi.lønn(1200) }
            assertDoesNotThrow { listOf(økonomi).betale(1.januar) }
        }
    }

    @Test
    fun `har betalinger`() {
        val data = ØkonomiData(79.5, 66.67, 1199.6, 640, 320)
        createØkonomi(data).also { økonomi ->
            økonomi.toMap().also { map ->
                assertEquals(79.5, map["grad"])
                assertEquals(66.67, map["arbeidsgiverBetalingProsent"])
                assertEquals(1199.6, map["lønn"])
                assertEquals(640, map["arbeidsgiversutbetaling"])
                assertEquals(320, map["personUtbetaling"])
            }
            // Indirect test of Økonomi state
            assertThrows<IllegalStateException> { økonomi.lønn(1200) }
            assertThrows<IllegalStateException> { listOf(økonomi).betale(1.januar) }
        }
    }
}
