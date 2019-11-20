package no.nav.helse.sak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.juli
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class VedtaksperiodeTest {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    }

    @Test
    internal fun `gyldig jsonrepresentasjon av tomt vedtaksperiode`() {
        val id = UUID.randomUUID()
        val aktørId = "1234"
        val organisasjonsnummer = "123456789"

        val vedtaksperiode = Vedtaksperiode(
                id = id,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer
        )

        val jsonRepresentation = vedtaksperiode.jsonRepresentation()

        assertEquals(id, jsonRepresentation.id)
        assertEquals(aktørId, jsonRepresentation.aktørId)
        assertEquals(organisasjonsnummer, jsonRepresentation.organisasjonsnummer)
        assertNull(jsonRepresentation.sykdomstidslinje)
    }

    @Test
    internal fun `gyldig vedtaksperiode fra jsonrepresentasjon av tomt vedtaksperiode`() {
        val id = UUID.randomUUID()
        val aktørId = "1234"
        val organisasjonsnummer = "123456789"

        val vedtaksperiode = Vedtaksperiode(
                id = id,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer
        )

        val jsonRepresentation = vedtaksperiode.jsonRepresentation()
        val gjenopprettetVedtaksperiode = Vedtaksperiode.fromJson(jsonRepresentation)

        assertEquals(jsonRepresentation, gjenopprettetVedtaksperiode.jsonRepresentation())
    }

    @Test
    internal fun `dagsats leses som intnode`() {
        val id = UUID.randomUUID()
        val aktørId = "1234"
        val organisasjonsnummer = "123456789"

        val dagsats = 1000

        val utbetalingslinje = Utbetalingslinje(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                dagsats = dagsats
        ).let {
            objectMapper.convertValue<ObjectNode>(it)
        }

        val jsonRepresentation = Vedtaksperiode.VedtaksperiodeJson(
                id = id,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                utbetalingslinjer = listOf(utbetalingslinje).let {
                    objectMapper.convertValue<JsonNode>(it)
                },
                godkjentAv = null,
                maksdato = null,
                sykdomstidslinje = null,
                tilstandType = Vedtaksperiode.TilstandType.TIL_GODKJENNING,
                utbetalingsreferanse = null
        )

        val gjenopprettetVedtaksperiode = Vedtaksperiode.fromJson(jsonRepresentation)
        val nyJson = gjenopprettetVedtaksperiode.jsonRepresentation()

        val dagsatsFraNyJson = nyJson.utbetalingslinjer?.first()?.get("dagsats")?.asInt()

        assertEquals(dagsats, dagsatsFraNyJson!!)
    }

    @Test
    internal fun `gamle dagsatser lagret som bigdecimal leses riktig`() {
        val id = UUID.randomUUID()
        val aktørId = "1234"
        val organisasjonsnummer = "123456789"

        val dagsats = 1000
        val dagsatsMedDesimal = "999.50".toBigDecimal()

        val utbetalingslinje = Utbetalingslinje(
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                dagsats = dagsats
        ).let {
            objectMapper.convertValue<ObjectNode>(it)
        }.also {
            it.set<DecimalNode>("dagsats", DecimalNode(dagsatsMedDesimal))
        }

        val jsonRepresentation = Vedtaksperiode.VedtaksperiodeJson(
                id = id,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                utbetalingslinjer = listOf(utbetalingslinje).let {
                    objectMapper.convertValue<JsonNode>(it)
                },
                godkjentAv = null,
                maksdato = null,
                sykdomstidslinje = null,
                tilstandType = Vedtaksperiode.TilstandType.TIL_GODKJENNING,
                utbetalingsreferanse = null
        )

        val gjenopprettetVedtaksperiode = Vedtaksperiode.fromJson(jsonRepresentation)
        val nyJson = gjenopprettetVedtaksperiode.jsonRepresentation()

        val dagsatsFraNyJson = nyJson.utbetalingslinjer?.first()?.get("dagsats")?.asInt()

        assertEquals(dagsats, dagsatsFraNyJson!!)
    }

    @Test
    fun `memento inneholder en tilstand`() {
        val id = UUID.randomUUID()
        val vedtaksperiode = Vedtaksperiode(
                id = id,
                aktørId = "aktørId",
                organisasjonsnummer = "orgnummer"
        )

        val memento = vedtaksperiode.memento()
        val node = objectMapper.readTree(memento.state)

        assertNotNull(node["tilstand"])
        assertFalse(node["tilstand"].isNull)
        assertTrue(node["tilstand"].textValue().isNotEmpty())
    }

    @Test
    fun `memento inneholder aktørId og vedtaksperiodeId`() {
        val id = UUID.randomUUID()
        val vedtaksperiode = Vedtaksperiode(
                id = id,
                aktørId = "aktørId",
                organisasjonsnummer = "orgnummer"
        )

        val memento = vedtaksperiode.memento()
        val node = objectMapper.readTree(memento.state)

        assertEquals(id.toString(), node["id"].textValue())
        assertEquals("aktørId", node["aktørId"].textValue())
        assertEquals("orgnummer", node["organisasjonsnummer"].textValue())
    }

    @Test
    fun `restore bygger opp likt objekt fra lagret memento`() {
        val id = UUID.randomUUID()
        val vedtaksperiode = Vedtaksperiode(
                id = id,
                aktørId = "aktørId",
                organisasjonsnummer = "orgnummer"
        )
        vedtaksperiode.håndter(nySøknadHendelse())
        vedtaksperiode.håndter(sendtSøknadHendelse())
        vedtaksperiode.håndter(inntektsmeldingHendelse())

        val inMemento = vedtaksperiode.memento()

        val nyttVedtaksperiode = Vedtaksperiode.restore(inMemento)
        val outMemento = nyttVedtaksperiode.memento()
        val inNode = objectMapper.readTree(inMemento.state)
        val outNode = objectMapper.readTree(outMemento.state)

        assertEquals(inNode, outNode)

    }

    @Test
    fun `nytt vedtaksperiode godtar ny søknad`() {
        val vedtaksperiode = Vedtaksperiode(
                id = UUID.randomUUID(),
                aktørId = "aktørId",
                organisasjonsnummer = ""
        )
        assertTrue(vedtaksperiode.håndter(nySøknadHendelse()))
    }

    @Test
    fun `eksisterende vedtaksperiode godtar ikke søknader som ikke overlapper tidslinje i sendt søknad`() {
        val vedtaksperiode = Vedtaksperiode(
                id = UUID.randomUUID(),
                aktørId = "aktørId",
                organisasjonsnummer = ""
        )
        vedtaksperiode.håndter(nySøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 1.juli, tom = 20.juli)), egenmeldinger = emptyList(), fravær = emptyList()))

        assertFalse(vedtaksperiode.håndter(sendtSøknadHendelse(søknadsperioder = listOf(SoknadsperiodeDTO(fom = 21.juli, tom = 25.juli)), egenmeldinger = emptyList(), fravær = emptyList())))

    }
}
