package no.nav.helse.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelPåminnelse
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.Periode
import no.nav.helse.juli
import no.nav.helse.oktober
import no.nav.helse.september
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.april
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadstypeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SykepengesoknadDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
        val vedtaksperiode = Vedtaksperiode.nyPeriode(
            inntektsmelding(
                førsteFraværsdag = førsteFraværsdag
            )
        )

        assertEquals(førsteFraværsdag, førsteFraværsdag(vedtaksperiode))
    }

    @Test
    fun `om en inntektsmelding ikke er mottat skal første fraværsdag returnere null`() {
        val vedtaksperiode = Vedtaksperiode.nyPeriode(
            nySøknad(perioder = listOf(Triple(1.juli, 20.juli, 100)))
        )

        assertNull(førsteFraværsdag(vedtaksperiode))
    }

    private fun førsteFraværsdag(vedtaksperiode: Vedtaksperiode): LocalDate? {
        var _førsteFraværsdag: LocalDate? = null
        vedtaksperiode.accept(object : VedtaksperiodeVisitor {
            override fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {
                _førsteFraværsdag = førsteFraværsdag
            }
        })
        return _førsteFraværsdag
    }

    private fun inntektsmelding(førsteFraværsdag: LocalDate = LocalDate.now()) =
        ModelInntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = ModelInntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 1000.0
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

    private fun sendtSøknad(
        perioder: List<ModelSendtSøknad.Periode> = listOf(
            ModelSendtSøknad.Periode.Sykdom(
                16.september,
                5.oktober,
                100
            )
        ), rapportertDato: LocalDateTime = LocalDateTime.now()
    ) =
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
