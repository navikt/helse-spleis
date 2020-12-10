package no.nav.helse.hendelser

import no.nav.helse.hendelser.Vilkårsgrunnlag.Grunnlagsdata
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosent.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class VilkårsgrunnlagTest {
    private companion object {
        private const val aktørId = "123"
        private const val fødselsnummer = "234"
        private const val orgnummer = "345"
        private val INNTEKT = 1000.0.månedlig
    }

    private lateinit var person: Person

    @BeforeEach
    fun setup() {
        person = Person(aktørId, fødselsnummer).apply {
            håndter(sykmelding())
            håndter(søknad())
            håndter(inntektsmelding())
        }
    }

    @Test
    fun `samme inntekt fra inntektskomponenten og inntektsmelding lagres i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag()
        person.håndter(vilkårsgrunnlag)
        assertEquals(0.prosent, dataForVilkårsvurdering()?.avviksprosent)
        assertEquals(12000.årlig, dataForVilkårsvurdering()?.beregnetÅrsinntektFraInntektskomponenten)
    }

    @Test
    fun `verdiene fra vurderingen blir lagret i vedtaksperioden`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            inntektperioder {
                1.januar(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt 1250.månedlig
                }}
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(20.prosent, dataForVilkårsvurdering()?.avviksprosent)
        assertEquals(15000.årlig, dataForVilkårsvurdering()?.beregnetÅrsinntektFraInntektskomponenten)
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
            arbeidsforhold = listOf(Opptjeningvurdering.Arbeidsforhold(orgnummer, skjæringstidspunkt().plusDays(1)))
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
    fun `arbeidsforhold kun for andre orgnr gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Opptjeningvurdering.Arbeidsforhold("eitAnnaOrgNummer", 4.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertEquals(0, dataForVilkårsvurdering()?.antallOpptjeningsdagerErMinst)
        assertEquals(false, dataForVilkårsvurdering()?.harOpptjening)
        assertEquals(TilstandType.TIL_INFOTRYGD, hentTilstand()?.type)
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

    private fun skjæringstidspunkt(): LocalDate {
        var dato: LocalDate? = null
        person.accept(object : PersonVisitor {
            override fun visitSkjæringstidspunkt(skjæringstidspunkt: LocalDate) {
                dato = skjæringstidspunkt
            }
        })
        return requireNotNull(dato)
    }


    private fun dataForVilkårsvurdering(): Grunnlagsdata? {
        var _dataForVilkårsvurdering: Grunnlagsdata? = null
        person.accept(object : PersonVisitor {
            override fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: Grunnlagsdata?) {
                _dataForVilkårsvurdering = dataForVilkårsvurdering
            }
        })
        return _dataForVilkårsvurdering
    }

    private fun hentTilstand(): Vedtaksperiodetilstand? {
        var _tilstand: Vedtaksperiodetilstand? = null
        person.accept(object : PersonVisitor {
            override fun preVisitVedtaksperiode(
                vedtaksperiode: Vedtaksperiode,
                id: UUID,
                tilstand: Vedtaksperiodetilstand,
                periode: Periode,
                opprinneligPeriode: Periode,
                hendelseIder: List<UUID>
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
                periode: Periode,
                opprinneligPeriode: Periode,
                hendelseIder: List<UUID>
            ) {
                _id = id
            }
        })
        return _id.toString()
    }

    private fun vilkårsgrunnlag(
        inntektsmåneder: List<Inntektsvurdering.ArbeidsgiverInntekt> = inntektperioder {
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
        fødselsnummer = fødselsnummer,
        orgnummer = orgnummer,
        inntektsvurdering = Inntektsvurdering(inntektsmåneder),
        opptjeningvurdering = Opptjeningvurdering(arbeidsforhold),
        medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja)
    )

    private fun sykmelding() = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = listOf(Sykmeldingsperiode(16.januar, 30.januar, 100)),
        mottatt = 1.april.atStartOfDay()
    )

    private fun søknad() = Søknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fødselsnummer,
        aktørId = aktørId,
        orgnummer = orgnummer,
        perioder = listOf(Søknad.Søknadsperiode.Sykdom(16.januar, 30.januar, 100)),
        harAndreInntektskilder = false,
        sendtTilNAV = 30.januar.atStartOfDay(),
        permittert = false
    )

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, INNTEKT, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            ferieperioder = listOf(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )
}
