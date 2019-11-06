package no.nav.helse.unit.søknad

import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.Uke
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NySøknadHendelseTest {

    @Test
    fun `en søknad med perioder mindre enn 100% fører til en hendelse som ikke kan behandles`() {
        val nySøknad = nySøknadHendelse(søknadsperioder = listOf(
                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 60),
                SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100)
        ))

        assertFalse(nySøknad.kanBehandles())
    }

    @Test
    fun `en søknad hvor alle perioder har en grad på 100% fører til en hendelse som kan behandles`() {
        val nySøknad = nySøknadHendelse(søknadsperioder = listOf(
                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100),
                SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100)
        ))

        assertTrue(nySøknad.kanBehandles())
    }
}
