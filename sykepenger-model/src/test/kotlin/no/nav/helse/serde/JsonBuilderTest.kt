package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.*
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.*
import java.util.*

class JsonBuilderTest {

    private val objectMapper = jacksonObjectMapper()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .setMixIns(
            mutableMapOf(
                Arbeidsgiver::class.java to ArbeidsgiverMixin::class.java,
                Vedtaksperiode::class.java to VedtaksperiodeMixin::class.java,
                Utbetaling::class.java to UtbetalingMixin::class.java,
            )
        )
        .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
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
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
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
    fun `Lagrer dødsdato på person`() {
        val fom = 1.januar
        val tom = 31.januar
        val dødPerson = Person(aktørId, fnr).apply {
            håndter(sykmelding(fom = fom, tom = tom))
            fangeVedtaksperiode()
            håndter(
                søknad(
                    hendelseId = UUID.randomUUID(),
                    fom = fom,
                    tom = tom,
                    sendtSøknad = tom.atStartOfDay()
                )
            )
            fangeSykdomstidslinje()
            håndter(inntektsmelding(fom = fom))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId, dødsdato = 1.januar))
            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId, dødsdato = 1.januar))
        }
        testSerialiseringAvPerson(dødPerson)
    }

    @Test
    fun `Person med infotrygdforlengelse`() {
        testSerialiseringAvPerson(personMedInfotrygdForlengelse())
    }

    @Test
    fun `Serialisering av feriepenger`() {
        Toggles.SendFeriepengeOppdrag.enable {
            testSerialiseringAvPerson(personMedFeriepenger())
        }
    }

    @Test
    fun `Skal ikke serialisere feriepenger når toggle er disabled`() {
        Toggles.SendFeriepengeOppdrag.disable {
            val søknadhendelseId = UUID.randomUUID()

            val personMedFeriepenger = personMedFeriepenger(søknadhendelseId = søknadhendelseId)

            val jsonBuilder = JsonBuilder()
            personMedFeriepenger.accept(jsonBuilder)
            val json = jsonBuilder.toString()

            val result = SerialisertPerson(json).deserialize()
            val jsonBuilder2 = JsonBuilder()
            result.accept(jsonBuilder2)
            val json2 = jsonBuilder2.toString()

            objectMapper.readTree(json).also {
                assertFalse(it.path("arbeidsgivere").first().hasNonNull("ferieutbetalinger"))
            }
            assertEquals(json, json2)
        }
    }

    @Test
    fun `Skal serialisere ukjentdager på sykdomstidslinjen til ghost`() =
        Toggles.FlereArbeidsgivereUlikFom.enable {
            testSerialiseringAvPerson(personMedGhost())
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
                håndter(utbetalingsgrunnlag(vedtaksperiodeId = UUID.fromString(vedtaksperiodeId), skjæringstidspunkt = fom.plusDays(15)))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
                fangeUtbetalinger()
                håndter(overføring())
                håndter(utbetalt())
                fangeVedtaksperiode()
            }

        fun personMedLiteGap() = person(fom = 1.januar, tom = 20.januar).apply {
            håndter(sykmelding(fom = 1.februar, tom = 10.februar))
            fangeVedtaksperiode()
            håndter(søknad(fom = 1.februar, tom = 10.februar))
            håndter(utbetalingshistorikk())
            håndter(utbetalingsgrunnlag(vedtaksperiodeId = UUID.fromString(vedtaksperiodeId), skjæringstidspunkt = 16.januar))
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
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
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
                            Søknad.Søknadsperiode.Sykdom(1.januar, 9.januar, 100.prosent)
                        ),
                        hendelseId = søknadhendelseId,
                        andreInntektsKilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD"))
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
                håndter(utbetalingsgrunnlag(vedtaksperiodeId = UUID.fromString(vedtaksperiodeId), skjæringstidspunkt = fom.plusDays(15)))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
                fangeUtbetalinger()
                håndter(overføring())
                håndter(utbetalt())
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

        fun personMedInfotrygdForlengelse(søknadhendelseId: UUID = UUID.randomUUID()): Person {
            val refusjoner = listOf(ArbeidsgiverUtbetalingsperiode(orgnummer, 1.desember(2017), 31.desember(2017), 100.prosent, 31000.månedlig))
            val inntektshistorikk = listOf(Inntektsopplysning(orgnummer, 1.desember(2017), 31000.månedlig, true))
            return Person(aktørId, fnr).apply {
                håndter(sykmelding(fom = 1.januar, tom = 31.januar))
                fangeVedtaksperiode()
                håndter(søknad(fom = 1.januar, tom = 31.januar, hendelseId = søknadhendelseId))
                håndter(utbetalingshistorikk(refusjoner, inntektshistorikk))
                håndter(utbetalingsgrunnlag(vedtaksperiodeId = UUID.fromString(vedtaksperiodeId), skjæringstidspunkt = 1.desember))
                håndter(
                    ytelser(
                        hendelseId = søknadhendelseId,
                        vedtaksperiodeId = vedtaksperiodeId,
                        inntektshistorikk = listOf(Inntektsopplysning(orgnummer, 1.desember(2017), 31000.månedlig, true)),
                        utbetalinger = refusjoner
                    )
                )
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
                fangeUtbetalinger()
                håndter(overføring())
                håndter(utbetalt())
            }
        }

        fun personMedFeriepenger(
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
                håndter(utbetalingsgrunnlag(vedtaksperiodeId = UUID.fromString(vedtaksperiodeId), skjæringstidspunkt = fom.plusDays(15)))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
                fangeUtbetalinger()
                håndter(overføring())
                håndter(utbetalt())
                fangeVedtaksperiode()
                håndter(
                    utbetalingshistorikkForFeriepenger(
                        opptjeningsår = Year.of(2018),
                        utbetalinger = listOf(
                            Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                                orgnummer,
                                1.mars,
                                31.mars,
                                1431,
                                31.mars
                            )
                        ),
                        feriepengehistorikk = listOf(Feriepenger(orgnummer, 3211, 1.mai(2019), 31.mai(2019)))
                    )
                )
                håndter(
                    utbetalingshistorikkForFeriepenger(
                        opptjeningsår = Year.of(2020),
                        utbetalinger = listOf(
                            Utbetalingsperiode.Arbeidsgiverutbetalingsperiode(
                                orgnummer,
                                1.februar(2020),
                                28.februar(2020),
                                1800,
                                28.februar(2020)
                            ),
                            Utbetalingsperiode.Personutbetalingsperiode(
                                "0",
                                1.mars(2020),
                                31.mars(2020),
                                1800,
                                31.mars(2020)
                            )
                        ),
                        feriepengehistorikk = listOf(Feriepenger(orgnummer, 3211, 1.mai(2021), 31.mai(2021)))
                    )
                )
            }

        fun personMedGhost(
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
                håndter(utbetalingsgrunnlag(
                    vedtaksperiodeId = UUID.fromString(vedtaksperiodeId),
                    skjæringstidspunkt = fom.plusDays(15),
                    arbeidsforhold = listOf(
                        Arbeidsforhold(orgnummer, LocalDate.EPOCH, null),
                        Arbeidsforhold("04201337", LocalDate.EPOCH, null)
                    ),
                    inntekter = inntektperioderForSykepengegrunnlag {
                        1.oktober(2017) til 1.desember(2017) inntekter {
                            orgnummer inntekt 31000.månedlig
                            "04201337" inntekt 32000.månedlig
                        }
                    }
                ))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(vilkårsgrunnlag(
                    vedtaksperiodeId = vedtaksperiodeId,
                    inntektperioderForSammenligningsgrunnlag {
                        1.januar(2017) til 1.desember(2017) inntekter {
                            orgnummer inntekt 31000.månedlig
                            "04201337" inntekt 32000.månedlig
                        }
                    }
                ))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
                fangeUtbetalinger()
                håndter(overføring())
                håndter(utbetalt())
                fangeVedtaksperiode()
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
                    skjæringstidspunkt: LocalDate,
                    periodetype: Periodetype,
                    forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
                    hendelseIder: Set<UUID>,
                    inntektsmeldingInfo: InntektsmeldingInfo?,
                    inntektskilde: Inntektskilde
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
            sykmeldingSkrevet = fom.atStartOfDay(),
            mottatt = tom.atStartOfDay()
        )

        fun søknad(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            sendtSøknad: LocalDateTime = tom.plusDays(5).atTime(LocalTime.NOON),
            perioder: List<Søknad.Søknadsperiode> = listOf(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent)),
            andreInntektsKilder: List<Søknad.Inntektskilde> = emptyList()
        ) = Søknad(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = perioder,
            andreInntektskilder = andreInntektsKilder,
            sendtTilNAV = sendtSøknad,
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
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
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )

        fun vilkårsgrunnlag(
            vedtaksperiodeId: String,
            inntektsvurdering: List<ArbeidsgiverInntekt> = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt 31000.månedlig
                }
            },
            inntektsvurderingForSykepengegrunnlag: List<ArbeidsgiverInntekt> = inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt 31000.månedlig
                }
            },
            arbeidsforhold: List<Arbeidsforhold> = listOf(
                Arbeidsforhold(
                    orgnummer,
                    1.januar(2017)
                )
            )
        ) = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(inntektsvurdering),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntektsvurderingForSykepengegrunnlag),
            opptjeningvurdering = Opptjeningvurdering(
               arbeidsforhold
            ),
            arbeidsforhold = arbeidsforhold,
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja)
        )

        fun utbetalingshistorikk(
            utbetalinger: List<Infotrygdperiode> = emptyList(),
            inntektsopplysning: List<Inntektsopplysning> = emptyList()
        ) = Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            arbeidskategorikoder = emptyMap(),
            harStatslønn = false,
            perioder = utbetalinger,
            inntektshistorikk = inntektsopplysning,
            ugyldigePerioder = emptyList(),
            besvart = LocalDateTime.now()
        )

        private fun utbetalingsgrunnlag(
            vedtaksperiodeId: UUID = UUID.randomUUID(),
            skjæringstidspunkt: LocalDate,
            inntekter: List<ArbeidsgiverInntekt> = listOf(
                ArbeidsgiverInntekt(orgnummer, (0..3).map {
                    val yearMonth = YearMonth.from(skjæringstidspunkt).minusMonths(3L - it)
                    ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                        yearMonth = yearMonth,
                        type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                        inntekt = 31000.månedlig,
                        fordel = "juicy fordel",
                        beskrivelse = "juicy beskrivelse"
                    )
                })
            ),
            arbeidsforhold: List<Arbeidsforhold> = listOf(Arbeidsforhold(orgnummer, 1.januar, null))
        ) = Utbetalingsgrunnlag(
            UUID.randomUUID(),
            AbstractPersonTest.AKTØRID, AbstractPersonTest.UNG_PERSON_FNR_2018,
            orgnummer,
            vedtaksperiodeId,
            InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold
        )


        fun ytelser(
            hendelseId: UUID = UUID.randomUUID(),
            vedtaksperiodeId: String,
            dødsdato: LocalDate? = null,
            inntektshistorikk: List<Inntektsopplysning> = emptyList(),
            utbetalinger: List<Infotrygdperiode> = emptyList()
        ) = Aktivitetslogg().let {
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
                    arbeidskategorikoder = emptyMap(),
                    harStatslønn = false,
                    perioder = utbetalinger,
                    inntektshistorikk = inntektshistorikk,
                    ugyldigePerioder = emptyList(),
                    aktivitetslogg = it,
                    besvart = LocalDateTime.now()
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
                dødsinfo = Dødsinfo(dødsdato),
                arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
                dagpenger = Dagpenger(emptyList()),
                aktivitetslogg = it
            )
        }

        private fun utbetalingshistorikkForFeriepenger(
            utbetalinger: List<Utbetalingsperiode> = listOf(),
            feriepengehistorikk: List<Feriepenger> = listOf(),
            opptjeningsår: Year = Year.of(2017),
            skalBeregnesManuelt: Boolean = false
        ): UtbetalingshistorikkForFeriepenger {
            return UtbetalingshistorikkForFeriepenger(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = aktørId,
                fødselsnummer = fnr,
                utbetalinger = utbetalinger,
                feriepengehistorikk = feriepengehistorikk,
                opptjeningsår = opptjeningsår,
                skalBeregnesManuelt = skalBeregnesManuelt,
                arbeidskategorikoder = Arbeidskategorikoder(
                    listOf(KodePeriode(LocalDate.MIN til LocalDate.MAX, Arbeidskategorikode.Arbeidstaker))
                )
            )
        }

        fun Person.utbetalingsgodkjenning(vedtaksperiodeId: String) = Utbetalingsgodkjenning(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            utbetalingId = UUID.fromString(this.aktivitetslogg.behov().last { it.type == Behovtype.Godkjenning }.kontekst().getValue("utbetalingId")),
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandler = "en_saksbehandler_ident",
            saksbehandlerEpost = "mille.mellomleder@nav.no",
            utbetalingGodkjent = true,
            godkjenttidspunkt = LocalDateTime.now(),
            automatiskBehandling = false,
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

        fun Person.overføring() = UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            fagsystemId = utbetalingsliste.getValue(orgnummer).last().arbeidsgiverOppdrag().fagsystemId(),
            utbetalingId = this.aktivitetslogg.behov().last { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )

        fun Person.utbetalt() = UtbetalingHendelse(
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

