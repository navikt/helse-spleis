package no.nav.helse.serde.api

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.api.InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO
import no.nav.helse.serde.mapping.SpeilDagtype
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.*

internal class SpeilBuilderTest: AbstractEndToEndTest() {

    @Test
    fun `happy case`() {
        nyttVedtak(1.januar, 31.januar)
        val personDTO = speilApi()
        val førsteVedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO

        assertEquals(UNG_PERSON_FNR_2018, personDTO.fødselsnummer)
        assertEquals(1, personDTO.arbeidsgivere.size)
        assertNotNull(personDTO.versjon)
        assertEquals(1, førsteVedtaksperiode.beregningIder.size)
    }

    /**
     * Test for å verifisere at kontrakten mellom Spleis og Speil opprettholdes.
     * Hvis du trenger å gjøre endringer i denne testen må du sannsynligvis også gjøre endringer i Speil.
     */
    @Test
    fun `personDTO-en inneholder de feltene Speil forventer`() {
        val fom = 1.januar
        val tom = 31.januar

        val sykmeldingId = UUID.randomUUID()
        val sykmelding = SykmeldingDTO(sykmeldingId.toString(), 1.januar, 31.januar, 1.januar.atStartOfDay())
        val søknadId = UUID.randomUUID()
        val søknad = SøknadNavDTO(søknadId.toString(), 1.januar, 31.januar, 1.januar.atStartOfDay(), sendtNav = 1.februar.atStartOfDay())
        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding = InntektsmeldingDTO(inntektsmeldingId.toString(), mottattDato = 2.januar.atStartOfDay(), INNTEKT.reflection { _, månedlig, _, _ -> månedlig })

        val hendelser = listOf(sykmelding, søknad, inntektsmelding)

        håndterSykmelding(Sykmeldingsperiode(sykmelding.fom, sykmelding.tom, 100.prosent), id = sykmeldingId)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(søknad.fom, søknad.tom, 100.prosent), id = søknadId, sendtTilNav = 1.februar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), id = inntektsmeldingId)

        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        val personDTO = serializePersonForSpeil(person, hendelser)

        assertEquals(UNG_PERSON_FNR_2018, personDTO.fødselsnummer)
        assertEquals(AKTØRID, personDTO.aktørId)
        assertEquals(1, personDTO.arbeidsgivere.size)

        val arbeidsgiver = personDTO.arbeidsgivere.first()
        assertEquals(ORGNUMMER, arbeidsgiver.organisasjonsnummer)
        assertEquals(1, arbeidsgiver.vedtaksperioder.size)

        val vedtaksperiode = arbeidsgiver.vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1.januar, vedtaksperiode.fom)
        assertEquals(31.januar, vedtaksperiode.tom)
        assertEquals(TilstandstypeDTO.Utbetalt, vedtaksperiode.tilstand)
        assertTrue(vedtaksperiode.fullstendig)

        val modellUtbetaling = inspektør.utbetalinger.first()
        val apiUtbetalinger = vedtaksperiode.utbetalinger
        assertEquals(
            modellUtbetaling.arbeidsgiverOppdrag().fagsystemId(),
            apiUtbetalinger.arbeidsgiverUtbetaling!!.fagsystemId
        )
        assertEquals(
            modellUtbetaling.personOppdrag().fagsystemId(),
            apiUtbetalinger.personUtbetaling!!.fagsystemId
        )
        assertEquals(
            modellUtbetaling.arbeidsgiverOppdrag().førstedato,
            apiUtbetalinger.arbeidsgiverUtbetaling!!.linjer.first().fom
        )
        assertEquals(
            modellUtbetaling.arbeidsgiverOppdrag().sistedato,
            apiUtbetalinger.arbeidsgiverUtbetaling!!.linjer.first().tom
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

        assertEquals("Ola Nordmann", vedtaksperiode.godkjentAv)

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
        assertEquals(1.februar.atStartOfDay(), søknadsfrist?.sendtNav)
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
    fun `dager før skjæringstidspunkt og etter sisteSykedag skal kuttes vekk fra utbetalingstidslinje`() {
        nyttVedtak(1.januar, 31.januar)
        val personDTO = speilApi()
        val vedtaksperiodeDTO = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1.januar, vedtaksperiodeDTO.utbetalingstidslinje.first().dato)
        assertEquals(31.januar, vedtaksperiodeDTO.utbetalingstidslinje.last().dato)
    }

    @Test
    fun `annullerer feilet revurdering`() {
        nyttVedtak(1.januar, 31.januar)

        håndterOverstyring()
        håndterYtelser()
        håndterSimulering(simuleringOK = false)
        håndterAnnullerUtbetaling()

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        val beregningIdVedtaksperiode = vedtaksperiode.beregningIder.last()
        val beregningIderUtbetalingshistorikk = personDTO.arbeidsgivere.first().utbetalingshistorikk.map { it.beregningId }

        assertEquals(1, vedtaksperiode.beregningIder.size)
        assertTrue(vedtaksperiode.fullstendig)
        assertEquals(2, beregningIderUtbetalingshistorikk.size)
        assertTrue(beregningIderUtbetalingshistorikk.contains(beregningIdVedtaksperiode))
    }

    @Test
    fun `kan mappe perioder som har beregning men står i avsluttet uten utbetaling`() {
        tilGodkjenning(1.januar, 19.januar, 100.prosent, 1.januar)
        håndterOverstyring(
            listOf(
                ManuellOverskrivingDag(17.januar, Feriedag),
                ManuellOverskrivingDag(18.januar, Feriedag),
                ManuellOverskrivingDag(19.januar, Feriedag)
            )
        )
        håndterYtelser()

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first()
        assertTrue(vedtaksperiode.fullstendig)
    }

    @Test
    fun `mapping av utbetalingshistorikk`() {
        nyttVedtak(1.januar, 31.januar)
        val personDTO = speilApi()
        val tidslinje = personDTO.arbeidsgivere.first().utbetalingshistorikk.first()
        val utbetaling = inspektør.utbetalinger.first()

        assertEquals(1, personDTO.arbeidsgivere.first().utbetalingshistorikk.size)
        assertEquals(31, tidslinje.beregnettidslinje.size)
        assertEquals(31, tidslinje.hendelsetidslinje.size)
        assertEquals(31, tidslinje.utbetaling.utbetalingstidslinje.size)

        assertEquals("UTBETALT", tidslinje.utbetaling.status)
        assertEquals("UTBETALING", tidslinje.utbetaling.type)
        assertEquals(237, tidslinje.utbetaling.gjenståendeSykedager)
        assertEquals(11, tidslinje.utbetaling.forbrukteSykedager)
        assertEquals(15741, tidslinje.utbetaling.arbeidsgiverNettoBeløp)
        assertEquals(utbetaling.arbeidsgiverOppdrag().fagsystemId(), tidslinje.utbetaling.arbeidsgiverFagsystemId)
        assertNotNull(tidslinje.utbetaling.vurdering)
        assertNotNull(tidslinje.tidsstempel)
        tidslinje.utbetaling.vurdering?.also {
            assertEquals("Ola Nordmann", it.ident)
            assertEquals(true, it.godkjent)
            assertEquals(false, it.automatisk)
            assertNotNull(it.tidsstempel)
        }
    }

    @Test
    fun `generer ett utbetalingshistorikkelement per utbetaling, selv om utbetalingene peker på samme sykdomshistorikk`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))

        //Spill igjennom første
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode, UtbetalingHendelse.Oppdragstatus.AKSEPTERT)

        //Spill igjennom andre
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        val personDTO = speilApi()
        assertEquals(2, personDTO.arbeidsgivere.first().utbetalingshistorikk.size)
        val arbeidsgiver = personDTO.arbeidsgivere.first()
        val førsteElement = arbeidsgiver.utbetalingshistorikk.first()
        val andreElement = arbeidsgiver.utbetalingshistorikk[1]

        assertEquals((arbeidsgiver.vedtaksperioder[1] as VedtaksperiodeDTO).beregningIder[0], førsteElement.beregningId)
        assertEquals((arbeidsgiver.vedtaksperioder.first() as VedtaksperiodeDTO).beregningIder[0], andreElement.beregningId)
        assertEquals(LocalDate.of(2018, 2, 28), førsteElement.utbetaling.utbetalingstidslinje.last().dato)
        assertEquals(LocalDate.of(2018, 1, 31), andreElement.utbetaling.utbetalingstidslinje.last().dato)
    }

    @Test
    fun `kobler beregningsId i vedtaksperioden til utbetalingshistorikken`() {
        nyttVedtak(1.januar, 31.januar)
        val personDTO = speilApi()

        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        assertEquals(1, vedtaksperiode.beregningIder.size)
        val utbetalingFraHistorikk = personDTO.arbeidsgivere.first().utbetalingshistorikk.first().utbetaling
        assertEquals(vedtaksperiode.beregningIder.first(), utbetalingFraHistorikk.beregningId)
        assertEquals(Utbetaling.Utbetalingtype.UTBETALING.name, utbetalingFraHistorikk.type)
        assertEquals(28.desember, utbetalingFraHistorikk.maksdato)
        assertTrue(personDTO.arbeidsgivere.first().utbetalingshistorikk.first().beregningId in vedtaksperiode.beregningIder)
    }

    @Test
    fun `mapping av utbetalingshistorikk med flere perioder`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 14.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 14.februar, 100.prosent))
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        val personDTO = speilApi()
        val nyesteHistorikkElement = personDTO.arbeidsgivere.first().utbetalingshistorikk.first()
        val eldsteHistorikkElement = personDTO.arbeidsgivere.first().utbetalingshistorikk.last()
        assertEquals(2, personDTO.arbeidsgivere.first().utbetalingshistorikk.size)
        assertEquals(1.februar, nyesteHistorikkElement.hendelsetidslinje.first().dagen)
        assertEquals(14.februar, nyesteHistorikkElement.hendelsetidslinje.last().dagen)
        assertEquals(1.januar, nyesteHistorikkElement.beregnettidslinje.first().dagen)
        assertEquals(14.februar, nyesteHistorikkElement.beregnettidslinje.last().dagen)

        assertEquals(1.januar, eldsteHistorikkElement.hendelsetidslinje.first().dagen)
        assertEquals(31.januar, eldsteHistorikkElement.hendelsetidslinje.last().dagen)
        assertEquals(1.januar, eldsteHistorikkElement.hendelsetidslinje.first().dagen)
        assertEquals(31.januar, eldsteHistorikkElement.hendelsetidslinje.last().dagen)

        assertNotNull(eldsteHistorikkElement.utbetaling.vurdering)
        assertNull(nyesteHistorikkElement.utbetaling.vurdering)
        eldsteHistorikkElement.utbetaling.vurdering?.also {
            assertEquals("Ola Nordmann", it.ident)
            assertEquals(true, it.godkjent)
            assertEquals(false, it.automatisk)
            assertNotNull(it.tidsstempel)
        }
    }

    @Test
    fun `person uten utbetalingsdager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 9.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 9.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        val personDTO = speilApi()

        assertEquals(
            TilstandstypeDTO.IngenUtbetaling,
            (personDTO.arbeidsgivere.first().vedtaksperioder.first()).tilstand
        )

        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.first() as VedtaksperiodeDTO
        assertTrue(vedtaksperiode.fullstendig)
        assertEquals(9, vedtaksperiode.utbetalingstidslinje.size)
    }

    @Test
    fun `person med foreldet dager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNav = 1.juni)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        val personDTO = speilApi()

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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        val personDTO = speilApi()

        val arbeidsgiver = personDTO.arbeidsgivere[0]
        val vedtaksperioder = arbeidsgiver.vedtaksperioder

        assertFalse(vedtaksperioder.first().fullstendig)
    }

    @Test
    fun `passer på at vedtakene har alle hendelsene`() {
        val sykmelding1Id = UUID.randomUUID()
        val sykmelding1 = SykmeldingDTO(sykmelding1Id.toString(), 1.januar, 31.januar, 1.januar.atStartOfDay())
        val søknad1Id = UUID.randomUUID()
        val søknad1 = SøknadNavDTO(søknad1Id.toString(), 1.januar, 31.januar, 1.januar.atStartOfDay(), 31.januar.atStartOfDay())
        val inntektsmeldingId = UUID.randomUUID()
        val inntektsmelding = InntektsmeldingDTO(inntektsmeldingId.toString(), mottattDato = 2.januar.atStartOfDay(), INNTEKT.reflection { _, månedlig, _, _ -> månedlig })

        val sykmelding2Id = UUID.randomUUID()
        val sykmelding2 = SykmeldingDTO(sykmelding2Id.toString(), 1.februar, 14.februar, 1.februar.atStartOfDay())
        val søknad2Id = UUID.randomUUID()
        val søknad2 = SøknadNavDTO(søknad2Id.toString(), 1.februar, 14.februar, 1.februar.atStartOfDay(), 14.februar.atStartOfDay())

        val hendelser = listOf(sykmelding1, søknad1, inntektsmelding, sykmelding2, søknad2)

        håndterSykmelding(Sykmeldingsperiode(sykmelding1.fom, sykmelding1.tom, 100.prosent), id = sykmelding1Id)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(søknad1.fom, søknad1.tom, 100.prosent), id = søknad1Id)
        håndterInntektsmelding(listOf(1.januar til 16.januar), id = inntektsmeldingId)
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(sykmelding2.fom, sykmelding2.tom, 100.prosent), id = sykmelding2Id)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(søknad2.fom, søknad2.tom, 100.prosent), id = søknad2Id)
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val personDTO = speilApi(hendelser)

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
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 14.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 14.februar, 100.prosent))
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val personDTO = speilApi()
        val vedtaksperioder = personDTO.arbeidsgivere.first().vedtaksperioder.filterIsInstance<VedtaksperiodeDTO>()
        val utbetalinger = vedtaksperioder[1].utbetalteUtbetalinger

        val utbetalteUtbetalinger = inspektør.utbetalinger.filter { it.erUtbetalt() }
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
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 14.februar)
        nyttVedtak(20.februar, 28.februar)

        val personDTO = speilApi()

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
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, skjæringstidspunktFraInfotrygd, 31000.månedlig, true))

        håndterSykmelding(Sykmeldingsperiode(fom1Periode, tom1Periode, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom1Periode, tom1Periode, 100.prosent))
        // Til infotrygd pga overlapp
        håndterUtbetalingshistorikk(1.vedtaksperiode, utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, skjæringstidspunktFraInfotrygd, 4.januar, 100.prosent, 31000.månedlig)))

        håndterSykmelding(Sykmeldingsperiode(fom2Periode, tom2Periode, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom2Periode, tom2Periode, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, besvart = 16.februar.atStartOfDay(), inntektshistorikk = inntektshistorikk, utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(
            ORGNUMMER,
            skjæringstidspunktFraInfotrygd,
            tom1Periode,
            100.prosent,
            31000.månedlig
        )))
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, besvart = 17.februar.atStartOfDay(), inntektshistorikk = inntektshistorikk, utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(
            ORGNUMMER,
            skjæringstidspunktFraInfotrygd,
            tom1Periode,
            100.prosent,
            31000.månedlig
        )))
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        val personDTO = speilApi()

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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 9.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 9.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(10.januar, 25.januar, 100.prosent))

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        val personDTO = speilApi()

        val vedtaksperiodeDTO = personDTO.arbeidsgivere[0].vedtaksperioder[1] as VedtaksperiodeDTO
        assertNotNull(vedtaksperiodeDTO.dataForVilkårsvurdering)
        assertNotNull(vedtaksperiodeDTO.vilkår.opptjening)
        assertTrue(personDTO.arbeidsgivere[0].vedtaksperioder[0].fullstendig)
        assertTrue(personDTO.arbeidsgivere[0].vedtaksperioder[1].fullstendig)
    }

    @Test
    fun `perioder uten utbetaling får utbetalingstidslinje`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 9.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 9.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(10.januar, 25.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        val personDTO = speilApi()
        assertEquals(9, personDTO.arbeidsgivere.first().vedtaksperioder.first().utbetalingstidslinje.size)
    }



    @Test
    fun `null gjenstående dager ved oppnådd maksdato`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        forlengVedtak(1.april, 30.april)
        forlengVedtak(1.mai, 31.mai)
        forlengVedtak(1.juni, 30.juni)
        forlengVedtak(1.juli, 31.juli)
        forlengVedtak(1.august, 31.august)
        forlengVedtak(1.september, 30.september)
        forlengVedtak(1.oktober, 31.oktober)
        forlengVedtak(1.november, 30.november)
        forlengVedtak(1.desember, 31.desember) //Maksdato nådd

        håndterSykmelding(Sykmeldingsperiode(1.januar(2019), 31.januar(2019), 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar(2019), 31.januar(2019), 100.prosent))
        håndterUtbetalingsgrunnlag(13.vedtaksperiode)
        håndterYtelser(13.vedtaksperiode)
        håndterUtbetalingsgodkjenning(13.vedtaksperiode)
        håndterUtbetalt(13.vedtaksperiode)

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(0, vedtaksperiode.vilkår.sykepengedager.gjenståendeDager)
    }

    @Test
    fun `Skal ta med forkastede vedtaksperioder`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, utbetalingGodkjent = false) // Avvist av saksbehandler

        nyttVedtak(1.mars, 31.mars)

        val personDTO = speilApi()
        assertEquals(2, personDTO.arbeidsgivere.first().vedtaksperioder.size)
    }

    @Test
    fun `Skal ta med annullerte vedtaksperioder`() {
        nyttVedtak(1.januar, 31.januar)
        håndterAnnullerUtbetaling()

        val personDTO = speilApi()
        assertEquals(1, personDTO.arbeidsgivere.first().vedtaksperioder.size)
        assertEquals(TilstandstypeDTO.TilAnnullering, personDTO.arbeidsgivere[0].vedtaksperioder[0].tilstand)
    }

    @Test
    fun `lager ikke utbetalingshistorikkelement av forkastet utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterOverstyring() // Overstyring forkaster utbetaling og sender periode tilbake

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val personDTO = speilApi()
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        val utbetalingshistorikk = personDTO.arbeidsgivere.first().utbetalingshistorikk

        assertEquals(31, vedtaksperiode.utbetalingstidslinje.size)
        assertEquals(1, vedtaksperiode.beregningIder.size)
        assertEquals(1, utbetalingshistorikk.size)
        assertEquals(vedtaksperiode.beregningIder.first(), utbetalingshistorikk.first().beregningId)
        assertEquals("IKKE_UTBETALT", utbetalingshistorikk.first().utbetaling.status)
    }

    @Test
    fun `Setter tilstand Utbetalt på vedtaksperioden uavhengig av utbetaling i påfølgende vedtaksperioder`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 10.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 10.februar, 100.prosent), Søknad.Søknadsperiode.Arbeid(1.februar, 10.februar))

        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        val personDTO = speilApi()
        assertEquals(TilstandstypeDTO.Utbetalt, personDTO.arbeidsgivere[0].vedtaksperioder[0].tilstand)
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
        val (person, hendelser) = ingenutbetalingPåfølgendeBetaling()

        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(1, vedtaksperiode.aktivitetslogg.size)
        assertNotEquals(vedtaksperiode.id, vedtaksperiode.aktivitetslogg[0].vedtaksperiodeId)
    }

    @Test
    fun `Sender med varsler for alle tidligere tilstøtende perioder som er avsluttet uten utbetaling`() { //Siden warnings ofte ligger på den første
        val (person, hendelser) = toPerioderIngenUtbetalingDeretterUtbetaling()

        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere.first().vedtaksperioder.last() as VedtaksperiodeDTO
        assertEquals(2, vedtaksperiode.aktivitetslogg.size)
        assertEquals(
            "Utdanning oppgitt i perioden i søknaden. Vurder rett til sykepenger og korriger sykmeldingsperioden",
            vedtaksperiode.aktivitetslogg[0].melding
        )
        assertEquals(
            "Utenlandsopphold oppgitt i perioden i søknaden. Vurder rett til sykepenger og korriger sykmeldingperioden",
            vedtaksperiode.aktivitetslogg[1].melding
        )
        assertNotEquals(vedtaksperiode.id, vedtaksperiode.aktivitetslogg[0].vedtaksperiodeId)
        assertNotEquals(vedtaksperiode.id, vedtaksperiode.aktivitetslogg[1].vedtaksperiodeId)
    }

    @Test
    fun `legger ved kildeId sammen med dag i tidslinja`() {
        val (person, hendelser) = person()
        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[0] as VedtaksperiodeDTO
        assertEquals(UUID.fromString(hendelser[1].id), vedtaksperiode.sykdomstidslinje[0].kilde.kildeId)
    }

    @Test
    fun `egen tilstandstype for perioder med kun fravær - feriedager`() {
        val (person, hendelser) = andrePeriodeKunFerie()
        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[1] as VedtaksperiodeDTO
        assertTrue(vedtaksperiode.fullstendig)
        assertEquals(TilstandstypeDTO.KunFerie, vedtaksperiode.tilstand)
    }

    @Test
    fun `egen tilstandstype for perioder med kun fravær - permisjonsdager (gir warning) `() {
        val (person, hendelser) = andrePeriodeKunPermisjon()
        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[1] as VedtaksperiodeDTO
        assertTrue(vedtaksperiode.fullstendig)
        assertEquals(TilstandstypeDTO.KunFerie, vedtaksperiode.tilstand)
    }

    @Test
    fun `perioder med søknad arbeidsgiver blir ufullstendig`() {
        val (person, hendelser) = kunSøknadArbeidsgiver()
        val personDTO = serializePersonForSpeil(person, hendelser)
        val vedtaksperiode = personDTO.arbeidsgivere[0].vedtaksperioder[0] as UfullstendigVedtaksperiodeDTO
        assertFalse(vedtaksperiode.fullstendig)
        assertEquals(TilstandstypeDTO.IngenUtbetaling, vedtaksperiode.tilstand)
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
            håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
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
            Inntektsopplysning(orgnr1, 20.januar(2021), inntekt, true),
            Inntektsopplysning(orgnr2, 20.januar(2021), inntekt, true)
        )

        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(orgnr1, 20.januar(2021), 26.januar(2021), 100.prosent, inntekt),
            ArbeidsgiverUtbetalingsperiode(orgnr2, 20.januar(2021), 26.januar(2021), 100.prosent, inntekt)
        )

        person.håndter(utbetalingshistorikk(vedtaksperiodeId = vedtaksperiodeId1, utbetalinger = utbetalinger, orgnummer = orgnr1))
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId1, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId1)), orgnummer = orgnr1))
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
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId2, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId2)), orgnummer = orgnr2))
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
    fun `markerer forkastede vedtaksperioder som forkastet`() {
        val person = Person(aktørId, fnr)

        person.håndter(sykmelding(fom = 1.januar).first)
        person.håndter(søknad(fom = 1.januar).first)
        person.håndter(inntektsmelding(fom = 1.januar).first)
        val vedtaksperiodeId = person.collectVedtaksperiodeIder(orgnummer).last()
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
        person.håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
        person.håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
        person.håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
        person.håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
        val utbetalingFagsystemId = person.collectUtbetalingFagsystemIDer().first()
        person.håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, utbetalingID = utbetalingFagsystemId.utbetalingId))
        person.håndter(overføring(utbetalingFagsystemID = utbetalingFagsystemId))
        person.håndter(utbetalt(utbetalingFagsystemID = utbetalingFagsystemId))

        // forkast periode
        person.håndter(annullering(utbetalingFagsystemId.arbeidsgiversFagsystemId))

        val serialisertPerson = serializePersonForSpeil(person)
        val vedtaksperiode = serialisertPerson.arbeidsgivere.first().vedtaksperioder.first()
        assertTrue(vedtaksperiode.erForkastet)
    }

    @Test
    fun `markerer vedtaksperioder som ikke forkastet`() {
        val person = Person(aktørId, fnr)

        person.håndter(sykmelding(fom = 1.januar).first)
        person.håndter(søknad(fom = 1.januar).first)
        person.håndter(inntektsmelding(fom = 1.januar).first)
        val vedtaksperiodeId = person.collectVedtaksperiodeIder(orgnummer).last()
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
        person.håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
        person.håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
        person.håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
        person.håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
        val utbetalingFagsystemId = person.collectUtbetalingFagsystemIDer().first()
        person.håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, utbetalingID = utbetalingFagsystemId.utbetalingId))
        person.håndter(overføring(utbetalingFagsystemID = utbetalingFagsystemId))
        person.håndter(utbetalt(utbetalingFagsystemID = utbetalingFagsystemId))

        val serialisertPerson = serializePersonForSpeil(person)
        val vedtaksperiode = serialisertPerson.arbeidsgivere.first().vedtaksperioder.first()
        assertFalse(vedtaksperiode.erForkastet)
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
            Inntektsopplysning(orgnr1, 20.januar(2021), inntekt, true),
            Inntektsopplysning(orgnr2, 20.januar(2021), inntekt, true)
        )

        val utbetalinger = listOf(
            ArbeidsgiverUtbetalingsperiode(orgnr1, 20.januar(2021), 26.januar(2021), 100.prosent, inntekt),
            ArbeidsgiverUtbetalingsperiode(orgnr2, 20.januar(2021), 26.januar(2021), 100.prosent, inntekt)
        )

        person.håndter(utbetalingshistorikk(vedtaksperiodeId = vedtaksperiodeId1, utbetalinger = utbetalinger, orgnummer = orgnr1))
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId1, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId2)), orgnummer = orgnr1))
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
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId2, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId2)), orgnummer = orgnr2))
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
                andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANNET"))
            ).first
        )
        assertTrue(serializePersonForSpeil(person).arbeidsgivere.isEmpty())
    }

    @Test
    fun `Dødsdato ligger på person`() {
        val fom = 1.januar
        val tom = 31.januar
        val person = Person(aktørId, fnr)

        sykmelding(fom = fom, tom = tom).also { (sykmelding, _) ->
            person.håndter(sykmelding)
        }
        person.fangeVedtaksperiodeId()
        søknad(
            hendelseId = UUID.randomUUID(),
            fom = fom,
            tom = tom,
            sendtSøknad = fom.plusDays(1).atStartOfDay()
        ).also { (søknad, _) ->
            person.håndter(søknad)
        }
        person.håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId, dødsdato = 1.januar))

        assertEquals(1.januar, serializePersonForSpeil(person).dødsdato)
    }

    @Test
    fun `Forlengelse får med warnings fra vilkårsprøving gjort i forrige periode`() {
        val fom = 1.januar
        val tom = 31.januar
        val forlengelseFom = 1.februar
        val forlengelseTom = 28.februar
        val person = Person(aktørId, fnr)
        sykmelding(fom = fom, tom = tom).also { (sykmelding) ->
            person.håndter(sykmelding)
        }
        person.fangeVedtaksperiodeId()
        søknad(
            hendelseId = UUID.randomUUID(),
            fom = fom,
            tom = tom,
            sendtSøknad = fom.plusDays(1).atStartOfDay()
        ).also { (søknad, _) -> person.håndter(søknad) }
        inntektsmelding(
            fom = fom,
            refusjon = Inntektsmelding.Refusjon(opphørsdato = null, inntekt = 1000.månedlig, endringerIRefusjon = emptyList()),
            beregnetInntekt = 1000.månedlig
        ).also { (inntektsmelding, _) -> person.håndter(inntektsmelding) }
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
        person.håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
        person.håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId, inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
            1.januar(2017) til 1.desember(2017) inntekter {
                orgnummer inntekt 1000.månedlig
            }
        })))
        person.håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
        person.håndter(
            utbetalingsgodkjenning(
                vedtaksperiodeId = vedtaksperiodeId,
                automatiskBehandling = false,
                aktivitetslogg = person.aktivitetslogg
            )
        )

        sykmelding(
            fom = forlengelseFom,
            tom = forlengelseTom
        ).also { (sykmelding, _) -> person.håndter(sykmelding) }
        person.fangeVedtaksperiodeId()
        søknad(
            hendelseId = UUID.randomUUID(),
            fom = forlengelseFom,
            tom = forlengelseTom,
            sendtSøknad = forlengelseTom.atStartOfDay()
        ).also { (søknad, _) -> person.håndter(søknad) }
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
        person.håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
        person.håndter(
            utbetalingsgodkjenning(
                vedtaksperiodeId = vedtaksperiodeId,
                automatiskBehandling = false,
                aktivitetslogg = person.aktivitetslogg
            )
        )

        val serialisertVedtaksperiode = serializePersonForSpeil(person).arbeidsgivere.single().vedtaksperioder.last() as VedtaksperiodeDTO
        assertTrue(serialisertVedtaksperiode.aktivitetslogg.any { it.melding == "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag" })
    }

    @Test
    fun `Vedtaksperioder fra flere arbeidsgivere får samme vilkårsgrunnlag-warnings`() {
        val fom = 1.januar
        val tom = 31.januar
        val person = Person(aktørId, fnr)
        person.håndter(sykmelding(orgnummer = orgnummer, fom = fom, tom = tom).first)
        person.håndter(sykmelding(orgnummer = orgnummer2, fom = fom, tom = tom, grad = 100.prosent).first)
        val vedtaksperiodeId1 = person.collectVedtaksperiodeIder(orgnummer).last()
        val vedtaksperiodeId2 = person.collectVedtaksperiodeIder(orgnummer2).last()
        person.håndter(
            søknad(
                hendelseId = UUID.randomUUID(),
                fom = fom,
                tom = tom,
                sendtSøknad = fom.plusDays(1).atStartOfDay()
            ).first
        )
        person.håndter(
            inntektsmelding(
                organisasjonsnummer = orgnummer,
                fom = fom,
                refusjon = Inntektsmelding.Refusjon(opphørsdato = null, inntekt = 1000.månedlig, endringerIRefusjon = emptyList()),
                beregnetInntekt = 1000.månedlig
            ).first
        )
        person.håndter(
            inntektsmelding(
                organisasjonsnummer = orgnummer2,
                fom = fom,
                refusjon = Inntektsmelding.Refusjon(opphørsdato = null, inntekt = 1000.månedlig, endringerIRefusjon = emptyList()),
                beregnetInntekt = 1000.månedlig
            ).first
        )
        person.håndter(søknad(orgnummer = orgnummer2, fom = fom, tom = tom, grad = 100.prosent).first)
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId2, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId2)), orgnummer = orgnummer2))
        person.håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId2, orgnummer = orgnummer2))
        person.håndter(
            vilkårsgrunnlag(
                vedtaksperiodeId = vedtaksperiodeId2,
                inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        orgnummer inntekt 1000.månedlig
                        orgnummer2 inntekt 1000.månedlig
                    }
                }),
                organisasjonsnummer = orgnummer2
            )
        )

        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId1, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId1)), orgnummer = orgnummer))
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId1,
                orgnummer = orgnummer
            )
        )

        person.håndter(
            utbetalingsgodkjenning(
                vedtaksperiodeId = vedtaksperiodeId1,
                automatiskBehandling = false,
                aktivitetslogg = person.aktivitetslogg
            )
        )
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId2, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId2)), orgnummer = orgnummer2))
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId2,
                orgnummer = orgnummer2
            )
        )
        person.håndter(
            utbetalingsgodkjenning(
                vedtaksperiodeId = vedtaksperiodeId2,
                automatiskBehandling = false,
                aktivitetslogg = person.aktivitetslogg
            )
        )

        val vedtaksperioder = serializePersonForSpeil(person)
            .arbeidsgivere.flatMap { it.vedtaksperioder }

        assertEquals(2, vedtaksperioder.size)
        assertTrue((vedtaksperioder.first() as VedtaksperiodeDTO).aktivitetslogg.any { it.melding == "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag" })
        assertTrue((vedtaksperioder.last() as VedtaksperiodeDTO).aktivitetslogg.any { it.melding == "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag" })

    }

    @Test
    fun `Begge arbeidsgivere har beregningsId og tilsvarende utbetalingshistorikkelement når første sendes til godkjenning`() {
        val fom = 1.januar
        val tom = 31.januar
        val person = Person(aktørId, fnr)
        person.håndter(sykmelding(orgnummer = orgnummer, fom = fom, tom = tom).first)
        person.håndter(sykmelding(orgnummer = orgnummer2, fom = fom, tom = tom, grad = 100.prosent).first)
        val vedtaksperiodeId1 = person.collectVedtaksperiodeIder(orgnummer).last()
        val vedtaksperiodeId2 = person.collectVedtaksperiodeIder(orgnummer2).last()
        person.håndter(
            søknad(
                hendelseId = UUID.randomUUID(),
                fom = fom,
                tom = tom,
                sendtSøknad = fom.plusDays(1).atStartOfDay()
            ).first
        )
        person.håndter(
            inntektsmelding(
                organisasjonsnummer = orgnummer,
                fom = fom,
                refusjon = Inntektsmelding.Refusjon(opphørsdato = null, inntekt = 31000.månedlig, endringerIRefusjon = emptyList()),
                beregnetInntekt = 31000.månedlig
            ).first
        )
        person.håndter(
            inntektsmelding(
                organisasjonsnummer = orgnummer2,
                fom = fom,
                refusjon = Inntektsmelding.Refusjon(opphørsdato = null, inntekt = 31000.månedlig, endringerIRefusjon = emptyList()),
                beregnetInntekt = 31000.månedlig
            ).first
        )
        person.håndter(søknad(orgnummer = orgnummer2, fom = fom, tom = tom, grad = 100.prosent).first)
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId2, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId2)), orgnummer = orgnummer2))
        person.håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId2, orgnummer = orgnummer2))
        person.håndter(
            vilkårsgrunnlag(
                vedtaksperiodeId = vedtaksperiodeId2,
                inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        orgnummer inntekt 31000.månedlig
                        orgnummer2 inntekt 31000.månedlig
                    }
                }),
                organisasjonsnummer = orgnummer2
            )
        )
        person.håndter(utbetalingsgrunnlag(vedtaksperiodeId1, person.fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId1)), orgnummer = orgnummer))
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId1,
                orgnummer = orgnummer
            )
        )
        person.håndter(
            simulering(
                vedtaksperiodeId = vedtaksperiodeId1,
                orgnummer = orgnummer
            )
        )

        assertBeregningsider(person)

        person.håndter(
            utbetalingsgodkjenning(
                vedtaksperiodeId = vedtaksperiodeId1,
                automatiskBehandling = false,
                aktivitetslogg = person.aktivitetslogg
            )
        )
        person.håndter(
            ytelser(
                vedtaksperiodeId = vedtaksperiodeId2,
                orgnummer = orgnummer2
            )
        )
        person.håndter(
            simulering(
                vedtaksperiodeId = vedtaksperiodeId2,
                orgnummer = orgnummer2
            )
        )

        assertBeregningsider(person)
    }

    @Test
    fun `tar med annulleringer som separate historikkelementer()`() {
        val (person, hendelser) = personToPerioderAnnullert()
        val personDto = serializePersonForSpeil(person, hendelser)
        val utbetalingshistorikk = personDto.arbeidsgivere.first().utbetalingshistorikk
        assertEquals(3, utbetalingshistorikk.size)
        val annulleringElement = utbetalingshistorikk.first { it.utbetaling.erAnnullering() }
        assertFalse(personDto.arbeidsgivere.flatMap { it.vedtaksperioder.flatMap { vedtaksperiode -> (vedtaksperiode as VedtaksperiodeDTO).beregningIder } }
            .contains(annulleringElement.beregningId))
    }

    private fun <T> Collection<T>.assertOnNonEmptyCollection(func: (T) -> Unit) {
        assertTrue(isNotEmpty())
        forEach(func)
    }

    private fun personToPerioderAnnullert(): Pair<Person, List<HendelseDTO>> = Person(aktørId, fnr).run {
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
            håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
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
            håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
            håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
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

    private data class UtbetalingFagsystemID(val utbetalingId: UUID, val arbeidsgiversFagsystemId: String)

    private fun Person.collectUtbetalingFagsystemIDer(): MutableList<UtbetalingFagsystemID> {
        val utbetalingerIder = mutableListOf<UtbetalingFagsystemID>()
        accept(object : PersonVisitor {
            override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
                utbetalinger.forEach {
                    it.accept(object : UtbetalingVisitor {
                        override fun preVisitUtbetaling(
                            utbetaling: Utbetaling,
                            id: UUID,
                            beregningId: UUID,
                            type: Utbetaling.Utbetalingtype,
                            tilstand: Utbetaling.Tilstand,
                            tidsstempel: LocalDateTime,
                            oppdatert: LocalDateTime,
                            arbeidsgiverNettoBeløp: Int,
                            personNettoBeløp: Int,
                            maksdato: LocalDate,
                            forbrukteSykedager: Int?,
                            gjenståendeSykedager: Int?
                        ) {
                            utbetalingerIder.add(
                                UtbetalingFagsystemID(
                                    utbetalingId = id,
                                    arbeidsgiversFagsystemId = utbetaling.arbeidsgiverOppdrag().fagsystemId()
                                )
                            )
                        }
                    })
                }
            }
        })
        return utbetalingerIder
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
                hendelseIder: Set<UUID>,
                inntektsmeldingInfo: InntektsmeldingInfo?,
                inntektskilde: Inntektskilde
            ) {
                currentArbeidsgiver.add(id.toString())
            }
        })
    }.getValue(orgnummer)

    @BeforeEach
    fun beforeEach() {
        vedtaksperiodeIder.clear()
        utbetalingsliste.clear()
    }

    private fun assertBeregningsider(person: Person) {
        val personDTO = serializePersonForSpeil(person)
        val arbeidsgiver1 = personDTO.arbeidsgivere.first()
        val arbeidsgiver2 = personDTO.arbeidsgivere.last()
        val vedtaksperiode1 = arbeidsgiver1.vedtaksperioder.first() as VedtaksperiodeDTO
        val vedtaksperiode2 = arbeidsgiver2.vedtaksperioder.first() as VedtaksperiodeDTO

        assertEquals(1, vedtaksperiode1.beregningIder.size)
        assertEquals(1, vedtaksperiode2.beregningIder.size)

        assertTrue(arbeidsgiver1.utbetalingshistorikk.map { it.beregningId }.contains(vedtaksperiode1.beregningIder.first()))
        assertTrue(arbeidsgiver2.utbetalingshistorikk.map { it.beregningId }.contains(vedtaksperiode2.beregningIder.first()))
    }

    companion object {
        private const val aktørId = "12345"
        private const val fnr = "12020052345"
        private const val orgnummer = "987654321"
        private const val orgnummer2 = "123456789"
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
                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
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
                        håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
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
                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
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
                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId, arbeidsavklaringspenger = listOf(fom.minusDays(60) til tom.minusDays(60))))
                    håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId, arbeidsavklaringspenger = listOf(fom.minusDays(60) til tom.minusDays(60))))
                    fangeUtbetalinger()
                    håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
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
                    søknad(
                        hendelseId = søknadhendelseId,
                        fom = 1.januar,
                        tom = 9.januar,
                        andrePerioder = listOf(Søknad.Søknadsperiode.Utdanning(3.januar, 4.januar))
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
                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId, medlemskapstatus = medlemskapstatus))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))


                    fangeVedtaksperiodeId()
                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))

                    håndter(simulering(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
                    fangeUtbetalinger()
                    håndter(overføring(this@run.aktivitetslogg))
                    håndter(utbetalt(this@run.aktivitetslogg))
                }
            }

        private fun toPerioderIngenUtbetalingDeretterUtbetaling(
            søknadhendelseId: UUID = UUID.randomUUID(),
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja
        ): Pair<Person, List<HendelseDTO>> =
            Person(aktørId, fnr).run {
                this to mutableListOf<HendelseDTO>().apply {
                    sykmelding(fom = 1.januar, tom = 9.januar).also { (sykmelding, sykmeldingDto) ->
                        håndter(sykmelding)
                        add(sykmeldingDto)
                    }
                    søknad( //Sender inn vanlig søknad i stedet for arbeidsgiversøknad for å kunne opprette warnings
                        hendelseId = søknadhendelseId,
                        fom = 1.januar,
                        tom = 9.januar,
                        andrePerioder = listOf(Søknad.Søknadsperiode.Utdanning(3.januar, 4.januar))
                    ).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    fangeVedtaksperiodeId()
                    sykmelding(fom = 10.januar, tom = 14.januar).also { (sykmelding, sykmeldingDto) ->
                        håndter(sykmelding)
                        add(sykmeldingDto)
                    }
                    søknad(
                        hendelseId = søknadhendelseId,
                        fom = 10.januar,
                        tom = 14.januar,
                        andrePerioder = listOf(Søknad.Søknadsperiode.Utlandsopphold(11.januar, 12.januar))
                    ).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    inntektsmelding(fom = 1.januar).also { (inntektsmelding, inntektsmeldingDTO) ->
                        håndter(inntektsmelding)
                        add(inntektsmeldingDTO)
                    }
                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId, medlemskapstatus = medlemskapstatus))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))

                    fangeVedtaksperiodeId()

                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(vilkårsgrunnlag(vedtaksperiodeId = vedtaksperiodeId, medlemskapstatus = medlemskapstatus))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))

                    sykmelding(fom = 15.januar, tom = 25.januar).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    søknad(fom = 15.januar, tom = 25.januar).also { (søknad, søknadDTO) ->
                        håndter(søknad)
                        add(søknadDTO)
                    }

                    fangeVedtaksperiodeId()
                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
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
                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
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
                        andrePerioder = listOf(Ferie(25.januar, 31.januar))
                    ).also { (søknad, søknadDTO) ->
                        håndter(søknad)
                        add(søknadDTO)
                    }
                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                }
            }

        private fun andrePeriodeKunPermisjon(
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Pair<Person, List<HendelseDTO>> =
            Person(aktørId, fnr).run {
                this to mutableListOf<HendelseDTO>().apply {
                    sykmelding(fom = 1.januar, tom = 24.januar).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    fangeVedtaksperiodeId()
                    søknad(
                        hendelseId = søknadhendelseId,
                        fom = 1.januar,
                        tom = 24.januar
                    ).also { (søknad, søknadNavDTO) ->
                        håndter(søknad)
                        add(søknadNavDTO)
                    }
                    inntektsmelding(fom = 1.januar).also { (inntektsmelding, inntektsmeldingDTO) ->
                        håndter(inntektsmelding)
                        add(inntektsmeldingDTO)
                    }

                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
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
                        andrePerioder = listOf(Permisjon(25.januar, 31.januar))
                    ).also { (søknad, søknadDTO) ->
                        håndter(søknad)
                        add(søknadDTO)
                    }
                    håndter(utbetalingsgrunnlag(vedtaksperiodeId, fangSkjæringstidspunkt(UUID.fromString(vedtaksperiodeId))))
                    håndter(ytelser(vedtaksperiodeId = vedtaksperiodeId))
                    håndter(utbetalingsgodkjenning(vedtaksperiodeId = vedtaksperiodeId, aktivitetslogg = this@run.aktivitetslogg))
                }
            }

        private fun kunSøknadArbeidsgiver(
            søknadhendelseId: UUID = UUID.randomUUID()
        ): Pair<Person, List<HendelseDTO>> =
            Person(aktørId, fnr).run {
                this to mutableListOf<HendelseDTO>().apply {
                    sykmelding(fom = 1.januar, tom = 16.januar).also { (sykmelding, sykmeldingDTO) ->
                        håndter(sykmelding)
                        add(sykmeldingDTO)
                    }
                    fangeVedtaksperiodeId()
                    søknadSendtTilArbeidsgiver(
                        hendelseId = søknadhendelseId,
                        fom = 1.januar,
                        tom = 16.januar
                    ).also { (søknadArbeidsgiver, søknadArbeidsgiverDTO) ->
                        håndter(søknadArbeidsgiver)
                        add(søknadArbeidsgiverDTO)
                    }
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
                    hendelseIder: Set<UUID>,
                    inntektsmeldingInfo: InntektsmeldingInfo?,
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
        private fun Person.fangSkjæringstidspunkt(vedtaksperiodeId: UUID): LocalDate {
            lateinit var skjæringstidpunkt: LocalDate
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
                    if (id == vedtaksperiodeId) {
                        skjæringstidpunkt = skjæringstidspunkt
                    }
                }
            })
            return skjæringstidpunkt
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
            sykmeldingSkrevet = fom.atStartOfDay(),
            mottatt = tom.atStartOfDay()
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
            grad: Prosentdel = 100.prosent,
            andreInntektskilder: List<Søknad.Inntektskilde> = emptyList()
        ) = Søknad(
            meldingsreferanseId = hendelseId,
            fnr = fnr,
            aktørId = aktørId,
            orgnummer = orgnummer,
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(fom, tom, grad)) + andrePerioder,
            andreInntektskilder = andreInntektskilder,
            sendtTilNAV = sendtSøknad,
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
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
            perioder = listOf(SøknadArbeidsgiver.Søknadsperiode(fom, tom, 100.prosent, 0.prosent)),
            sykmeldingSkrevet = LocalDateTime.now()
        ) to SøknadArbeidsgiverDTO(
            id = hendelseId.toString(),
            fom = fom,
            tom = tom,
            rapportertdato = tom.atStartOfDay(),
            sendtArbeidsgiver = sendtTilArbeidsgiver
        )

        private fun inntektsmelding(
            hendelseId: UUID = UUID.randomUUID(),
            fom: LocalDate,
            refusjon: Inntektsmelding.Refusjon = Inntektsmelding.Refusjon(
                opphørsdato = null,
                inntekt = 31000.månedlig,
                endringerIRefusjon = emptyList()
            ),
            beregnetInntekt: Inntekt = 31000.månedlig,
            organisasjonsnummer: String = orgnummer
        ) = Inntektsmelding(
            meldingsreferanseId = hendelseId,
            refusjon = refusjon,
            orgnummer = organisasjonsnummer,
            fødselsnummer = fnr,
            aktørId = aktørId,
            førsteFraværsdag = fom,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = listOf(Periode(fom, fom.plusDays(15))),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        ) to InntektsmeldingDTO(
            id = hendelseId.toString(),
            beregnetInntekt = beregnetInntekt.reflection { _, månedlig, _, _ -> månedlig },
            mottattDato = fom.atStartOfDay()
        )

        private fun vilkårsgrunnlag(
            vedtaksperiodeId: String,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
            inntektsvurdering: Inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt 31000.månedlig
                }
            }),
            organisasjonsnummer: String = orgnummer
        ) = Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = organisasjonsnummer,
            inntektsvurdering = inntektsvurdering,
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Arbeidsforhold(
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
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt 31000.månedlig
                }
                1.januar(2017) til 1.januar(2017) inntekter {
                    orgnummer2 inntekt 1
                }
            }),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Arbeidsforhold(
                        orgnummer,
                        1.januar(2017)
                    )
                )
            ),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja)
        )

        private fun utbetalingsgrunnlag(
            vedtaksperiodeId: String,
            skjæringstidspunkt: LocalDate,
            orgnummer: String = this.orgnummer,
            inntekter: List<ArbeidsgiverInntekt> = listOf(
                ArbeidsgiverInntekt(orgnummer, (0..3).map {
                    val yearMonth = YearMonth.from(skjæringstidspunkt).minusMonths(3L - it)
                    ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                        yearMonth = yearMonth,
                        type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                        inntekt = INNTEKT,
                        fordel = "juidy fordel",
                        beskrivelse = "juicy beskrivelse"
                    )
                })
            )
        ) = Utbetalingsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId,
            fnr,
            orgnummer,
            UUID.fromString(vedtaksperiodeId),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = listOf(Arbeidsforhold(orgnummer, 1.januar, null))
        )

        private fun ytelser(
            hendelseId: UUID = UUID.randomUUID(),
            vedtaksperiodeId: String,
            orgnummer: String = SpeilBuilderTest.orgnummer,
            utbetalinger: List<Infotrygdperiode> = listOf(),
            inntektshistorikk: List<Inntektsopplysning> = listOf(),
            arbeidsavklaringspenger: List<Periode> = emptyList(),
            dødsdato: LocalDate? = null
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
                dødsinfo = Dødsinfo(dødsdato),
                arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
                dagpenger = Dagpenger(emptyList()),
                aktivitetslogg = it
            )
        }

        private fun utbetalingshistorikk(
            meldingsreferanseId: UUID = UUID.randomUUID(),
            vedtaksperiodeId: String,
            utbetalinger: List<Infotrygdperiode> = listOf(),
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
            harStatslønn = false,
            perioder = utbetalinger,
            aktivitetslogg = aktivitetslogg,
            inntektshistorikk = inntektshistorikk,
            ugyldigePerioder = emptyList(),
            besvart = LocalDateTime.now()
        )

        private fun utbetalingsgodkjenning(
            vedtaksperiodeId: String,
            utbetalingGodkjent: Boolean = true,
            automatiskBehandling: Boolean = false,
            utbetalingID: UUID
        ) =
            Utbetalingsgodkjenning(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = aktørId,
                fødselsnummer = fnr,
                organisasjonsnummer = orgnummer,
                utbetalingId = utbetalingID,
                vedtaksperiodeId = vedtaksperiodeId,
                saksbehandler = if (automatiskBehandling) "Automatisk behandlet" else "en_saksbehandler_ident",
                saksbehandlerEpost = "mille.mellomleder@nav.no",
                utbetalingGodkjent = utbetalingGodkjent,
                godkjenttidspunkt = LocalDateTime.now(),
                automatiskBehandling = automatiskBehandling,
            )

        private fun utbetalingsgodkjenning(
            vedtaksperiodeId: String,
            utbetalingGodkjent: Boolean = true,
            automatiskBehandling: Boolean = false,
            aktivitetslogg: IAktivitetslogg
        ) = utbetalingsgodkjenning(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingGodkjent = utbetalingGodkjent,
            automatiskBehandling = automatiskBehandling,
            utbetalingID = UUID.fromString(aktivitetslogg.behov().last { it.type == Behovtype.Godkjenning }.kontekst().getValue("utbetalingId"))
        )


        private fun simulering(vedtaksperiodeId: String, orgnummer: String = Companion.orgnummer, simuleringOk: Boolean = true) = Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            simuleringOK = simuleringOk,
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

        private fun overføring(utbetalingFagsystemID: UtbetalingFagsystemID) = UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            fagsystemId = utbetalingFagsystemID.arbeidsgiversFagsystemId,
            utbetalingId = utbetalingFagsystemID.utbetalingId.toString(),
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )

        private fun overføring(aktivitetslogg: IAktivitetslogg) =
            overføring(
                UtbetalingFagsystemID(
                    UUID.fromString(aktivitetslogg.behov().last { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId")),
                    utbetalingsliste.getValue(orgnummer).last().arbeidsgiverOppdrag().fagsystemId()
                )
            )


        private fun utbetalt(utbetalingFagsystemID: UtbetalingFagsystemID) = UtbetalingHendelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fnr,
            orgnummer = orgnummer,
            fagsystemId = utbetalingFagsystemID.arbeidsgiversFagsystemId,
            utbetalingId = utbetalingFagsystemID.utbetalingId.toString(),
            status = UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )

        private fun utbetalt(aktivitetslogg: IAktivitetslogg) =
            utbetalt(
                UtbetalingFagsystemID(
                    UUID.fromString(aktivitetslogg.behov().last { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId")),
                    utbetalingsliste.getValue(orgnummer).last().arbeidsgiverOppdrag().fagsystemId()
                )
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

