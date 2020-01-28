package no.nav.helse.serde

import no.nav.helse.behov.Behov
import no.nav.helse.fixtures.februar
import no.nav.helse.fixtures.januar
import no.nav.helse.fixtures.juli
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

private const val aktørId = "12345"
private const val fnr = "12020052345"
private const val orgnummer = "987654321"
private var vedtaksperiodeId = "1"

internal class JsonBuilderTest {
    @Test
    internal fun `print person som json`() {
        val person = Person(aktørId, fnr).apply {
            addObserver(object : PersonObserver {
                override fun vedtaksperiodeTrengerLøsning(event: Behov) {
                    if (event.hendelsetype() == ArbeidstakerHendelse.Hendelsestype.Vilkårsgrunnlag) {
                        vedtaksperiodeId = event.vedtaksperiodeId()
                    }
                }
            })

            håndter(nySøknad)
            håndter(sendtSøknad)
            håndter(inntektsmelding)
            håndter(vilkårsgrunnlag)
            håndter(ytelser)
            håndter(manuellSaksbehandling)
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
        ModelSendtSøknad.Periode.Sykdom(1.januar, 31.januar, 100)
    ),
    originalJson = "{}",
    aktivitetslogger = Aktivitetslogger()
)

private val inntektsmelding = ModelInntektsmelding(
    hendelseId = UUID.randomUUID(),
    refusjon = ModelInntektsmelding.Refusjon(1.juli, 1000.00, emptyList()),
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

private val vilkårsgrunnlag
    get() = ModelVilkårsgrunnlag(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
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
        aktivitetslogger = Aktivitetslogger()

    )

private val ytelser
    get() = ModelYtelser(
        hendelseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fnr,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        sykepengehistorikk = ModelSykepengehistorikk(
            utbetalinger = listOf(
                ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver(
                    fom = 1.januar.minusYears(1),
                    tom = 31.januar.minusYears(1),
                    dagsats = 1000
                )
            ),
            inntektshistorikk = emptyList(),
            aktivitetslogger = Aktivitetslogger()
        ),
        foreldrepenger = ModelForeldrepenger(
            foreldrepengeytelse = Periode(
                fom = 1.januar.minusYears(2),
                tom = 31.januar.minusYears(2)
            ),
            svangerskapsytelse = Periode(
                fom = 1.juli.minusYears(2),
                tom = 31.juli.minusYears(2)
            ),
            aktivitetslogger = Aktivitetslogger()
        ),
        rapportertdato = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
    )

private val manuellSaksbehandling
    get() = ModelManuellSaksbehandling(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        aktørId = aktørId,
        fødselsnummer = fnr,
        organisasjonsnummer = orgnummer,
        utbetalingGodkjent = true,
        saksbehandler = "en_saksbehandler_ident",
        rapportertdato = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger()
    )

