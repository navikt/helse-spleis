package no.nav.helse.hendelse

import no.nav.helse.TestConstants.`seks måneder og én dag før første sykedag`
import no.nav.helse.TestConstants.`én dag færre enn seks måneder før første sykedag`
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sykepengehistorikkHendelse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SykepengehistorikkTest {

    @Test
    fun `historikk eldre enn seks måneder skal ikke påvirke en sak`() {
        val sykdomstidslinje = nySøknadHendelse().sykdomstidslinje()
        val sykepengeHistorikk = sykepengehistorikkHendelse(`seks måneder og én dag før første sykedag`)
        assertFalse(sykepengeHistorikk.påvirkerSakensMaksdato(sykdomstidslinje))
    }

    @Test
    fun `historikk yngre enn seks måneder skal påvirke en sak`() {
        val sykdomstidslinje = nySøknadHendelse().sykdomstidslinje()
        val sykepengeHistorikk = sykepengehistorikkHendelse(`én dag færre enn seks måneder før første sykedag`)
        assertTrue(sykepengeHistorikk.påvirkerSakensMaksdato(sykdomstidslinje))
    }
}
