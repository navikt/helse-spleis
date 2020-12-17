package no.nav.helse.person

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class ArbeidstakerHendelseTest {

    private companion object {
        private const val FØDSELSNUMMER = "fnr"
        private const val AKTØR = "aktørId"
        private const val ORGNR = "orgnr"
        private val MELDINGSREFERANSE = UUID.randomUUID()
    }

    @Test
    fun kontekst() {
        assertEquals(mapOf(
            "meldingsreferanseId" to MELDINGSREFERANSE.toString(),
            "aktørId" to AKTØR,
            "fødselsnummer" to FØDSELSNUMMER,
            "organisasjonsnummer" to ORGNR
        ), Testhendelse().toSpesifikkKontekst().kontekstMap)
    }

    private class Testhendelse : ArbeidstakerHendelse(MELDINGSREFERANSE) {
        override fun aktørId() = AKTØR
        override fun fødselsnummer() = FØDSELSNUMMER
        override fun organisasjonsnummer() = ORGNR
    }
}
