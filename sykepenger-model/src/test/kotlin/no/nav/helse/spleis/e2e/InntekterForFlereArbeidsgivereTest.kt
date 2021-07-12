package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
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
        Toggles.FlereArbeidsgivereUlikFom.enable {
            nyPeriode(1.januar til 31.januar, a1, inntekt = 16000.månedlig, inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 15000
                    a3 inntekt 7500
                    a4 inntekt 2500
                }
            },
                arbeidsforhold = listOf(
                    Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Arbeidsforhold(a3, LocalDate.EPOCH, null),
                    Arbeidsforhold(a4, LocalDate.EPOCH, null)
                ))
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
                }
            ).håndter(Person::håndter)

            assertNoErrors(a1Inspektør)
            assertNoErrors(a2Inspektør)

            assertInntektForDato(16000.månedlig, 1.januar, a1Inspektør)
            assertInntektForDato(null, 1.januar, a2Inspektør)
            assertInntektForDato(7500.månedlig, 1.januar, a3Inspektør)
            assertInntektForDato(2500.månedlig, 1.januar, a4Inspektør)

            val vilkårsgrunnlag = a1Inspektør.vilkårsgrunnlag(1.vedtaksperiode(a1)) as VilkårsgrunnlagHistorikk.Grunnlagsdata?
            assertEquals(300000.årlig, vilkårsgrunnlag?.sammenligningsgrunnlag)
        }
    }

    @Test
    fun `Sammenligningsgrunnlag når det finnes inntekter fra flere arbeidsgivere`() {
        nyPeriode(1.januar til 31.januar, a1, inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
            1.oktober(2017) til 1.desember(2017) inntekter {
                a1 inntekt 15000
                a3 inntekt 7500
                a4 inntekt 2500
            }
        },
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a3, LocalDate.EPOCH, null),
                Arbeidsforhold(a4, LocalDate.EPOCH, null)
            ))

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
                }
            ))

        assertEquals(300000.årlig, person.sammenligningsgrunnlag(1.januar))
    }

    @Test
    fun `Inntekter fra flere arbeidsgivere fra infotrygd`() {
        nyPeriode(
            1.januar til 31.januar, a1, 25000.månedlig, inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
                1.oktober(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 23500.månedlig
                    a2 inntekt 4900.månedlig
                }
            },
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )
        )

        vilkårsgrunnlag(a1.id(0), orgnummer = a1, inntekter = inntektperioderForSammenligningsgrunnlag {
            1.januar(2017) til 1.desember(2017) inntekter {
                a1 inntekt 24000
            }
        }).håndter(Person::håndter)

        ytelser(
            a1.id(0), orgnummer = a1, inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.januar, 24500.månedlig, true),
                Inntektsopplysning(a2, 1.januar(2016), 5000.månedlig, true)
            )
        ).håndter(Person::håndter)

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

    @Test
    fun `Skatteinntekter for sykepengegrunnlag legges i inntektshistorikken`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            nyPeriode(
                1.januar til 31.januar, a1, 25000.månedlig, inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 15000
                    }
                },
                arbeidsforhold = listOf(
                    Arbeidsforhold(a1, LocalDate.EPOCH, null)
                )
            )
            vilkårsgrunnlag(
                a1.id(0),
                orgnummer = a1,
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 24000
                    }
                }
            ).håndter(Person::håndter)

            assertEquals(3, a1Inspektør.inntektInspektør.sisteInnslag?.size)
            assertEquals(3, a1Inspektør.inntektInspektør.antallInnslag)
            assertEquals(
                25000.månedlig,
                a1Inspektør.inntektInspektør.sisteInnslag?.get(0)?.sykepengegrunnlag
            )
            assertEquals(
                15000.månedlig,
                a1Inspektør.inntektInspektør.sisteInnslag?.get(1)?.sykepengegrunnlag
            )
            assertEquals(
                24000.månedlig,
                a1Inspektør.inntektInspektør.sisteInnslag?.get(2)?.sammenligningsgrunnlag
            )
        }
    }

    @Test
    fun `Skatteinntekter og inntektsmelding for en arbeidsgiver og kun skatt for andre arbeidsgiver - gir korrekt sykepenge- og sammenligningsgrunnlag`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            nyPeriode(
                1.januar til 31.januar, a1, 25000.månedlig, inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 15000
                        a2 inntekt 21000
                    }
                },
                arbeidsforhold = listOf(
                    Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Arbeidsforhold(a2, LocalDate.EPOCH, null)
                )
            )
            vilkårsgrunnlag(
                a1.id(0),
                orgnummer = a1,
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 24000
                        a2 inntekt 20000
                    }
                }
            ).håndter(Person::håndter)

            assertEquals(552000.årlig, person.sykepengegrunnlag(1.januar, 1.januar))
            assertEquals(528000.årlig, person.sammenligningsgrunnlag(1.januar))
        }
    }

    @Disabled("8-28 b")
    @Test
    fun `Skatteinntekter og inntektsmelding for en arbeidsgiver og kun skatt (i to måneder) for andre arbeidsgiver - gir korrekt sykepenge- og sammenligningsgrunnlag`() {
        Toggles.FlereArbeidsgivereUlikFom.enable {
            nyPeriode(
                1.januar til 31.januar, a1, 25000.månedlig,
                inntekterForSykepengegrunnlag = inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.november(2017) inntekter {
                        a1 inntekt 15000
                    }
                    1.november(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 15000
                        a2 inntekt 21000
                    }
                },
                arbeidsforhold = listOf(
                    Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Arbeidsforhold(a2, 1.november, null)
                )
            )
            vilkårsgrunnlag(
                a1.id(0),
                orgnummer = a1,
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt 24000
                        a2 inntekt 20000
                    }
                }
            ).håndter(Person::håndter)

            assertEquals(552000.årlig, person.sykepengegrunnlag(1.januar, 1.januar))
            assertEquals(528000.årlig, person.sammenligningsgrunnlag(1.januar))
        }
    }

    private fun nyPeriode(
        periode: Periode,
        orgnummer: String,
        inntekt: Inntekt = INNTEKT,
        inntekterForSykepengegrunnlag: List<ArbeidsgiverInntekt>,
        arbeidsforhold: List<Arbeidsforhold>
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
        håndterUtbetalingsgrunnlag(
            vedtaksperiodeId = 1.vedtaksperiode(orgnummer),
            orgnummer = orgnummer,
            inntekter = inntekterForSykepengegrunnlag,
            arbeidsforhold = arbeidsforhold
        )
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
                arbeidsforhold ?: listOf(
                    Arbeidsforhold(orgnummer, 1.januar(2017))
                )
            )
        ).apply {
            hendelselogg = this
        }
    }
}
