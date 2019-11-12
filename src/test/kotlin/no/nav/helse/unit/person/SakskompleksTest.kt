package no.nav.helse.unit.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.juli
import no.nav.helse.person.Sakskompleks
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class SakskompleksTest {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    }

    @Test
    internal fun `gyldig jsonrepresentasjon av tomt sakskompleks`() {
        val id = UUID.randomUUID()
        val aktørId = "1234"
        val organisasjonsnummer = "123456789"

        val sakskompleks = Sakskompleks(
                id = id,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer
        )

        val jsonRepresentation = sakskompleks.jsonRepresentation()

        assertEquals(id, jsonRepresentation.id)
        assertEquals(aktørId, jsonRepresentation.aktørId)
        assertEquals(organisasjonsnummer, jsonRepresentation.organisasjonsnummer)
        assertNull(jsonRepresentation.sykdomstidslinje)
    }

    @Test
    internal fun `gyldig sakskompleks fra jsonrepresentasjon av tomt sakskompleks`() {
        val id = UUID.randomUUID()
        val aktørId = "1234"
        val organisasjonsnummer = "123456789"

        val sakskompleks = Sakskompleks(
                id = id,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer
        )

        val jsonRepresentation = sakskompleks.jsonRepresentation()
        val gjenopprettetSakskompleks = Sakskompleks.fromJson(jsonRepresentation)

        assertEquals(jsonRepresentation, gjenopprettetSakskompleks.jsonRepresentation())
    }

    @Test
    internal fun `gamle dagsatser lagret som bigdecimal leses riktig`() {
        val id = UUID.randomUUID()
        val aktørId = "1234"
        val organisasjonsnummer = "123456789"

        val dagsats = 1000
        val dagsatsMedDesimal = BigDecimal("999.50")

        val utbetalingslinje = Utbetalingslinje(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                dagsats = dagsats
        ).let {
            objectMapper.convertValue<ObjectNode>(it)
        }.also {
            it["dagsats"] = TextNode(dagsatsMedDesimal.toString())
        }

        val jsonRepresentation = Sakskompleks.SakskompleksJson(
                id = id,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                utbetalingslinjer = listOf(utbetalingslinje).let {
                    objectMapper.convertValue<JsonNode>(it)
                },
                godkjentAv = null,
                maksdato = null,
                sykdomstidslinje = null,
                tilstandType = Sakskompleks.TilstandType.TIL_GODKJENNING
        )

        val gjenopprettetSakskompleks = Sakskompleks.fromJson(jsonRepresentation)
        val nyJson = gjenopprettetSakskompleks.jsonRepresentation()

        val dagsatsFraNyJson = nyJson.utbetalingslinjer?.first()?.get("dagsats")?.asInt()

        assertEquals(dagsats, dagsatsFraNyJson!!)
    }

    @Test
    fun `memento inneholder en tilstand`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
                id = id,
                aktørId = "aktørId",
                organisasjonsnummer = "orgnummer"
        )

        val memento = sakskompleks.memento()
        val node = objectMapper.readTree(memento.state)

        assertNotNull(node["tilstand"])
        assertFalse(node["tilstand"].isNull)
        assertTrue(node["tilstand"].textValue().isNotEmpty())
    }

    @Test
    fun `memento inneholder aktørId og sakskompleksId`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
                id = id,
                aktørId = "aktørId",
                organisasjonsnummer = "orgnummer"
        )

        val memento = sakskompleks.memento()
        val node = objectMapper.readTree(memento.state)

        assertEquals(id.toString(), node["id"].textValue())
        assertEquals("aktørId", node["aktørId"].textValue())
        assertEquals("orgnummer", node["organisasjonsnummer"].textValue())
    }

    @Test
    fun `restore bygger opp likt objekt fra lagret memento`() {
        val id = UUID.randomUUID()
        val sakskompleks = Sakskompleks(
                id = id,
                aktørId = "aktørId",
                organisasjonsnummer = "orgnummer"
        )
        sakskompleks.håndterNySøknad(nySøknadHendelse())
        sakskompleks.håndterSendtSøknad(sendtSøknadHendelse())
        sakskompleks.håndterInntektsmelding(inntektsmeldingHendelse())

        val inMemento = sakskompleks.memento()

        val nyttSakskompleks = Sakskompleks.restore(inMemento)
        val outMemento = nyttSakskompleks.memento()
        val inNode = objectMapper.readTree(inMemento.state)
        val outNode = objectMapper.readTree(outMemento.state)

        assertEquals(inNode, outNode)

    }

    @Test
    fun `nytt sakskompleks godtar ny søknad`() {
        val sakskompleks = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "aktørId",
                organisasjonsnummer = ""
        )
        assertTrue(sakskompleks.håndterNySøknad(nySøknadHendelse()))
    }

    @Test
    fun `eksisterende sakskompleks godtar ikke søknader som ikke overlapper tidslinje i sendt søknad`() {
        val sakskompleks = Sakskompleks(
                id = UUID.randomUUID(),
                aktørId = "aktørId",
                organisasjonsnummer = ""
        )
        sakskompleks.håndterNySøknad(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))

        assertFalse(sakskompleks.håndterSendtSøknad(sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 21.juli, tom = 25.juli)), egenmeldinger = emptyList(), fravær = emptyList())))

    }
}
