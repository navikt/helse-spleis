package no.nav.helse.serde.api

import java.time.LocalDate
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.person.Varselkode.RV_SI_3
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.Inntektkilde
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterAnnullerUtbetaling
import no.nav.helse.spleis.e2e.håndterInntektsmelding
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
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SpeilBuilderTest : AbstractEndToEndTest() {
    @Test
    fun `Dødsdato ligger på person`() {
        val fom = 1.januar
        val tom = 31.januar
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1))
        håndterInntektsmelding(listOf(fom til fom.plusDays(15)))
        håndterYtelser(1.vedtaksperiode, dødsdato = 1.januar)

        assertEquals(1.januar, serializePersonForSpeil(person).dødsdato)
    }

    @Test
    fun `Negativt nettobeløp på simulering skal gi warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        håndterYtelser(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = a1,
        )
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

        val arbeidsgiverInntektA2 = personDto.vilkårsgrunnlagHistorikk.values.last()[1.januar]?.inntekter?.first { it.organisasjonsnummer == a2 }

        assertEquals(0.0, arbeidsgiverInntektA2?.omregnetÅrsinntekt?.beløp)
        assertEquals(Inntektkilde.IkkeRapportert, arbeidsgiverInntektA2?.omregnetÅrsinntekt?.kilde)
    }

    @Test
    fun `tar med refusjonshistorikk pr arbeidsgiver`() {
        nyttVedtak(
            fom = 1.januar,
            tom = 31.januar,
            grad = 100.prosent,
            refusjon = Inntektsmelding.Refusjon(
                beløp = INNTEKT,
                opphørsdato = null,
                endringerIRefusjon = listOf(
                    Inntektsmelding.Refusjon.EndringIRefusjon(beløp = INNTEKT.plus(1000.månedlig), 19.januar),
                    Inntektsmelding.Refusjon.EndringIRefusjon(beløp = INNTEKT.plus(2000.månedlig), 23.januar),
                )
            )
        )

        val personDto = speilApi()
        val periode: BeregnetPeriode = personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode
        val refusjon = requireNotNull(periode.refusjon)

        assertEquals(2, refusjon.endringer.size)
        assertEquals(32000.0, refusjon.endringer.first().beløp)
        assertEquals(19.januar, refusjon.endringer.first().dato)
        assertEquals(33000.0, refusjon.endringer.last().beløp)
        assertEquals(23.januar, refusjon.endringer.last().dato)
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
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag.get(speilVilkårsgrunnlagId)
        assertTrue(vilkårsgrunnlag is SpleisVilkårsgrunnlag)
    }

    @Test
    fun `refusjon ligger på vilkårsgrunnlaget`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag.get(speilVilkårsgrunnlagId) as? SpleisVilkårsgrunnlag
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(INNTEKT, null, endringerIRefusjon = listOf(
            Inntektsmelding.Refusjon.EndringIRefusjon(Inntekt.INGEN, 1.februar))))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val personDto = speilApi()
        val speilVilkårsgrunnlagId = (personDto.arbeidsgivere.first().generasjoner.first().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val vilkårsgrunnlag = personDto.vilkårsgrunnlag.get(speilVilkårsgrunnlagId) as? SpleisVilkårsgrunnlag
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
        assertEquals(Inntekt.INGEN, refusjonsopplysninger.last().beløp.månedlig)
        assertEquals(inntektsmeldingId, refusjonsopplysninger.last().meldingsreferanseId)
    }

    @Test
    fun `korrigert inntektsmelding i Avsluttet, velger opprinnelig refusjon`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        val vilkårsgrunnlagId = (speilApi().arbeidsgivere.first().generasjoner.last().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val refusjon = (speilApi().arbeidsgivere.first().generasjoner.last().perioder.first() as BeregnetPeriode).refusjon
        assertEquals(INNTEKT, refusjon?.beløp?.månedlig)

        håndterInntektsmelding(listOf(1.januar til 16.januar),  refusjon = Inntektsmelding.Refusjon(20000.månedlig, null))
        val vilkårsgrunnlagIdEtterIm = (speilApi().arbeidsgivere.first().generasjoner.last().perioder.first() as BeregnetPeriode).vilkårsgrunnlagId
        val refusjonEtterIm = (speilApi().arbeidsgivere.first().generasjoner.last().perioder.first() as BeregnetPeriode).refusjon
        assertEquals(vilkårsgrunnlagId, vilkårsgrunnlagIdEtterIm)
        assertEquals(INNTEKT, refusjonEtterIm?.beløp?.månedlig)
    }
}
