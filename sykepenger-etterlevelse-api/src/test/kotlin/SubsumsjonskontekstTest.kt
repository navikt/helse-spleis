package no.nav.helse.etterlevelse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SubsumsjonskontekstTest {

    @Test
    fun kontekster() {
        val rootkontekst = Subsumsjonskontekst(KontekstType.Fødselsnummer, "fnr")
        val arbeidsgiverkontekst = Subsumsjonskontekst(KontekstType.Organisasjonsnummer, "a1")
        val vedtaksperiodekontekst = Subsumsjonskontekst(KontekstType.Vedtaksperiode, "vedtaksperiodeId")

        val kontekststi = rootkontekst.barn(arbeidsgiverkontekst).barn(vedtaksperiodekontekst)
        assertEquals(listOf("vedtaksperiodeId", "a1", "fnr"), kontekststi.kontekster().map { it.verdi })
    }

    @Test
    fun `sikre kun en konteksttype i sti`() {
        val rootkontekst = Subsumsjonskontekst(KontekstType.Fødselsnummer, "fnr")
        val a1kontekst = Subsumsjonskontekst(KontekstType.Organisasjonsnummer, "a1")
        val a2kontekst = Subsumsjonskontekst(KontekstType.Organisasjonsnummer, "a2")

        val kontekst = rootkontekst.barn(a1kontekst)
        assertThrows<IllegalStateException> {
            kontekst.barn(a2kontekst)
        }

    }
}