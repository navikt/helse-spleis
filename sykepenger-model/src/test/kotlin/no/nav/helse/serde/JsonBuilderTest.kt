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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
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

    @Test
    fun `serialisering og deserialisering skal funke for alle states`() {
        testSerialiseringAvPerson(lagPerson(TilstandType.MOTTATT_SYKMELDING))
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
        private lateinit var aktivitetslogg: Aktivitetslogg

        internal fun lagPerson(stopState: TilstandType = TilstandType.TIL_UTBETALING): Person {
            aktivitetslogg = Aktivitetslogg()

            val person = Person(aktørId, fnr).apply {
                håndter(sykmelding)

                accept(object : PersonVisitor {
                    override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
                        vedtaksperiodeId = id.toString()
                    }
                })

                assertEquals(TilstandType.MOTTATT_SYKMELDING, hentTilstand(this).type)
                if (stopState == TilstandType.MOTTATT_SYKMELDING) return@apply
                håndter(søknad)
                assertEquals(TilstandType.UNDERSØKER_HISTORIKK, hentTilstand(this).type)
                if (stopState == TilstandType.UNDERSØKER_HISTORIKK) return@apply
                håndter(inntektsmelding)
                assertEquals(TilstandType.AVVENTER_VILKÅRSPRØVING, hentTilstand(this).type)
                if (stopState == TilstandType.AVVENTER_VILKÅRSPRØVING) return@apply
                håndter(vilkårsgrunnlag)
                assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand(this).type)
                if (stopState == TilstandType.AVVENTER_HISTORIKK) return@apply
                håndter(ytelser)
                assertEquals(TilstandType.AVVENTER_GODKJENNING, hentTilstand(this).type)
                if (stopState == TilstandType.AVVENTER_GODKJENNING) return@apply
                håndter(manuellSaksbehandling)
                assertEquals(TilstandType.TIL_UTBETALING, hentTilstand(this).type)
                if (stopState == TilstandType.TIL_UTBETALING) return@apply
            }

            assertFalse(aktivitetslogg.hasErrors()) { "Aktivitetslogg contains errors: $aktivitetslogg" }

            return person
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


        private val sykmelding
            get() = Sykmelding(
                meldingsreferanseId = UUID.randomUUID(),
                fnr = fnr,
                aktørId = aktørId,
                orgnummer = orgnummer,
                sykeperioder = listOf(Triple(1.januar, 31.januar, 100))
            )

        private val søknad
            get() = Søknad(
                meldingsreferanseId = UUID.randomUUID(),
                fnr = fnr,
                aktørId = aktørId,
                orgnummer = orgnummer,
                perioder = listOf(
                    Søknad.Periode.Sykdom(1.januar, 31.januar, 100)
                ),
                harAndreInntektskilder = false
            )

        private val inntektsmelding
            get() = Inntektsmelding(
                meldingsreferanseId = UUID.randomUUID(),
                refusjon = Inntektsmelding.Refusjon(1.juli, 31000.00, emptyList()),
                orgnummer = orgnummer,
                fødselsnummer = fnr,
                aktørId = aktørId,
                førsteFraværsdag = 1.januar,
                beregnetInntekt = 31000.00,
                arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
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
            get() = Aktivitetslogg().let {Ytelser(
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
            )}

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

