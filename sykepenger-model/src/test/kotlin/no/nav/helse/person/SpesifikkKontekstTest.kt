package no.nav.helse.person

import org.junit.jupiter.api.Assertions.assertEquals
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
}
