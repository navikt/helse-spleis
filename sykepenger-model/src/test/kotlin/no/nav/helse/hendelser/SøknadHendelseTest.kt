package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.TestConstants.nySøknadHendelse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SøknadHendelseTest {

    @Test
    internal fun fromJson() {
        val nySøknad = nySøknadHendelse().toJson()
        assertTrue(SøknadHendelse.fromJson(nySøknad) is NySøknad)

        val oldJsonFormat = ObjectMapper().readTree(nySøknad).also {
            (it as ObjectNode).put("hendelsetype", SykdomshendelseType.NySøknadMottatt.name)
        }.toString()
        assertTrue(SøknadHendelse.fromJson(oldJsonFormat) is NySøknad)
    }
}
