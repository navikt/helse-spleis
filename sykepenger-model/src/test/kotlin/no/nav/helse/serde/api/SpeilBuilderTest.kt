package no.nav.helse.serde.api

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver
import no.nav.helse.hendelser.Utbetalingshistorikk.Inntektsopplysning
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.serde.api.InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.*

class SpeilBuilderTest {

    @Test
    fun `happy case`() {
        val (person, hendelser) = person()
        val personDTO = serializePersonForSpeil(person, hendelser)

        assertEquals("12020052345", personDTO.fødselsnummer)
        assertEquals(1, personDTO.arbeidsgivere.size)
    }

    @Test
    fun `mapping av utbetalingshistorikk`() {
        val (person, hendelser) = person()
        val personDTO = serializePersonForSpeil(person, hendelser)

        assertEquals(1, personDTO.arbeidsgivere.first().utbetalingshistorikk.size)
        val tidslinje = personDTO.arbeidsgivere.first().utbetalingshistorikk.first()
        assertEquals(31, tidslinje.beregnettidslinje.size)
        assertEquals(16, tidslinje.hendelsetidslinje.size)
        assertEquals(1, tidslinje.utbetalinger.size)
        assertEquals(31, tidslinje.utbetalinger.first().utbetalingstidslinje.size)
    }

    @Test
    fun `legger på beregningId på vedtakserperioden`() {
        val (person, hendelser) = person()
        val personDTO = serializePersonForSpeil(person, hendelser)

        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1, vedtaksperiode.beregningIder.size)
    }

    @Test
    fun `kobler beregningsId i vedtaksperioden til utbetalingshistorikken`() {
        val (person, hendelser) = person()
        val personDTO = serializePersonForSpeil(person, hendelser)

        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        val utbetalingFraHistorikk = personDTO.arbeidsgivere.first().utbetalingshistorikk.first().utbetalinger.first()
        assertEquals(1, vedtaksperiode.beregningIder.size)
        assertEquals(vedtaksperiode.beregningIder.first(), utbetalingFraHistorikk.beregningId)
        assertEquals(Utbetaling.Utbetalingtype.UTBETALING.name, utbetalingFraHistorikk.type)
        assertEquals(28.desember, utbetalingFraHistorikk.maksdato)
    }

    @Test
    fun `mapping av utbetalingshistorikk med flere perioder`() {
        val (person, hendelser) = person()

        person.run {
            håndter(sykmelding(fom = 1.februar, tom = 14.februar).first)
            håndter(søknad(fom = 1.februar, tom = 14.februar).first)
            fangeVedtaksperiodeId()
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
            håndter(simulering(vedtaksperiodeIder.last()))
        }

        val personDTO = serializePersonForSpeil(person, hendelser)
        val nyesteHistorikkElement = personDTO.arbeidsgivere.first().utbetalingshistorikk.first()
        val eldsteHistorikkElement = personDTO.arbeidsgivere.first().utbetalingshistorikk.last()
        assertEquals(2, personDTO.arbeidsgivere.first().utbetalingshistorikk.size)
        assertEquals(1.februar, nyesteHistorikkElement.hendelsetidslinje.first().dagen)
        assertEquals(14.februar, nyesteHistorikkElement.hendelsetidslinje.last().dagen)
        assertEquals(1.januar, nyesteHistorikkElement.beregnettidslinje.first().dagen)
        assertEquals(14.februar, nyesteHistorikkElement.beregnettidslinje.last().dagen)

        assertEquals(1.januar, eldsteHistorikkElement.hendelsetidslinje.first().dagen)
        assertEquals(16.januar, eldsteHistorikkElement.hendelsetidslinje.last().dagen)
        assertEquals(1.januar, eldsteHistorikkElement.hendelsetidslinje.first().dagen)
        assertEquals(16.januar, eldsteHistorikkElement.hendelsetidslinje.last().dagen)
    }

    @Test
    fun `dager før skjæringstidspunkt og etter sisteSykedag skal kuttes vekk fra utbetalingstidslinje`() {
        val (person, hendelser) = person()
        val personDTO = serializePersonForSpeil(person, hendelser)

        val vedtaksperiodeDTO = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1.januar, vedtaksperiodeDTO.utbetalingstidslinje.first().dato)
        assertEquals(31.januar, vedtaksperiodeDTO.utbetalingstidslinje.last().dato)
    }

    @Test
    fun `person uten utbetalingsdager`() {
        val (person, hendelser) = ingenBetalingsperson()
        val personDTO = serializePersonForSpeil(person, hendelser)
        assertEquals(
            TilstandstypeDTO.IngenUtbetaling,
            (personDTO.arbeidsgivere.first().vedtaksperioder.first()).tilstand
        )

        val vedtaksperiode: UfullstendigVedtaksperiodeDTO = personDTO.arbeidsgivere.first().vedtaksperioder.first() as UfullstendigVedtaksperiodeDTO
        assertEquals(9, vedtaksperiode.utbetalingstidslinje.size)
    }

    @Test
    fun `person med foreldet dager`() {
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
        assertEquals(SpeilDagtype.FORELDET_SYKEDAG, sykdomstidslinje.first().type)
        assertEquals(SpeilDagtype.FORELDET_SYKEDAG, sykdomstidslinje.last().type)
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
        var vedtaksperiodeIder: List<String>

        val sykmelding1Id = UUID.randomUUID()
        val søknad1Id = UUID.randomUUID()
        val inntektsmeldingId = UUID.randomUUID()
        val sykmelding2Id = UUID.randomUUID()
        val søknad2Id = UUID.randomUUID()

        val (person, hendelser) = Person(aktørId, fnr).run {
            this to mutableListOf<HendelseDTO>().apply {
                sykmelding(hendelseId = sykmelding1Id, fom = 1.januar, tom = 31.januar).also { (sykmelding, sykmeldingDto) ->
                    håndter(sykmelding)
                    add(sykmeldingDto)
                }
                søknad(hendelseId = søknad1Id, fom = 1.januar, tom = 31.januar).also { (søknad, søknadDTO) ->
                    håndter(søknad)
                    add(søknadDTO)
                }
                inntektsmelding(hendelseId = inntektsmeldingId, fom = 1.januar).also { (inntektsmelding, inntektsmeldingDTO) ->
                    håndter(inntektsmelding)
                    add(inntektsmeldingDTO)
                }

                vedtaksperiodeIder = collectVedtaksperiodeIder()

                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeIder.last(), aktivitetslogg = this@run.aktivitetslogg))
                fangeUtbetalinger()
                håndter(overføring(this@run.aktivitetslogg))
                håndter(utbetalt(this@run.aktivitetslogg))

                sykmelding(hendelseId = sykmelding2Id, fom = 1.februar, tom = 14.februar).also { (sykmelding, sykmeldingDto) ->
                    håndter(sykmelding)
                    add(sykmeldingDto)
                }
                søknad(hendelseId = søknad2Id, fom = 1.februar, tom = 14.februar).also { (søknad, søknadDTO) ->
                    håndter(søknad)
                    add(søknadDTO)
                }

                vedtaksperiodeIder = collectVedtaksperiodeIder()

                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeIder.last(), aktivitetslogg = this@run.aktivitetslogg))
            }
        }

        val personDTO = serializePersonForSpeil(person, hendelser)

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()

        assertEquals(2, vedtaksperioder.size)
        assertEquals(3, vedtaksperioder.first().hendelser.size)
        assertEquals(3, vedtaksperioder.last().hendelser.size)
        assertEquals(inntektsmeldingId, vedtaksperioder.first().inntektsmeldingId)
        assertEquals(inntektsmeldingId, vedtaksperioder.last().inntektsmeldingId)
        assertTrue(vedtaksperioder.first().hendelser.map { UUID.fromString(it.id) }.containsAll(listOf(sykmelding1Id, søknad1Id, inntektsmeldingId)))
        assertTrue(vedtaksperioder.last().hendelser.map { UUID.fromString(it.id) }.containsAll(listOf(sykmelding2Id, søknad2Id, inntektsmeldingId)))
    }

    @Test
    fun `Utbetalinger blir lagt riktig på hver vedtaksperiode`() {
        var vedtaksperiodeIder: List<String>

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
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeIder.last(), aktivitetslogg = this@run.aktivitetslogg))
                fangeUtbetalinger()
                håndter(overføring(this@run.aktivitetslogg))
                håndter(utbetalt(this@run.aktivitetslogg))

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
                fangeUtbetalinger()
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeIder.last(), aktivitetslogg = this@run.aktivitetslogg))
            }
        }

        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()
        val utbetalinger = vedtaksperioder[1].utbetalteUtbetalinger

        // Sjekker at ubetalte utbetalinger er filtrert vekk
        val utbetalteUtbetalinger = utbetalingsliste.getValue(orgnummer).filter { it.erUtbetalt() }
        assertEquals(
            utbetalteUtbetalinger.last().arbeidsgiverOppdrag().fagsystemId(),
            utbetalinger.arbeidsgiverUtbetaling!!.fagsystemId
        )
        assertNull(utbetalinger.personUtbetaling)
        assertEquals(
            utbetalteUtbetalinger.last().arbeidsgiverOppdrag().førstedato,
            utbetalinger.arbeidsgiverUtbetaling!!.linjer.first().fom
        )
        assertEquals(
            utbetalteUtbetalinger.last().arbeidsgiverOppdrag().sistedato,
            utbetalinger.arbeidsgiverUtbetaling!!.linjer.first().tom
        )
    }

    @Test
    fun `passer på at alle vedtak får fellesdata for sykefraværet`() {
        var vedtaksperiodeIder: List<String>

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
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeIder.last(), aktivitetslogg = this@run.aktivitetslogg))
                fangeUtbetalinger()
                håndter(overføring(this@run.aktivitetslogg))
                håndter(utbetalt(this@run.aktivitetslogg))

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
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeIder.last(), aktivitetslogg = this@run.aktivitetslogg))
                håndter(overføring(this@run.aktivitetslogg))
                håndter(utbetalt(this@run.aktivitetslogg))

                sykmelding(fom = 20.februar, tom = 28.februar).also { (sykmelding, sykmeldingDto) ->
                    håndter(sykmelding)
                    add(sykmeldingDto)
                }
                søknad(fom = 20.februar, tom = 28.februar).also { (søknad, søknadDTO) ->
                    håndter(søknad)
                    add(søknadDTO)
                }
                inntektsmelding(fom = 20.februar).also { (inntektsmelding, inntektsmeldingDTO) ->
                    håndter(inntektsmelding)
                    add(inntektsmeldingDTO)
                }

                vedtaksperiodeIder = collectVedtaksperiodeIder()

                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeIder.last()))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeIder.last(), aktivitetslogg = this@run.aktivitetslogg))
            }
        }

        val personDTO = serializePersonForSpeil(person, hendelser)

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()

        assertEquals(3, vedtaksperioder.size)
        assertEquals(vedtaksperioder.first().gruppeId, vedtaksperioder[1].gruppeId)
        assertNotEquals(vedtaksperioder.first().gruppeId, vedtaksperioder.last().gruppeId)

        assertNotNull(vedtaksperioder.first().dataForVilkårsvurdering)
        assertNotNull(vedtaksperioder.first().vilkår.opptjening)
        assertEquals(vedtaksperioder.first().dataForVilkårsvurdering, vedtaksperioder[1].dataForVilkårsvurdering)
        assertEquals(vedtaksperioder.first().vilkår.opptjening, vedtaksperioder[1].vilkår.opptjening)

        assertEquals(220, vedtaksperioder.last().vilkår.sykepengedager.gjenståendeDager)
        assertEquals(28, vedtaksperioder.last().vilkår.sykepengedager.forbrukteSykedager)
        assertEquals(2.januar(2019), vedtaksperioder.last().vilkår.sykepengedager.maksdato)
    }

    @Test
    fun `forlengelse fra Infotrygd får riktig skjæringstidspunkt`() {
        val fom1Periode = 1.januar
        val tom1Periode = 31.januar
        val skjæringstidspunktFraInfotrygd = 1.desember(2017)
        val fom2Periode = 1.februar
        val tom2Periode = 14.februar
        val inntektshistorikk = listOf(Inntektsopplysning(skjæringstidspunktFraInfotrygd, 31000.månedlig, orgnummer, true))

        val (person, hendelser) = Person(aktørId, fnr).run {
            this to mutableListOf<HendelseDTO>().apply {
                sykmelding(fom = fom1Periode, tom = tom1Periode).also { (sykmelding, sykmeldingDTO) ->
                    håndter(sykmelding)
                    add(sykmeldingDTO)
                }

                var sisteVedtaksperiodeId = collectVedtaksperiodeIder().first()
                søknad(
                    hendelseId = UUID.randomUUID(),
                    fom = fom1Periode,
                    tom = tom1Periode,
                    sendtSøknad = 1.april.atStartOfDay()
                )
                    .also { (søknad, søknadDTO) ->
                        håndter(søknad)
                        add(søknadDTO)
                    }

                // Her går den til Infotrygd pga overlap
                håndter(
                    utbetalingshistorikk(
                        vedtaksperiodeId = sisteVedtaksperiodeId,
                        utbetalinger = listOf(RefusjonTilArbeidsgiver(skjæringstidspunktFraInfotrygd, 4.januar, 31000.månedlig, 100.prosent, orgnummer))
                    )
                )

                // Ny periode
                sykmelding(fom = fom2Periode, tom = tom2Periode).also { (sykmelding, sykmeldingDto) ->
                    håndter(sykmelding)
                    add(sykmeldingDto)
                }
                søknad(fom = fom2Periode, tom = tom2Periode).also { (søknad, søknadDTO) ->
                    håndter(søknad)
                    add(søknadDTO)
                }
                sisteVedtaksperiodeId = collectVedtaksperiodeIder().first()

                håndter(
                    utbetalingshistorikk(
                        vedtaksperiodeId = sisteVedtaksperiodeId,
                        utbetalinger = listOf(RefusjonTilArbeidsgiver(skjæringstidspunktFraInfotrygd, tom1Periode, 31000.månedlig, 100.prosent, orgnummer)),
                        inntektshistorikk = inntektshistorikk
                    )
                )
                håndter(vilkårsgrunnlag(vedtaksperiodeId = sisteVedtaksperiodeId))
                håndter(
                    ytelser(
                        vedtaksperiodeId = sisteVedtaksperiodeId,
                        utbetalinger = listOf(RefusjonTilArbeidsgiver(skjæringstidspunktFraInfotrygd, tom1Periode, 31000.månedlig, 100.prosent, orgnummer)),
                        inntektshistorikk = inntektshistorikk
                    )
                )

                håndter(simulering(vedtaksperiodeId = sisteVedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = sisteVedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
            }
        }

        val personDTO = serializePersonForSpeil(person, hendelser)

        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()
            .also {
                assertEquals(1, it.size)
            }

        // Denne periode er forlengelse av Infotrygd-periode.
        assertEquals(ForlengelseFraInfotrygd.JA, vedtaksperioder.first().forlengelseFraInfotrygd)
        assertEquals(Periodetype.OVERGANG_FRA_IT, vedtaksperioder.first().periodetype)
    }

    @Test
    fun `hvis første vedtaksperiode er ferdigbehandlet arbeidsgiverperiode vises den som ferdigbehandlet`() {
        val (person, hendelser) = ingenutbetalingPåfølgendeBetaling()
        val personDTO = serializePersonForSpeil(person, hendelser)

        val vedtaksperiodeDTO = personDTO.arbeidsgivere[0].vedtaksperioder[1] as VedtaksperiodeDTO
        assertNotNull(vedtaksperiodeDTO.dataForVilkårsvurdering)
        assertNotNull(vedtaksperiodeDTO.vilkår.opptjening)
        assertTrue(personDTO.arbeidsgivere[0].vedtaksperioder[1].fullstendig)
//        This assertion will not pass until SpeilBuilder can map employer's period correctly - https://trello.com/c/4FjMVwna")
//        assertTrue(personDTO.arbeidsgivere[0].vedtaksperioder[0].fullstendig)
    }

    @Test
    fun `perioder uten utbetaling får utbetalingstidslinje`() {
        val (person, hendelser) = ingenutbetalingPåfølgendeBetaling()
        val personDTO = serializePersonForSpeil(person, hendelser)

        assertEquals(9, personDTO.arbeidsgivere.first().vedtaksperioder.first().utbetalingstidslinje.size)
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

        assertEquals(fnr, personDTO.fødselsnummer)
        assertEquals(aktørId, personDTO.aktørId)
        assertEquals(1, personDTO.arbeidsgivere.size)

        val arbeidsgiver = personDTO.arbeidsgivere.first()
        assertEquals(orgnummer, arbeidsgiver.organisasjonsnummer)
        assertEquals(1, arbeidsgiver.vedtaksperioder.size)

        val vedtaksperiode = arbeidsgiver.vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1.januar, vedtaksperiode.fom)
        assertEquals(31.januar, vedtaksperiode.tom)
        assertEquals(TilstandstypeDTO.Utbetalt, vedtaksperiode.tilstand)
        assertTrue(vedtaksperiode.fullstendig)

        val utbetalinger = vedtaksperiode.utbetalinger
        assertEquals(
            utbetalingsliste.getValue(orgnummer).first().arbeidsgiverOppdrag().fagsystemId(),
            utbetalinger.arbeidsgiverUtbetaling!!.fagsystemId
        )
        assertEquals(
            utbetalingsliste.getValue(orgnummer).first().personOppdrag().fagsystemId(),
            utbetalinger.personUtbetaling!!.fagsystemId
        )
        assertEquals(
            utbetalingsliste.getValue(orgnummer).first().arbeidsgiverOppdrag().førstedato,
            utbetalinger.arbeidsgiverUtbetaling!!.linjer.first().fom
        )
        assertEquals(
            utbetalingsliste.getValue(orgnummer).first().arbeidsgiverOppdrag().sistedato,
            utbetalinger.arbeidsgiverUtbetaling!!.linjer.first().tom
        )

        val utbetalingstidslinje = vedtaksperiode.utbetalingstidslinje
        assertEquals(31, utbetalingstidslinje.size)
        assertEquals(TypeDataDTO.ArbeidsgiverperiodeDag, utbetalingstidslinje.first().type)
        assertEquals(TypeDataDTO.NavDag, utbetalingstidslinje.last().type)
        assertEquals(100.0, (utbetalingstidslinje.last() as NavDagDTO).grad)

        assertEquals(15741, vedtaksperiode.totalbeløpArbeidstaker)

        val sykdomstidslinje = vedtaksperiode.sykdomstidslinje
        assertEquals(31, sykdomstidslinje.size)
        assertEquals(SpeilDagtype.SYKEDAG, sykdomstidslinje.first().type)
        assertEquals(100.0, (sykdomstidslinje.last()).grad)
        assertEquals("Søknad", sykdomstidslinje.first().kilde.type.toString())
        assertEquals(1.januar, sykdomstidslinje.first().dagen)

        assertEquals("en_saksbehandler_ident", vedtaksperiode.godkjentAv)

        val vilkår = vedtaksperiode.vilkår

        val sykepengedager = vilkår.sykepengedager
        assertEquals(11, sykepengedager.forbrukteSykedager)
        assertEquals(fom, sykepengedager.skjæringstidspunkt)
        assertEquals(fom.plusDays(16), sykepengedager.førsteSykepengedag)
        assertEquals(28.desember, sykepengedager.maksdato)
        assertEquals(237, sykepengedager.gjenståendeDager)
        assertTrue(sykepengedager.oppfylt)

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

        val medlemskapstatus = vilkår.medlemskapstatus
        assertEquals(MedlemskapstatusDTO.JA, medlemskapstatus)

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
    fun `Yes hello does this work‽`() {
        val fom = 1.januar(2018)
        val tom = 31.januar(2018)

        val (person, hendelser) = person(
            fom = fom, tom = tom,
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
            )
        )

        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(0, vedtaksperiode.vilkår.sykepengedager.gjenståendeDager)
    }

    @Test
    fun `Sí, hola, ⸘funciona‽`() {
        val fom = 1.januar(2018)
        val tom = 31.januar(2018)

        val (person, hendelser) = person(
            fom = fom, tom = tom,
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
            )
        )

        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(0, vedtaksperiode.vilkår.sykepengedager.gjenståendeDager)
    }

    @Test
    fun `Skal ta med forkastede vedtaksperioder`() {
        val (person, hendelser) = tilbakerulletPerson()
        val personDTO = serializePersonForSpeil(person, hendelser)
        assertEquals(2, personDTO.arbeidsgivere.first().vedtaksperioder.size)
    }

    @Test
    fun `Skal ta med annullerte vedtaksperioder`() {
        val (person, hendelser) = annullertPersonIkkeUtbetalt()
        val personDTO = serializePersonForSpeil(person, hendelser)
        assertEquals(1, personDTO.arbeidsgivere.first().vedtaksperioder.size)
        assertEquals(TilstandstypeDTO.TilAnnullering, personDTO.arbeidsgivere[0].vedtaksperioder[0].tilstand)
    }

    @Test
    fun `Sender unike advarsler per periode`() {
        val (person, hendelser) = personMedToAdvarsler(fom = 1.januar(2018), tom = 31.januar(2018))

        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(vedtaksperiode.aktivitetslogg.distinctBy { it.melding }, vedtaksperiode.aktivitetslogg)
    }

    @Test
    fun `Sender med varsler for tidligere periode som er avsluttet uten utbetaling`() {
        val (person, hendelser) = ingenutbetalingPåfølgendeBetaling(medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke)

        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(1, vedtaksperiode.aktivitetslogg.size)
        assertNotEquals(vedtaksperiode.id, vedtaksperiode.aktivitetslogg[0].vedtaksperiodeId)
    }

    @Test
    fun `legger ved kildeId sammen med dag i tidslinja`() {
        val (person, hendelser) = person()
        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[0] as VedtaksperiodeDTO
        assertEquals(UUID.fromString(hendelser[1].id), vedtaksperiode.sykdomstidslinje[0].kilde.kildeId)
    }

    @Test
    fun `egen tilstandstype for perioder med kun feriedager`() {
        val (person, hendelser) = andrePeriodeKunFerie()
        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[1] as UfullstendigVedtaksperiodeDTO
        assertEquals(TilstandstypeDTO.KunFerie, vedtaksperiode.tilstand)
    }

    @Test
    fun `inkluderer ikke arbeidsgivere uten sykdomshistorikk`() {
        val (person, hendelser) = personMedEkstraArbeidsgiverUtenSykdomshistorikk()
        val personDTO = serializePersonForSpeil(person, hendelser)
        assertEquals(1, personDTO.arbeidsgivere.size)
        assertEquals(1, personDTO.arbeidsgivere[0].vedtaksperioder.size)
    }

    @Test
    fun `ny inntekt inkluderes`() {
        val (person, hendelser) = person()
        val personDTO = serializePersonForSpeil(person, hendelser)
        assertEquals(1, personDTO.arbeidsgivere.size)
        assertEquals(1, personDTO.arbeidsgivere[0].vedtaksperioder.size)
        val inntektsgrunnlag = personDTO.inntektsgrunnlag.find { it.skjæringstidspunkt == 1.januar }
        assertEquals(31000.0 * 12, inntektsgrunnlag?.sykepengegrunnlag)
        assertEquals(31000.0 * 12, inntektsgrunnlag?.omregnetÅrsinntekt)
        assertEquals(31000.0 * 12, inntektsgrunnlag?.sammenligningsgrunnlag)
        assertEquals(0.0, inntektsgrunnlag?.avviksprosent)
        assertEquals(31000.0 * 12 / 260, inntektsgrunnlag?.maksUtbetalingPerDag)
        assertEquals(1, inntektsgrunnlag?.inntekter?.size)
        inntektsgrunnlag?.inntekter?.forEach { arbeidsgiverinntekt ->
            assertEquals(orgnummer, arbeidsgiverinntekt.arbeidsgiver)

            assertEquals(InntektkildeDTO.Inntektsmelding, arbeidsgiverinntekt.omregnetÅrsinntekt?.kilde)
            assertEquals(31000.0 * 12, arbeidsgiverinntekt.omregnetÅrsinntekt?.beløp)
            assertEquals(31000.0, arbeidsgiverinntekt.omregnetÅrsinntekt?.månedsbeløp)
            assertNull(arbeidsgiverinntekt.omregnetÅrsinntekt?.inntekterFraAOrdningen)

            assertEquals(31000.0 * 12, arbeidsgiverinntekt.sammenligningsgrunnlag?.beløp)
            assertEquals(12, arbeidsgiverinntekt.sammenligningsgrunnlag?.inntekterFraAOrdningen?.size)
            arbeidsgiverinntekt.sammenligningsgrunnlag?.inntekterFraAOrdningen?.forEachIndexed { index, inntekterFraAOrdningen ->
                assertEquals(YearMonth.of(2017, index + 1), inntekterFraAOrdningen.måned)
                assertEquals(31000.0, inntekterFraAOrdningen.sum)
            }
        }
    }

    @Test
    fun `legger ved felt for automatisk behandling for riktig periode`() {
        val fom = 1.januar
        val tom = 31.januar

        val (person, hendelser) = person(fom, tom, automatiskBehandling = true)
        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[0] as VedtaksperiodeDTO
        assertTrue(vedtaksperiode.automatiskBehandlet)
        assertEquals("Automatisk behandlet", vedtaksperiode.godkjentAv)

        val hendelserForForlengelse = hendelser.toMutableList()

        val (forlengelseFom, forlengelseTom) = tom.let { it.plusDays(1) to it.plusDays(14) }
        person.run {
            sykmelding(fom = forlengelseFom, tom = forlengelseTom).also { (sykmelding, sykmeldingDTO) ->
                håndter(sykmelding)
                hendelserForForlengelse.add(sykmeldingDTO)
            }
            fangeVedtaksperiodeId()
            søknad(
                hendelseId = UUID.randomUUID(),
                fom = forlengelseFom,
                tom = forlengelseTom,
                sendtSøknad = forlengelseFom.plusDays(1).atStartOfDay()
            ).also { (søknad, søknadDTO) ->
                håndter(søknad)
                hendelserForForlengelse.add(søknadDTO)
            }
            håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
            fangeUtbetalinger()
            håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
        }

        val forlengelsePersonDTO = serializePersonForSpeil(person, hendelserForForlengelse)
        val forlengelse = forlengelsePersonDTO.arbeidsgivere[0].vedtaksperioder[1] as VedtaksperiodeDTO
        assertFalse(forlengelse.automatiskBehandlet)
    }

    @Test
    fun `Total sykdomsgrad ved en arbeidsgiver`() {
        val (person, hendelser) = person()

        val vedtaksperiodeDTO = serializePersonForSpeil(person, hendelser)
            .arbeidsgivere.first()
            .vedtaksperioder.first() as VedtaksperiodeDTO

        assertEquals(100.0, vedtaksperiodeDTO.utbetalingstidslinje.filterIsInstance<NavDagDTO>().first().totalGrad)
    }

    @Test
    fun `Total sykdomsgrad ved flere arbeidsgivere`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        val inntekt = 30000.månedlig
        val orgnr1 = "123456879"
        val orgnr2 = "987654321"

        val person = Person(aktørId, fnr)

        person.håndter(sykmelding(orgnummer = orgnr1, fom = periode.start, tom = periode.endInclusive, grad = 50.prosent).first)
        person.håndter(sykmelding(orgnummer = orgnr2, fom = periode.start, tom = periode.endInclusive, grad = 100.prosent).first)
        person.håndter(søknad(orgnummer = orgnr1, fom = periode.start, tom = periode.endInclusive, grad = 50.prosent).first)


        val vedtaksperiodeId1 = person.collectVedtaksperiodeIder(orgnr1).last()
        val vedtaksperiodeId2 = person.collectVedtaksperiodeIder(orgnr2).last()

        val inntektshistorikk = listOf(
            Inntektsopplysning(20.januar(2021), inntekt, orgnr1, true),
            Inntektsopplysning(20.januar(2021), inntekt, orgnr2, true)
        )

        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), inntekt, 100.prosent, orgnr1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), inntekt, 100.prosent, orgnr2)
        )

        person.håndter(utbetalingshistorikk(vedtaksperiodeId = vedtaksperiodeId1, utbetalinger = utbetalinger, orgnummer = orgnr1))
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId1,
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                orgnummer = orgnr1
            )
        )
        person.håndter(søknad(orgnummer = orgnr2, fom = periode.start, tom = periode.endInclusive, grad = 100.prosent).first)
        person.håndter(utbetalingshistorikk(vedtaksperiodeId = vedtaksperiodeId2, utbetalinger = utbetalinger, orgnummer = orgnr2))
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId2,
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                orgnummer = orgnr2
            )
        )
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId1,
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                orgnummer = orgnr1
            )
        )
        person.håndter(simulering(vedtaksperiodeId1, orgnummer = orgnr1))

        val navdagDTO = serializePersonForSpeil(person)
            .arbeidsgivere.first()
            .vedtaksperioder.last()
            .utbetalingstidslinje.filterIsInstance<NavDagDTO>().last()

        assertEquals(75.0, navdagDTO.totalGrad)
    }

    @Test
    fun `Inntektskilde ved flere arbeidsgivere`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        val inntekt = 30000.månedlig
        val orgnr1 = "123456879"
        val orgnr2 = "987654321"

        val person = Person(aktørId, fnr)

        person.håndter(sykmelding(orgnummer = orgnr1, fom = periode.start, tom = periode.endInclusive, grad = 50.prosent).first)
        person.håndter(sykmelding(orgnummer = orgnr2, fom = periode.start, tom = periode.endInclusive, grad = 100.prosent).first)
        person.håndter(søknad(orgnummer = orgnr1, fom = periode.start, tom = periode.endInclusive, grad = 50.prosent).first)


        val vedtaksperiodeId1 = person.collectVedtaksperiodeIder(orgnr1).last()
        val vedtaksperiodeId2 = person.collectVedtaksperiodeIder(orgnr2).last()

        val inntektshistorikk = listOf(
            Inntektsopplysning(20.januar(2021), inntekt, orgnr1, true),
            Inntektsopplysning(20.januar(2021), inntekt, orgnr2, true)
        )

        val utbetalinger = listOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), inntekt, 100.prosent, orgnr1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), inntekt, 100.prosent, orgnr2)
        )

        person.håndter(utbetalingshistorikk(vedtaksperiodeId = vedtaksperiodeId1, utbetalinger = utbetalinger, orgnummer = orgnr1))
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId1,
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                orgnummer = orgnr1
            )
        )
        person.håndter(søknad(orgnummer = orgnr2, fom = periode.start, tom = periode.endInclusive, grad = 100.prosent).first)
        person.håndter(utbetalingshistorikk(vedtaksperiodeId = vedtaksperiodeId2, utbetalinger = utbetalinger, orgnummer = orgnr2))
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId2,
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                orgnummer = orgnr2
            )
        )
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId1,
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk,
                orgnummer = orgnr1
            )
        )
        person.håndter(simulering(vedtaksperiodeId1, orgnummer = orgnr1))

        val vedtaksperiode = serializePersonForSpeil(person)
            .arbeidsgivere.first()
            .vedtaksperioder.single()

        assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, vedtaksperiode.inntektskilde)
    }

    @Test
    fun `arbeidsgivere uten vedtaksperioder som skal vises i speil, filtreres bort`() {
        val person = Person(aktørId, fnr)
        person.håndter(sykmelding(orgnummer = orgnummer, fom = 1.februar, tom = 28.februar, grad = 100.prosent).first)
        person.håndter(
            søknad(
                orgnummer = orgnummer,
                fom = 1.februar,
                tom = 28.februar,
                grad = 100.prosent,
                andrePerioder = listOf(Søknad.Søknadsperiode.Permisjon(1.januar, 31.januar))
            ).first
        )
        assertTrue(serializePersonForSpeil(person).arbeidsgivere.isEmpty())
    }

    private fun <T> Collection<T>.assertOnNonEmptyCollection(func: (T) -> Unit) {
        assertTrue(isNotEmpty())
        forEach(func)
    }

    private fun Person.collectVedtaksperiodeIder(orgnummer: String = SpeilBuilderTest.orgnummer) = mutableMapOf<String, List<String>>().apply {
        accept(object : PersonVisitor {
            var currentArbeidsgiver = mutableListOf<String>()

            override fun postVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
                put(organisasjonsnummer, currentArbeidsgiver)
                currentArbeidsgiver = mutableListOf()
            }

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
                hendelseIder: List<UUID>,
                inntektsmeldingId: UUID?,
                inntektskilde: Inntektskilde
            ) {
                currentArbeidsgiver.add(id.toString())
            }
        })
    }.getValue(orgnummer)


    companion object {
        private const val aktørId = "12345"
        private const val fnr = "12020052345"
        private const val orgnummer = "987654321"
        private const val orgnummer2 = "1234"
        private lateinit var vedtaksperiodeId: String
        private val vedtaksperiodeIder: MutableList<String> = mutableListOf()
        private val utbetalingsliste: MutableMap<String, List<Utbetaling>> = mutableMapOf()

        private fun person(
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            sendtSøknad: LocalDate = 1.april,
            påfølgendePerioder: List<ClosedRange<LocalDate>> = emptyList(),
            automatiskBehandling: Boolean = false
        ): Pair<Person, List<HendelseDTO>> =
            Person(aktørId, fnr).run {
                this to mutableListOf<HendelseDTO>().apply {
                    sykmelding(fom = fom, tom = tom).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    fangeVedtaksperiodeId()
                    søknad(
                        hendelseId = UUID.randomUUID(),
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
                    fangeUtbetalinger()
                    håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                    utbetalingsgodkjenning(
                        vedtaksperiodeId = vedtaksperiodeId,
                        automatiskBehandling = automatiskBehandling,
                        aktivitetslogg = this@run.aktivitetslogg
                    ).also {
                        håndter(it)
                        if (it.behov().any { behov -> behov.type == Behovtype.Utbetaling }) {
                            håndter(overføring(it))
                            håndter(utbetalt(it))
                        }
                    }

                    påfølgendePerioder.forEach { periode ->
                        sykmelding(
                            fom = periode.start,
                            tom = periode.endInclusive
                        ).also { (sykmelding, sykmeldingDTO) ->
                            håndter(sykmelding)
                            add(sykmeldingDTO)
                        }
                        fangeVedtaksperiodeId()
                        søknad(
                            hendelseId = UUID.randomUUID(),
                            fom = periode.start,
                            tom = periode.endInclusive,
                            sendtSøknad = periode.endInclusive.atStartOfDay()
                        ).also { (søknad, søknadDTO) ->
                            håndter(søknad)
                            add(søknadDTO)
                        }
                        håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                        håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                        fangeUtbetalinger()
                        håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                        utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg).also {
                            håndter(it)
                            if (it.behov().any { behov -> behov.type == Behovtype.Utbetaling }) {
                                håndter(overføring(it))
                                håndter(utbetalt(it))
                            }
                        }
                    }
                }
            }

        private fun personMedEkstraArbeidsgiverUtenSykdomshistorikk(
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
                    håndter(vilkårsgrunnlagMedFlerInntekter(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    fangeUtbetalinger()
                    håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
                    håndter(overføring(this@run.aktivitetslogg))
                    håndter(utbetalt(this@run.aktivitetslogg))

                    påfølgendePerioder.forEach { periode ->
                        sykmelding(
                            fom = periode.start,
                            tom = periode.endInclusive
                        ).also { (sykmelding, sykmeldingDTO) ->
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
                        fangeUtbetalinger()
                        håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                        håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
                        håndter(overføring(this@run.aktivitetslogg))
                        håndter(utbetalt(this@run.aktivitetslogg))
                    }
                }
            }

        private fun tilbakerulletPerson(): Pair<Person, List<HendelseDTO>> = Person(aktørId, fnr).run {
            this to mutableListOf<HendelseDTO>().apply {
                sykmelding(fom = 1.januar, tom = 31.januar).also { (sykmelding, sykmeldingDTO) ->
                    håndter(sykmelding)
                    add(sykmeldingDTO)
                }
                fangeVedtaksperiodeId()
                søknad(
                    fom = 1.januar,
                    tom = 31.januar,
                    sendtSøknad = 1.april.atStartOfDay()
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
                fangeUtbetalinger()
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
                håndter(overføring(this@run.aktivitetslogg))
                håndter(utbetalt(this@run.aktivitetslogg))

                sykmelding(fom = 1.februar, tom = 28.februar).also { (sykmelding, sykmeldingDTO) ->
                    håndter(sykmelding)
                    add(sykmeldingDTO)
                }
                fangeVedtaksperiodeId()
                søknad(
                    fom = 1.februar,
                    tom = 28.februar,
                    sendtSøknad = 1.april.atStartOfDay()
                ).also { (søknad, søknadDTO) ->
                    håndter(søknad)
                    add(søknadDTO)
                }
                inntektsmelding(fom = 1.februar).also { (inntektsmelding, inntektsmeldingDTO) ->
                    håndter(inntektsmelding)
                    add(inntektsmeldingDTO)
                }
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                fangeUtbetalinger()
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, utbetalingGodkjent = false, aktivitetslogg = this@run.aktivitetslogg))

                sykmelding(fom = 1.mars, tom = 31.mars).also { (sykmelding, sykmeldingDTO) ->
                    håndter(sykmelding)
                    add(sykmeldingDTO)
                }
                fangeVedtaksperiodeId()
                søknad(
                    fom = 1.mars,
                    tom = 31.mars,
                    sendtSøknad = 1.april.atStartOfDay()
                ).also { (søknad, søknadDTO) ->
                    håndter(søknad)
                    add(søknadDTO)
                }
                inntektsmelding(fom = 1.mars).also { (inntektsmelding, inntektsmeldingDTO) ->
                    håndter(inntektsmelding)
                    add(inntektsmeldingDTO)
                }
                håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                fangeUtbetalinger()
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
                håndter(overføring(this@run.aktivitetslogg))
                håndter(utbetalt(this@run.aktivitetslogg))
            }
        }

        private fun annullertPersonIkkeUtbetalt(): Pair<Person, List<HendelseDTO>> = Person(aktørId, fnr).run {
            this to mutableListOf<HendelseDTO>().apply {
                sykmelding(fom = 1.januar, tom = 31.januar).also { (sykmelding, sykmeldingDTO) ->
                    håndter(sykmelding)
                    add(sykmeldingDTO)
                }
                fangeVedtaksperiodeId()
                søknad(
                    fom = 1.januar,
                    tom = 31.januar,
                    sendtSøknad = 1.april.atStartOfDay()
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
                fangeUtbetalinger()
                håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
                håndter(overføring(this@run.aktivitetslogg))
                håndter(utbetalt(this@run.aktivitetslogg))


                val utbetalteUtbetalinger = utbetalingsliste.getValue(orgnummer).filter { it.erUtbetalt() }
                håndter(annullering(fagsystemId = utbetalteUtbetalinger.last().arbeidsgiverOppdrag().fagsystemId()))
            }
        }

        private fun personMedToAdvarsler(
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar
        ): Pair<Person, List<HendelseDTO>> =
            Person(aktørId, fnr).run {
                this to mutableListOf<HendelseDTO>().apply {
                    sykmelding(fom = fom, tom = tom).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    fangeVedtaksperiodeId()
                    søknad(
                        hendelseId = UUID.randomUUID(), fom = fom, tom = tom, sendtSøknad = 1.april.atStartOfDay()
                    ).also { (søknad, søknadDTO) ->
                        håndter(søknad)
                        add(søknadDTO)
                    }
                    inntektsmelding(fom = fom).also { (inntektsmelding, inntektsmeldingDTO) ->
                        håndter(inntektsmelding)
                        add(inntektsmeldingDTO)
                    }
                    håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId, arbeidsavklaringspenger = listOf(fom.minusDays(60) til tom.minusDays(60))))
                    fangeUtbetalinger()
                    håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
                }
            }

        private fun ingenBetalingsperson(
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
                }
            }

        private fun ingenutbetalingPåfølgendeBetaling(
            søknadhendelseId: UUID = UUID.randomUUID(),
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja
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
                    håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId, medlemskapstatus = medlemskapstatus))
                    fangeVedtaksperiodeId()
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
                    fangeUtbetalinger()
                    håndter(overføring(this@run.aktivitetslogg))
                    håndter(utbetalt(this@run.aktivitetslogg))
                }
            }

        private fun andrePeriodeKunFerie(
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Pair<Person, List<HendelseDTO>> =
            Person(aktørId, fnr).run {
                this to mutableListOf<HendelseDTO>().apply {
                    sykmelding(fom = 1.januar, tom = 24.januar).also { (sykmelding, sykmeldingDto) ->
                        håndter(sykmelding)
                        add(sykmeldingDto)
                    }
                    fangeVedtaksperiodeId()
                    søknad(
                        hendelseId = søknadhendelseId,
                        fom = 1.januar,
                        tom = 24.januar
                    ).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    inntektsmelding(fom = 1.januar).also { (inntektsmelding, inntektsmeldingDTO) ->
                        håndter(inntektsmelding)
                        add(inntektsmeldingDTO)
                    }
                    håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
                    fangeUtbetalinger()
                    håndter(overføring(this@run.aktivitetslogg))
                    håndter(utbetalt(this@run.aktivitetslogg))

                    sykmelding(fom = 25.januar, tom = 31.januar).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    fangeVedtaksperiodeId()
                    søknad(
                        fom = 25.januar,
                        tom = 31.januar,
                        andrePerioder = listOf(Søknad.Søknadsperiode.Ferie(25.januar, 31.januar))
                    ).also { (søknad, søknadDTO) ->
                        håndter(søknad)
                        add(søknadDTO)
                    }
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                }
            }

        private fun Person.fangeVedtaksperiodeId() {
            accept(object : PersonVisitor {
                var iPeriode = false

                override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
                    iPeriode = true
                }

                override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
                    iPeriode = false
                }

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
                    hendelseIder: List<UUID>,
                    inntektsmeldingId: UUID?,
                    inntektskilde: Inntektskilde
                ) {
                    vedtaksperiodeIder.add(id.toString())
                    if (iPeriode) vedtaksperiodeId = id.toString()

                }
            })
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

        private fun sykmelding(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            orgnummer: String = SpeilBuilderTest.orgnummer,
            grad: Prosentdel = 100.prosent
        ) = Sykmelding(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            sykeperioder = listOf(Sykmeldingsperiode(fom, tom, grad)),
            mottatt = fom.plusMonths(3).atStartOfDay()
        ) to SykmeldingDTO(
            id = hendelseId.toString(),
            fom = fom,
            tom = tom,
            rapportertdato = fom.atStartOfDay()
        )

        private fun søknad(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            sendtSøknad: LocalDateTime = tom.plusDays(5).atTime(LocalTime.NOON),
            andrePerioder: List<Søknad.Søknadsperiode> = emptyList(),
            orgnummer: String = SpeilBuilderTest.orgnummer,
            grad: Prosentdel = 100.prosent
        ) = Søknad(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(fom, tom, grad)) + andrePerioder,
            andreInntektskilder = emptyList(),
            sendtTilNAV = sendtSøknad,
            permittert = false,
            merknaderFraSykmelding = emptyList()
        ) to SøknadNavDTO(
            id = hendelseId.toString(),
            fom = fom,
            tom = tom,
            rapportertdato = sendtSøknad,
            sendtNav = sendtSøknad
        )

        private fun søknadSendtTilArbeidsgiver(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate = 1.januar,
            tom: LocalDate = 31.januar,
            sendtTilArbeidsgiver: LocalDateTime = tom.atStartOfDay()
        ) = SøknadArbeidsgiver(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = listOf(SøknadArbeidsgiver.Søknadsperiode(fom, tom, 100.prosent, 0.prosent))
        ) to SøknadArbeidsgiverDTO(
            id = hendelseId.toString(),
            fom = fom,
            tom = tom,
            rapportertdato = tom.atStartOfDay(),
            sendtArbeidsgiver = sendtTilArbeidsgiver
        )

        private fun inntektsmelding(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate
        ) = Inntektsmelding(
            meldingsreferanseId = hendelseId,
            refusjon = Inntektsmelding.Refusjon(
                opphørsdato = null,
                inntekt = 31000.månedlig,
                endringerIRefusjon = emptyList()
            ),
            orgnummer = orgnummer,
            fødselsnummer = fnr,
            aktørId = aktørId,
            førsteFraværsdag = fom,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(15))),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ) to InntektsmeldingDTO(
            id = hendelseId.toString(),
            beregnetInntekt = 31000.00,
            mottattDato = fom.atStartOfDay()
        )

        private fun vilkårsgrunnlag(
            vedtaksperiodeId: String,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja
        ) = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.januar(2017) til 1.desember(2017) inntekter {
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
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus)
        )

        private fun vilkårsgrunnlagMedFlerInntekter(vedtaksperiodeId: String) = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.januar(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt 31000.månedlig
                }
                1.januar(2017) til 1.januar(2017) inntekter {
                    orgnummer2 inntekt 1
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

        private fun ytelser(
            hendelseId: UUID = UUID.randomUUID(),
            vedtaksperiodeId: String,
            orgnummer: String = SpeilBuilderTest.orgnummer,
            utbetalinger: List<Utbetalingshistorikk.Infotrygdperiode> = listOf(),
            inntektshistorikk: List<Inntektsopplysning> = listOf(),
            arbeidsavklaringspenger: List<Periode> = emptyList()
        ) = Aktivitetslogg().let {
            Ytelser(
                meldingsreferanseId = hendelseId,
                aktørId = aktørId,
                fødselsnummer = fnr,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingshistorikk = utbetalingshistorikk(
                    utbetalinger = utbetalinger,
                    meldingsreferanseId = hendelseId,
                    vedtaksperiodeId = vedtaksperiodeId,
                    inntektshistorikk = inntektshistorikk,
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
                arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
                dagpenger = Dagpenger(emptyList())
            )
        }

        private fun utbetalingshistorikk(
            meldingsreferanseId: UUID = UUID.randomUUID(),
            vedtaksperiodeId: String,
            utbetalinger: List<Utbetalingshistorikk.Infotrygdperiode> = listOf(),
            inntektshistorikk: List<Inntektsopplysning> = listOf(),
            aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
            orgnummer: String = SpeilBuilderTest.orgnummer
        ) = Utbetalingshistorikk(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            arbeidskategorikoder = emptyMap(),
            utbetalinger = utbetalinger,
            aktivitetslogg = aktivitetslogg,
            inntektshistorikk = inntektshistorikk
        )

        private fun utbetalingsgodkjenning(
            vedtaksperiodeId: String,
            utbetalingGodkjent: Boolean = true,
            automatiskBehandling: Boolean = false,
            aktivitetslogg: IAktivitetslogg
        ) =
            Utbetalingsgodkjenning(
                meldingsreferanseId = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiodeId,
                aktørId = aktørId,
                fødselsnummer = fnr,
                organisasjonsnummer = orgnummer,
                utbetalingId = UUID.fromString(aktivitetslogg.behov().last { it.type == Behovtype.Godkjenning }.kontekst().getValue("utbetalingId")),
                utbetalingGodkjent = utbetalingGodkjent,
                saksbehandler = if (automatiskBehandling) "Automatisk behandlet" else "en_saksbehandler_ident",
                godkjenttidspunkt = LocalDateTime.now(),
                automatiskBehandling = automatiskBehandling,
                saksbehandlerEpost = "mille.mellomleder@nav.no",
                makstidOppnådd = false,
            )

        private fun simulering(vedtaksperiodeId: String, orgnummer: String = SpeilBuilderTest.orgnummer) = Simulering(
            meldingsreferanseId = UUID.randomUUID(),
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

        private fun overføring(aktivitetslogg: IAktivitetslogg) = UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            fagsystemId = utbetalingsliste.getValue(orgnummer).last().arbeidsgiverOppdrag().fagsystemId(),
            utbetalingId = aktivitetslogg.behov().last { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )

        private fun utbetalt(aktivitetslogg: IAktivitetslogg) = UtbetalingHendelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            fagsystemId = utbetalingsliste.getValue(orgnummer).last().arbeidsgiverOppdrag().fagsystemId(),
            utbetalingId = aktivitetslogg.behov().last { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
            status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )

        private fun annullering(fagsystemId: String) = AnnullerUtbetaling(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            organisasjonsnummer = orgnummer,
            fagsystemId = fagsystemId,
            saksbehandlerIdent = "en_saksbehandler_ident",
            saksbehandlerEpost = "saksbehandler@nav.no",
            opprettet = LocalDateTime.now()
        )
    }
}

