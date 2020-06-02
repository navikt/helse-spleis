package no.nav.helse.økonomi

import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.DagData
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.reflection.createØkonomi
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

internal class CreateØkonomiTest {

    @Test
    fun `opprette bare prosenter`() {
        val data = økonomiData(79.5, 66.67)
        createØkonomi(data).also { økonomi ->
            økonomi.toMap().also { map ->
                assertEquals(79.5, map["grad"])
                assertEquals(66.67, map["arbeidsgiverBetalingProsent"])
                assertNull(map["dekningsgrunnlag"])
            }
            // Indirect test of Økonomi state is KunGrad
            assertThrows<IllegalStateException> { listOf(økonomi).betal(1.januar) }
            assertDoesNotThrow { økonomi.dekningsgrunnlag(1200) }
        }
    }

    @Test
    fun `opprette med bare lønn`() {
        val data = økonomiData(79.5, 66.67, 1199.6)
        createØkonomi(data).also { økonomi ->
            økonomi.toMap().also { map ->
                assertEquals(79.5, map["grad"])
                assertEquals(66.67, map["arbeidsgiverBetalingProsent"])
                assertEquals(1199.6, map["dekningsgrunnlag"])
                assertNull(map["arbeidsgiverbeløp"])
                assertNull(map["personbeløp"])
                assertNull(map["er6GBegrenset"])
            }
            // Indirect test of Økonomi state is HarLønn
            assertThrows<IllegalStateException> { økonomi.dekningsgrunnlag(1200) }
            assertDoesNotThrow { listOf(økonomi).betal(1.januar) }
        }
    }

    @Test
    fun `har betalinger`() {
        val data = økonomiData(79.5, 66.67, 1199.6, 640, 320, true)
        createØkonomi(data).also { økonomi ->
            økonomi.toMap().also { map ->
                assertEquals(79.5, map["grad"])
                assertEquals(66.67, map["arbeidsgiverBetalingProsent"])
                assertEquals(1199.6, map["dekningsgrunnlag"])
                assertEquals(640, map["arbeidsgiverbeløp"])
                assertEquals(320, map["personbeløp"])
                assertTrue(map["er6GBegrenset"] as Boolean)
            }
            // Indirect test of Økonomi state
            assertThrows<IllegalStateException> { økonomi.dekningsgrunnlag(1200) }
            assertThrows<IllegalStateException> { listOf(økonomi).betal(1.januar) }
        }
    }

    private fun økonomiData(
        grad: Double,
        arbeidsgiverBetalingProsent: Double,
        dagsats: Double? = null,
        arbeidsgiverbeløp: Int? = null,
        personbeløp: Int? = null,
        er6GBegrenset: Boolean = false
    ) = DagData(
        1.januar,
        JsonDagType.SYKEDAG,
        PersonData.ArbeidsgiverData.VedtaksperiodeData.KildeData("type", UUID.randomUUID()),
        grad,
        arbeidsgiverBetalingProsent,
        dagsats,
        arbeidsgiverbeløp,
        personbeløp,
        er6GBegrenset,
        null
    )
}
