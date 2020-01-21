package no.nav.helse.person

import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.oktober
import no.nav.helse.september
import no.nav.helse.toJson
import no.nav.helse.toJsonNode
import no.nav.inntektsmeldingkontrakt.*
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class ArbeidstakerHendelseTest {

    private val nySøknad = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = "fnr",
        aktørId = "aktørId",
        orgnummer = "orgnr",
        rapportertdato = LocalDateTime.now(),
        sykeperioder = listOf(
            Triple(LocalDate.now(), LocalDate.now(), 100)
        ),
        aktivitetslogger = Aktivitetslogger(),
        originalJson = SykepengesoknadDTO(
            id = "123",
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = SoknadsstatusDTO.NY,
            aktorId = "aktørId",
            fnr = "fnr",
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(
                "Hello world",
                "123456789"
            ),
            fom = 16.september,
            tom = 5.oktober,
            opprettet = LocalDateTime.now(),
            sendtNav = LocalDateTime.now(),
            egenmeldinger = emptyList(),
            soknadsperioder = listOf(
                SoknadsperiodeDTO(16.september, 5.oktober,100)
            ),
            fravar = emptyList()
        ).toJsonNode().toString()).toJson()

    private val sendtSøknad = ModelSendtSøknad(
        UUID.randomUUID(),
        "fnr",
        "aktørId",
        "123456789",
        LocalDateTime.now(),
        listOf(ModelSendtSøknad.Periode.Sykdom(16.september, 5.oktober, 100)),
        Aktivitetslogger(),
        SykepengesoknadDTO(
            id = "123",
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = SoknadsstatusDTO.SENDT,
            aktorId = "aktørId",
            fnr = "fnr",
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(
                "Hello world",
                "123456789"
            ),
            fom = 16.september,
            tom = 5.oktober,
            opprettet = LocalDateTime.now(),
            sendtNav = LocalDateTime.now(),
            egenmeldinger = emptyList(),
            soknadsperioder = listOf(
                SoknadsperiodeDTO(16.september, 5.oktober,100)
            ),
            fravar = emptyList()
        ).toJsonNode().toString()
    ).toJson()
    private val inntektsmelding = ModelInntektsmelding(
        UUID.randomUUID(),
        ModelInntektsmelding.Refusjon(null, 0.0, null),
        "orgnr",
        "fnr",
        "aktørId",
        LocalDateTime.now(),
        LocalDate.now(),
        0.0,
        Aktivitetslogger(),
        Inntektsmelding(
            inntektsmeldingId = "",
            arbeidstakerFnr = "fnr",
            arbeidstakerAktorId = "aktørId",
            virksomhetsnummer = "virksomhetsnummer",
            arbeidsgiverFnr = null,
            arbeidsgiverAktorId = null,
            arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
            arbeidsforholdId = null,
            beregnetInntekt = BigDecimal.ONE,
            refusjon = Refusjon(BigDecimal.ONE, null),
            endringIRefusjoner = emptyList(),
            opphoerAvNaturalytelser = emptyList(),
            gjenopptakelseNaturalytelser = emptyList(),
            arbeidsgiverperioder = listOf(
                Periode(10.september, 10.september.plusDays(16))
            ),
            status = Status.GYLDIG,
            arkivreferanse = "",
            ferieperioder = emptyList(),
            foersteFravaersdag = LocalDate.now(),
            mottattDato = LocalDateTime.now()
        ).toJson(),
        listOf(1.januar..2.januar),
        ferieperioder = emptyList()
    ).toJson()

    @Test
    internal fun `deserialize NySøknad`() {
        assertTrue(ArbeidstakerHendelse.fromJson(nySøknad) is ModelNySøknad)
    }

    @Test
    internal fun `deserialize SendtSøknad`() {
        assertTrue(ArbeidstakerHendelse.fromJson(sendtSøknad) is ModelSendtSøknad)
    }

    @Test
    internal fun `deserialize Inntektsmelding`() {
        assertTrue(ArbeidstakerHendelse.fromJson(inntektsmelding) is ModelInntektsmelding)
    }
}
