package no.nav.helse.hendelse

import no.nav.helse.hendelse.TestHendelser.`seks måneder og én dag før første sykedag`
import no.nav.helse.hendelse.TestHendelser.`én dag færre enn seks måneder før første sykedag`
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class SykepengeHistorikkTest {

    @Test
    fun `historikk eldre enn seks måneder skal ikke påvirke en sak`() {
        val sykdomstidslinje = TestHendelser.sendtSøknad().sykdomstidslinje()
        val sykepengeHistorikk = TestHendelser.sykepengeHistorikk(`seks måneder og én dag før første sykedag`)
        assertFalse(sykepengeHistorikk.påvirkerSakensMaksdato(sykdomstidslinje))
    }

    @Test
    fun `historikk yngre enn seks måneder skal påvirke en sak`() {
        val sykdomstidslinje = TestHendelser.sendtSøknad().sykdomstidslinje()
        val sykepengeHistorikk = TestHendelser.sykepengeHistorikk(`én dag færre enn seks måneder før første sykedag`)
        assert(sykepengeHistorikk.påvirkerSakensMaksdato(sykdomstidslinje))
    }
}