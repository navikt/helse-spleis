package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.serde.migration.JsonMigration
import no.nav.helse.serde.migration.V5BegrensGradTilMellom0Og100
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
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
        assertEquals(TilstandType.AVSLUTTET, tilstand)
        val personPre = objectMapper.writeValueAsString(person)
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val personDeserialisert = SerialisertPerson(jsonBuilder.toString())
            .deserialize()
        val personPost = objectMapper.writeValueAsString(personDeserialisert)
        assertEquals(personPre, personPost, personPre.toString())
    }

    @Test
    internal fun `støtter deserialisering av person uten ny sykdomstidslinje`() {
        val person = person()
        val initialPerson = objectMapper.writeValueAsString(person)
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val json = objectMapper.readTree(jsonBuilder.toString()).also { personJsonNode ->
            personJsonNode["arbeidsgivere"][0]["vedtaksperioder"][0]["sykdomshistorikk"].forEach { historikk ->
                (historikk as ObjectNode).let { sykdomshistorikk ->
                    sykdomshistorikk.remove("nyHendelseSykdomstidslinje")
                    sykdomshistorikk.remove("nyBeregnetSykdomstidslinje")
                }
            }
        }
        JsonMigration.medSkjemaversjon(listOf(V5BegrensGradTilMellom0Og100()), json)

        val serialisertPerson = SerialisertPerson(json.toString())
        val personDeserialisert = serialisertPerson.deserialize()
        val migratedPerson = objectMapper.writeValueAsString(personDeserialisert)

        assertEquals(
            removeNotMigratedInfo(initialPerson),
            removeNotMigratedInfo(migratedPerson),
            "Migrated, serialized and deserialized person should match initial person"
        )
    }

    private fun removeNotMigratedInfo(person: String) =
        (objectMapper.readTree(person) as ObjectNode).let { personNode ->
            personNode.path("arbeidsgivere").forEach { arbeidsgiver ->
                arbeidsgiver.path("perioder").forEach { vedtaksperiode ->
                    vedtaksperiode.path("sykdomshistorikk")["elementer"].forEach { sykdomshistorikkelement ->
                        (sykdomshistorikkelement as ObjectNode).apply {

                            // As new sykdomstidslinje does not yet calculate beregnetSykdomstidlinje, we cannot compare them
                            remove("nyBeregnetSykdomstidslinje")

                            (get("nyHendelseSykdomstidslinje") as ObjectNode).apply {
                                remove("id") // not migrated
                                replace("låstePerioder", arrayNode()) // not migrated
                                put(
                                    "tidsstempel",
                                    get("tidsstempel").asText().replace(Regex("T.*"), "")
                                ) // not migrated, timestamp will differ
                            }
                        }
                    }
                }
            }
            objectMapper.writeValueAsString(personNode)
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
        private lateinit var tilstand: TilstandType
        private lateinit var sykdomstidslinje: NySykdomstidslinje

        internal fun person(
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            sendtSøknad: LocalDate = 1.april,
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(fom = fom, tom = tom))
                fangeVedtaksperiode()
                håndter(
                    søknad(
                        hendelseId = søknadhendelseId,
                        fom = fom,
                        tom = tom,
                        sendtSøknad = sendtSøknad.atStartOfDay()
                    )
                )
                fangeSykdomstidslinje()
                sykdomstidslinje.lås(Periode(5.januar, 10.januar))
                håndter(inntektsmelding(fom = fom))
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
                fangeVedtaksperiode()
            }

        internal fun ingenBetalingsperson(
            sendtSøknad: LocalDate = 1.april,
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(fom = 1.januar, tom = 9.januar))
                fangeVedtaksperiode()
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
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
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
                fangeVedtaksperiode()
                håndter(
                    søknad(
                        hendelseId = søknadhendelseId,
                        fom = fom,
                        tom = tom,
                        sendtSøknad = sendtSøknad.atStartOfDay()
                    )
                )
                håndter(
                    inntektsmelding(
                        fom = fom,
                        perioder = listOf(Periode(fom, 4.januar), Periode(8.januar, 16.januar))
                    )
                )
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
            }

        internal fun ingenutbetalingPåfølgendeBetaling(
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(fom = 1.januar, tom = 9.januar))
                fangeVedtaksperiode()
                håndter(
                    søknadSendtTilArbeidsgiver(
                        hendelseId = søknadhendelseId,
                        fom = 1.januar,
                        tom = 9.januar
                    )
                )
                håndter(sykmelding(fom = 10.januar, tom = 25.januar))
                håndter(søknad(fom = 10.januar, tom = 25.januar))
                fangeVedtaksperiode()
                håndter(inntektsmelding(fom = 16.januar))
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
            }

        private fun Person.fangeVedtaksperiode() {
            accept(object : PersonVisitor {
                override fun preVisitVedtaksperiode(
                    vedtaksperiode: Vedtaksperiode,
                    id: UUID,
                    gruppeId: UUID
                ) {
                    vedtaksperiodeId = id.toString()
                }

                override fun visitTilstand(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) {
                    JsonBuilderTest.tilstand = tilstand.type
                }
            })
        }

        private fun Person.fangeSykdomstidslinje() {
            accept(object : PersonVisitor {
                override fun preVisitNySykdomstidslinje(
                    tidslinje: NySykdomstidslinje,
                    låstePerioder: List<Periode>,
                    id: UUID,
                    tidsstempel: LocalDateTime
                ) {
                    sykdomstidslinje = tidslinje
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
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(fom, tom, 100)),
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
            refusjon = Inntektsmelding.Refusjon(null, 31000.00, emptyList()),
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

        internal fun utbetalingsgodkjenning(vedtaksperiodeId: String) = Utbetalingsgodkjenning(
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

