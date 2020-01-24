package no.nav.helse.serde

import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Person
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

private const val aktørId = "12345"
private const val fnr = "12020052345"
private const val orgnummer = "987654321"

internal class JsonBuilderTest {
    @Test
    internal fun `maybe what?`(){
        val person = Person(aktørId, fnr).apply {
            håndter(nySøknad)
            håndter(sendtSøknad)
            håndter(inntektsmelding)
        }

        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        println(jsonBuilder.toString())
    }
}

private val nySøknad = ModelNySøknad(
    hendelseId = UUID.randomUUID(),
    fnr = fnr,
    aktørId = aktørId,
    orgnummer = orgnummer,
    rapportertdato = LocalDateTime.now(),
    sykeperioder = listOf(Triple(1.januar, 31.januar, 100)),
    originalJson = "{}",
    aktivitetslogger = Aktivitetslogger()
)

private val sendtSøknad = ModelSendtSøknad(
    hendelseId = UUID.randomUUID(),
    fnr = fnr,
    aktørId = aktørId,
    orgnummer = orgnummer,
    rapportertdato = LocalDateTime.now(),
    perioder = listOf(ModelSendtSøknad.Periode.Sykdom(1.januar, 31.januar, 100)),
    originalJson = "{}",
    aktivitetslogger = Aktivitetslogger()
)

private val inntektsmelding = ModelInntektsmelding(
    hendelseId = UUID.randomUUID(),
    refusjon = ModelInntektsmelding.Refusjon(1.januar, 1000.00, emptyList()),
    orgnummer = orgnummer,
    fødselsnummer = fnr,
    aktørId = aktørId,
    mottattDato = 1.februar.atStartOfDay(),
    førsteFraværsdag = 1.januar,
    beregnetInntekt = 1000.00,
    originalJson = "{}",
    arbeidsgiverperioder = listOf(1.januar..16.januar),
    ferieperioder = emptyList(),
    aktivitetslogger = Aktivitetslogger()
)
