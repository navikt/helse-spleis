package no.nav.helse.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.påminnelseHendelse
import no.nav.helse.fixtures.S
import no.nav.helse.fixtures.april
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.juli
import no.nav.helse.oktober
import no.nav.helse.september
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeTest {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    }

    @Test
    internal fun `gyldig jsonrepresentasjon av tomt vedtaksperiode`() {
        val aktørId = "1234"
        val fødselsnummer = "5678"
        val organisasjonsnummer = "123456789"

        val vedtaksperiode = Vedtaksperiode.nyPeriode(
            nySøknadHendelse(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                arbeidsgiver = ArbeidsgiverDTO(
                    orgnummer = organisasjonsnummer
                )
            )
        )

        val jsonRepresentation = vedtaksperiode.memento()

        assertEquals(aktørId, jsonRepresentation.aktørId)
        assertEquals(fødselsnummer, jsonRepresentation.fødselsnummer)
        assertEquals(organisasjonsnummer, jsonRepresentation.organisasjonsnummer)
        assertNotNull(jsonRepresentation.sykdomstidslinje)
    }

    @Test
    internal fun `gyldig vedtaksperiode fra jsonrepresentasjon av tomt vedtaksperiode`() {
        val aktørId = "1234"
        val fødselsnummer = "5678"
        val organisasjonsnummer = "123456789"

        val originalJson = Vedtaksperiode.nyPeriode(
            nySøknadHendelse(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                arbeidsgiver = ArbeidsgiverDTO(
                    orgnummer = organisasjonsnummer
                )
            )
        ).memento()

        val gjenopprettetJson = Vedtaksperiode.restore(originalJson)

        assertEquals(
            objectMapper.valueToTree<JsonNode>(originalJson.state()),
            objectMapper.valueToTree<JsonNode>(gjenopprettetJson.memento().state())
        )
    }

    @Test
    internal fun `dagsats leses som intnode`() {
        val id = UUID.randomUUID()
        val aktørId = "1234"
        val fødselsnummer = "5678"
        val organisasjonsnummer = "123456789"

        val dagsats = 1000

        val utbetalingslinje = Utbetalingslinje(
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            dagsats = dagsats
        ).let {
            objectMapper.convertValue<ObjectNode>(it)
        }

        val memento = Vedtaksperiode.Memento(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingslinjer = listOf(utbetalingslinje).let {
                objectMapper.convertValue<JsonNode>(it)
            },
            godkjentAv = null,
            maksdato = null,
            sykdomstidslinje = ObjectMapper().readTree(nySøknadHendelse().sykdomstidslinje().toJson()),
            tilstandType = TilstandType.TIL_GODKJENNING,
            utbetalingsreferanse = null,
            førsteFraværsdag = null,
            dataForVilkårsvurdering = null
        )

        val gjenopprettetVedtaksperiode = Vedtaksperiode.restore(memento)
        val nyJson = gjenopprettetVedtaksperiode.memento()

        val dagsatsFraNyJson = nyJson.utbetalingslinjer?.first()?.get("dagsats")?.asInt()

        assertEquals(dagsats, dagsatsFraNyJson!!)
    }

    @Test
    internal fun `gamle dagsatser lagret som bigdecimal leses riktig`() {
        val id = UUID.randomUUID()
        val aktørId = "1234"
        val fødselsnummer = "5678"
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

        val jsonRepresentation = Vedtaksperiode.Memento(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            utbetalingslinjer = listOf(utbetalingslinje).let {
                objectMapper.convertValue<JsonNode>(it)
            },
            godkjentAv = null,
            maksdato = null,
            sykdomstidslinje = ObjectMapper().readTree(nySøknadHendelse().sykdomstidslinje().toJson()),
            tilstandType = TilstandType.TIL_GODKJENNING,
            utbetalingsreferanse = null,
            førsteFraværsdag = null,
            dataForVilkårsvurdering = null
        )

        val gjenopprettetVedtaksperiode = Vedtaksperiode.restore(jsonRepresentation)
        val nyJson = gjenopprettetVedtaksperiode.memento()

        val dagsatsFraNyJson = nyJson.utbetalingslinjer?.first()?.get("dagsats")?.asInt()

        assertEquals(dagsats, dagsatsFraNyJson!!)
    }

    @Test
    fun `eksisterende vedtaksperiode godtar ikke søknader som ikke overlapper tidslinje i sendt søknad`() {
        val vedtaksperiode = Vedtaksperiode.nyPeriode(
            nySøknadHendelse(
                søknadsperioder = listOf(
                    SoknadsperiodeDTO(
                        fom = 1.juli,
                        tom = 20.juli
                    )
                ), egenmeldinger = emptyList(), fravær = emptyList()
            )
        )

        assertFalse(
            vedtaksperiode.håndter(
                sendtSøknad(
                    perioder = listOf(
                        Periode.Sykdom(
                            fom = 21.juli,
                            tom = 25.juli,
                            grad = 100
                        )
                    )
                )
            )
        )

    }

    @Test
    fun `påminnelse returnerer boolean etter om påminnelsen ble håndtert eller ikke`() {
        val id = UUID.randomUUID()
        val vedtaksperiode = Vedtaksperiode(id, "123", "123", "123", 1.S)

        assertFalse(vedtaksperiode.håndter(påminnelseHendelse(UUID.randomUUID(), TilstandType.START)))
        assertTrue(vedtaksperiode.håndter(påminnelseHendelse(id, TilstandType.START)))
    }

    @Test
    fun `første fraversdag skal returnere første fraversdag fra inntektsmelding`() {
        val førsteFraværsdag = 20.april
        val vedtaksperiode = Vedtaksperiode.nyPeriode(inntektsmelding(
            førsteFraværsdag = førsteFraværsdag
        ))

        assertEquals(førsteFraværsdag, vedtaksperiode.førsteFraværsdag())
    }

    @Test
    fun `om en inntektsmelding ikke er mottat skal første fraværsdag returnere null`() {
        val vedtaksperiode = Vedtaksperiode.nyPeriode(
            nySøknadHendelse(
                søknadsperioder = listOf(
                    SoknadsperiodeDTO(
                        fom = 1.juli,
                        tom = 20.juli
                    )
                ), egenmeldinger = emptyList(), fravær = emptyList()
            )
        )

        assertEquals(null, vedtaksperiode.førsteFraværsdag())
    }

    private fun inntektsmelding(førsteFraværsdag: LocalDate = LocalDate.now()) =
        ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 1000.0,
                endringerIRefusjon = null
            ),
            orgnummer = "orgnr",
            fødselsnummer = "fnr",
            aktørId = "aktørId",
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = 1000.0,
            aktivitetslogger = Aktivitetslogger(),
            originalJson = "{}",
            arbeidsgiverperioder = listOf(10.september..10.september.plusDays(16)),
            ferieperioder = emptyList()
        )

    private fun sendtSøknad(perioder: List<Periode> = listOf(Periode.Sykdom(16.september, 5.oktober, 100)), rapportertDato: LocalDateTime = LocalDateTime.now()) =
        ModelSendtSøknad(
            UUID.randomUUID(),
            "fnr",
            "aktørId",
            "orgnr",
            rapportertDato,
            perioder,
            Aktivitetslogger(),
            "{}"
        )
}
