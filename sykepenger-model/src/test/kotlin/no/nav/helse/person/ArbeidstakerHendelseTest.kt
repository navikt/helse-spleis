package no.nav.helse.person

import no.nav.helse.TestConstants
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.toJson
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
        problemer = Problemer(),
        originalJson = TestConstants.søknadDTO(
            aktørId = "aktørId",
            status = SoknadsstatusDTO.NY
        ).toJson()).toJson()
    private val sendtSøknad = sendtSøknadHendelse().toJson()
    private val inntektsmelding = inntektsmeldingHendelse().toJson()

    @Test
    internal fun `deserialize NySøknad`() {
        assertTrue(ArbeidstakerHendelse.fromJson(nySøknad) is ModelNySøknad)
    }

    @Test
    internal fun `deserialize SendtSøknad`() {
        assertTrue(ArbeidstakerHendelse.fromJson(sendtSøknad) is SendtSøknad)
    }

    @Test
    internal fun `deserialize Inntektsmelding`() {
        assertTrue(ArbeidstakerHendelse.fromJson(inntektsmelding) is Inntektsmelding)
    }
}
