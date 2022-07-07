package no.nav.helse.serde

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Year
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Dødsinfo
import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.Arbeidskategorikode
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder.KodePeriode
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Feriepenger
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger.Utbetalingsperiode
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.UgyldigPeriode
import no.nav.helse.sisteBehov
import no.nav.helse.somFødselsnummer
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class JsonBuilderTest {
    companion object {
        private const val aktørId = "12345"
        private val fnr = "12029240045".somFødselsnummer()
        private val orgnummer = "987654321"
    }

    private val person get() = Person(aktørId, fnr, fnr.alder(), MaskinellJurist())

    @Test
    fun `gjenoppbygd Person skal være lik opprinnelig Person - The Jackson Way`() {
        val person = person()
        assertEquals(TilstandType.AVSLUTTET, tilstand)
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val personPost = SerialisertPerson(jsonBuilder.toString())
            .deserialize(MaskinellJurist())

        assertJsonEquals(person, personPost)
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
        val dødPerson = person.apply {
            håndter(sykmelding(fom = fom, tom = tom))
            håndter(
                søknad(
                    hendelseId = UUID.randomUUID(),
                    fom = fom,
                    tom = tom,
                    sendtSøknad = tom.atStartOfDay()
                )
            )
            fangeSykdomstidslinje()
            fangeVedtaksperiode()
            håndter(inntektsmelding(fom = fom))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId, dødsdato = 1.januar))
            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId, dødsdato = 1.januar))
        }
        testSerialiseringAvPerson(dødPerson)
    }

    @Disabled
    @Test
    fun `Person med infotrygdforlengelse`() {
        testSerialiseringAvPerson(personMedInfotrygdForlengelse())
    }

    @Test
    fun `Serialisering av feriepenger`() {
        Toggle.SendFeriepengeOppdrag.enable {
            testSerialiseringAvPerson(personMedFeriepenger())
        }
    }

    @Test
    fun `Skal ikke serialisere feriepenger når toggle er disabled`() {
        Toggle.SendFeriepengeOppdrag.disable {
            val søknadhendelseId = UUID.randomUUID()

            val personMedFeriepenger = personMedFeriepenger(søknadhendelseId = søknadhendelseId)

            val jsonBuilder = JsonBuilder()
            personMedFeriepenger.accept(jsonBuilder)
            val json = jsonBuilder.toString()

            val result = SerialisertPerson(json).deserialize(MaskinellJurist())
            val jsonBuilder2 = JsonBuilder()
            result.accept(jsonBuilder2)
            val json2 = jsonBuilder2.toString()

            serdeObjectMapper.readTree(json).also {
                assertFalse(it.path("arbeidsgivere").first().hasNonNull("ferieutbetalinger"))
            }
            assertEquals(json, json2)
        }
    }

    @Test
    fun `Skal serialisere ukjentdager på sykdomstidslinjen til ghost`() =
        testSerialiseringAvPerson(personMedGhost())

    @Test
    fun `Skal serialisere ghost uten inntekt`() =
        testSerialiseringAvPerson(personMedGhostUtenInntekt())

    @Test
    fun `Serialisering av ugyldig periode i infotrygdhistorikk`() = testSerialiseringAvPerson(personMedUgyldigPeriodeIHistorikken())

    private fun testSerialiseringAvPerson(person: Person) {
        val jsonBuilder = JsonBuilder()
        person.accept(jsonBuilder)
        val json = jsonBuilder.toString()

        val result = SerialisertPerson(json).deserialize(MaskinellJurist())
        val jsonBuilder2 = JsonBuilder()
        result.accept(jsonBuilder2)
        val json2 = jsonBuilder2.toString()

        serdeObjectMapper.readTree(json).also {
            assertTrue(it.hasNonNull("skjemaVersjon"))
            assertEquals(SerialisertPerson.gjeldendeVersjon(), it["skjemaVersjon"].intValue())
        }
        assertEquals(json, json2)
        assertJsonEquals(person, result)
    }

    @Test
    fun `En forlengelse med ny tilstandsflyt`() {
        val person = person.apply {
            håndter(sykmelding(fom = 1.januar, tom = 31.januar))
            håndter(
                søknad(
                    hendelseId = UUID.randomUUID(),
                    fom = 1.januar,
                    tom = 31.januar,
                    sendtSøknad = 1.februar.atStartOfDay()
                )
            )
        }
        testSerialiseringAvPerson(person)
    }

    @Test
    fun `Person med ny tilstandsflyt som venter på inntektsmelding`() {
        val person = person.apply {
            håndter(sykmelding(fom = 1.januar, tom = 31.januar))
            håndter(
                søknad(
                    hendelseId = UUID.randomUUID(),
                    fom = 1.januar,
                    tom = 31.januar,
                    sendtSøknad = 1.februar.atStartOfDay()
                )
            )

            håndter(sykmelding(fom = 1.februar, tom = 28.februar))
            håndter(
                søknad(
                    hendelseId = UUID.randomUUID(),
                    fom = 1.februar,
                    tom = 28.februar,
                    sendtSøknad = 1.mars.atStartOfDay()
                )
            )
            håndter(inntektsmelding(fom = 1.januar))
        }
        testSerialiseringAvPerson(person)
    }

    private lateinit var vedtaksperiodeId: String
    private lateinit var tilstand: TilstandType
    private lateinit var sykdomstidslinje: Sykdomstidslinje
    private val utbetalingsliste: MutableMap<String, List<Utbetaling>> = mutableMapOf()

    private fun person(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        sendtSøknad: LocalDate = 1.april,
        søknadhendelseId: UUID = UUID.randomUUID()
    ): Person =
        person.apply {
            håndter(sykmelding(fom = fom, tom = tom))
            håndter(
                søknad(
                    hendelseId = søknadhendelseId,
                    fom = fom,
                    tom = tom,
                    sendtSøknad = sendtSøknad.atStartOfDay()
                )
            )
            fangeSykdomstidslinje()
            fangeVedtaksperiode()
            håndter(inntektsmelding(fom = fom))
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

    private fun personMedGhostUtenInntekt(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        sendtSøknad: LocalDate = 1.april,
        søknadhendelseId: UUID = UUID.randomUUID()
    ): Person =
        person.apply {
            håndter(sykmelding(fom = fom, tom = tom))
            håndter(
                søknad(
                    hendelseId = søknadhendelseId,
                    fom = fom,
                    tom = tom,
                    sendtSøknad = sendtSøknad.atStartOfDay()
                )
            )
            fangeVedtaksperiode()
            fangeSykdomstidslinje()
            håndter(inntektsmelding(fom = fom))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
            håndter(
                vilkårsgrunnlag(
                    vedtaksperiodeId = vedtaksperiodeId,
                    arbeidsforhold = listOf(
                        Vilkårsgrunnlag.Arbeidsforhold(orgnummer, 1.januar(2017)),
                        Vilkårsgrunnlag.Arbeidsforhold("987654326", 1.desember(2017))
                    )
                )
            )
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
            håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
            håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
            fangeUtbetalinger()
            håndter(overføring())
            håndter(utbetalt())
            fangeVedtaksperiode()
        }

    private fun ingenBetalingsperson(
        sendtSøknad: LocalDate = 1.april,
        søknadhendelseId: UUID = UUID.randomUUID()
    ): Person =
        person.apply {
            håndter(sykmelding(fom = 1.januar, tom = 9.januar))
            håndter(
                søknad(
                    fom = 1.januar,
                    tom = 9.januar,
                    sendtSøknad = sendtSøknad.atStartOfDay(),
                    hendelseId = søknadhendelseId
                )
            )
            fangeVedtaksperiode()
            håndter(inntektsmelding(fom = 1.januar))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
        }

    private fun forkastedeVedtaksperioderperson(
        sendtSøknad: LocalDate = 1.april,
        søknadhendelseId: UUID = UUID.randomUUID()
    ): Person =
        person.apply {
            håndter(sykmelding(fom = 1.januar, tom = 9.januar))
            håndter(
                søknad(
                    fom = 1.januar,
                    tom = 9.januar,
                    sendtSøknad = sendtSøknad.atStartOfDay(),
                    perioder = listOf(
                        Sykdom(1.januar, 9.januar, 100.prosent)
                    ),
                    hendelseId = søknadhendelseId,
                    andreInntektsKilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD"))
                )
            )
        }

    private fun friskeHelgedagerPerson(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        sendtSøknad: LocalDate = 1.april,
        søknadhendelseId: UUID = UUID.randomUUID()
    ): Person =
        person.apply {
            håndter(sykmelding(fom = fom, tom = tom))
            håndter(
                søknad(
                    hendelseId = søknadhendelseId,
                    fom = fom,
                    tom = tom,
                    sendtSøknad = sendtSøknad.atStartOfDay()
                )
            )
            fangeVedtaksperiode()
            håndter(
                inntektsmelding(
                    fom = fom,
                    perioder = listOf(Periode(fom, 4.januar), Periode(8.januar, 16.januar))
                )
            )
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
            håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
            håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId))
            fangeUtbetalinger()
            håndter(overføring())
            håndter(utbetalt())
        }

    private fun refusjonOpphørerPerson(
        søknadhendelseId: UUID = UUID.randomUUID()
    ): Person =
        person.apply {
            håndter(sykmelding(fom = 1.januar, tom = 9.januar))
            håndter(
                søknad(
                    fom = 1.januar,
                    tom = 9.januar,
                    hendelseId = søknadhendelseId
                )
            )
            fangeVedtaksperiode()
            håndter(inntektsmelding(fom = 1.januar, refusjon = Inntektsmelding.Refusjon(31000.månedlig, 4.januar, emptyList())))
        }

    private fun personMedInfotrygdForlengelse(søknadhendelseId: UUID = UUID.randomUUID()): Person {
        val refusjoner = listOf(ArbeidsgiverUtbetalingsperiode(orgnummer, 1.desember(2017), 31.desember(2017), 100.prosent, 31000.månedlig))
        val inntektshistorikk = listOf(Inntektsopplysning(orgnummer, 1.desember(2017), 31000.månedlig, true))
        return person.apply {
            håndter(sykmelding(fom = 1.januar, tom = 31.januar))
            håndter(søknad(fom = 1.januar, tom = 31.januar, hendelseId = søknadhendelseId))
            fangeVedtaksperiode()
            håndter(utbetalingshistorikk(refusjoner, inntektshistorikk))
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

    private fun personMedUgyldigPeriodeIHistorikken(søknadhendelseId: UUID = UUID.randomUUID()): Person {
        val refusjoner = listOf(ArbeidsgiverUtbetalingsperiode(orgnummer, 1.desember(2017), 24.desember(2017), 100.prosent, 31000.månedlig))
        val inntektshistorikk = listOf(Inntektsopplysning(orgnummer, 1.desember(2017), 31000.månedlig, true))
        val ugyldigePerioder = listOf(UgyldigPeriode(1.mai(2017), 20.mai(2017), 0), UgyldigPeriode(1.februar(2017), 31.januar(2017), 100))
        return person.apply {
            håndter(sykmelding(fom = 1.januar, tom = 31.januar))
            håndter(søknad(fom = 1.januar, tom = 31.januar, hendelseId = søknadhendelseId))
            fangeVedtaksperiode()
            håndter(utbetalingshistorikk(refusjoner, inntektshistorikk, ugyldigePerioder))
        }
    }

    private fun personMedFeriepenger(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        sendtSøknad: LocalDate = 1.april,
        søknadhendelseId: UUID = UUID.randomUUID()
    ): Person =
        person.apply {
            håndter(sykmelding(fom = fom, tom = tom))
            håndter(
                søknad(
                    hendelseId = søknadhendelseId,
                    fom = fom,
                    tom = tom,
                    sendtSøknad = sendtSøknad.atStartOfDay()
                )
            )
            fangeSykdomstidslinje()
            fangeVedtaksperiode()
            håndter(inntektsmelding(fom = fom))
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
                        ),
                        Utbetalingsperiode.Personutbetalingsperiode(
                            orgnummer,
                            1.april,
                            25.april,
                            1800,
                            30.april
                        ),
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

    private fun personMedGhost(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        sendtSøknad: LocalDate = 1.april,
        søknadhendelseId: UUID = UUID.randomUUID()
    ): Person =
        person.apply {
            håndter(sykmelding(fom = fom, tom = tom))
            håndter(
                søknad(
                    hendelseId = søknadhendelseId,
                    fom = fom,
                    tom = tom,
                    sendtSøknad = sendtSøknad.atStartOfDay()
                )
            )
            fangeSykdomstidslinje()
            fangeVedtaksperiode()
            håndter(inntektsmelding(fom = fom))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
            håndter(
                vilkårsgrunnlag(
                    vedtaksperiodeId = vedtaksperiodeId,
                    inntektsvurdering = inntektperioderForSammenligningsgrunnlag {
                        1.januar(2017) til 1.desember(2017) inntekter {
                            orgnummer inntekt 31000.månedlig
                            "654321987" inntekt 32000.månedlig
                        }
                    },
                    inntektsvurderingForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
                        1.oktober(2017) til 1.desember(2017) inntekter {
                            orgnummer inntekt 31000.månedlig
                            "654321987" inntekt 32000.månedlig
                        }
                    },
                    arbeidsforhold = listOf(
                        Vilkårsgrunnlag.Arbeidsforhold(orgnummer, LocalDate.EPOCH, null),
                        Vilkårsgrunnlag.Arbeidsforhold("654321987", LocalDate.EPOCH, null)
                    )
                )
            )
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
                periodetype: () -> Periodetype,
                skjæringstidspunkt: () -> LocalDate,
                skjæringstidspunktFraInfotrygd: LocalDate?,
                forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
                hendelseIder: Set<Dokumentsporing>,
                inntektsmeldingInfo: InntektsmeldingInfo?,
                inntektskilde: Inntektskilde
            ) {
                vedtaksperiodeId = id.toString()
                this@JsonBuilderTest.tilstand = tilstand.type
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

    private fun sykmelding(
        hendelseId: UUID = UUID.randomUUID(),
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar
    ) = Sykmelding(
        meldingsreferanseId = hendelseId,
        fnr = fnr.toString(),
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = listOf(Sykmeldingsperiode(fom, tom, 100.prosent)),
        sykmeldingSkrevet = fom.atStartOfDay(),
        mottatt = tom.atStartOfDay()
    )

    private fun søknad(
        hendelseId: UUID = UUID.randomUUID(),
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        sendtSøknad: LocalDateTime = tom.plusDays(5).atTime(LocalTime.NOON),
        perioder: List<Søknad.Søknadsperiode> = listOf(Sykdom(fom, tom, 100.prosent)),
        andreInntektsKilder: List<Søknad.Inntektskilde> = emptyList()
    ) = Søknad(
        meldingsreferanseId = hendelseId,
        fnr = fnr.toString(),
        aktørId = aktørId,
        orgnummer = orgnummer,
        perioder = perioder,
        andreInntektskilder = andreInntektsKilder,
        sendtTilNAVEllerArbeidsgiver = sendtSøknad,
        permittert = false,
        merknaderFraSykmelding = emptyList(),
        sykmeldingSkrevet = LocalDateTime.now()
    )

    private fun inntektsmelding(
        hendelseId: UUID = UUID.randomUUID(),
        fom: LocalDate,
        perioder: List<Periode> = listOf(Periode(fom, fom.plusDays(15))),
        refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList())
    ) = Inntektsmelding(
        meldingsreferanseId = hendelseId,
        refusjon = refusjon,
        orgnummer = orgnummer,
        fødselsnummer = fnr.toString(),
        aktørId = aktørId,
        førsteFraværsdag = fom,
        beregnetInntekt = 31000.månedlig,
        arbeidsgiverperioder = perioder,
        arbeidsforholdId = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt = null,
        mottatt = LocalDateTime.now()
    )

    private fun vilkårsgrunnlag(
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
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold> = listOf(Vilkårsgrunnlag.Arbeidsforhold(orgnummer, 1.januar(2017)))
    ) = Vilkårsgrunnlag(
        meldingsreferanseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        aktørId = aktørId,
        fødselsnummer = fnr,
        orgnummer = orgnummer,
        inntektsvurdering = Inntektsvurdering(inntektsvurdering),
        medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
        inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntektsvurderingForSykepengegrunnlag, arbeidsforhold = emptyList()),
        arbeidsforhold = arbeidsforhold
    )

    private fun utbetalingshistorikk(
        utbetalinger: List<Infotrygdperiode> = emptyList(),
        inntektsopplysning: List<Inntektsopplysning> = emptyList(),
        ugyldigePerioder: List<UgyldigPeriode> = emptyList(),
        besvart: LocalDateTime = LocalDateTime.now()
    ) = Utbetalingshistorikk(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fnr.toString(),
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        element = InfotrygdhistorikkElement.opprett(
            oppdatert = besvart,
            hendelseId = UUID.randomUUID(),
            perioder = utbetalinger,
            inntekter = inntektsopplysning,
            arbeidskategorikoder = emptyMap(),
            ugyldigePerioder = ugyldigePerioder,
            harStatslønn = false
        )
    )

    private fun ytelser(
        hendelseId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: String,
        dødsdato: LocalDate? = null,
        inntektshistorikk: List<Inntektsopplysning> = emptyList(),
        utbetalinger: List<Infotrygdperiode> = emptyList(),
        ugyldigePerioder: List<UgyldigPeriode> = emptyList()
    ) = Aktivitetslogg().let {
        Ytelser(
            meldingsreferanseId = hendelseId,
            aktørId = aktørId,
            fødselsnummer = fnr.toString(),
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            infotrygdhistorikk = InfotrygdhistorikkElement.opprett(
                oppdatert = LocalDateTime.now(),
                hendelseId = hendelseId,
                perioder = utbetalinger,
                inntekter = inntektshistorikk,
                arbeidskategorikoder = emptyMap(),
                ugyldigePerioder = ugyldigePerioder,
                harStatslønn = false
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = Periode(
                    fom = 1.januar.minusYears(2),
                    tom = 31.januar.minusYears(2)
                ),
                svangerskapsytelse = Periode(
                    fom = 1.juli.minusYears(2),
                    tom = 31.juli.minusYears(2)
                )
            ),
            pleiepenger = Pleiepenger(
                perioder = emptyList()
            ),
            omsorgspenger = Omsorgspenger(
                perioder = emptyList()
            ),
            opplæringspenger = Opplæringspenger(
                perioder = emptyList()
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = emptyList()
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
            fødselsnummer = fnr.toString(),
            utbetalinger = utbetalinger,
            feriepengehistorikk = feriepengehistorikk,
            opptjeningsår = opptjeningsår,
            skalBeregnesManuelt = skalBeregnesManuelt,
            arbeidskategorikoder = Arbeidskategorikoder(
                listOf(KodePeriode(LocalDate.MIN til LocalDate.MAX, Arbeidskategorikode.Arbeidstaker))
            )
        )
    }

    private fun Person.utbetalingsgodkjenning(vedtaksperiodeId: String) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fnr.toString(),
        organisasjonsnummer = orgnummer,
        utbetalingId = UUID.fromString(this.aktivitetslogg.behov().last { it.type == Behovtype.Godkjenning }.kontekst().getValue("utbetalingId")),
        vedtaksperiodeId = vedtaksperiodeId,
        saksbehandler = "en_saksbehandler_ident",
        saksbehandlerEpost = "mille.mellomleder@nav.no",
        utbetalingGodkjent = true,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = false,
    )

    private fun Person.simulering(vedtaksperiodeId: String) = Simulering(
        meldingsreferanseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        aktørId = aktørId,
        fødselsnummer = fnr.toString(),
        orgnummer = orgnummer,
        fagsystemId = personLogg.sisteBehov(Behovtype.Simulering).detaljer().getValue("fagsystemId") as String,
        fagområde = personLogg.sisteBehov(Behovtype.Simulering).detaljer().getValue("fagområde") as String,
        simuleringOK = true,
        melding = "Hei Aron",
        utbetalingId = UUID.fromString(personLogg.sisteBehov(Behovtype.Simulering).kontekst().getValue("utbetalingId")),
        simuleringResultat = Simulering.SimuleringResultat(
            totalbeløp = 1000,
            perioder = emptyList()
        )
    )

    private fun annullering(fagsystemId: String) = AnnullerUtbetaling(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fnr.toString(),
        organisasjonsnummer = orgnummer,
        saksbehandlerIdent = "Z999999",
        saksbehandlerEpost = "tbd@nav.no",
        opprettet = LocalDateTime.now(),
        fagsystemId = fagsystemId
    )

    private fun Person.overføring() = UtbetalingOverført(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fnr.toString(),
        orgnummer = orgnummer,
        fagsystemId = utbetalingsliste.getValue(orgnummer).last().inspektør.arbeidsgiverOppdrag.fagsystemId(),
        utbetalingId = this.aktivitetslogg.behov().last { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
        avstemmingsnøkkel = 123456L,
        overføringstidspunkt = LocalDateTime.now()
    )

    private fun Person.utbetalt() = UtbetalingHendelse(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fnr.toString(),
        orgnummer = orgnummer,
        fagsystemId = utbetalingsliste.getValue(orgnummer).last().inspektør.arbeidsgiverOppdrag.fagsystemId(),
        utbetalingId = this.aktivitetslogg.behov().last { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
        status = Oppdragstatus.AKSEPTERT,
        melding = "hei",
        avstemmingsnøkkel = 123456L,
        overføringstidspunkt = LocalDateTime.now()
    )
}

