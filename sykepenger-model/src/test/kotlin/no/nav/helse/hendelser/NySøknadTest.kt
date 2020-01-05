package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.Uke
import no.nav.helse.person.UtenforOmfangException
import no.nav.helse.readResource
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class NySøknadTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Test
    fun `en søknad med perioder mindre enn 100% fører til en hendelse som ikke kan behandles`() {
        val nySøknad = nySøknadHendelse(
            søknadsperioder = listOf(
                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 60),
                SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100)
            )
        )

        assertFalse(nySøknad.kanBehandles())
    }

    @Test
    fun `en søknad hvor alle perioder har en grad på 100% fører til en hendelse som kan behandles`() {
        val nySøknad = nySøknadHendelse(
            søknadsperioder = listOf(
                SoknadsperiodeDTO(fom = Uke(1).mandag, tom = Uke(1).torsdag, sykmeldingsgrad = 100),
                SoknadsperiodeDTO(fom = Uke(1).fredag, tom = Uke(1).fredag, sykmeldingsgrad = 100)
            )
        )

        assertTrue(nySøknad.kanBehandles())
    }

    @Test
    fun `søknad med overlappende sykdomsperioder kastes upstairs`() {
        val overlappendePerioder =
            """
                    [
                        {
                          "fom": "2019-06-01",
                          "tom": "2019-06-14",
                          "sykmeldingsgrad": 100,
                          "faktiskGrad": null,
                          "avtaltTimer": null,
                          "faktiskTimer": null,
                          "sykmeldingstype": null
                        },
                        {
                          "fom": "2019-06-14",
                          "tom": "2019-06-20",
                          "sykmeldingsgrad": 100,
                          "faktiskGrad": null,
                          "avtaltTimer": null,
                          "faktiskTimer": null,
                          "sykmeldingstype": null
                        }
                    ]
                """.trimIndent()

        assertThrows<UtenforOmfangException> {
            val json = objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()) as ObjectNode
            json.set<ObjectNode>("status", JsonNodeFactory.instance.textNode("NY"))
            json.set<ObjectNode>("soknadsperioder", (objectMapper.readTree(overlappendePerioder)))

            requireNotNull(NySøknad.Builder().build(json.toString())).sykdomstidslinje()
        }
    }
}
