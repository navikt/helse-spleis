package no.nav.helse.unit.person.hendelser.søknad

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.Uke
import no.nav.helse.person.UtenforOmfangException
import no.nav.helse.person.hendelser.søknad.NySøknadHendelse
import no.nav.helse.person.hendelser.søknad.Sykepengesøknad
import no.nav.helse.readResource
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class NySøknadHendelseTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val testSøknadJson = objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource())
        private val testSøknad = Sykepengesøknad(testSøknadJson)
        private val testSøknadSerialisert = objectMapper.readTree(objectMapper.writeValueAsString(testSøknad))
    }

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

    @Test
    fun `skal svare med id`() {
        Assertions.assertEquals("68da259c-ff7f-47cf-8fa0-c348ae95e220", testSøknad.id)
    }

    @Test
    fun `skal svare med sykmeldingId`() {
        Assertions.assertEquals("71bd853d-36a1-49df-a34c-6e02cf727cfa", testSøknad.sykmeldingId)
    }

    @Test
    fun `serialisering av søknaden skal være lik den deserialiserte json`() {
        Assertions.assertEquals(testSøknadJson, testSøknadSerialisert)
    }

    @Test
    fun `kan parse søknad uten arbeidGjenopptatt`() {
        val json = objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()) as ObjectNode
        json.remove("arbeidGjenopptatt")

        val søknad = Sykepengesøknad(json)
        Assertions.assertNull(søknad.arbeidGjenopptatt)
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
            json.set("status", JsonNodeFactory.instance.textNode("NY"))
            json.set("soknadsperioder", (objectMapper.readTree(overlappendePerioder)))

            NySøknadHendelse(Sykepengesøknad(json)).sykdomstidslinje()
        }
    }
}
