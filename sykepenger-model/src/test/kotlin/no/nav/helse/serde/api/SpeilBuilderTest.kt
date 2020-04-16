package no.nav.helse.serde.api

import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.*


internal class SpeilBuilderTest {
    private val aktørId = "1234"
    private val fnr = "12020052345"

    @Test
    internal fun `dager før førsteFraværsdag og etter sisteSykedag skal kuttes vekk fra utbetalingstidslinje`() {
        val (person, hendelser) = person()
        val personDTO = serializePersonForSpeil(person, hendelser)
        assertEquals(
            1.januar,
            (personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO).utbetalingstidslinje.first().dato
        )
        assertEquals(
            31.januar,
            (personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO).utbetalingstidslinje.last().dato
        )
    }

    @Test
    internal fun `person uten utbetalingsdager`() {
        val (person, hendelser) = ingenBetalingsperson()
        val personDTO = serializePersonForSpeil(person, hendelser)

        assertEquals(
            1.januar,
            (personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO).utbetalingstidslinje.first().dato
        )
        assertEquals(
            9.januar,
            (personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO).utbetalingstidslinje.last().dato
        )
    }

    @Test
    internal fun `person med foreldet dager`() {
        val (person, hendelser) = person(sendtSøknad = 1.juni)
        val personDTO = serializePersonForSpeil(person, hendelser)

        assertEquals(1, personDTO.arbeidsgivere.first().vedtaksperioder.size)

        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        val utbetalingstidslinje = vedtaksperiode.utbetalingstidslinje
        assertEquals(TypeDataDTO.ArbeidsgiverperiodeDag, utbetalingstidslinje.first().type)
        assertEquals(TypeDataDTO.ArbeidsgiverperiodeDag, utbetalingstidslinje[15].type)
        assertEquals(TypeDataDTO.ForeldetDag, utbetalingstidslinje[16].type)
        assertEquals(TypeDataDTO.ForeldetDag, utbetalingstidslinje.last().type)

        val sykdomstidslinje = vedtaksperiode.sykdomstidslinje
        assertEquals(JsonDagType.KUN_ARBEIDSGIVER_SYKEDAG, sykdomstidslinje.first().type)
        assertEquals(JsonDagType.KUN_ARBEIDSGIVER_SYKEDAG, sykdomstidslinje.last().type)
    }

    @Test
    fun `ufullstendig vedtaksperiode når tilstand er Venter`() {
        val (person, hendelser) = Person(aktørId, fnr).run {
            val (sykmelding, sykmeldingDTO) = sykmelding(fom = 1.januar, tom = 31.januar)
            håndter(sykmelding)
            this to listOf(sykmeldingDTO)
        }
        val personDTO = serializePersonForSpeil(person, hendelser)

        val arbeidsgiver = personDTO.arbeidsgivere[0]
        val vedtaksperioder = arbeidsgiver.vedtaksperioder

        assertFalse(vedtaksperioder.first().fullstendig)
    }

    @Test
    fun `passer på at vedtakene har alle hendelsene`() {
        var vedtaksperiodeIder: Set<String>

        val (person, hendelser) = Person(aktørId, fnr).run {
            this to mutableListOf<HendelseDTO>().apply {
                sykmelding(fom = 1.januar, tom = 31.januar).also { (sykmelding, sykmeldingDto) ->
                    håndter(sykmelding)
                    add(sykmeldingDto)
                }
                søknad(fom = 1.januar, tom = 31.januar).also { (søknad, søknadDTO) ->
                    håndter(søknad)
                    add(søknadDTO)
                }
                inntektsmelding(fom = 1.januar).also { (inntektsmelding, inntektsmeldingDTO) ->
                    håndter(inntektsmelding)
                    add(inntektsmeldingDTO)
                }

                vedtaksperiodeIder = collectVedtaksperiodeIder()

                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeIder.last()))

                sykmelding(fom = 1.februar, tom = 14.februar).also { (sykmelding, sykmeldingDto) ->
                    håndter(sykmelding)
                    add(sykmeldingDto)
                }
                søknad(fom = 1.februar, tom = 14.februar).also { (søknad, søknadDTO) ->
                    håndter(søknad)
                    add(søknadDTO)
                }

                vedtaksperiodeIder = collectVedtaksperiodeIder()

                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeIder.last()))
            }
        }

        val personDTO = serializePersonForSpeil(person, hendelser)

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()

        assertEquals(2, vedtaksperioder.size)
        assertEquals(3, vedtaksperioder.first().hendelser.size)
        assertEquals(2, vedtaksperioder.last().hendelser.size)
        assertEquals(
            hendelser.subList(0, 3).map { it.id }.sorted(),
            vedtaksperioder.first().hendelser.map { it.id }.sorted()
        )
        assertEquals(
            hendelser.subList(3, 5).map { it.id }.sorted(),
            vedtaksperioder.last().hendelser.map { it.id }.sorted()
        )
    }

    @Test
    fun `passer på at alle vedtak får fellesdata for sykefraværet`() {
        var vedtaksperiodeIder: Set<String>

        val (person, hendelser) = Person(aktørId, fnr).run {
            this to mutableListOf<HendelseDTO>().apply {
                sykmelding(fom = 1.januar, tom = 31.januar).also { (sykmelding, sykmeldingDto) ->
                    håndter(sykmelding)
                    add(sykmeldingDto)
                }
                søknad(fom = 1.januar, tom = 31.januar).also { (søknad, søknadDTO) ->
                    håndter(søknad)
                    add(søknadDTO)
                }
                inntektsmelding(fom = 1.januar).also { (inntektsmelding, inntektsmeldingDTO) ->
                    håndter(inntektsmelding)
                    add(inntektsmeldingDTO)
                }

                vedtaksperiodeIder = collectVedtaksperiodeIder()

                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeIder.last()))

                sykmelding(fom = 1.februar, tom = 14.februar).also { (sykmelding, sykmeldingDto) ->
                    håndter(sykmelding)
                    add(sykmeldingDto)
                }
                søknad(fom = 1.februar, tom = 14.februar).also { (søknad, søknadDTO) ->
                    håndter(søknad)
                    add(søknadDTO)
                }

                vedtaksperiodeIder = collectVedtaksperiodeIder()

                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeIder.last()))
            }
        }

        val personDTO = serializePersonForSpeil(person, hendelser)

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder as List<VedtaksperiodeDTO>

        assertEquals(2, vedtaksperioder.size)
        assertEquals(vedtaksperioder.first().gruppeId, vedtaksperioder.last().gruppeId)
        assertEquals(vedtaksperioder.first().dataForVilkårsvurdering, vedtaksperioder.last().dataForVilkårsvurdering)
        assertEquals(vedtaksperioder.first().vilkår.opptjening, vedtaksperioder.last().vilkår.opptjening)
    }

    @Test
    fun `hvis første vedtaksperiode er ferdigbehandlet arbeidsgiverperiode vises den som ferdigbehandlet`() {
        val (person, hendelser) = ingenutbetalingPåfølgendeBetaling()
        val personDTO = serializePersonForSpeil(person, hendelser)

        val vedtaksperiodeDTO = personDTO.arbeidsgivere[0].vedtaksperioder[1] as VedtaksperiodeDTO
        assertNotNull(vedtaksperiodeDTO.dataForVilkårsvurdering)
        assertNotNull(vedtaksperiodeDTO.vilkår.opptjening)
        assertTrue(personDTO.arbeidsgivere[0].vedtaksperioder[1].fullstendig)
//        This assertion will not pass until SpeilBuilder can map empoyers period correctly - https://trello.com/c/4FjMVwna")
//        assertTrue(personDTO.arbeidsgivere[0].vedtaksperioder[0].fullstendig)
    }

    /**
     * Test for å verifisere at kontrakten mellom Spleis og Speil opprettholdes.
     * Hvis du trenger å gjøre endringer i denne testen må du sannsynligvis også gjøre endringer i Speil.
     */
    @Test
    fun `personDTO-en inneholder de feltene Speil forventer`() {
        val fom = 1.januar
        val tom = 31.januar

        val (person, hendelser) = person(fom = fom, tom = tom, sendtSøknad = 1.februar)
        val personDTO = serializePersonForSpeil(person, hendelser)

        assertEquals("12020052345", personDTO.fødselsnummer)
        assertEquals("12345", personDTO.aktørId)
        assertEquals(1, personDTO.arbeidsgivere.size)

        val arbeidsgiver = personDTO.arbeidsgivere.first()
        assertEquals("987654321", arbeidsgiver.organisasjonsnummer)
        assertEquals(1, arbeidsgiver.vedtaksperioder.size)

        val vedtaksperiode = arbeidsgiver.vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1.januar, vedtaksperiode.fom)
        assertEquals(31.januar, vedtaksperiode.tom)
        assertEquals(TilstandstypeDTO.Utbetalt, vedtaksperiode.tilstand)
        assertTrue(vedtaksperiode.fullstendig)

        val utbetalingstidslinje = vedtaksperiode.utbetalingstidslinje
        assertEquals(31, utbetalingstidslinje.size)
        assertEquals(TypeDataDTO.ArbeidsgiverperiodeDag, utbetalingstidslinje.first().type)
        assertEquals(TypeDataDTO.NavDag, utbetalingstidslinje.last().type)
        assertEquals(100.0, (utbetalingstidslinje.last() as NavDagDTO).grad)

        assertEquals(15741, vedtaksperiode.totalbeløpArbeidstaker)

        val sykdomstidslinje = vedtaksperiode.sykdomstidslinje
        assertEquals(31, sykdomstidslinje.size)
        assertEquals(JsonDagType.SYKEDAG_SØKNAD, sykdomstidslinje.first().type)
        assertEquals(1.januar, sykdomstidslinje.first().dagen)

        assertEquals("en_saksbehandler_ident", vedtaksperiode.godkjentAv)

        val vilkår = vedtaksperiode.vilkår
        val sykepengegrunnlag = vilkår.sykepengegrunnlag
        assertEquals(`1G`.beløp(fom).toInt(), sykepengegrunnlag!!.grunnbeløp)
        assertTrue(sykepengegrunnlag.oppfylt!!)
        assertEquals(31000.0 * 12, sykepengegrunnlag.sykepengegrunnlag)

        val sykepengedager = vilkår.sykepengedager
        assertEquals(11, sykepengedager.forbrukteSykedager)
        assertEquals(fom, sykepengedager.førsteFraværsdag)
        assertEquals(fom.plusDays(16), sykepengedager.førsteSykepengedag)
        assertEquals(28.desember, sykepengedager.maksdato)
        assertEquals(237, sykepengedager.gjenståendeDager)
        assertTrue(sykepengedager.oppfylt!!)

        val alder = vilkår.alder
        assertEquals(17, alder.alderSisteSykedag)
        assertTrue(alder.oppfylt!!)

        val opptjening = vilkår.opptjening
        assertEquals(365, opptjening?.antallKjenteOpptjeningsdager)
        assertEquals(1.januar(2017), opptjening?.fom)
        assertTrue(opptjening?.oppfylt!!)

        val søknadsfrist = vilkår.søknadsfrist
        assertEquals(tom.plusDays(1).atStartOfDay(), søknadsfrist?.sendtNav)
        assertEquals(fom, søknadsfrist?.søknadFom)
        assertEquals(tom, søknadsfrist?.søknadTom)
        assertTrue(søknadsfrist!!.oppfylt)

        assertEquals(31000.0, vedtaksperiode.inntektFraInntektsmelding)
        assertEquals(3, vedtaksperiode.hendelser.size)

        assertEquals(372000.0, vedtaksperiode.dataForVilkårsvurdering?.beregnetÅrsinntektFraInntektskomponenten)
        assertEquals(0.0, vedtaksperiode.dataForVilkårsvurdering?.avviksprosent)

        vedtaksperiode.simuleringsdata?.let { simulering ->
            assertNotNull(simulering.totalbeløp)
            simulering.perioder.assertOnNonEmptyCollection { periode ->
                assertNotNull(periode.fom)
                assertNotNull(periode.tom)
                periode.utbetalinger.assertOnNonEmptyCollection { utbetaling ->
                    assertNotNull(utbetaling.utbetalesTilNavn)
                    utbetaling.detaljer.assertOnNonEmptyCollection { detalj ->
                        assertNotNull(detalj.beløp)
                        assertNotNull(detalj.konto)
                        assertNotNull(detalj.sats)
                        assertTrue(detalj.klassekodeBeskrivelse.isNotEmpty())
                    }
                }
            }
        }
    }

    @Test
    internal fun `Yes hello does this work?!`() {
        val fom = 1.januar(2018)
        val tom = 31.januar(2018)

        val (person, hendelser) = person(fom = fom, tom = tom,
        påfølgendePerioder = listOf(
            1.februar(2018).rangeTo(28.februar(2018)),
            1.mars(2018).rangeTo(31.mars(2018)),
            1.april(2018).rangeTo(30.april(2018)),
            1.mai(2018).rangeTo(31.mai(2018)),
            1.juni(2018).rangeTo(30.juni(2018)),
            1.juli(2018).rangeTo(31.juli(2018)),
            1.august(2018).rangeTo(31.august(2018)),
            1.september(2018).rangeTo(30.september(2018)),
            1.oktober(2018).rangeTo(31.oktober(2018)),
            1.november(2018).rangeTo(30.november(2018)),
            1.desember(2018).rangeTo(31.desember(2018)),
            1.januar(2019).rangeTo(31.januar(2019))
        ))
        person.aktivitetslogg.toString()
        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(0, vedtaksperiode.vilkår.sykepengedager.gjenståendeDager)
    }

    private fun <T> Collection<T>.assertOnNonEmptyCollection(func: (T) -> Unit) {
        assertTrue(isNotEmpty())
        forEach(func)
    }

    private fun Person.collectVedtaksperiodeIder() = mutableSetOf<String>().apply {
        accept(object : PersonVisitor {
            override fun preVisitVedtaksperiode(
                vedtaksperiode: Vedtaksperiode,
                id: UUID,
                gruppeId: UUID
            ) {
                add(id.toString())
            }
        })
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
            søknadhendelseId: UUID = UUID.randomUUID(),
            påfølgendePerioder: List<ClosedRange<LocalDate>> = emptyList()
        ): Pair<Person, List<HendelseDTO>> =
            Person(aktørId, fnr).run {
                this to mutableListOf<HendelseDTO>().apply {
                    sykmelding(fom = fom, tom = tom).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    fangeVedtaksperiodeId()
                    søknad(
                        hendelseId = søknadhendelseId,
                        fom = fom,
                        tom = tom,
                        sendtSøknad = sendtSøknad.atStartOfDay()
                    ).also { (søknad, søknadDTO) ->
                        håndter(søknad)
                        add(søknadDTO)
                    }
                    inntektsmelding(fom = fom).also { (inntektsmelding, inntektsmeldingDTO) ->
                        håndter(inntektsmelding)
                        add(inntektsmeldingDTO)
                    }
                    håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))

                    påfølgendePerioder.forEach { periode ->

                        sykmelding(fom = periode.start, tom = periode.endInclusive).also { (sykmelding, sykmeldingDTO) ->
                            håndter(sykmelding)
                            add(sykmeldingDTO)
                        }
                        fangeVedtaksperiodeId()
                        søknad(
                            hendelseId = søknadhendelseId,
                            fom = periode.start,
                            tom = periode.endInclusive,
                            sendtSøknad = periode.endInclusive.atStartOfDay()
                        ).also { (søknad, søknadDTO) ->
                            håndter(søknad)
                            add(søknadDTO)
                        }
                        håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                        håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                        håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                        håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeId))
                        håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
                    }
                }
            }

        internal fun ingenBetalingsperson(
            sendtSøknad: LocalDate = 1.april,
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Pair<Person, List<HendelseDTO>> =
            Person(aktørId, fnr).run {
                this to mutableListOf<HendelseDTO>().apply {
                    sykmelding(fom = 1.januar, tom = 9.januar).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    fangeVedtaksperiodeId()
                    søknad(
                        fom = 1.januar,
                        tom = 9.januar,
                        sendtSøknad = sendtSøknad.atStartOfDay(),
                        hendelseId = søknadhendelseId
                    ).also { (søknad, søknadDTO) ->
                        håndter(søknad)
                        add(søknadDTO)
                    }
                    inntektsmelding(fom = 1.januar).also { (inntektsmelding, inntektsmeldingDTO) ->
                        håndter(inntektsmelding)
                        add(inntektsmeldingDTO)
                    }
                    håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
                }
            }

        internal fun ingenutbetalingPåfølgendeBetaling(
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Pair<Person, List<HendelseDTO>> =
            Person(aktørId, fnr).run {
                this to mutableListOf<HendelseDTO>().apply {
                    sykmelding(fom = 1.januar, tom = 9.januar).also { (sykmelding, sykmeldingDto) ->
                        håndter(sykmelding)
                        add(sykmeldingDto)
                    }
                    fangeVedtaksperiodeId()
                    søknadSendtTilArbeidsgiver(
                        hendelseId = søknadhendelseId,
                        fom = 1.januar,
                        tom = 9.januar
                    ).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    sykmelding(fom = 10.januar, tom = 25.januar).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    søknad(fom = 10.januar, tom = 25.januar).also { (søknad, søknadDTO) ->
                        håndter(søknad)
                        add(søknadDTO)
                    }
                    inntektsmelding(fom = 1.januar).also { (inntektsmelding, inntektsmeldingDTO) ->
                        håndter(inntektsmelding)
                        add(inntektsmeldingDTO)
                    }
                    håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                    fangeVedtaksperiodeId()
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(manuellSaksbehandling(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalt(vedtaksperiodeId = vedtaksperiodeId))
                }
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
        ) to SykmeldingDTO(
            id = hendelseId.toString(),
            fom = fom,
            tom = tom,
            rapportertdato = fom.atStartOfDay()
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
        ) to SøknadNavDTO(
            id = hendelseId.toString(),
            fom = fom,
            tom = tom,
            rapportertdato = sendtSøknad,
            sendtNav = sendtSøknad
        )

        internal fun søknadSendtTilArbeidsgiver(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            sendtTilArbeidsgiver: LocalDateTime = tom.atStartOfDay()
        ) = SøknadArbeidsgiver(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = listOf(SøknadArbeidsgiver.Periode.Sykdom(fom, tom, 100, 100))
        ) to SøknadArbeidsgiverDTO(
            id = hendelseId.toString(),
            fom = fom,
            tom = tom,
            rapportertdato = tom.atStartOfDay(),
            sendtArbeidsgiver = sendtTilArbeidsgiver
        )

        internal fun inntektsmelding(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate
        ) = Inntektsmelding(
            meldingsreferanseId = hendelseId,
            refusjon = Inntektsmelding.Refusjon(
                opphørsdato = null,
                beløpPrMåned = 31000.00,
                endringerIRefusjon = emptyList()
            ),
            orgnummer = orgnummer,
            fødselsnummer = fnr,
            aktørId = aktørId,
            førsteFraværsdag = fom,
            beregnetInntekt = 31000.00,
            arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(15))),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ) to InntektsmeldingDTO(
            id = hendelseId.toString(),
            beregnetInntekt = 31000.00,
            mottattDato = fom.atStartOfDay()
        )

        internal fun vilkårsgrunnlag(vedtaksperiodeId: String) = Vilkårsgrunnlag(
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering((1..12)
                .map { YearMonth.of(2018, it) to 31000.0 }
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
            simuleringResultat = simuleringResultat()
        )

        private fun simuleringResultat() = Simulering.SimuleringResultat(
            totalbeløp = 9999,
            perioder = listOf(
                Simulering.SimulertPeriode(
                    Periode(1.januar(2020), 2.januar(2020)),
                    utbetalinger = listOf(
                        Simulering.SimulertUtbetaling(
                            forfallsdato = 3.januar(2020),
                            utbetalesTil = Simulering.Mottaker(id = orgnummer, navn = "Syk Nordmann"),
                            feilkonto = true,
                            detaljer = listOf(
                                Simulering.Detaljer(
                                    Periode(1.januar(2020), 2.januar(2020)),
                                    konto = "12345678910og1112",
                                    beløp = 9999,
                                    tilbakeføring = false,
                                    sats = Simulering.Sats(
                                        sats = 1111,
                                        antall = 9,
                                        type = "DAGLIG"
                                    ),
                                    klassekode = Simulering.Klassekode(
                                        kode = "SPREFAG-IOP",
                                        beskrivelse = "Sykepenger, Refusjon arbeidsgiver"
                                    ),
                                    refunderesOrgnummer = orgnummer,
                                    uføregrad = 100,
                                    utbetalingstype = "YTELSE"
                                )
                            )
                        )
                    )
                )
            )
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

