package no.nav.helse.unit.hendelser.søknad

import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.TestConstants.søknadDTO
import no.nav.helse.TestConstants.søknadsperiode
import no.nav.helse.Uke
import no.nav.helse.toSendtSøknadHendelse
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SendtSøknadTest {

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
        val sendtSøknad = sendtSøknadHendelse(
                søknadsperioder = listOf(
                        SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100),
                        SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100)
                ),
                sendtNav = Uke(2).mandag.atStartOfDay()
        )

        assertTrue(sendtSøknad.kanBehandles())
    }

    @Test
    fun `en søknad hvor alle perioder har en grad på 100% fører til en hendelse som kan behandles`() {
        val sendtSøknad = sendtSøknadHendelse(
                søknadsperioder = listOf(
                        SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100),
                        SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100, faktiskGrad = 100)
                ),
                sendtNav = Uke(2).mandag.atStartOfDay()
        )

        assertTrue(sendtSøknad.kanBehandles())
    }

    @Test
    fun `en søknad kan bare gjelde for perioder opptil 3 måneder tilbake i tid fra da søknaden ble sendt`() {
        val december31 = LocalDate.of(2019, 12, 31)
        val september1 = LocalDate.of(2019, 9, 1)
        val sendtSøknad = sendtSøknadHendelse(søknadsperioder = listOf(
                søknadsperiode(
                        fom = september1,
                        tom = september1.plusDays(21)
                )),
                sendtNav = december31.atStartOfDay()
        )

        assertTrue(sendtSøknad.kanBehandles())
    }

    @Test
    fun `en søknad kan ikke gjelde for perioder lengre enn 3 måneder tilbake i tid fra da søknaden ble sendt`() {
        val december31 = LocalDate.of(2019, 12, 31)
        val august31 = LocalDate.of(2019, 8, 31)
        val sendtSøknad = søknadDTO(søknadsperioder = listOf(
                søknadsperiode(
                        fom = august31,
                        tom = august31.plusDays(21)
                )),
                status = SoknadsstatusDTO.SENDT,
                sendtNav = december31.atStartOfDay()
        ).toSendtSøknadHendelse()

        assertFalse(sendtSøknad.kanBehandles())
    }
}
