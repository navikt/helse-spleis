package no.nav.helse.hendelser

import no.nav.helse.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.person.*
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.assertWarning
import no.nav.helse.testhelpers.fangeSkjæringstidspunkt
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class VilkårsgrunnlagTest : AbstractPersonTest() {
    private companion object {
        private const val aktørId = "123"
        private val UNG_PERSON_FNR_2018 = "12029240045".somFødselsnummer()
        private val orgnummer = "987654321"
        private val INNTEKT = 30000.0.månedlig
    }

    @BeforeEach
    fun setup() {
        person = Person(aktørId, UNG_PERSON_FNR_2018, MaskinellJurist())
        person.addObserver(observatør)
        person.håndter(sykmelding())
        person.håndter(inntektsmelding())
        person.håndter(søknad())
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
                    orgnummer inntekt 37500.månedlig
                }}
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(37500.månedlig, Prosent.ratio(0.2), 28, true)
    }

    @Test
    fun `27 dager opptjening fører til warning`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(orgnummer, 5.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 27, false)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertTrue(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `arbeidsforhold nyere enn første fraværsdag`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(orgnummer, fangeSkjæringstidspunkt(person).plusDays(1)))
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 0, false)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertTrue(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `28 dager opptjening fører til OK opptjening`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(orgnummer, 4.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 28, true)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertFalse(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `arbeidsforhold kun for andre orgnr gir samme antall opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold("eitAnnaOrgNummer", 4.desember(2017)))
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 28, true)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertWarning("Arbeidsgiver er ikke registrert i Aa-registeret.", AktivitetsloggFilter.person())
    }

    @Test
    fun `ingen arbeidsforhold gir 0 opptjente dager`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = emptyList()
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 0, false)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertTrue(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `Kan opptjene arbeidsdager over flere arbeidsgivere`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold("ORGNR1", 1.januar, 14.januar),
            Vilkårsgrunnlag.Arbeidsforhold("ORGNR2", 15.januar, null)
        ))
        vilkårsgrunnlag.valider(
            grunnlagForSykepengegrunnlag = sykepengegrunnlag(),
            sammenligningsgrunnlag = sammenligningsgrunnlag(skjæringstidspunkt = 31.januar),
            skjæringstidspunkt = 31.januar,
            antallArbeidsgivereFraAareg = 1,
            periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
            subsumsjonObserver = MaskinellJurist()
        )

        assertFalse(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    private fun assertGrunnlagsdata(
        forventetSammenligningsgrunnlag: Inntekt,
        forventetAvviksprosent: Prosent,
        forventetAntallOpptjeningsdager: Int,
        forventetHarOpptjening: Boolean
    ) {
        val idInnhenter = IdInnhenter { observatør.vedtaksperiode(orgnummer, 0) }
        val grunnlagsdata = TestArbeidsgiverInspektør(person, orgnummer).vilkårsgrunnlag(idInnhenter) ?: fail("Forventet at vilkårsgrunnlag er satt")
        val grunnlagsdataInspektør = GrunnlagsdataInspektør(grunnlagsdata)
        assertEquals(forventetSammenligningsgrunnlag, grunnlagsdataInspektør.sammenligningsgrunnlag)
        assertEquals(forventetAvviksprosent, grunnlagsdataInspektør.avviksprosent)
        assertEquals(forventetAntallOpptjeningsdager, grunnlagsdataInspektør.antallOpptjeningsdagerErMinst)
        assertEquals(forventetHarOpptjening, grunnlagsdataInspektør.harOpptjening)
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
                skjæringstidspunktFraInfotrygd: LocalDate?,
                periodetype: Periodetype,
                forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
                hendelseIder: Set<Sporing>,
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
                skjæringstidspunktFraInfotrygd: LocalDate?,
                periodetype: Periodetype,
                forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
                hendelseIder: Set<Sporing>,
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
        skatteinntekter: List<ArbeidsgiverInntekt> = inntektperioderForSykepengegrunnlag {
            1.oktober(2017) til 1.desember(2017) inntekter {
                orgnummer inntekt INNTEKT

            }
        },
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold> = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(
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
        medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
        inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = skatteinntekter, arbeidsforhold = emptyList()),
        arbeidsforhold = arbeidsforhold
    )

    private fun sykmelding() = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018.toString(),
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = listOf(Sykmeldingsperiode(16.januar, 30.januar, 100.prosent)),
        sykmeldingSkrevet = 1.april.atStartOfDay(),
        mottatt = 1.april.atStartOfDay()
    )

    private fun søknad() = Søknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018.toString(),
        aktørId = aktørId,
        orgnummer = orgnummer,
        perioder = listOf(Sykdom(16.januar, 30.januar, 100.prosent)),
        andreInntektskilder = emptyList(),
        sendtTilNAVEllerArbeidsgiver = 30.januar.atStartOfDay(),
        permittert = false,
        merknaderFraSykmelding = emptyList(),
        sykmeldingSkrevet = LocalDateTime.now()
    )

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
            orgnummer = orgnummer,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            aktørId = aktørId,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )

    private fun ytelser() = Ytelser(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = UNG_PERSON_FNR_2018.toString(),
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId(),
        utbetalingshistorikk = Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
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

    private fun sykepengegrunnlag(inntekt: Inntekt = INNTEKT) = Sykepengegrunnlag(
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning("orgnummer", Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), LocalDate.now(), UUID.randomUUID(), inntekt)
        )),
        sykepengegrunnlag = inntekt,
        grunnlagForSykepengegrunnlag = inntekt,
        begrensning = ER_IKKE_6G_BEGRENSET,
        deaktiverteArbeidsforhold = emptyList()
    )

    private fun sammenligningsgrunnlag(inntekt: Inntekt = INNTEKT, skjæringstidspunkt: LocalDate) = Sammenligningsgrunnlag(
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning("ORGNR1",
                Inntektshistorikk.SkattComposite(UUID.randomUUID(), (0 until 12).map {
                    Inntektshistorikk.Skatt.Sammenligningsgrunnlag(
                        dato = skjæringstidspunkt,
                        hendelseId = UUID.randomUUID(),
                        beløp = inntekt,
                        måned = YearMonth.from(skjæringstidspunkt).minusMonths(12L - it),
                        type = Inntektshistorikk.Skatt.Inntekttype.LØNNSINNTEKT,
                        fordel = "fordel",
                        beskrivelse = "beskrivelse"
                    )
                })
            )),
    )
}
