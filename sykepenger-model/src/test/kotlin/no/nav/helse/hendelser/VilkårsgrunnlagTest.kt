package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagTest : AbstractPersonTest() {
    private companion object {
        private val INNTEKT = 30000.0.månedlig
    }

    @BeforeEach
    fun setup() {
        person = Person(AKTØRID, UNG_PERSON_FNR_2018, UNG_PERSON_FØDSELSDATO.alder, MaskinellJurist())
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
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 28, true)
    }

    @Test
    fun `verdiene fra vurderingen blir lagret i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
                inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 37500.månedlig
                }}
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(37500.månedlig, Prosent.ratio(0.2), 28, true)
    }

    @Test
    fun `27 dager opptjening fører til warning`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 5.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 27, false)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand())
        assertTrue(vilkårsgrunnlag.harVarslerEllerVerre())
    }

    @Test
    fun `arbeidsforhold nyere enn første fraværsdag`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 2.januar))
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 0, false)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand())
        assertTrue(vilkårsgrunnlag.harVarslerEllerVerre())
    }

    @Test
    fun `28 dager opptjening fører til OK opptjening`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 4.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 28, true)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand())
        assertFalse(vilkårsgrunnlag.harVarslerEllerVerre())
    }

    @Test
    fun `ingen arbeidsforhold gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = emptyList()
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 0, false)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand())
        assertTrue(vilkårsgrunnlag.harVarslerEllerVerre())
    }

    @Test
    fun `Kan opptjene arbeidsdager over flere arbeidsgivere`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold("ORGNR1", 1.januar, 14.januar),
            Vilkårsgrunnlag.Arbeidsforhold("ORGNR2", 15.januar, null)
        ))
        vilkårsgrunnlag.valider(
            grunnlagForSykepengegrunnlag = INNTEKT.sykepengegrunnlag,
            skjæringstidspunkt = 31.januar,
            subsumsjonObserver = MaskinellJurist()
        )

        assertFalse(vilkårsgrunnlag.harVarslerEllerVerre())
    }

    private fun assertGrunnlagsdata(
        forventetSammenligningsgrunnlag: Inntekt,
        forventetAvviksprosent: Prosent,
        forventetAntallOpptjeningsdager: Int,
        forventetHarOpptjening: Boolean
    ) {
        val idInnhenter = IdInnhenter { observatør.vedtaksperiode(ORGNUMMER, 0) }
        val grunnlagsdata = TestArbeidsgiverInspektør(person, ORGNUMMER).vilkårsgrunnlag(idInnhenter) ?: fail("Forventet at vilkårsgrunnlag er satt")
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(grunnlagsdata)
        assertEquals(forventetSammenligningsgrunnlag, grunnlagsdataInspektør.sammenligningsgrunnlag.inspektør.sammenligningsgrunnlag)
        assertEquals(forventetAvviksprosent, grunnlagsdataInspektør.avviksprosent)
        assertEquals(forventetAntallOpptjeningsdager, grunnlagsdataInspektør.antallOpptjeningsdagerErMinst)
        assertEquals(forventetHarOpptjening, grunnlagsdataInspektør.harOpptjening)
    }

    private fun hentTilstand() =
        person.inspektør.sisteVedtaksperiodeTilstander().entries.single().value

    private fun vedtaksperiodeId() = person.inspektør.sisteVedtaksperiodeTilstander().entries.single().key.toString()

    private fun vilkårsgrunnlag(
        inntektsmåneder: List<ArbeidsgiverInntekt> = inntektperioderForSammenligningsgrunnlag {
            1.januar(2017) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT
            }
        },
        skatteinntekter: List<ArbeidsgiverInntekt> = inntektperioderForSykepengegrunnlag {
            1.oktober(2017) til 1.desember(2017) inntekter {
                ORGNUMMER inntekt INNTEKT

            }
        },
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold> = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(
                ORGNUMMER,
                4.desember(2017)
            )
        )
    ) = Vilkårsgrunnlag(
        meldingsreferanseId = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId(),
        aktørId = AKTØRID,
        personidentifikator = UNG_PERSON_FNR_2018,
        orgnummer = ORGNUMMER,
        inntektsvurdering = Inntektsvurdering(inntektsmåneder),
        medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
        inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = skatteinntekter, arbeidsforhold = emptyList()),
        arbeidsforhold = arbeidsforhold
    )

    private fun sykmelding() = a1Hendelsefabrikk.lagSykmelding(
        sykeperioder = arrayOf(Sykmeldingsperiode(16.januar, 30.januar, 100.prosent)),
        sykmeldingSkrevet = 1.april.atStartOfDay(),
        id = UUID.randomUUID()
    )

    private fun søknad() = a1Hendelsefabrikk.lagSøknad(
        perioder = arrayOf(Sykdom(16.januar, 30.januar, 100.prosent)),
        sendtTilNAVEllerArbeidsgiver = 30.januar
    )

    private fun inntektsmelding() = a1Hendelsefabrikk.lagInntektsmelding(
        refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
        førsteFraværsdag = 1.januar,
        beregnetInntekt = INNTEKT,
        arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
        arbeidsforholdId = null,
        begrunnelseForReduksjonEllerIkkeUtbetalt = null
    )

    private fun ytelser() = Ytelser(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018.toString(),
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = vedtaksperiodeId(),
        infotrygdhistorikk = null,
        foreldrepermisjon = Foreldrepermisjon(null, null),
        pleiepenger = Pleiepenger(emptyList()),
        omsorgspenger = Omsorgspenger(emptyList()),
        opplæringspenger = Opplæringspenger(emptyList()),
        institusjonsopphold = Institusjonsopphold(emptyList()),
        dødsinfo = Dødsinfo(null),
        arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
        dagpenger = Dagpenger(emptyList()),
        aktivitetslogg = Aktivitetslogg()
    )
}
