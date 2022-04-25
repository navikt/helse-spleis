package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.Sammenligningsgrunnlag
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.assertWarning
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
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
        person = Person(AKTØRID, UNG_PERSON_FNR_2018, MaskinellJurist())
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
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertTrue(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `arbeidsforhold nyere enn første fraværsdag`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 2.januar))
        )
        person.håndter(vilkårsgrunnlag)
        assertGrunnlagsdata(INNTEKT, Prosent.ratio(0.0), 0, false)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, hentTilstand()?.type)
        assertTrue(vilkårsgrunnlag.hasWarningsOrWorse())
    }

    @Test
    fun `28 dager opptjening fører til OK opptjening`() {
        val vilkårsgrunnlag = vilkårsgrunnlag(
            arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 4.desember(2017)))
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
            opptjening = Opptjening.opptjening(
                arbeidsforhold = listOf(
                    Opptjening.ArbeidsgiverOpptjeningsgrunnlag(a1, listOf(Arbeidsforholdhistorikk.Arbeidsforhold(1.januar, 14.januar, false))),
                    Opptjening.ArbeidsgiverOpptjeningsgrunnlag(a2, listOf(Arbeidsforholdhistorikk.Arbeidsforhold(15.januar, null, false)))
                ),
                skjæringstidspunkt = 31.januar,
                subsumsjonObserver = MaskinellJurist()
            ),
            antallArbeidsgivereFraAareg = 1,
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
        val idInnhenter = IdInnhenter { observatør.vedtaksperiode(ORGNUMMER, 0) }
        val grunnlagsdata = TestArbeidsgiverInspektør(person, ORGNUMMER).vilkårsgrunnlag(idInnhenter) ?: fail("Forventet at vilkårsgrunnlag er satt")
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
                periodetype: () -> Periodetype,
                skjæringstidspunkt: () -> LocalDate,
                skjæringstidspunktFraInfotrygd: LocalDate?,
                forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
                hendelseIder: Set<Dokumentsporing>,
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
                periodetype: () -> Periodetype,
                skjæringstidspunkt: () -> LocalDate,
                skjæringstidspunktFraInfotrygd: LocalDate?,
                forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
                hendelseIder: Set<Dokumentsporing>,
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
        fødselsnummer = UNG_PERSON_FNR_2018,
        orgnummer = ORGNUMMER,
        inntektsvurdering = Inntektsvurdering(inntektsmåneder),
        medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
        inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = skatteinntekter, arbeidsforhold = emptyList()),
        arbeidsforhold = arbeidsforhold
    )

    private fun sykmelding() = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018.toString(),
        aktørId = AKTØRID,
        orgnummer = ORGNUMMER,
        sykeperioder = listOf(Sykmeldingsperiode(16.januar, 30.januar, 100.prosent)),
        sykmeldingSkrevet = 1.april.atStartOfDay(),
        mottatt = 1.april.atStartOfDay()
    )

    private fun søknad() = Søknad(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018.toString(),
        aktørId = AKTØRID,
        orgnummer = ORGNUMMER,
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
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            aktørId = AKTØRID,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )

    private fun ytelser() = Ytelser(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018.toString(),
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = vedtaksperiodeId(),
        utbetalingshistorikk = Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            organisasjonsnummer = ORGNUMMER,
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
