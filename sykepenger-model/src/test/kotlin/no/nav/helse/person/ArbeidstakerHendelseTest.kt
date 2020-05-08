package no.nav.helse.person

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ArbeidstakerHendelseTest {

    private companion object {
        private const val FØDSELSNUMMER = "fnr"
        private const val AKTØR = "aktørId"
        private const val ORGNR = "orgnr"
    }

    @Test
    fun kontekst() {
        assertEquals(mapOf(
            "aktørId" to AKTØR,
            "fødselsnummer" to FØDSELSNUMMER,
            "organisasjonsnummer" to ORGNR
        ), Testhendelse().toSpesifikkKontekst().kontekstMap)
    }

    private class Testhendelse : ArbeidstakerHendelse() {
        override fun aktørId() = AKTØR
        override fun fødselsnummer() = FØDSELSNUMMER
        override fun organisasjonsnummer() = ORGNR
    }
}
