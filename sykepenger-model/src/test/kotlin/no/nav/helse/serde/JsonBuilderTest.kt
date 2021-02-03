package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class JsonBuilderTest {

    private val objectMapper = jacksonObjectMapper()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .setMixIns(
            mutableMapOf(
                Arbeidsgiver::class.java to ArbeidsgiverMixin::class.java,
                Vedtaksperiode::class.java to VedtaksperiodeMixin::class.java,
                Utbetaling::class.java to UtbetalingMixin::class.java
            )
        )
        .registerModule(JavaTimeModule())

    @JsonIgnoreProperties("person")
    private class ArbeidsgiverMixin

    @JsonIgnoreProperties("person", "arbeidsgiver")
    private class VedtaksperiodeMixin

    @JsonIgnoreProperties("observers", "forrigeHendelse")
    private class UtbetalingMixin

    @Test
    fun `gjenoppbygd Person skal være lik opprinnelig Person - The Jackson Way`() {
        val person = person()
        assertEquals(TilstandType.AVSLUTTET, tilstand)
        val jsonBuilder = JsonBuilder(person)
        val personPost = SerialisertPerson(jsonBuilder.toString())
            .deserialize()

        assertJsonEquals(person, personPost)
    }

    private fun assertJsonEquals(expected: Any, actual: Any) {
        val expectedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expected)
        val actualJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual)
        assertEquals(expectedJson, actualJson)
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
    fun `med forkastede`() {
        testSerialiseringAvPerson(forkastedeVedtaksperioderperson())
    }

    @Test
    fun `med annullering`() {
        val person = person().apply {
            håndter(annullering(fangeArbeidsgiverFagsystemId()))
        }
        testSerialiseringAvPerson(person)
    }

    @Test
    fun `med opphør av refusjon`() {
        testSerialiseringAvPerson(refusjonOpphørerPerson())
    }

    @Test
    fun `gjenoppbygd person med friske helgedager er lik opprinnelig person med friske helgedager`() {
        testSerialiseringAvPerson(friskeHelgedagerPerson())
    }

    @Test
    fun `gjenoppbygd person er lik opprinnelig person med kopiert inntekt`() {
        Toggles.PraksisendringEnabled.enable {
            testSerialiseringAvPerson(personMedLiteGap())
        }
    }

    private fun testSerialiseringAvPerson(person: Person) {
        val jsonBuilder = JsonBuilder(person)
        val json = jsonBuilder.toString()

        val result = SerialisertPerson(json).deserialize()
        val jsonBuilder2 = JsonBuilder(result)
        val json2 = jsonBuilder2.toString()

        objectMapper.readTree(json).also {
            assertTrue(it.hasNonNull("skjemaVersjon"))
            assertEquals(SerialisertPerson.gjeldendeVersjon(), it["skjemaVersjon"].intValue())
        }
        assertEquals(json, json2)
        assertDeepEquals(person, result)
    }

    private companion object {
        private const val aktørId = "12345"
        private const val fnr = "12020052345"
        private const val orgnummer = "987654321"
        private lateinit var vedtaksperiodeId: String
        private lateinit var tilstand: TilstandType
        private lateinit var sykdomstidslinje: Sykdomstidslinje
        private val utbetalingsliste: MutableMap<String, List<Utbetaling>> = mutableMapOf()

        fun person(
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
                håndter(inntektsmelding(fom = fom))
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
                fangeUtbetalinger()
                håndter(overføring(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
                fangeVedtaksperiode()
            }

        fun personMedLiteGap() = person(fom = 1.januar, tom = 20.januar).apply {
            håndter(sykmelding(fom = 1.februar, tom = 10.februar))
            fangeVedtaksperiode()
            håndter(søknad(fom = 1.februar, tom = 10.februar))
            håndter(utbetalingshistorikk())
        }

        fun ingenBetalingsperson(
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
            }

        fun forkastedeVedtaksperioderperson(
            sendtSøknad: LocalDate = 1.april,
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(fom = 1.januar, tom = 9.januar))
                håndter(
                    søknad(
                        fom = 1.januar,
                        tom = 9.januar,
                        sendtSøknad = sendtSøknad.atStartOfDay(),
                        perioder = listOf(
                            Søknad.Søknadsperiode.Sykdom(1.januar, 9.januar, 100.prosent),
                            Søknad.Søknadsperiode.Permisjon(1.januar, 9.januar)
                        ),
                        hendelseId = søknadhendelseId
                    )
                )
            }

        fun friskeHelgedagerPerson(
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
                fangeUtbetalinger()
                håndter(overføring(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
            }

        fun refusjonOpphørerPerson(
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Person =
            Person(aktørId, fnr).apply {
                håndter(sykmelding(fom = 1.januar, tom = 9.januar))
                fangeVedtaksperiode()
                håndter(
                    søknad(
                        fom = 1.januar,
                        tom = 9.januar,
                        hendelseId = søknadhendelseId
                    )
                )
                håndter(inntektsmelding(fom = 1.januar, refusjon = Inntektsmelding.Refusjon(4.januar, 31000.månedlig, emptyList())))
            }

        private fun Person.fangeUtbetalinger() {
            utbetalingsliste.clear()
            accept(object : PersonVisitor {
                private lateinit var orgnr: String
                override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
                    orgnr = organisasjonsnummer
                }

                override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
                    utbetalingsliste[orgnr] = utbetalinger
                }
            })
        }

        private fun Person.fangeVedtaksperiode() {
            accept(object : PersonVisitor {
                override fun preVisitVedtaksperiode(
                    vedtaksperiode: Vedtaksperiode,
                    id: UUID,
                    tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
                    opprettet: LocalDateTime,
                    oppdatert: LocalDateTime,
                    periode: Periode,
                    opprinneligPeriode: Periode,
                    hendelseIder: List<UUID>
                ) {
                    vedtaksperiodeId = id.toString()
                    JsonBuilderTest.tilstand = tilstand.type
                }
            })
        }

        private fun Person.fangeArbeidsgiverFagsystemId(): String {
            var result: String? = null
            accept(object : PersonVisitor {
                override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
                    result = oppdrag.fagsystemId()
                }
            })
            return requireNotNull(result)
        }

        private fun Person.fangeSykdomstidslinje() {
            accept(object : PersonVisitor {
                override fun preVisitSykdomstidslinje(
                    tidslinje: Sykdomstidslinje,
                    låstePerioder: List<Periode>
                ) {
                    sykdomstidslinje = tidslinje
                }
            })
        }

        fun sykmelding(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar
        ) = Sykmelding(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            sykeperioder = listOf(Sykmeldingsperiode(fom, tom, 100.prosent)),
            mottatt = fom.plusMonths(3).atStartOfDay()
        )

        fun søknad(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            sendtSøknad: LocalDateTime = tom.plusDays(5).atTime(LocalTime.NOON),
            perioder: List<Søknad.Søknadsperiode> = listOf(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent))
        ) = Søknad(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = perioder,
            andreInntektskilder = emptyList(),
            sendtTilNAV = sendtSøknad,
            permittert = false
        )

        fun inntektsmelding(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate,
            perioder: List<Periode> = listOf(Periode(fom, fom.plusDays(15))),
            refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(null, 31000.månedlig, emptyList())
        ) = Inntektsmelding(
            meldingsreferanseId = hendelseId,
            refusjon = refusjon,
            orgnummer = orgnummer,
            fødselsnummer = fnr,
            aktørId = aktørId,
            førsteFraværsdag = fom,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = perioder,
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )

        fun vilkårsgrunnlag(vedtaksperiodeId: String) = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(inntektperioder {
                1.januar(2018) til 1.desember(2018) inntekter {
                    orgnummer inntekt 31000.månedlig
                }
            }),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Opptjeningvurdering.Arbeidsforhold(
                        orgnummer,
                        1.januar(2017)
                    )
                )
            ),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja)
        )

        fun utbetalingshistorikk(
            utbetalinger: List<Utbetalingshistorikk.Infotrygdperiode> = emptyList(),
            inntektsopplysning: List<Utbetalingshistorikk.Inntektsopplysning> = emptyList()
        ) = Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalinger = utbetalinger,
            inntektshistorikk = inntektsopplysning
        )

        fun ytelser(hendelseId: UUID = UUID.randomUUID(), vedtaksperiodeId: String) = Aktivitetslogg().let {
            Ytelser(
                meldingsreferanseId = hendelseId,
                aktørId = aktørId,
                fødselsnummer = fnr,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingshistorikk = Utbetalingshistorikk(
                    meldingsreferanseId = hendelseId,
                    aktørId = aktørId,
                    fødselsnummer = fnr,
                    organisasjonsnummer = orgnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    utbetalinger = listOf(
                        Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver(
                            fom = 1.januar.minusYears(1),
                            tom = 31.januar.minusYears(1),
                            inntekt = 31000.månedlig,
                            grad = 100.prosent,
                            orgnummer = orgnummer
                        )
                    ),
                    inntektshistorikk = listOf(
                        Utbetalingshistorikk.Inntektsopplysning(1.januar.minusYears(1), 25000.månedlig, orgnummer, true)
                    ),
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
                pleiepenger = Pleiepenger(
                    perioder = emptyList(),
                    aktivitetslogg = it
                ),
                omsorgspenger = Omsorgspenger(
                    perioder = emptyList(),
                    aktivitetslogg = it
                ),
                opplæringspenger = Opplæringspenger(
                    perioder = emptyList(),
                    aktivitetslogg = it
                ),
                institusjonsopphold = Institusjonsopphold(
                    perioder = emptyList(),
                    aktivitetslogg = it
                ),
                aktivitetslogg = it,
                dødsinfo = Dødsinfo(null),
                arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
                dagpenger = Dagpenger(emptyList())
            )
        }

        fun Person.utbetalingsgodkjenning(vedtaksperiodeId: String) = Utbetalingsgodkjenning(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = UUID.fromString(this.aktivitetslogg.behov().last { it.type == Behovtype.Godkjenning }.kontekst().getValue("utbetalingId")),
            aktørId = aktørId,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            utbetalingGodkjent = true,
            saksbehandler = "en_saksbehandler_ident",
            godkjenttidspunkt = LocalDateTime.now(),
            automatiskBehandling = false,
            saksbehandlerEpost = "mille.mellomleder@nav.no",
            makstidOppnådd = false,
        )

        fun simulering(vedtaksperiodeId: String) = Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            simuleringOK = true,
            melding = "Hei Aron",
            simuleringResultat = null
        )

        fun annullering(fagsystemId: String) = AnnullerUtbetaling(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            saksbehandlerIdent = "Z999999",
            saksbehandlerEpost = "tbd@nav.no",
            opprettet = LocalDateTime.now(),
            fagsystemId = fagsystemId
        )

        fun Person.overføring(vedtaksperiodeId: String) = UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            fagsystemId = utbetalingsliste.getValue(orgnummer).last().arbeidsgiverOppdrag().fagsystemId(),
            utbetalingId = this.aktivitetslogg.behov().last { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )

        fun Person.utbetalt(vedtaksperiodeId: String) = UtbetalingHendelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            fagsystemId = utbetalingsliste.getValue(orgnummer).last().arbeidsgiverOppdrag().fagsystemId(),
            utbetalingId = this.aktivitetslogg.behov().last { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
            status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )
    }
}

