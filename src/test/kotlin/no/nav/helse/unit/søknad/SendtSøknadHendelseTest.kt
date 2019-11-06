package no.nav.helse.unit.søknad

import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.Uke
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SendtSøknadHendelseTest {

    @Test
    fun `en sendt søknad hvor bruker har ført opp grad mindre enn 100% fører til en hendelse som ikke kan behandles`() {
        val sendtSøknad = sendtSøknadHendelse(søknadsperioder = listOf(

                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100),
                SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100, faktiskGrad = 60)
        ))

        assertFalse(sendtSøknad.kanBehandles())
    }

    @Test
    fun `en søknad hvor alle perioder har en sykmeldingsgrad på 100% fører til en hendelse som kan behandles`() {
        val sendtSøknad = sendtSøknadHendelse(søknadsperioder = listOf(
                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100),
                SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100)
        ))

        assertTrue(sendtSøknad.kanBehandles())
    }

    @Test
    fun `en søknad hvor alle perioder har en grad på 100% fører til en hendelse som kan behandles`() {
        val sendtSøknad = sendtSøknadHendelse(søknadsperioder = listOf(
                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100),
                SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100, faktiskGrad = 100)
        ))

        assertTrue(sendtSøknad.kanBehandles())
    }
}
