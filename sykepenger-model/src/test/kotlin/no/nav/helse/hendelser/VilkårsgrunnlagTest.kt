package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        private val INNTEKT = 1000.0.månedlig
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
        person.håndter(ytelser())
    }

    @Test
    fun `samme inntekt fra inntektskomponenten og inntektsmelding lagres i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag()
        person.håndter(vilkårsgrunnlag)
        assertEquals(Prosent.ratio(0.0), dataForVilkårsvurdering()?.avviksprosent)
        assertEquals(12000.årlig, dataForVilkårsvurdering()?.sammenligningsgrunnlag)
    }

    @Test
    fun `verdiene fra vurderingen blir lagret i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
                inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt 1250.månedlig
                }}
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(Prosent.ratio(0.2), dataForVilkårsvurdering()?.avviksprosent)
        assertEquals(15000.årlig, dataForVilkårsvurdering()?.sammenligningsgrunnlag)
        assertEquals(28, dataForVilkårsvurdering()!!.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering()?.harOpptjening)
    }

    @Test
    fun `27 dager opptjening fører til at vilkårsvurdering feiler`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Opptjeningvurdering.Arbeidsforhold(orgnummer, 5.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(27, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand()?.type)
    }

    @Test
    fun `arbeidsforhold nyere enn første fraværsdag`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Opptjeningvurdering.Arbeidsforhold(orgnummer, fangeSkjæringstidspunkt(person).plusDays(1)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(0, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand()?.type)
    }

    @Test
    fun `28 dager opptjening fører til OK opptjening`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Opptjeningvurdering.Arbeidsforhold(orgnummer, 4.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(28, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
    }

    @Test
    fun `arbeidsforhold kun for andre orgnr gir samme antall opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Opptjeningvurdering.Arbeidsforhold("eitAnnaOrgNummer", 4.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(28, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(true, dataForVilkårsvurdering()?.harOpptjening)
    }

    @Test
    fun `ingen arbeidsforhold gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = emptyList()
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(0, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand()?.type)
    }

    @Test
    fun `Kan opptjene arbeidsdager over flere arbeidsgivere`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(arbeidsforhold = listOf(
            Opptjeningvurdering.Arbeidsforhold("ORGNR1", 1.januar, 14.januar),
            Opptjeningvurdering.Arbeidsforhold("ORGNR2", 15.januar, null)
        ))
        vilkårsgrunnlag.valider(INNTEKT, INNTEKT, 31.januar, Periodetype.FØRSTEGANGSBEHANDLING)

        assertFalse(vilkårsgrunnlag.hasErrorsOrWorse())
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
                hendelseIder: List<UUID>,
                inntektsmeldingId: UUID?,
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
                hendelseIder: List<UUID>,
                inntektsmeldingId: UUID?,
                inntektskilde: Inntektskilde
            ) {
                _id = id
            }
        })
        return _id.toString()
    }

    private fun vilkårsgrunnlag(
        inntektsmåneder: List<Inntektsvurdering.ArbeidsgiverInntekt> = inntektperioder {
            inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
            1.januar(2017) til 1.desember(2017) inntekter {
                orgnummer inntekt INNTEKT
            }
        },
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = listOf(
            Opptjeningvurdering.Arbeidsforhold(
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
        opprettet = 1.april.atStartOfDay()
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
        merknaderFraSykmelding = emptyList()
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
            ferieperioder = listOf(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )

    private fun ytelser() = Ytelser(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId(),
        utbetalingshistorikk = Utbetalingshistorikk(
            UUID.randomUUID(),
            aktørId,
            UNG_PERSON_FNR_2018,
            orgnummer,
            vedtaksperiodeId(),
            emptyMap(),
            false,
            emptyList(),
            emptyList(),
            emptyList(),
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
