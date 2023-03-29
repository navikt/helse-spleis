package no.nav.helse.serde.api

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SI_3
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.GhostPeriodeDTO
import no.nav.helse.serde.api.dto.InfotrygdVilkårsgrunnlag
import no.nav.helse.serde.api.dto.Inntektkilde
import no.nav.helse.serde.api.dto.SammenslåttDag
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.dto.SykdomstidslinjedagKildetype
import no.nav.helse.serde.api.dto.SykdomstidslinjedagType
import no.nav.helse.serde.api.dto.UberegnetPeriode
import no.nav.helse.serde.api.dto.Utbetalingsinfo
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterAnnullerUtbetaling
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.spleis.e2e.speilApi
import no.nav.helse.spleis.e2e.standardSimuleringsresultat
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SpeilBuilderTest : AbstractEndToEndTest() {
    @Test
    fun `Dødsdato ligger på person`() {
        val dødsdato = 1.januar
        createTestPerson(UNG_PERSON_FNR_2018, UNG_PERSON_FØDSELSDATO, dødsdato)
        assertEquals(dødsdato, serializePersonForSpeil(person).dødsdato)
    }

    @Test
    fun `nav utbetaler agp`() {
        tilGodkjenning(1.januar, 31.januar, 100.prosent, 1.januar)
        val id = UUID.randomUUID()
        håndterOverstyrTidslinje((1.januar til 16.januar).map {
            ManuellOverskrivingDag(it, Dagtype.SykedagNav, 100)
        }, meldingsreferanseId = id)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        val speilJson = serializePersonForSpeil(person)
        val tidslinje = speilJson.arbeidsgivere.single().generasjoner.single().perioder.single().sammenslåttTidslinje
        val forventetFørstedag = SammenslåttDag(
            dagen = 1.januar,
            sykdomstidslinjedagtype = SykdomstidslinjedagType.SYKEDAG,
            utbetalingstidslinjedagtype = UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag,
            kilde = Sykdomstidslinjedag.SykdomstidslinjedagKilde(SykdomstidslinjedagKildetype.Saksbehandler, id),
            grad = 100.0,
            utbetalingsinfo = Utbetalingsinfo(
                inntekt = 1431,
                utbetaling = 1431,
                personbeløp = 0,
                arbeidsgiverbeløp = 1431,
                refusjonsbeløp = 1431,
                totalGrad = 100.0
            )
        )
        assertEquals(forventetFørstedag, tidslinje.first())
    }

    @Test
    fun `lager ikke hvit pølse i helg`() {
        håndterSøknad(Sykdom(1.januar, 19.januar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val speilJson = serializePersonForSpeil(person)
        assertEquals(emptyList<GhostPeriodeDTO>(), speilJson.arbeidsgivere.single().ghostPerioder)
    }

    @Test
    fun `Negativt nettobeløp på simulering skal gi warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode, simuleringsresultat = standardSimuleringsresultat(ORGNUMMER, totalbeløp = -1))
        assertVarsel(RV_SI_3)

        val personDto = speilApi()
        val periode: BeregnetPeriode = personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode
        val warnings = periode.aktivitetslogg.filter { it.alvorlighetsgrad ==  "W" }
        val errors = periode.aktivitetslogg.filter { it.alvorlighetsgrad ==  "E" }

        assertTrue(errors.isEmpty())
        assertEquals(warnings.size, 1)
        assertTrue(warnings.map { it.melding }.contains("Det er simulert et negativt beløp."))
    }

    @Test
    fun `Skal ikke ha varsel om flere inntektsmeldinger ved replay av samme IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val personDto = speilApi()
        val periode: BeregnetPeriode = personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode
        val warnings = periode.aktivitetslogg.filter { it.alvorlighetsgrad ==  "W" }
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `Viser inntektsgrunnlag for arbeidsforhold som startet innen 3 måneder før skjæringstidspunktet, selvom vi ikke har inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 25.november(2017), null),
            )
        )
        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        val personDto = serializePersonForSpeil(person)

        assertEquals(listOf(a1, a2).map(String::toString), personDto.arbeidsgivere.map { it.organisasjonsnummer })
//        assertEquals(listOf(a1, a2).map(String::toString), personDto.inntektsgrunnlag.single().inntekter.map { it.arbeidsgiver })

        val vilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val arbeidsgiverInntektA2 = personDto.vilkårsgrunnlag[vilkårsgrunnlagId]?.inntekter?.first { it.organisasjonsnummer == a2 }

        assertEquals(0.0, arbeidsgiverInntektA2?.omregnetÅrsinntekt?.beløp)
        assertEquals(Inntektkilde.IkkeRapportert, arbeidsgiverInntektA2?.omregnetÅrsinntekt?.kilde)
    }

    @Test
    fun `beregnet periode peker på vilkårsgrunnlagid`() {
        nyttVedtak(1.januar, 31.januar)
        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val spleisVilkårsgrunnlagId = inspektør.vilkårsgrunnlagHistorikkInnslag().single().vilkårsgrunnlagFor(1.januar)?.inspektør?.vilkårsgrunnlagId
        val spleisVilkårsgrunnlagIdFraVedtaksperiodeUtbetaling = inspektør.arbeidsgiver.inspektør.aktiveVedtaksperioder().single().inspektør.utbetalingIdTilVilkårsgrunnlagId.second!!

        assertEquals(speilVilkårsgrunnlagId, spleisVilkårsgrunnlagId)
        assertEquals(speilVilkårsgrunnlagId, spleisVilkårsgrunnlagIdFraVedtaksperiodeUtbetaling)
    }

    @Test
    fun `beregnet periode peker på vilkårsgrunnlagid for infotrygdvilkårsgrunnlag`() {
        createOvergangFraInfotrygdPerson()
        forlengVedtak(1.mars, 31.mars, 100.prosent)

        val infotrygdVilkårsgrunnlag = inspektør.vilkårsgrunnlagHistorikkInnslag().first().vilkårsgrunnlagFor(1.januar)
        assertTrue(infotrygdVilkårsgrunnlag is VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag)

        val personDto = speilApi()
        val infotrygdVilkårsgrunnlagId = infotrygdVilkårsgrunnlag?.inspektør?.vilkårsgrunnlagId
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val infotrygdVilkårsgrunnlagIdFraVedtaksperiodeUtbetaling = inspektør.arbeidsgiver.inspektør.aktiveVedtaksperioder().last().inspektør.utbetalingIdTilVilkårsgrunnlagId.second!!

        assertEquals(speilVilkårsgrunnlagId, infotrygdVilkårsgrunnlagId)
        assertEquals(speilVilkårsgrunnlagId, infotrygdVilkårsgrunnlagIdFraVedtaksperiodeUtbetaling)
    }

    @Test
    fun `annullert periode skal ikke ha vilkårsgrunnlagsId`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterAnnullerUtbetaling()
        val personDto = speilApi()
        val utbetaltPeriode = (personDto.arbeidsgivere.first().generasjoner.last().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val annullertPeriode = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId

        assertNotNull(utbetaltPeriode)
        assertNull(annullertPeriode)
    }

    @Test
    fun `beregnet periode peker på et vilkårsgrunnlag`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagId]
        assertTrue(vilkårsgrunnlag is SpleisVilkårsgrunnlag)
    }

    @Test
    fun `refusjon ligger på vilkårsgrunnlaget`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagId] as? SpleisVilkårsgrunnlag
        assertTrue(vilkårsgrunnlag!!.arbeidsgiverrefusjoner.isNotEmpty())
        val arbeidsgiverrefusjon = vilkårsgrunnlag.arbeidsgiverrefusjoner.single()
        assertEquals(ORGNUMMER, arbeidsgiverrefusjon.arbeidsgiver)
        val refusjonsopplysning = arbeidsgiverrefusjon.refusjonsopplysninger.single()

        assertEquals(1.januar, refusjonsopplysning.fom)
        assertEquals(null, refusjonsopplysning.tom)
        assertEquals(INNTEKT,refusjonsopplysning.beløp.månedlig)
    }

    @Test
    fun `refusjon ligger på vilkårsgrunnlaget - også for infotrygd`() {
        createOvergangFraInfotrygdPerson()
        forlengVedtak(1.mars, 31.mars, 100.prosent)
        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagId] as? InfotrygdVilkårsgrunnlag
        assertTrue(vilkårsgrunnlag!!.arbeidsgiverrefusjoner.isNotEmpty())
        val arbeidsgiverrefusjon = vilkårsgrunnlag.arbeidsgiverrefusjoner.single()
        assertEquals(ORGNUMMER, arbeidsgiverrefusjon.arbeidsgiver)
        val refusjonsopplysning = arbeidsgiverrefusjon.refusjonsopplysninger.single()

        assertEquals(1.januar, refusjonsopplysning.fom)
        assertEquals(null, refusjonsopplysning.tom)
        assertEquals(INNTEKT,refusjonsopplysning.beløp.månedlig)
    }

    @Test
    fun `endring i refusjon`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INNTEKT, null, endringerIRefusjon = listOf(
            Inntektsmelding.Refusjon.EndringIRefusjon(INGEN, 1.februar))))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag[speilVilkårsgrunnlagId] as? SpleisVilkårsgrunnlag
        assertTrue(vilkårsgrunnlag!!.arbeidsgiverrefusjoner.isNotEmpty())
        val arbeidsgiverrefusjon = vilkårsgrunnlag.arbeidsgiverrefusjoner.single()
        assertEquals(ORGNUMMER, arbeidsgiverrefusjon.arbeidsgiver)
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
    fun `korrigert inntektsmelding i Avsluttet, velger opprinnelig refusjon`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        val vilkårsgrunnlagId = (speilApi().arbeidsgivere.first().generasjoner.last().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId

        håndterInntektsmelding(listOf(1.januar til 16.januar),  refusjon = Inntektsmelding.Refusjon(20000.månedlig, null))
        val vilkårsgrunnlagIdEtterIm = (speilApi().arbeidsgivere.first().generasjoner.last().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        assertEquals(vilkårsgrunnlagId, vilkårsgrunnlagIdEtterIm)

        val speilVilkårsgrunnlagId = (speilApi().arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = speilApi().vilkårsgrunnlag[speilVilkårsgrunnlagId] as? SpleisVilkårsgrunnlag
        assertEquals(INNTEKT, vilkårsgrunnlag!!.arbeidsgiverrefusjoner.single().refusjonsopplysninger.single().beløp.månedlig)
    }

    @Test
    fun `Endring til ingen refusjon i forlengelsen`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), 1.februar, refusjon = Inntektsmelding.Refusjon(INGEN, null))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        val januarVilkårsgrunnlagId = (speilApi().arbeidsgivere.first().generasjoner.last().perioder.last() as BeregnetPeriode).vilkårsgrunnlagId
        val februarVilkårsgrunnlagId = (speilApi().arbeidsgivere.first().generasjoner.last().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = speilApi().vilkårsgrunnlag

        assertEquals(1, vilkårsgrunnlag[januarVilkårsgrunnlagId]!!.arbeidsgiverrefusjoner.single().refusjonsopplysninger.size)
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
        assertEquals(listOf(søknadId), periode.hendelser.map { UUID.fromString(it.id) })
    }

}
