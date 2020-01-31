package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.ModelForeldrepenger
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelManuellSaksbehandling
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.hendelser.ModelSykepengehistorikk
import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import no.nav.helse.hendelser.ModelYtelser
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeObserver
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juli
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

private const val aktørId = "12345"
private const val fnr = "12020052345"
private const val orgnummer = "987654321"
private var vedtaksperiodeId = "1"

internal class JsonBuilderTest {
    private val objectMapper = jacksonObjectMapper()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(JavaTimeModule())

    @Test
    @Disabled("Out of memory 😭")
    internal fun `gjenoppbygd Person skal være lik opprinnelig Person - The Jackson Way`() {
        val person = lagPerson()
        person.get<Person, MutableList<PersonObserver>>("observers").clear()
        person.get<Person, MutableList<Arbeidsgiver>>("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.get<Arbeidsgiver, MutableList<VedtaksperiodeObserver>>("vedtaksperiodeObservers")
                .clear()
            arbeidsgiver.get<Arbeidsgiver, MutableList<Vedtaksperiode>>("perioder").forEach { vedtaksperiode ->
                vedtaksperiode.get<Vedtaksperiode, MutableList<VedtaksperiodeObserver>>("observers").clear()
            }
        }
        val personPre = objectMapper.writeValueAsString(person)
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val personDeserialisert = parsePerson(jsonBuilder.toString())
        val personPost = objectMapper.writeValueAsString(personDeserialisert)

        assertEquals(personPre, personPost)
    }

    private fun lagPerson() =
        Person(aktørId, fnr).apply {
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

    @Test
    internal fun `print person som json`() {
        val person = lagPerson()
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        println(jsonBuilder.toString())
    }

    @Test
    fun `gjenoppbygd Person skal være lik opprinnelig Person`() {
        val person = lagPerson()
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val json = jsonBuilder.toString()

        val result = parsePerson(json)
        val jsonBuilder2 = JsonBuilder()
        result.accept(jsonBuilder2)
        val json2 = jsonBuilder2.toString()

        assertEquals(json, json2)
        assertDeepEquals(person, result)
    }
}

private val nySøknad
    get() = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = orgnummer,
        rapportertdato = LocalDateTime.now(),
        sykeperioder = listOf(Triple(1.januar, 31.januar, 100)),
        originalJson = "{}",
        aktivitetslogger = Aktivitetslogger()
    )

private val sendtSøknad
    get() = ModelSendtSøknad(
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

private val inntektsmelding
    get() = ModelInntektsmelding(
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
