package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SykdomstidslinjeHendelseTest {
    private companion object {
        private val MELDING = UUID.randomUUID()
        private const val FØDSELSNUMMER = "fnr"
        private const val AKTØR = "aktørId"
        private const val ORGNR = "orgnr"
    }

    @Test
    fun kontekst() {
        assertEquals(mapOf(
            "meldingsreferanseId" to MELDING.toString(),
            "aktørId" to AKTØR,
            "fødselsnummer" to FØDSELSNUMMER,
            "organisasjonsnummer" to ORGNR
        ), Testhendelse().toSpesifikkKontekst().kontekstMap)
    }

    private class Testhendelse : SykdomstidslinjeHendelse(MELDING, LocalDateTime.now()) {
        override fun aktørId() = AKTØR
        override fun fødselsnummer() = FØDSELSNUMMER
        override fun organisasjonsnummer() = ORGNR
        override fun sykdomstidslinje(): Sykdomstidslinje = TODO("not implemented")
        override fun valider(periode: Periode): Aktivitetslogg = TODO("not implemented")
        override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = TODO("not implemented")
    }
}
