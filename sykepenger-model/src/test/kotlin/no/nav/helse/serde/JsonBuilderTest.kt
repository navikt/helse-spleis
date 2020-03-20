package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juli
import no.nav.helse.testhelpers.mai
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
        val person = person()
        val personPre = objectMapper.writeValueAsString(person)
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val personDeserialisert = SerialisertPerson(jsonBuilder.toString())
            .deserialize()
        val personPost = objectMapper.writeValueAsString(personDeserialisert)
        assertEquals(personPre, personPost, personPre.toString())
    }

    @Test
    fun `gjenoppbygd Person skal være lik opprinnelig Person`() {
        testSerialiseringAvPerson(person())
    }

    @Test
    fun `ingen betalingsperson`() {
        testSerialiseringAvPerson(ingenBetalingsperson())
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

        internal fun person(sendtSøknad: LocalDate = 1.april): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(1.januar, 31.januar))
                fangeVedtaksperiodeId()
                håndter(søknad(1.januar, 31.januar, sendtSøknad.atStartOfDay()))
                håndter(inntektsmelding(1.januar))
                håndter(vilkårsgrunnlag)
                håndter(ytelser)
                håndter(manuellSaksbehandling)
                håndter(utbetalt)
            }

        internal fun ingenBetalingsperson(sendtSøknad: LocalDate = 1.april): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(1.januar, 9.januar))
                fangeVedtaksperiodeId()
                håndter(søknad(1.januar, 9.januar, sendtSøknad.atStartOfDay()))
                håndter(inntektsmelding(1.januar))
                håndter(vilkårsgrunnlag)
                håndter(ytelser)
                håndter(manuellSaksbehandling)
                håndter(utbetalt)
            }

        private fun Person.fangeVedtaksperiodeId() {
            accept(object : PersonVisitor {
                override fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {
                    vedtaksperiodeId = id.toString()
                }
            })
        }

        private fun sykmelding(fom: LocalDate, tom: LocalDate) = Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            sykeperioder = listOf(Triple(fom, tom, 100))
        )

        private fun søknad(fom: LocalDate, tom: LocalDate, sendtSøknad: LocalDateTime) = Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = listOf(
                Søknad.Periode.Sykdom(fom, tom, 100)
            ),
            harAndreInntektskilder = false,
            sendtTilNAV = sendtSøknad
        )

        private fun inntektsmelding(fom: LocalDate) = Inntektsmelding(
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
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(
                        orgnummer,
                        1.januar(2017)
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

        private val utbetalt get() =
            Utbetaling(
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = aktørId,
                fødselsnummer = fnr,
                orgnummer = orgnummer,
                utbetalingsreferanse = "ref",
                status = Utbetaling.Status.FERDIG,
                melding = "hei"
            )
    }
}

