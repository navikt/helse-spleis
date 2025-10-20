package no.nav.helse.spleis.graphql

import java.time.LocalDate.EPOCH
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.dto.AnnulleringskandidatDto
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.speil.dto.AnnullertPeriode
import no.nav.helse.spleis.speil.dto.BeregnetPeriode
import no.nav.helse.spleis.speil.dto.InfotrygdVilkårsgrunnlag
import no.nav.helse.spleis.speil.dto.Inntektkilde
import no.nav.helse.spleis.speil.dto.Periodetilstand
import no.nav.helse.spleis.speil.dto.PersonDTO
import no.nav.helse.spleis.speil.dto.SammenslåttDag
import no.nav.helse.spleis.speil.dto.SpleisVilkårsgrunnlag
import no.nav.helse.spleis.speil.dto.Sykdomstidslinjedag
import no.nav.helse.spleis.speil.dto.SykdomstidslinjedagKildetype
import no.nav.helse.spleis.speil.dto.SykdomstidslinjedagType
import no.nav.helse.spleis.speil.dto.UberegnetPeriode
import no.nav.helse.spleis.speil.dto.Utbetalingsinfo
import no.nav.helse.spleis.speil.dto.UtbetalingstidslinjedagType
import no.nav.helse.spleis.testhelpers.OverstyrtArbeidsgiveropplysning
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SpeilBuilderTest : AbstractSpeilBuilderTest() {

    @Test
    fun `Skjæringstidspunkt er etter ny g, men før virkningstidspunkt for gyldigSomMinsteinntektKrav`() {
        håndterSøknad(28.mai(2025) til 13.juni(2025))
        håndterArbeidsgiveropplysninger(listOf(28.mai(2025) til 12.juni(2025)), beregnetInntekt = 64000.årlig)
        håndterVilkårsgrunnlag()
        håndterYtelser()
        val vilkårsgrunnlag = speilApi().vilkårsgrunnlag.values.first() as SpleisVilkårsgrunnlag
        assertTrue(vilkårsgrunnlag.oppfyllerKravOmMinstelønn)
    }

    @Test
    fun `Skjæringstidspunkt er etter ny g, og etter virkningstidspunkt for gyldigSomMinsteinntektKrav`() {
        håndterSøknad(3.juni(2025) til 25.juni(2025))
        håndterArbeidsgiveropplysninger(listOf(3.juni(2025) til 18.juni(2025)), beregnetInntekt = 64000.årlig)
        håndterVilkårsgrunnlag()
        håndterYtelser()
        val vilkårsgrunnlag = speilApi().vilkårsgrunnlag.values.first() as SpleisVilkårsgrunnlag
        assertFalse(vilkårsgrunnlag.oppfyllerKravOmMinstelønn)
    }


    @Test
    fun `mapper ut annulleringskandidater på beregnede perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        val annulleringskandidaterJanuarVedtaksperiode = (speilApi().arbeidsgivere.first().generasjoner.first().perioder.last() as BeregnetPeriode).annulleringskandidater
        val annulleringskandidaterFebruarVedtaksperiode = (speilApi().arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).annulleringskandidater

        val forventetJanuar = listOf(AnnulleringskandidatDto(1.vedtaksperiode(a1), a1, 1.januar, 31.januar), AnnulleringskandidatDto(2.vedtaksperiode(a1), a1, 1.februar, 28.februar))
        val forventetFebruar = listOf(AnnulleringskandidatDto(2.vedtaksperiode(a1), a1, 1.februar, 28.februar))
        assertEquals(forventetJanuar, annulleringskandidaterJanuarVedtaksperiode)
        assertEquals(forventetFebruar, annulleringskandidaterFebruarVedtaksperiode)
    }

    @Test
    fun `selv om vi har refusjonsopplysninger fra begge arbeidsgivere mappes det kun ut for den som har beregnet perioder`() {
        håndterSøknad(1.januar til 10.januar, a1) // AUU

        håndterSøknad(11.januar til 26.januar, a2) // AUU
        håndterSøknad(11.januar til 26.januar, a1) // Skal utbetales

        håndterSøknad(27.januar til 27.februar, a2) // Skal utbetales

        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), orgnummer = a1, vedtaksperiode = 2)
        håndterArbeidsgiveropplysninger(listOf(11.januar til 26.januar), orgnummer = a2, vedtaksperiode = 2)
        håndterVilkårsgrunnlag(listOf(a1 to INNTEKT, a2 to INNTEKT))
        håndterYtelser()
        håndterSimulering()

        val arbeidsgiverrefusjoner = speilApi().vilkårsgrunnlag.values.single().arbeidsgiverrefusjoner
        assertEquals(1, arbeidsgiverrefusjoner.size)
        assertEquals(a1, arbeidsgiverrefusjoner.single().arbeidsgiver)
    }

    @Test
    fun `totalgrad må jo være lik for avslag og utbetaling`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 19.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(1.januar, orgnummer = a1, beregnetInntekt = 836352.årlig)
        håndterVilkårsgrunnlag(arbeidsgivere = listOf(a1 to 836352.årlig, a2 to 168276.årlig))
        håndterYtelser()

        speilApi().assertTotalgrad(15.81748468089681, 17.januar til 19.januar, 22.januar til 26.januar, 29.januar til 31.januar)

        håndterMinimumSykdomsgradsvurderingMelding(perioderMedMinimumSykdomsgradVurdertOK = setOf(17.januar til 31.januar))
        håndterYtelser()

        speilApi().assertTotalgrad(15.81748468089681, 17.januar til 19.januar, 22.januar til 26.januar, 29.januar til 31.januar)
    }

    private fun PersonDTO.assertTotalgrad(forventet: Double, vararg perioder: Periode) {
        val totalgrader = (arbeidsgivere[0]
            .generasjoner[0]
            .perioder[0] as BeregnetPeriode)
            .sammenslåttTidslinje
            .filter { sammenslåttDag -> perioder.any { sammenslåttDag.dagen in it } }
            .map { it.utbetalingsinfo?.totalGrad }
        assertTrue(totalgrader.all { it == forventet }) { "Her er det noe som ikke er $forventet: $totalgrader" }
    }

    @Test
    fun `Dødsdato ligger på person`() {
        val dødsdato = 1.januar
        håndterDødsmelding(dødsdato)
        assertEquals(dødsdato, speilApi().dødsdato)
    }

    @Test
    fun `nav utbetaler agp`() {
        tilGodkjenning(1.januar, 31.januar)
        val id = UUID.randomUUID()
        håndterOverstyrTidslinje((1.januar til 16.januar).map {
            ManuellOverskrivingDag(it, Dagtype.SykedagNav, 100)
        }, meldingsreferanseId = id)
        håndterYtelserTilGodkjenning()
        val speilJson = speilApi()
        val tidslinje = speilJson.arbeidsgivere.single().generasjoner.single().perioder.single().sammenslåttTidslinje
        val forventetFørstedag = SammenslåttDag(
            dagen = 1.januar,
            sykdomstidslinjedagtype = SykdomstidslinjedagType.SYKEDAG_NAV,
            utbetalingstidslinjedagtype = UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag,
            kilde = Sykdomstidslinjedag.SykdomstidslinjedagKilde(SykdomstidslinjedagKildetype.Saksbehandler, id),
            grad = 100,
            utbetalingsinfo = Utbetalingsinfo(
                personbeløp = 0,
                arbeidsgiverbeløp = 2161,
                totalGrad = 100.0
            )
        )
        assertEquals(forventetFørstedag, tidslinje.first())
    }

    @Test
    fun `nav skal ikke utbetale agp for kort periode likevel - perioden går så til AUU`() {
        håndterSøknad(1.januar til 16.januar)
        håndterLpsInntektsmelding(1.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFravaer")
        håndterVilkårsgrunnlag()
        håndterYtelserTilGodkjenning()
        val idOverstyring = UUID.randomUUID()
        håndterOverstyrTidslinje((1.januar til 16.januar).map {
            ManuellOverskrivingDag(it, Dagtype.Sykedag, 100)
        }, meldingsreferanseId = idOverstyring)
        val speilJson = speilApi()
        val generasjoner = speilJson.arbeidsgivere.single().generasjoner
        assertEquals(2, generasjoner.size)
        val tidslinje = generasjoner[0].perioder.single().sammenslåttTidslinje
        val forventetFørstedag = SammenslåttDag(
            dagen = 1.januar,
            sykdomstidslinjedagtype = SykdomstidslinjedagType.SYKEDAG,
            utbetalingstidslinjedagtype = UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag,
            kilde = Sykdomstidslinjedag.SykdomstidslinjedagKilde(SykdomstidslinjedagKildetype.Saksbehandler, idOverstyring),
            grad = 100,
            utbetalingsinfo = null
        )
        assertEquals(forventetFørstedag, tidslinje.first())
    }

    @Test
    fun `Viser inntektsgrunnlag for arbeidsforhold som startet innen 3 måneder før skjæringstidspunktet, selvom vi ikke har inntekt`() {
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(1.januar, orgnummer = a1)
        håndterVilkårsgrunnlag(
            inntekter = listOf(a1 to 31000.månedlig),
            arbeidsforhold = listOf(a1 to EPOCH, a2 to 25.november(2017))
        )
        håndterYtelserTilGodkjenning()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()

        val personDto = speilApi()

        assertEquals(listOf(a1, a2).map(String::toString), personDto.arbeidsgivere.map { it.organisasjonsnummer })

        val vilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val arbeidsgiverInntektA2 = personDto.vilkårsgrunnlag[vilkårsgrunnlagId]?.inntekter?.first { it.organisasjonsnummer == a2 }

        assertEquals(0.0, arbeidsgiverInntektA2?.omregnetÅrsinntekt?.beløp)
        assertEquals(Inntektkilde.IkkeRapportert, arbeidsgiverInntektA2?.omregnetÅrsinntekt?.kilde)
    }

    @Test
    fun `beregnet periode peker på vilkårsgrunnlagid for infotrygdvilkårsgrunnlag`() {
        createOvergangFraInfotrygdPerson()
        forlengVedtak(1.mars, 31.mars)

        val infotrygdVilkårsgrunnlag = dto().vilkårsgrunnlagHistorikk.historikk.first().vilkårsgrunnlag.first { it.skjæringstidspunkt == 1.januar }
        val infotrygdVilkårsgrunnlagId = infotrygdVilkårsgrunnlag.vilkårsgrunnlagId

        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId

        assertEquals(speilVilkårsgrunnlagId, infotrygdVilkårsgrunnlagId)
    }

    @Test
    fun `annullert periode skal ikke ha vilkårsgrunnlagsId`() {
        val utbetaling = nyttVedtak(1.januar, 31.januar)
        håndterAnnullerUtbetaling(utbetaling)
        val personDto = speilApi()
        val generasjoner = personDto.arbeidsgivere.first().generasjoner
        assertEquals(2, generasjoner.size)
        assertInstanceOf(BeregnetPeriode::class.java, generasjoner.last().perioder.single())
        assertInstanceOf(AnnullertPeriode::class.java, generasjoner.first().perioder.single())
    }

    @Test
    fun `beregnet periode peker på et vilkårsgrunnlag`() {
        nyttVedtak(1.januar, 31.januar)
        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagId]
        assertTrue(vilkårsgrunnlag is SpleisVilkårsgrunnlag)
    }

    @Test
    fun `refusjon ligger på vilkårsgrunnlaget`() {
        nyttVedtak(1.januar, 31.januar)
        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagId] as? SpleisVilkårsgrunnlag
        assertTrue(vilkårsgrunnlag!!.arbeidsgiverrefusjoner.isNotEmpty())
        val arbeidsgiverrefusjon = vilkårsgrunnlag.arbeidsgiverrefusjoner.single()
        assertEquals(a1, arbeidsgiverrefusjon.arbeidsgiver)
        val refusjonsopplysning = arbeidsgiverrefusjon.refusjonsopplysninger.single()

        assertEquals(1.januar, refusjonsopplysning.fom)
        assertEquals(null, refusjonsopplysning.tom)
        assertEquals(INNTEKT, refusjonsopplysning.beløp.månedlig)
    }

    @Test
    fun `refusjon ligger på vilkårsgrunnlaget - også for infotrygd`() {
        createOvergangFraInfotrygdPerson()
        forlengVedtak(1.mars, 31.mars)
        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagId] as? InfotrygdVilkårsgrunnlag
        assertTrue(vilkårsgrunnlag!!.arbeidsgiverrefusjoner.isNotEmpty())
        val arbeidsgiverrefusjon = vilkårsgrunnlag.arbeidsgiverrefusjoner.single()
        assertEquals(a1, arbeidsgiverrefusjon.arbeidsgiver)
        val refusjonsopplysninger = arbeidsgiverrefusjon.refusjonsopplysninger
        assertEquals(1, refusjonsopplysninger.size)

        assertEquals(1.februar, refusjonsopplysninger[0].fom)
        assertEquals(null, refusjonsopplysninger[0].tom)
        assertEquals(31000.0, refusjonsopplysninger[0].beløp)
    }

    @Test
    fun `endring i refusjon frem i tid`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        val inntektsmeldingId = håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(
                INNTEKT, null, endringerIRefusjon = listOf(
                    Inntektsmelding.Refusjon.EndringIRefusjon(INGEN, 1.februar)
                )
            ),
        )
        håndterVilkårsgrunnlag()
        håndterYtelserTilGodkjenning()

        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagId] as? SpleisVilkårsgrunnlag
        assertTrue(vilkårsgrunnlag!!.arbeidsgiverrefusjoner.isNotEmpty())
        val arbeidsgiverrefusjon = vilkårsgrunnlag.arbeidsgiverrefusjoner.single()
        assertEquals(a1, arbeidsgiverrefusjon.arbeidsgiver)
        val refusjonsopplysninger = arbeidsgiverrefusjon.refusjonsopplysninger

        assertEquals(2, refusjonsopplysninger.size)

        assertEquals(1.januar, refusjonsopplysninger.first().fom)
        assertEquals(31.januar, refusjonsopplysninger.first().tom)
        assertEquals(INNTEKT, refusjonsopplysninger.first().beløp.månedlig)
        assertEquals(inntektsmeldingId, refusjonsopplysninger.first().meldingsreferanseId)
        assertEquals(1.februar, refusjonsopplysninger.last().fom)
        assertEquals(null, refusjonsopplysninger.last().tom)
        assertEquals(INGEN, refusjonsopplysninger.last().beløp.månedlig)
        assertEquals(inntektsmeldingId, refusjonsopplysninger.last().meldingsreferanseId)
    }

    @Test
    fun `saksbehandler endrer refusjon frem i tid`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            opplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    orgnummer = a1,
                    inntekt = INNTEKT,
                    refusjonsopplysninger = listOf(Triple(1.januar, 31.januar, INNTEKT), Triple(1.februar, null, INGEN))
                )
            )
        )
        var personDto = speilApi()
        val beregnetPeriode = personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode
        assertEquals(Periodetilstand.Utbetalt, beregnetPeriode.periodetilstand)
        val speilVilkårsgrunnlagId = beregnetPeriode.vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagId] as? SpleisVilkårsgrunnlag
        val refusjon = vilkårsgrunnlag!!.arbeidsgiverrefusjoner.single()
        val refusjonsopplysninger = refusjon.refusjonsopplysninger
        assertEquals(2, refusjonsopplysninger.size)
        assertEquals(1.januar, refusjonsopplysninger.first().fom)
        assertEquals(31.januar, refusjonsopplysninger.first().tom)
        assertEquals(INNTEKT, refusjonsopplysninger.first().beløp.månedlig)
        assertEquals(1.februar, refusjonsopplysninger.last().fom)
        assertEquals(null, refusjonsopplysninger.last().tom)
        assertEquals(INGEN, refusjonsopplysninger.last().beløp.månedlig)
    }

    @Test
    fun `korrigert inntektsmelding i Avsluttet, velger tom liste med refusjon for eldste generasjon`() {
        nyttVedtak(1.januar, 31.januar)
        håndterKorrigerendeArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(20000.månedlig, null),
        )
        håndterYtelser()

        val speil = speilApi()
        val generasjoner = speil.arbeidsgivere.first().generasjoner
        assertEquals(2, generasjoner.size)

        generasjoner.last().also { eldsteGenerasjon ->
            assertEquals(1, eldsteGenerasjon.perioder.size)
            val vilkårsgrunnlagId = (eldsteGenerasjon.perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
            val vilkårsgrunnlag = speil.vilkårsgrunnlag[vilkårsgrunnlagId] as? SpleisVilkårsgrunnlag
            assertEquals(20000.månedlig, vilkårsgrunnlag!!.arbeidsgiverrefusjoner.single().refusjonsopplysninger.single().beløp.månedlig)
        }
        generasjoner.first().also { nyesteGenerasjon ->
            assertEquals(1, nyesteGenerasjon.perioder.size)
            val vilkårsgrunnlagId = (nyesteGenerasjon.perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
            val vilkårsgrunnlag = speil.vilkårsgrunnlag[vilkårsgrunnlagId] as? SpleisVilkårsgrunnlag
            assertEquals(20000.månedlig, vilkårsgrunnlag!!.arbeidsgiverrefusjoner.single().refusjonsopplysninger.single().beløp.månedlig)
        }
    }

    @Test
    fun `Endring til ingen refusjon i forlengelsen`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelserTilGodkjenning()
        håndterLpsInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.februar,
            refusjon = Inntektsmelding.Refusjon(INGEN, null)
        )
        håndterYtelserTilGodkjent()
        håndterYtelserTilGodkjenning()

        val januarVilkårsgrunnlagId = (speilApi().arbeidsgivere.first().generasjoner.last().perioder.last() as BeregnetPeriode).vilkårsgrunnlagId
        val februarVilkårsgrunnlagId = (speilApi().arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = speilApi().vilkårsgrunnlag

        assertTrue(vilkårsgrunnlag[januarVilkårsgrunnlagId]!!.arbeidsgiverrefusjoner.isNotEmpty())
        assertEquals(2, vilkårsgrunnlag[februarVilkårsgrunnlagId]!!.arbeidsgiverrefusjoner.single().refusjonsopplysninger.size)

        val førsteRefusjonsopplysning = vilkårsgrunnlag[februarVilkårsgrunnlagId]!!.arbeidsgiverrefusjoner.single().refusjonsopplysninger.first()
        val sisteRefusjonsopplysning = vilkårsgrunnlag[februarVilkårsgrunnlagId]!!.arbeidsgiverrefusjoner.single().refusjonsopplysninger.last()

        assertEquals(1.januar, førsteRefusjonsopplysning.fom)
        assertEquals(31.januar, førsteRefusjonsopplysning.tom)
        assertEquals(INNTEKT, førsteRefusjonsopplysning.beløp.månedlig)

        assertEquals(1.februar, sisteRefusjonsopplysning.fom)
        assertEquals(null, sisteRefusjonsopplysning.tom)
        assertEquals(INGEN, sisteRefusjonsopplysning.beløp.månedlig)
    }

    @Test
    fun `hendelser på uberegnet periode`() {
        val søknadId = håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        val periode = speilApi().arbeidsgivere.single().generasjoner.single().perioder.single() as UberegnetPeriode
        assertEquals(setOf(søknadId), periode.hendelser)
    }

    @Test
    fun `legger ved skjønnsmessig fastsatt sykepengegrunnlag`() {
        val inntektIm = 31000.0
        val inntektSkatt = 31000.0 * 2
        val inntektSkjønnsfastsatt = 31000 * 1.5
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(1.januar, beregnetInntekt = inntektIm.månedlig)
        håndterVilkårsgrunnlag(arbeidsgivere = listOf(a1 to inntektSkatt.månedlig))
        håndterYtelserTilGodkjenning()
        håndterSkjønnsmessigFastsettelse(
            skjæringstidspunkt = 1.januar,
            opplysninger = listOf(OverstyrtArbeidsgiveropplysning(a1, inntektSkjønnsfastsatt.månedlig))
        )
        håndterYtelserTilGodkjenning()

        val personDto = speilApi()
        val vilkårsgrunnlagId = (personDto.arbeidsgivere.single().generasjoner.single().perioder.single() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = (personDto.vilkårsgrunnlag[vilkårsgrunnlagId] as SpleisVilkårsgrunnlag)
        assertEquals(inntektIm * 12, vilkårsgrunnlag.omregnetÅrsinntekt)
        assertEquals(inntektSkjønnsfastsatt * 12, vilkårsgrunnlag.beregningsgrunnlag)
        assertEquals(inntektSkjønnsfastsatt * 12, vilkårsgrunnlag.inntekter.single().skjønnsmessigFastsatt!!.årlig)

        assertEquals(inntektSkjønnsfastsatt, vilkårsgrunnlag.inntekter.single().skjønnsmessigFastsatt!!.månedlig)

        assertEquals(Inntektkilde.Inntektsmelding.name, vilkårsgrunnlag.inntekter.single().omregnetÅrsinntekt.kilde.name)
        assertEquals(inntektIm * 12, vilkårsgrunnlag.inntekter.single().omregnetÅrsinntekt.beløp)
        assertEquals(inntektIm, vilkårsgrunnlag.inntekter.single().omregnetÅrsinntekt.månedsbeløp)
    }

    @Test
    fun `Sykmeldt 20 % fra to forskjellige arbeidsforhold gir faktisk rett på sykepenger`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 20.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 20.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(1.januar, orgnummer = a2, beregnetInntekt = 22966.54.månedlig)
        håndterArbeidsgiveropplysninger(1.januar, orgnummer = a1, beregnetInntekt = 18199.7.månedlig)
        håndterVilkårsgrunnlag(arbeidsgivere = listOf(a1 to 18199.7.månedlig, a2 to 22966.54.månedlig))
        håndterYtelser()

        val personDto = speilApi()
        assertEquals(20.0, (personDto.arbeidsgivere[0].generasjoner[0].perioder[0] as BeregnetPeriode).sammenslåttTidslinje[28].utbetalingsinfo!!.totalGrad)
    }

    @Test
    fun `Sykmeldt rett under 20 % fra to forskjellige arbeidsforhold gir ikke rett på sykepenger`() {
        håndterSøknad(Sykdom(1.januar, 31.januar, 19.99.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 19.99.prosent), orgnummer = a2)

        håndterArbeidsgiveropplysninger(1.januar, orgnummer = a2, beregnetInntekt = 22966.54.månedlig)
        håndterArbeidsgiveropplysninger(1.januar, orgnummer = a1, beregnetInntekt = 18199.7.månedlig)
        håndterVilkårsgrunnlag(arbeidsgivere = listOf(a1 to 18199.7.månedlig, a2 to 22966.54.månedlig))
        håndterYtelser()

        val personDto = speilApi()
        assertEquals(19.99, (personDto.arbeidsgivere[0].generasjoner[0].perioder[0] as BeregnetPeriode).sammenslåttTidslinje[28].utbetalingsinfo!!.totalGrad)
    }

    @Test
    fun `Skatteinntekt vises som AO`() {
        håndterSøknad(1.januar til 31.januar)
        håndterPåminnelse(tilstand = TilstandType.AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = LocalDateTime.now().minusMonths(4L))
        håndterSykepengegrunnlagForArbeidsgiver(
            skjæringstidspunkt = 1.januar,
            skatteinntekter = listOf(
                ArbeidsgiverInntekt.MånedligInntekt(desember(2017), 31000.månedlig, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(november(2017), 31000.månedlig, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                ArbeidsgiverInntekt.MånedligInntekt(oktober(2017), 31000.månedlig, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
            )
        )
        håndterVilkårsgrunnlag()
        håndterYtelser()
        håndterSimulering()

        val personDto = speilApi()
        val vilkårsgrunnlagId = personDto.vilkårsgrunnlag.keys.single()
        assertEquals(
            Inntektkilde.AOrdningen,
            personDto.vilkårsgrunnlag[vilkårsgrunnlagId]!!.inntekter.first().omregnetÅrsinntekt.kilde
        )
    }
}
