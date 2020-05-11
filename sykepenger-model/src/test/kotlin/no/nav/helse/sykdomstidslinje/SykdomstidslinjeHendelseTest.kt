package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
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
            "aktørId" to AKTØR,
            "fødselsnummer" to FØDSELSNUMMER,
            "organisasjonsnummer" to ORGNR,
            "id" to MELDING.toString()
        ), Testhendelse().toSpesifikkKontekst().kontekstMap)
    }

    private class Testhendelse : SykdomstidslinjeHendelse(MELDING) {
        override fun aktørId() = AKTØR
        override fun fødselsnummer() = FØDSELSNUMMER
        override fun organisasjonsnummer() = ORGNR
        override fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje = TODO("not implemented")
        override fun sykdomstidslinje(): Sykdomstidslinje = TODO("not implemented")
        override fun valider(periode: Periode): Aktivitetslogg = TODO("not implemented")
        override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = TODO("not implemented")
    }
}
