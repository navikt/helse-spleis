package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juli
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

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
    internal fun `gjenoppbygd Person skal være lik opprinnelig Person - The Jackson Way`() {
        val person = lagPerson().removeObservers()
        val personPre = objectMapper.writeValueAsString(person)
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val personDeserialisert = parsePerson(jsonBuilder.toString()).removeObservers()
        val personPost = objectMapper.writeValueAsString(personDeserialisert)

        assertEquals(personPre, personPost)
    }

    @Test
    fun `gjenoppbygd Person skal være lik opprinnelig Person`() {
        testSerialiseringAvPerson(lagPerson())
    }

    @Test
    fun `serialisering og deserialisering skal funke for alle states`() {
        testSerialiseringAvPerson(lagPerson(TilstandType.MOTTATT_NY_SØKNAD))
        testSerialiseringAvPerson(lagPerson(TilstandType.UNDERSØKER_HISTORIKK))
        testSerialiseringAvPerson(lagPerson(TilstandType.AVVENTER_VILKÅRSPRØVING))
        testSerialiseringAvPerson(lagPerson(TilstandType.AVVENTER_HISTORIKK))
        testSerialiseringAvPerson(lagPerson(TilstandType.AVVENTER_GODKJENNING))
        testSerialiseringAvPerson(lagPerson(TilstandType.TIL_UTBETALING))
    }

    private fun testSerialiseringAvPerson(person: Person) {
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

    companion object {
        internal fun lagPerson(stopState: TilstandType = TilstandType.TIL_UTBETALING) =
            Person(aktørId, fnr).apply {
                addObserver(object : PersonObserver {
                    override fun vedtaksperiodeTrengerLøsning(behov: Behov) {
                        if (behov.hendelsetype() == ArbeidstakerHendelse.Hendelsestype.Vilkårsgrunnlag) {
                            vedtaksperiodeId = behov.vedtaksperiodeId()
                        }
                    }
                })
                håndter(nySøknad)
                assertEquals(TilstandType.MOTTATT_NY_SØKNAD, hentTilstand(this)?.type)
                if (stopState == TilstandType.MOTTATT_NY_SØKNAD) return@apply
                håndter(sendtSøknad)
                assertEquals(TilstandType.UNDERSØKER_HISTORIKK, hentTilstand(this)?.type)
                if (stopState == TilstandType.UNDERSØKER_HISTORIKK) return@apply
                håndter(inntektsmelding)
                assertEquals(TilstandType.AVVENTER_VILKÅRSPRØVING, hentTilstand(this)?.type)
                if (stopState == TilstandType.AVVENTER_VILKÅRSPRØVING) return@apply
                håndter(vilkårsgrunnlag)
                assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand(this)?.type)
                if (stopState == TilstandType.AVVENTER_HISTORIKK) return@apply
                håndter(ytelser)
                assertEquals(TilstandType.AVVENTER_GODKJENNING, hentTilstand(this)?.type)
                if (stopState == TilstandType.AVVENTER_GODKJENNING) return@apply
                håndter(manuellSaksbehandling)
                assertEquals(TilstandType.TIL_UTBETALING, hentTilstand(this)?.type)
                if (stopState == TilstandType.TIL_UTBETALING) return@apply
            }

        private fun hentTilstand(person: Person): Vedtaksperiode.Vedtaksperiodetilstand? {
            var _tilstand: Vedtaksperiode.Vedtaksperiodetilstand? = null
            person.accept(object : PersonVisitor {
                override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
                    _tilstand = tilstand
                }
            })
            return _tilstand
        }

        private fun Person.removeObservers() = apply {
            get<Person, MutableList<PersonObserver>>("observers").clear()
            get<Person, MutableList<Arbeidsgiver>>("arbeidsgivere").forEach { arbeidsgiver ->
                arbeidsgiver.get<Arbeidsgiver, MutableList<VedtaksperiodeObserver>>("vedtaksperiodeObservers")
                    .clear()
                arbeidsgiver.get<Arbeidsgiver, MutableList<Vedtaksperiode>>("perioder").forEach { vedtaksperiode ->
                    vedtaksperiode.get<Vedtaksperiode, MutableList<VedtaksperiodeObserver>>("observers").clear()
                }
            }
        }


        internal val nySøknad
            get() = ModelNySøknad(
                hendelseId = UUID.randomUUID(),
                fnr = fnr,
                aktørId = aktørId,
                orgnummer = orgnummer,
                rapportertdato = LocalDateTime.now(),
                sykeperioder = listOf(Triple(1.januar, 31.januar, 100)),
                aktivitetslogger = Aktivitetslogger()
            )

        internal val sendtSøknad
            get() = ModelSendtSøknad(
                hendelseId = UUID.randomUUID(),
                fnr = fnr,
                aktørId = aktørId,
                orgnummer = orgnummer,
                sendtNav = LocalDateTime.now(),
                perioder = listOf(
                    ModelSendtSøknad.Periode.Sykdom(1.januar, 31.januar, 100)
                ),
                aktivitetslogger = Aktivitetslogger(),
                harAndreInntektskilder = false
            )

        internal val inntektsmelding
            get() = ModelInntektsmelding(
                hendelseId = UUID.randomUUID(),
                refusjon = ModelInntektsmelding.Refusjon(1.juli, 31000.00, emptyList()),
                orgnummer = orgnummer,
                fødselsnummer = fnr,
                aktørId = aktørId,
                mottattDato = 1.februar.atStartOfDay(),
                førsteFraværsdag = 1.januar,
                beregnetInntekt = 31000.00,
                arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
                ferieperioder = emptyList(),
                aktivitetslogger = Aktivitetslogger()
            )

        internal val vilkårsgrunnlag
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
                                beløp = 31000.0
                            )
                        )
                    )
                },
                arbeidsforhold = listOf(ModelVilkårsgrunnlag.Arbeidsforhold(orgnummer, 1.januar(2017))),
                erEgenAnsatt = false,
                aktivitetslogger = Aktivitetslogger()

            )

        internal val ytelser
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
                            dagsats = 31000
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

        internal val manuellSaksbehandling
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
    }
}

