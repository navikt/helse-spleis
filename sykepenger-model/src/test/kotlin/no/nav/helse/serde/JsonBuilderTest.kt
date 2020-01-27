package no.nav.helse.serde

import no.nav.helse.fixtures.august
import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Person
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

private const val aktørId = "12345"
private const val fnr = "12020052345"
private const val orgnummer = "987654321"

internal class JsonBuilderTest {
    @Test
    internal fun `print person som json`() {
        val person = Person(aktørId, fnr).apply {
            håndter(nySøknad)
            håndter(sendtSøknad)
            håndter(inntektsmelding)
            håndter(vilkårsgrunnlag)
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
    perioder = listOf(
        ModelSendtSøknad.Periode.Sykdom(1.januar, 31.januar, 100),
        ModelSendtSøknad.Periode.Utdanning(1.januar, 31.januar, 1.august)
    ),
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
    arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
    ferieperioder = emptyList(),
    aktivitetslogger = Aktivitetslogger()
)

private val vilkårsgrunnlag = ModelVilkårsgrunnlag(
    hendelseId = UUID.randomUUID(),
    vedtaksperiodeId = "1",
    aktørId = aktørId,
    fødselsnummer = fnr,
    orgnummer = orgnummer,
    rapportertDato = LocalDateTime.now(),
    inntektsmåneder = (1.rangeTo(12)).map {
        ModelVilkårsgrunnlag.Måned(
            årMåned = YearMonth.of(2018, it),
            inntektsliste = listOf(
                ModelVilkårsgrunnlag.Inntekt(
                    beløp = 1000.0
                )
            )
        )
    },
    erEgenAnsatt = false,
    aktivitetslogger = Aktivitetslogger(),
    originalJson = "{}"
)

