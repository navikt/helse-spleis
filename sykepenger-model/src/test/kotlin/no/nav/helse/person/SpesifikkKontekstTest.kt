package no.nav.helse.person

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class SpesifikkKontekstTest {

    @Test
    fun `to string`() {
        assertEquals("Person fødselsnummer: 1 aktørId: 2", SpesifikkKontekst("Person", mapOf(
            "fødselsnummer" to "1",
            "aktørId" to "2"
        )).melding())
    }

    @Test
    fun `to string uten kontekstMap`() {
        assertEquals("Person", SpesifikkKontekst("Person", emptyMap()).melding())
    }

    @Test
    fun `samme type og like kontekster er like`() {
        val ytelserKontekst = SpesifikkKontekst("Utbetalingshistorikk", mapOf(
            "fødselsnummer" to "1",
            "aktørId" to "2",
        ))
        val utbetalingshistorikkKontekst = SpesifikkKontekst("Utbetalingshistorikk", mapOf(
            "fødselsnummer" to "1",
            "aktørId" to "2",
        ))
        assertEquals(ytelserKontekst, utbetalingshistorikkKontekst)
        assertEquals(ytelserKontekst.hashCode(), utbetalingshistorikkKontekst.hashCode())
    }

    @Test
    fun `forskjellige typer, men med samme kontekster, er ikke like`() {
        val ytelserKontekst = SpesifikkKontekst("Ytelser", mapOf(
            "fødselsnummer" to "1",
            "aktørId" to "2",
        ))
        val utbetalingshistorikkKontekst = SpesifikkKontekst("Utbetalingshistorikk", mapOf(
            "fødselsnummer" to "1",
            "aktørId" to "2",
        ))
        assertNotEquals(ytelserKontekst, utbetalingshistorikkKontekst)
    }

    @Test
    fun `forskjellige typer gir forskjellige hashCodes`() {
        val kontekst1 = SpesifikkKontekst("a", mapOf(
            "b" to "c",
            "c" to "e",
        ))
        val kontekst2 = SpesifikkKontekst("b", mapOf(
            "b" to "c",
            "c" to "e",
        ))
        assertNotEquals(kontekst1, kontekst2)
        assertNotEquals(kontekst1.hashCode(), kontekst2.hashCode())
    }

    @Test
    fun `forskjellige kontekster gir forskjellige hashCodes`() {
        val kontekst1 = SpesifikkKontekst("a", mapOf(
            "b" to "c",
            "c" to "e",
        ))
        val kontekst2 = SpesifikkKontekst("a", mapOf(
            "b" to "c",
            "c" to "f",
        ))
        assertNotEquals(kontekst1, kontekst2)
        assertNotEquals(kontekst1.hashCode(), kontekst2.hashCode())
    }
}
