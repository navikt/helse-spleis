package no.nav.helse.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.april
import no.nav.helse.hendelser.*
import no.nav.helse.juli
import no.nav.helse.oktober
import no.nav.helse.september
import no.nav.helse.sykdomstidslinje.Utbetalingslinje
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeTest {

    private val aktør = "1234"
    private val fødselsnummer = "5678"
    private val organisasjonsnummer = "123456789"

    private companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Test
    internal fun `gyldig jsonrepresentasjon av tomt vedtaksperiode`() {
        val vedtaksperiode = Vedtaksperiode.nyPeriode(nySøknad())
        val jsonRepresentation = vedtaksperiode.memento()

        assertEquals(aktør, jsonRepresentation.aktørId)
        assertEquals(fødselsnummer, jsonRepresentation.fødselsnummer)
        assertEquals(organisasjonsnummer, jsonRepresentation.organisasjonsnummer)
        assertNotNull(jsonRepresentation.sykdomstidslinje)
    }

    @Test
    internal fun `gyldig vedtaksperiode fra jsonrepresentasjon av tomt vedtaksperiode`() {
        val originalJson = Vedtaksperiode.nyPeriode(nySøknad()).memento()
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
            sykdomstidslinje = ObjectMapper().readTree(nySøknad().sykdomstidslinje().toJson()),
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
            sykdomstidslinje = ObjectMapper().readTree(nySøknad().sykdomstidslinje().toJson()),
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
            nySøknad(perioder = listOf(Triple(1.juli, 20.juli, 100)))
        )

        assertFalse(
            vedtaksperiode.håndter(
                sendtSøknad(
                    perioder = listOf(
                        ModelSendtSøknad.Periode.Sykdom(
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

        assertFalse(vedtaksperiode.håndter(påminnelse(UUID.randomUUID(), TilstandType.START)))
        assertTrue(vedtaksperiode.håndter(påminnelse(id, TilstandType.START)))
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
            nySøknad(perioder = listOf(Triple(1.juli, 20.juli, 100)))
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
            orgnummer = organisasjonsnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktør,
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = 1000.0,
            aktivitetslogger = Aktivitetslogger(),
            originalJson = "{}",
            arbeidsgiverperioder = listOf(Periode(10.september, 10.september.plusDays(16))),
            ferieperioder = emptyList()
        )

    private fun nySøknad(
        fnr: String = fødselsnummer,
        aktørId: String = aktør,
        orgnummer: String = organisasjonsnummer,
        perioder: List<Triple<LocalDate, LocalDate, Int>> = listOf(Triple(16.september, 5.oktober, 100))
    ) = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = orgnummer,
        rapportertdato = LocalDateTime.now(),
        sykeperioder = perioder,
        aktivitetslogger = Aktivitetslogger(),
        originalJson = SykepengesoknadDTO(
            id = "123",
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = SoknadsstatusDTO.NY,
            aktorId = aktørId,
            fnr = fnr,
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(
                "Hello world",
                orgnummer
            ),
            fom = 16.september,
            tom = 5.oktober,
            opprettet = LocalDateTime.now(),
            egenmeldinger = emptyList(),
            soknadsperioder = perioder.map { SoknadsperiodeDTO(it.first, it.second, it.third) }
        ).toJsonNode().toString()
    )

    private fun sendtSøknad(perioder: List<ModelSendtSøknad.Periode> = listOf(ModelSendtSøknad.Periode.Sykdom(16.september, 5.oktober, 100)), rapportertDato: LocalDateTime = LocalDateTime.now()) =
        ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = fødselsnummer,
            aktørId = aktør,
            orgnummer = organisasjonsnummer,
            rapportertdato = rapportertDato,
            perioder = perioder,
            originalJson = "{}",
            aktivitetslogger = Aktivitetslogger()
        )

    private fun påminnelse(vedtaksperiodeId: UUID, tilstandType: TilstandType) = ModelPåminnelse(
        hendelseId = UUID.randomUUID(),
        aktørId = "",
        fødselsnummer = "",
        organisasjonsnummer = "",
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        tilstand = tilstandType,
        antallGangerPåminnet = 1,
        tilstandsendringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
    )

}
