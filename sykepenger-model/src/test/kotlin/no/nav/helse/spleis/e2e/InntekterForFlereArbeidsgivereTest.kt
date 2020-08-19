package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.YearMonth
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

    @Test
    fun ` `() {
        nyPeriode(1.januar til 31.januar, a1)
        assertNoErrors(a1Inspektør)

        person.håndter(vilkårsgrunnlag(
            a1.id(0),
            orgnummer = a1,
            inntekter = inntektperioder {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 25000
                }
            }
        ))

        assertNoErrors(a1Inspektør)

        assertEquals(25000.månedlig, a1Inspektør.inntektshistorikk.inntekt(1.januar(2017)))
    }

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

        assertEquals(15000.månedlig, a1Inspektør.inntektshistorikk.inntekt(1.januar(2017)))
        assertEquals(5000.månedlig, a2Inspektør.inntektshistorikk.inntekt(1.januar(2017)))
        assertEquals(3000.månedlig, a3Inspektør.inntektshistorikk.inntekt(1.januar(2017)))
        assertEquals(2000.månedlig, a4Inspektør.inntektshistorikk.inntekt(1.januar(2017)))
        assertEquals(7500.månedlig, a3Inspektør.inntektshistorikk.inntekt(1.juli(2017)))
        assertEquals(2500.månedlig, a4Inspektør.inntektshistorikk.inntekt(1.juli(2017)))

        assertEquals(300000.årlig, a1Inspektør.vilkårsgrunnlag(0).beregnetÅrsinntektFraInntektskomponenten)
    }

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

        assertEquals(15000.månedlig, a1Inspektør.inntektshistorikk.inntekt(1.januar(2017)))
        assertEquals(5000.månedlig, a2Inspektør.inntektshistorikk.inntekt(1.januar(2017)))
        assertEquals(3000.månedlig, a3Inspektør.inntektshistorikk.inntekt(1.januar(2017)))
        assertEquals(2000.månedlig, a4Inspektør.inntektshistorikk.inntekt(1.januar(2017)))
        assertEquals(7500.månedlig, a3Inspektør.inntektshistorikk.inntekt(1.juli(2017)))
        assertEquals(2500.månedlig, a4Inspektør.inntektshistorikk.inntekt(1.juli(2017)))

        assertEquals(300000.årlig, a1Inspektør.vilkårsgrunnlag(0).beregnetÅrsinntektFraInntektskomponenten)
    }

    private fun nyPeriode(periode: Periode, orgnummer: String) {
        person.håndter(
            sykmelding(
                UUID.randomUUID(),
                Sykmeldingsperiode(periode.start, periode.endInclusive, 100),
                orgnummer = orgnummer,
                mottatt = periode.endInclusive.atStartOfDay()
            )
        )
        person.håndter(
            søknad(
                UUID.randomUUID(),
                Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100),
                orgnummer = orgnummer
            )
        )
        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                listOf(Periode(periode.start, periode.start.plusDays(15))),
                førsteFraværsdag = periode.start,
                orgnummer = orgnummer
            )
        )
    }

    private fun vilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = emptyList(),
        egenAnsatt: Boolean = false,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        inntekter: Map<YearMonth, List<Pair<String, Inntekt>>>
    ): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(
                perioder = inntekter
            ),
            erEgenAnsatt = egenAnsatt,
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            opptjeningvurdering = Opptjeningvurdering(
                if (arbeidsforhold.isEmpty()) listOf(
                    Opptjeningvurdering.Arbeidsforhold(orgnummer, 1.januar(2017))
                )
                else arbeidsforhold
            ),
            dagpenger = Dagpenger(emptyList()),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList())
        ).apply {
            hendelselogg = this
        }
    }
}
