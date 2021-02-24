package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsvurdering.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Inntektsvurdering.Inntektsgrunnlag
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

internal class InntekterForFlereArbeidsgivereTest : AbstractEndToEndTest() {

    internal companion object {
        private const val a1 = "arbeidsgiver 1"
        private const val a2 = "arbeidsgiver 2"
        private const val a3 = "arbeidsgiver 3"
        private const val a4 = "arbeidsgiver 4"
    }

    private val a1Inspektør get() = TestArbeidsgiverInspektør(person, a1)
    private val a2Inspektør get() = TestArbeidsgiverInspektør(person, a2)
    private val a3Inspektør get() = TestArbeidsgiverInspektør(person, a3)
    private val a4Inspektør get() = TestArbeidsgiverInspektør(person, a4)

    @Disabled("Henting av inntekter er ikke implementert riktig")
    @Test
    fun `Inntekter fra flere arbeidsgivere`() {
        nyPeriode(1.januar til 31.januar, a1)
        assertNoErrors(a1Inspektør)
        assertNoErrors(a2Inspektør)

        person.håndter(
            vilkårsgrunnlag(
                a1.id(0),
                orgnummer = a1,
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 15000
                    }
                    1.januar(2017) til 1.juni(2017) inntekter {
                        a2 inntekt 5000
                        a3 inntekt 3000
                        a4 inntekt 2000
                    }
                    1.juli(2017) til 1.desember(2017) inntekter {
                        a3 inntekt 7500
                        a4 inntekt 2500
                    }
                }
            ))

        assertNoErrors(a1Inspektør)
        assertNoErrors(a2Inspektør)

        assertInntektForDato(15000.månedlig, 1.januar(2017), a1Inspektør)
        assertInntektForDato(5000.månedlig, 1.januar(2017), a2Inspektør)
        assertInntektForDato(3000.månedlig, 1.januar(2017), a3Inspektør)
        assertInntektForDato(2000.månedlig, 1.januar(2017), a4Inspektør)
        assertInntektForDato(7500.månedlig, 1.juli(2017), a3Inspektør)
        assertInntektForDato(2500.månedlig, 1.juli(2017), a4Inspektør)

        assertEquals(300000.årlig, a1Inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.beregnetÅrsinntektFraInntektskomponenten)
    }

    @Test
    fun `Sammenligningsgrunnlag når det finnes inntekter fra flere arbeidsgivere`() {
        nyPeriode(1.januar til 31.januar, a1)

        person.håndter(
            vilkårsgrunnlag(
                a1.id(0),
                orgnummer = a1,
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 15000
                    }
                    1.januar(2017) til 1.juni(2017) inntekter {
                        a2 inntekt 5000
                        a3 inntekt 3000
                        a4 inntekt 2000
                    }
                    1.juli(2017) til 1.desember(2017) inntekter {
                        a3 inntekt 7500
                        a4 inntekt 2500
                    }
                }
            ))

        assertEquals(300000.årlig, person.sammenligningsgrunnlag(1.januar))
    }

    @Disabled("Henting av inntekter er ikke implementert riktig")
    @Test
    fun `Flere inntekter fra samme arbeidsgiver på samme måned`() {
        nyPeriode(1.januar til 31.januar, a1)
        assertNoErrors(a1Inspektør)
        assertNoErrors(a2Inspektør)

        person.håndter(
            vilkårsgrunnlag(
                a1.id(0),
                orgnummer = a1,
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 9000
                        a1 inntekt 1000
                        a1 inntekt 5000
                    }
                    1.januar(2017) til 1.juni(2017) inntekter {
                        a2 inntekt 5000
                        a3 inntekt 3000
                        a4 inntekt 2000
                    }
                    1.juli(2017) til 1.desember(2017) inntekter {
                        a3 inntekt 7500
                        a4 inntekt 2500
                    }
                }
            ))

        assertNoErrors(a1Inspektør)
        assertNoErrors(a2Inspektør)

        assertInntektForDato(15000.månedlig, 1.januar(2017), a1Inspektør)
        assertInntektForDato(5000.månedlig, 1.januar(2017), a2Inspektør)
        assertInntektForDato(3000.månedlig, 1.januar(2017), a3Inspektør)
        assertInntektForDato(2000.månedlig, 1.januar(2017), a4Inspektør)
        assertInntektForDato(7500.månedlig, 1.juli(2017), a3Inspektør)
        assertInntektForDato(2500.månedlig, 1.juli(2017), a4Inspektør)

        assertEquals(300000.årlig, a1Inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.beregnetÅrsinntektFraInntektskomponenten)
    }

    @Test
    fun `Inntekter fra flere arbeidsgivere fra infotrygd`() {
        nyPeriode(1.januar til 31.januar, a1, 25000.månedlig)

        person.håndter(vilkårsgrunnlag(a1.id(0), orgnummer = a1, inntekter = inntektperioder {
            inntektsgrunnlag = Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
            1.januar(2017) til 1.desember(2017) inntekter {
                a1 inntekt 24000
            }
        }))

        person.håndter(
            ytelser(
                a1.id(0), orgnummer = a1, inntektshistorikk = listOf(
                    Utbetalingshistorikk.Inntektsopplysning(1.januar, 24500.månedlig, a1, true),
                    Utbetalingshistorikk.Inntektsopplysning(1.januar(2016), 5000.månedlig, a2, true)
                )
            )
        )

        assertEquals(3, a1Inspektør.inntektInspektør.antallInnslag)
        assertEquals(1, a2Inspektør.inntektInspektør.antallInnslag)
        assertEquals(5000.månedlig, a2Inspektør.inntektInspektør.sisteInnslag?.first()?.sykepengegrunnlag)
        assertEquals(24500.månedlig, a1Inspektør.inntektInspektør.sisteInnslag?.first { it.kilde == Kilde.INFOTRYGD }?.sykepengegrunnlag)
        assertEquals(
            24000.månedlig,
            a1Inspektør.inntektInspektør.sisteInnslag?.first { it.kilde == Kilde.SKATT }?.sammenligningsgrunnlag
        )
        assertEquals(
            25000.månedlig,
            a1Inspektør.inntektInspektør.sisteInnslag?.first { it.kilde == Kilde.INNTEKTSMELDING }?.sykepengegrunnlag
        )
    }

    private fun nyPeriode(periode: Periode, orgnummer: String, inntekt: Inntekt = INNTEKT) {
        person.håndter(
            sykmelding(
                UUID.randomUUID(),
                Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent),
                orgnummer = orgnummer,
                mottatt = periode.endInclusive.atStartOfDay()
            )
        )
        person.håndter(
            søknad(
                UUID.randomUUID(),
                Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent),
                orgnummer = orgnummer
            )
        )
        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
                beregnetInntekt = inntekt,
                førsteFraværsdag = periode.start,
                orgnummer = orgnummer
            )
        )
    }

    private fun vilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = emptyList(),
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        inntekter: List<ArbeidsgiverInntekt>
    ): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntekter
            ),
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            opptjeningvurdering = Opptjeningvurdering(
                if (arbeidsforhold.isEmpty()) listOf(
                    Opptjeningvurdering.Arbeidsforhold(orgnummer, 1.januar(2017))
                )
                else arbeidsforhold
            )
        ).apply {
            hendelselogg = this
        }
    }
}
