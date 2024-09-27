package no.nav.helse.etterlevelse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.helse.januar
import org.junit.jupiter.api.assertThrows
import kotlin.ranges.rangeTo

internal class BehandlingSubsumsjonsloggTest {

    private class SubsumsjonListLog : Subsumsjonslogg {
        val subsumsjoner = mutableListOf<Subsumsjon>()
        override fun logg(subsumsjon: Subsumsjon) {
            subsumsjoner.add(subsumsjon)
        }
    }

    @Test
    fun `jurist lytter på endringer av kontekst`() {
        val subsumsjonslogg = SubsumsjonListLog()
        val vedtaksperiodeJurist = BehandlingSubsumsjonslogg(
            subsumsjonslogg, listOf(
                Subsumsjonskontekst(KontekstType.Fødselsnummer, "10052088033"),
                Subsumsjonskontekst(KontekstType.Organisasjonsnummer, "123456789"),
                Subsumsjonskontekst(KontekstType.Vedtaksperiode, "6bce6c83-28ab-4a8c-b7f6-8402988bc8fc"),
            )
        )

        vedtaksperiodeJurist.logg(`§ 8-2 ledd 1`(true, LocalDate.now(), 1, emptyList(), 1))

        assertKontekster(
            subsumsjonslogg.subsumsjoner[0],
             "10052088033" to KontekstType.Fødselsnummer,
            "123456789" to KontekstType.Organisasjonsnummer,
            "6bce6c83-28ab-4a8c-b7f6-8402988bc8fc" to KontekstType.Vedtaksperiode
        )
    }

    @Test
    fun `kan ikke ha duplikate orgnr`() {
        val subsumsjonslogg = SubsumsjonListLog()
        val vedtaksperiodeJurist = BehandlingSubsumsjonslogg(
            subsumsjonslogg, listOf(
                Subsumsjonskontekst(KontekstType.Fødselsnummer, "10052088033"),
                Subsumsjonskontekst(KontekstType.Organisasjonsnummer, "123456789"),
                Subsumsjonskontekst(KontekstType.Organisasjonsnummer, "123456789"),
                Subsumsjonskontekst(KontekstType.Vedtaksperiode, "6bce6c83-28ab-4a8c-b7f6-8402988bc8fc"),
            )
        )

        assertThrows<IllegalStateException> {
            vedtaksperiodeJurist.logg(`§ 8-2 ledd 1`(true, LocalDate.now(), 1, emptyList(), 1))
        }
    }

    @Test
    fun `avviste dager`(){
        val subsumsjonslogg = SubsumsjonListLog()
        val vedtaksperiodeJurist = BehandlingSubsumsjonslogg(
            subsumsjonslogg, listOf(
                Subsumsjonskontekst(KontekstType.Fødselsnummer, "10052088033"),
                Subsumsjonskontekst(KontekstType.Organisasjonsnummer, "123456789"),
                Subsumsjonskontekst(KontekstType.Vedtaksperiode, "6bce6c83-28ab-4a8c-b7f6-8402988bc8fc"),
            )
        )
        `§ 8-13 ledd 1`(1.januar..31.januar, listOf(15.januar..16.januar), emptyList()).forEach {
            vedtaksperiodeJurist.logg(it)
        }
    }

    private fun assertKontekster(subsumsjon: Subsumsjon, vararg kontekster: Pair<String, KontekstType>) {
        assertEquals(
            kontekster.map { Subsumsjonskontekst(type = it.second, verdi = it.first) },
            subsumsjon.kontekster
        )
    }
}
