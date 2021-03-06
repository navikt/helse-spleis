package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagTest {
    private companion object {
        private const val aktørId = "123"
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val orgnummer = "345"
        private val INNTEKT = 30000.0.månedlig
    }

    private lateinit var person: Person
    private val observatør = TestObservatør()

    @BeforeEach
    fun setup() {
        person = Person(aktørId, UNG_PERSON_FNR_2018)
        person.addObserver(observatør)
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(utbetalingsgrunnlag())
        person.håndter(ytelser())
    }

    @Test
    fun `samme inntekt fra inntektskomponenten og inntektsmelding lagres i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag()
        person.håndter(vilkårsgrunnlag)
        assertEquals(Prosent.ratio(0.0), dataForVilkårsvurdering()?.avviksprosent)
        assertEquals(INNTEKT, dataForVilkårsvurdering()?.sammenligningsgrunnlag)
    }

    @Test
    fun `verdiene fra vurderingen blir lagret i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
                inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt 37500.månedlig
                }}
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(Prosent.ratio(0.2), dataForVilkårsvurdering()?.avviksprosent)
        assertEquals(37500.månedlig, dataForVilkårsvurdering()?.sammenligningsgrunnlag)
        assertEquals(28, dataForVilkårsvurdering()!!.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering()?.harOpptjening)
    }

    @Test
    fun `27 dager opptjening fører til warning`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Arbeidsforhold(orgnummer, 5.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(27, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertTrue(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `arbeidsforhold nyere enn første fraværsdag`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Arbeidsforhold(orgnummer, fangeSkjæringstidspunkt(person).plusDays(1)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(0, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertTrue(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `28 dager opptjening fører til OK opptjening`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Arbeidsforhold(orgnummer, 4.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(28, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertFalse(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `arbeidsforhold kun for andre orgnr gir samme antall opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Arbeidsforhold("eitAnnaOrgNummer", 4.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(28, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertFalse(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `ingen arbeidsforhold gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = emptyList()
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(0, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertTrue(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `Kan opptjene arbeidsdager over flere arbeidsgivere`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(arbeidsforhold = listOf(
            Arbeidsforhold("ORGNR1", 1.januar, 14.januar),
            Arbeidsforhold("ORGNR2", 15.januar, null)
        ))
        vilkårsgrunnlag.valider(INNTEKT, INNTEKT, 31.januar, Periodetype.FØRSTEGANGSBEHANDLING, 1)

        assertFalse(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    private fun dataForVilkårsvurdering(): VilkårsgrunnlagHistorikk.Grunnlagsdata? {
        val inspektør = TestArbeidsgiverInspektør(person)
        return inspektør.vilkårsgrunnlag(observatør.vedtaksperiode(orgnummer, 0)) as VilkårsgrunnlagHistorikk.Grunnlagsdata?
    }

    private fun hentTilstand(): Vedtaksperiodetilstand? {
        var _tilstand: Vedtaksperiodetilstand? = null
        person.accept(object : PersonVisitor {
            override fun preVisitVedtaksperiode(
                vedtaksperiode: Vedtaksperiode,
                id: UUID,
                tilstand: Vedtaksperiodetilstand,
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
                _tilstand = tilstand
            }
        })
        return _tilstand
    }

    private fun vedtaksperiodeId(): String {
        lateinit var _id: UUID
        person.accept(object : PersonVisitor {
            override fun preVisitVedtaksperiode(
                vedtaksperiode: Vedtaksperiode,
                id: UUID,
                tilstand: Vedtaksperiodetilstand,
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
                _id = id
            }
        })
        return _id.toString()
    }

    private fun vilkårsgrunnlag(
        inntektsmåneder: List<ArbeidsgiverInntekt> = inntektperioderForSammenligningsgrunnlag {
            1.januar(2017) til 1.desember(2017) inntekter {
                orgnummer inntekt INNTEKT
            }
        },
        arbeidsforhold: List<Arbeidsforhold> = listOf(
            Arbeidsforhold(
                orgnummer,
                4.desember(2017)
            )
        )
    ) = Vilkårsgrunnlag(
        meldingsreferanseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId(),
        aktørId = aktørId,
        fødselsnummer = UNG_PERSON_FNR_2018,
        orgnummer = orgnummer,
        inntektsvurdering = Inntektsvurdering(inntektsmåneder),
        opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
        medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja)
    )

    private fun sykmelding() = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = listOf(Sykmeldingsperiode(16.januar, 30.januar, 100.prosent)),
        sykmeldingSkrevet = 1.april.atStartOfDay(),
        mottatt = 1.april.atStartOfDay()
    )

    private fun søknad() = Søknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = aktørId,
        orgnummer = orgnummer,
        perioder = listOf(Søknad.Søknadsperiode.Sykdom(16.januar, 30.januar, 100.prosent)),
        andreInntektskilder = emptyList(),
        sendtTilNAV = 30.januar.atStartOfDay(),
        permittert = false,
        merknaderFraSykmelding = emptyList(),
        sykmeldingSkrevet = LocalDateTime.now()
    )

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, INNTEKT, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )

    private fun utbetalingsgrunnlag() = Utbetalingsgrunnlag(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = UNG_PERSON_FNR_2018,
        orgnummer = orgnummer,
        vedtaksperiodeId = UUID.fromString(vedtaksperiodeId()),
        inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(emptyList()),
        arbeidsforhold = listOf(Arbeidsforhold(orgnummer, 1.januar, null))
    )

    private fun ytelser() = Ytelser(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId(),
        utbetalingshistorikk = Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId(),
            arbeidskategorikoder = emptyMap(),
            harStatslønn = false,
            perioder = emptyList(),
            inntektshistorikk = emptyList(),
            ugyldigePerioder = emptyList(),
            besvart = LocalDateTime.now()
        ),
        foreldrepermisjon = Foreldrepermisjon(null, null, Aktivitetslogg()),
        pleiepenger = Pleiepenger(emptyList(), Aktivitetslogg()),
        omsorgspenger = Omsorgspenger(emptyList(), Aktivitetslogg()),
        opplæringspenger = Opplæringspenger(emptyList(), Aktivitetslogg()),
        institusjonsopphold = Institusjonsopphold(emptyList(), Aktivitetslogg()),
        dødsinfo = Dødsinfo(null),
        arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
        dagpenger = Dagpenger(emptyList()),
        aktivitetslogg = Aktivitetslogg()
    )
}
