package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// TODO: tester for at sykepengegrunnlaget er riktig
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
    fun `Inntekter fra flere arbeidsgivere`() {
        nyPeriode(
            periode = 1.januar til 31.januar,
            orgnummer = a1,
            inntekt = 16000.månedlig,
        )
        assertNoErrors(a1Inspektør)
        assertNoErrors(a2Inspektør)

        vilkårsgrunnlag(
            a1.id(0),
            orgnummer = a1,
            inntekter = inntektperioderForSammenligningsgrunnlag {
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
            },
            inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 15000
                    a3 inntekt 4750
                    a4 inntekt 2250
                }
            },
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, LocalDate.EPOCH, null),
                Arbeidsforhold(a3, LocalDate.EPOCH, null),
                Arbeidsforhold(a4, LocalDate.EPOCH, null)
            )
        ).håndter(Person::håndter)

        assertNoErrors(a1Inspektør)
        assertNoErrors(a2Inspektør)

        assertInntektForDato(16000.månedlig, 1.januar, a1Inspektør)
        assertInntektForDato(null, 1.januar, a2Inspektør)
        assertInntektForDato(4750.månedlig, 1.januar, a3Inspektør)
        assertInntektForDato(2250.månedlig, 1.januar, a4Inspektør)

        val vilkårsgrunnlag = a1Inspektør.vilkårsgrunnlag(1.vedtaksperiode(a1)) as VilkårsgrunnlagHistorikk.Grunnlagsdata?
        assertEquals(300000.årlig, vilkårsgrunnlag?.sammenligningsgrunnlag)

    }

    @Test
    fun `Sammenligningsgrunnlag når det finnes inntekter fra flere arbeidsgivere`() {
        val inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
            1.oktober(2017) til 1.desember(2017) inntekter {
                a1 inntekt 15000
                a3 inntekt 7500
                a4 inntekt 2500
            }
        }
        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null),
            Arbeidsforhold(a4, LocalDate.EPOCH, null)
        )
        nyPeriode(1.januar til 31.januar, a1, INNTEKT)

        person.håndter(
            vilkårsgrunnlag(
                a1.id(0),
                orgnummer = a1,
                inntekter = inntektperioderForSammenligningsgrunnlag {
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
                },
                inntekterForSykepengegrunnlag = inntekterForSykepengegrunnlag,
                arbeidsforhold = arbeidsforhold
            )
        )
        assertEquals(300000.årlig, person.sammenligningsgrunnlag(1.januar))
    }

    @Test
    fun `Inntekter fra flere arbeidsgivere fra infotrygd`() {
        val inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
            1.oktober(2017) til 1.desember(2017) inntekter {
                a1 inntekt 23500.månedlig
                a2 inntekt 4900.månedlig
            }
        }

        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )
        nyPeriode(1.januar til 31.januar, a1, 25000.månedlig)

        vilkårsgrunnlag(
            a1.id(0),
            orgnummer = a1,
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 24000
                }
            },
            inntekterForSykepengegrunnlag = inntekterForSykepengegrunnlag,
            arbeidsforhold = arbeidsforhold
        ).håndter(Person::håndter)

        ytelser(
            a1.id(0), orgnummer = a1, inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.januar, 24500.månedlig, true),
                Inntektsopplysning(a2, 1.januar(2016), 5000.månedlig, true)
            )
        ).håndter(Person::håndter)

        assertEquals(4, a1Inspektør.inntektInspektør.antallInnslag)
        assertEquals(2, a2Inspektør.inntektInspektør.antallInnslag)

        assertEquals(5000.månedlig, a2Inspektør.inntektInspektør.sisteInnslag?.opplysninger?.first { it.kilde == Kilde.INFOTRYGD }?.sykepengegrunnlag)
        assertEquals(24500.månedlig, a1Inspektør.inntektInspektør.sisteInnslag?.opplysninger?.first { it.kilde == Kilde.INFOTRYGD }?.sykepengegrunnlag)
        assertEquals(
            24000.månedlig,
            a1Inspektør.inntektInspektør.sisteInnslag?.opplysninger?.first { it.kilde == Kilde.SKATT && it.sammenligningsgrunnlag != null }?.sammenligningsgrunnlag
        )

        assertEquals(
            23500.månedlig,
            a1Inspektør.inntektInspektør.sisteInnslag?.opplysninger?.first { it.kilde == Kilde.SKATT && it.sykepengegrunnlag != null }?.sykepengegrunnlag
        )

        assertEquals(
            25000.månedlig,
            a1Inspektør.inntektInspektør.sisteInnslag?.opplysninger?.first { it.kilde == Kilde.INNTEKTSMELDING }?.sykepengegrunnlag
        )
    }

    @Test
    fun `Skatteinntekter for sykepengegrunnlag legges i inntektshistorikken`() {
        val inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
            1.oktober(2017) til 1.desember(2017) inntekter {
                a1 inntekt 15000
            }
        }
        val arbeidsforhold = listOf(Arbeidsforhold(a1, LocalDate.EPOCH, null))
        nyPeriode(1.januar til 31.januar, a1, 25000.månedlig)
        vilkårsgrunnlag(
            a1.id(0),
            orgnummer = a1,
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 24000
                }
            },
            inntekterForSykepengegrunnlag = inntekterForSykepengegrunnlag,
            arbeidsforhold = arbeidsforhold
        ).håndter(Person::håndter)

        assertEquals(3, a1Inspektør.inntektInspektør.sisteInnslag?.opplysninger?.size)
        assertEquals(3, a1Inspektør.inntektInspektør.antallInnslag)
        assertEquals(
            25000.månedlig,
            a1Inspektør.inntektInspektør.sisteInnslag?.opplysninger?.get(0)?.sykepengegrunnlag
        )
        assertEquals(
            15000.månedlig,
            a1Inspektør.inntektInspektør.sisteInnslag?.opplysninger?.get(1)?.sykepengegrunnlag
        )
        assertEquals(
            24000.månedlig,
            a1Inspektør.inntektInspektør.sisteInnslag?.opplysninger?.get(2)?.sammenligningsgrunnlag
        )

    }

    @Test
    fun `Skatteinntekter og inntektsmelding for en arbeidsgiver og kun skatt for andre arbeidsgiver - gir korrekt sykepenge- og sammenligningsgrunnlag`() {
        val inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
            1.oktober(2017) til 1.desember(2017) inntekter {
                a1 inntekt 15000
                a2 inntekt 21000
            }
        }
        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )
        nyPeriode(1.januar til 31.januar, a1, 25000.månedlig)
        vilkårsgrunnlag(
            a1.id(0),
            orgnummer = a1,
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 24000
                    a2 inntekt 20000
                }
            },
            inntekterForSykepengegrunnlag = inntekterForSykepengegrunnlag,
            arbeidsforhold = arbeidsforhold
        ).håndter(Person::håndter)

        assertEquals(552000.årlig, person.sykepengegrunnlag(1.januar))
        assertEquals(528000.årlig, person.sammenligningsgrunnlag(1.januar))

    }

    @Disabled("8-28 b")
    @Test
    fun `Skatteinntekter og inntektsmelding for en arbeidsgiver og kun skatt (i to måneder) for andre arbeidsgiver - gir korrekt sykepenge- og sammenligningsgrunnlag`() {
        val inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
            1.oktober(2017) til 1.november(2017) inntekter {
                a1 inntekt 15000
            }
            1.november(2017) til 1.desember(2017) inntekter {
                a1 inntekt 15000
                a2 inntekt 21000
            }
        }
        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, 1.november, null)
        )
        nyPeriode(1.januar til 31.januar, a1, 25000.månedlig)
        vilkårsgrunnlag(
            a1.id(0),
            orgnummer = a1,
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 24000
                    a2 inntekt 20000
                }
            },
            inntekterForSykepengegrunnlag = inntekterForSykepengegrunnlag,
            arbeidsforhold = arbeidsforhold
        ).håndter(Person::håndter)

        assertEquals(552000.årlig, person.sykepengegrunnlag(1.januar))
        assertEquals(528000.årlig, person.sammenligningsgrunnlag(1.januar))

    }

    private fun nyPeriode(
        periode: Periode,
        orgnummer: String,
        inntekt: Inntekt
    ) {
        sykmelding(
            UUID.randomUUID(),
            Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent),
            orgnummer = orgnummer,
            mottatt = periode.endInclusive.atStartOfDay()
        ).håndter(Person::håndter)
        søknad(
            UUID.randomUUID(),
            Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent),
            orgnummer = orgnummer
        ).håndter(Person::håndter)
        inntektsmelding(
            UUID.randomUUID(),
            arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
            beregnetInntekt = inntekt,
            førsteFraværsdag = periode.start,
            orgnummer = orgnummer
        ).håndter(Person::håndter)
        ytelser(
            vedtaksperiodeId = 1.vedtaksperiode(orgnummer),
            orgnummer = orgnummer,
            inntektshistorikk = emptyList(),
            besvart = LocalDateTime.now().minusHours(24)
        ).håndter(Person::håndter)
    }

    private fun vilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        arbeidsforhold: List<Arbeidsforhold>? = null,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        orgnummer: String = ORGNUMMER,
        inntekter: List<ArbeidsgiverInntekt>,
        inntekterForSykepengegrunnlag: List<ArbeidsgiverInntekt>

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
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekterForSykepengegrunnlag
            ),
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            opptjeningvurdering = Opptjeningvurdering(
                arbeidsforhold ?: listOf(
                    Arbeidsforhold(orgnummer, 1.januar(2017))
                )
            ),
            arbeidsforhold = arbeidsforhold ?: listOf(
                Arbeidsforhold(orgnummer, 1.januar(2017))
            )
        ).apply {
            hendelselogg = this

        }
    }
}
