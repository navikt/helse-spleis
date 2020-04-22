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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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

    @Test
    fun `gjenoppbygd person med friske helgedager er lik opprinnelig person med friske helgedager`() {
        testSerialiseringAvPerson(friskeHelgedagerPerson())
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

        internal fun person(
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            sendtSøknad: LocalDate = 1.april,
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(fom = fom, tom = tom))
                fangeVedtaksperiodeId()
                håndter(
                    søknad(
                        hendelseId = søknadhendelseId,
                        fom = fom,
                        tom = tom,
                        sendtSøknad = sendtSøknad.atStartOfDay()
                    )
                )
                håndter(inntektsmelding(fom = fom))
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
            }

        internal fun ingenBetalingsperson(
            sendtSøknad: LocalDate = 1.april,
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(fom = 1.januar, tom = 9.januar))
                fangeVedtaksperiodeId()
                håndter(
                    søknad(
                        fom = 1.januar,
                        tom = 9.januar,
                        sendtSøknad = sendtSøknad.atStartOfDay(),
                        hendelseId = søknadhendelseId
                    )
                )
                håndter(inntektsmelding(fom = 1.januar))
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
            }

        internal fun friskeHelgedagerPerson(
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            sendtSøknad: LocalDate = 1.april,
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(fom = fom, tom = tom))
                fangeVedtaksperiodeId()
                håndter(
                    søknad(
                        hendelseId = søknadhendelseId,
                        fom = fom,
                        tom = tom,
                        sendtSøknad = sendtSøknad.atStartOfDay()
                    )
                )
                håndter(inntektsmelding(fom = fom, perioder = listOf(Periode(fom, 4.januar), Periode(8.januar, 16.januar))))
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
            }

        internal fun ingenutbetalingPåfølgendeBetaling(
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(fom = 1.januar, tom = 9.januar))
                fangeVedtaksperiodeId()
                håndter(
                    søknadSendtTilArbeidsgiver(
                        hendelseId = søknadhendelseId,
                        fom = 1.januar,
                        tom = 9.januar
                    )
                )
                håndter(sykmelding(fom = 10.januar, tom = 25.januar))
                håndter(søknad(fom = 10.januar, tom = 25.januar))
                fangeVedtaksperiodeId()
                håndter(inntektsmelding(fom = 16.januar))
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
            }

        private fun Person.fangeVedtaksperiodeId() {
            accept(object : PersonVisitor {
                override fun preVisitVedtaksperiode(
                    vedtaksperiode: Vedtaksperiode,
                    id: UUID,
                    gruppeId: UUID
                ) {
                    vedtaksperiodeId = id.toString()
                }
            })
        }

        internal fun sykmelding(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar
        ) = Sykmelding(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            sykeperioder = listOf(Triple(fom, tom, 100))
        )

        internal fun søknad(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            sendtSøknad: LocalDateTime = tom.plusDays(5).atTime(LocalTime.NOON)
        ) = Søknad(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = listOf(Søknad.Periode.Sykdom(fom, tom, 100)),
            harAndreInntektskilder = false,
            sendtTilNAV = sendtSøknad,
            permittert = false
        )

        internal fun søknadSendtTilArbeidsgiver(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar
        ) = SøknadArbeidsgiver(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = listOf(SøknadArbeidsgiver.Søknadsperiode(fom, tom, 100, 100))
        )

        internal fun inntektsmelding(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate,
            perioder: List<Periode> = listOf(Periode(fom, fom.plusDays(15)))
        ) = Inntektsmelding(
            meldingsreferanseId = hendelseId,
            refusjon = Inntektsmelding.Refusjon(1.juli, 31000.00, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = fnr,
            aktørId = aktørId,
            førsteFraværsdag = fom,
            beregnetInntekt = 31000.00,
            arbeidsgiverperioder = perioder,
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )

        internal fun vilkårsgrunnlag(vedtaksperiodeId: String) = Vilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering((1..12)
                .map { YearMonth.of(2018, it) to (orgnummer to 31000.0) }
                .groupBy({ it.first }) { it.second }),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Opptjeningvurdering.Arbeidsforhold(
                        orgnummer,
                        1.januar(2017)
                    )
                )
            ),
            erEgenAnsatt = false,
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            dagpenger = Dagpenger(emptyList()),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList())
        )

        internal fun ytelser(hendelseId: UUID = UUID.randomUUID(), vedtaksperiodeId: String) = Aktivitetslogg().let {
            Ytelser(
                meldingsreferanseId = hendelseId,
                aktørId = aktørId,
                fødselsnummer = fnr,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingshistorikk = Utbetalingshistorikk(
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

        internal fun manuellSaksbehandling(vedtaksperiodeId: String) = ManuellSaksbehandling(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            utbetalingGodkjent = true,
            saksbehandler = "en_saksbehandler_ident",
            godkjenttidspunkt = LocalDateTime.now()
        )

        internal fun simulering(vedtaksperiodeId: String) = Simulering(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            simuleringOK = true,
            melding = "Hei Aron",
            simuleringResultat = null
        )

        internal fun utbetalt(vedtaksperiodeId: String) = UtbetalingHendelse(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            utbetalingsreferanse = "ref",
            status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
            melding = "hei"
        )
    }
}

