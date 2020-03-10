package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juli
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class JsonBuilderTest {

    private val objectMapper = jacksonObjectMapper()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .setMixIns(
            mutableMapOf(
                Arbeidsgiver::class.java to ArbeidsgiverMixin::class.java,
                Vedtaksperiode::class.java to VedtaksperiodeMixin::class.java
            )
        )
        .registerModule(JavaTimeModule())

    @JsonIgnoreProperties("person")
    private class ArbeidsgiverMixin

    @JsonIgnoreProperties("person", "arbeidsgiver")
    private class VedtaksperiodeMixin

    @Test
    internal fun `gjenoppbygd Person skal være lik opprinnelig Person - The Jackson Way`() {
        val person = lagPerson()
        val personPre = objectMapper.writeValueAsString(person)
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val personDeserialisert = SerialisertPerson(jsonBuilder.toString())
            .deserialize()
        val personPost = objectMapper.writeValueAsString(personDeserialisert)

        println(personPre)

        assertEquals(personPre, personPost)
    }

    @Test
    fun `gjenoppbygd Person skal være lik opprinnelig Person`() {
        testSerialiseringAvPerson(lagPerson())

        val jsonBuilder = JsonBuilder()
        lagPerson().accept(jsonBuilder)
        val json = jsonBuilder.toString()
        println(json)
    }

    private fun testSerialiseringAvPerson(person: Person) {
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val json = jsonBuilder.toString()

        val result = SerialisertPerson(json).deserialize()
        val jsonBuilder2 = JsonBuilder()
        result.accept(jsonBuilder2)
        val json2 = jsonBuilder2.toString()

        objectMapper.readTree(json).also {
            assertTrue(it.hasNonNull("skjemaVersjon"))
            assertEquals(SerialisertPerson.gjeldendeVersjon(), it["skjemaVersjon"].intValue())
        }
        assertEquals(json, json2)
        assertDeepEquals(person, result)
    }

    companion object {
        private const val aktørId = "12345"
        private const val fnr = "12020052345"
        private const val orgnummer = "987654321"
        private lateinit var vedtaksperiodeId: String

        internal data class PeriodeMedTilstand(
            val fom: LocalDate,
            val tom: LocalDate,
            val stopptilstand: TilstandType
        )

        internal fun lagPerson(vararg perioder: PeriodeMedTilstand): Person = Person(aktørId, fnr).apply {
            perioder.forEach {
                håndter(sykmelding(it.fom, it.tom))

                accept(object : PersonVisitor {
                    override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
                        vedtaksperiodeId = id.toString()
                    }
                })

                håndter(søknad(it.fom, it.tom))
                håndter(inntektsmelding(it.fom))
                håndter(vilkårsgrunnlag)
                håndter(ytelser)
                håndter(manuellSaksbehandling)
            }
        }

        internal fun lagPerson(stopState: TilstandType = TilstandType.TIL_UTBETALING): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding())

                accept(object : PersonVisitor {
                    override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
                        vedtaksperiodeId = id.toString()
                    }
                })

                håndter(søknad())
                håndter(inntektsmelding())
                håndter(vilkårsgrunnlag)
                håndter(ytelser)
                håndter(manuellSaksbehandling)
        }

        private fun hentTilstand(person: Person): Vedtaksperiode.Vedtaksperiodetilstand {
            lateinit var _tilstand: Vedtaksperiode.Vedtaksperiodetilstand
            person.accept(object : PersonVisitor {
                override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
                    _tilstand = tilstand
                }
            })
            return _tilstand
        }

        private fun sykmelding(fom: LocalDate = 1.januar, tom: LocalDate = 31.januar) = Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            sykeperioder = listOf(Triple(fom, tom, 100))
        )

        private fun søknad(fom: LocalDate = 1.januar, tom: LocalDate = 31.januar) = Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = listOf(
                Søknad.Periode.Sykdom(fom, tom, 100)
            ),
            harAndreInntektskilder = false,
            rapportertdato = tom.atStartOfDay()
        )

        private fun inntektsmelding(fom: LocalDate = 1.januar) = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(1.juli, 31000.00, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = fnr,
            aktørId = aktørId,
            førsteFraværsdag = fom,
            beregnetInntekt = 31000.00,
            arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(15))),
            ferieperioder = emptyList()
        )

        private val vilkårsgrunnlag
            get() = Vilkårsgrunnlag(
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = aktørId,
                fødselsnummer = fnr,
                orgnummer = orgnummer,
                inntektsmåneder = (1.rangeTo(12)).map {
                    Vilkårsgrunnlag.Måned(
                        årMåned = YearMonth.of(2018, it),
                        inntektsliste = listOf(31000.0)
                    )
                },
                arbeidsforhold = Vilkårsgrunnlag.MangeArbeidsforhold(
                    listOf(
                        Vilkårsgrunnlag.Arbeidsforhold(
                            orgnummer,
                            1.januar(2017)
                        )
                    )
                ),
                erEgenAnsatt = false
            )

        private val ytelser
            get() = Aktivitetslogg().let {
                Ytelser(
                    meldingsreferanseId = UUID.randomUUID(),
                    aktørId = aktørId,
                    fødselsnummer = fnr,
                    organisasjonsnummer = orgnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    utbetalingshistorikk = Utbetalingshistorikk(
                        ukjentePerioder = emptyList(),
                        utbetalinger = listOf(
                            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(
                                fom = 1.januar.minusYears(1),
                                tom = 31.januar.minusYears(1),
                                dagsats = 31000
                            )
                        ),
                        inntektshistorikk = emptyList(),
                        graderingsliste = emptyList(),
                        aktivitetslogg = it
                    ),
                    foreldrepermisjon = Foreldrepermisjon(
                        foreldrepengeytelse = Periode(
                            fom = 1.januar.minusYears(2),
                            tom = 31.januar.minusYears(2)
                        ),
                        svangerskapsytelse = Periode(
                            fom = 1.juli.minusYears(2),
                            tom = 31.juli.minusYears(2)
                        ),
                        aktivitetslogg = it
                    ),
                    aktivitetslogg = it
                )
            }

        private val manuellSaksbehandling
            get() = ManuellSaksbehandling(
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = aktørId,
                fødselsnummer = fnr,
                organisasjonsnummer = orgnummer,
                utbetalingGodkjent = true,
                saksbehandler = "en_saksbehandler_ident",
                godkjenttidspunkt = LocalDateTime.now()
            )
    }
}

