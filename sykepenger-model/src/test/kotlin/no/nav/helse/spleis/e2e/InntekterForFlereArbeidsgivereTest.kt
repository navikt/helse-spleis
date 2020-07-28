package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juli
import no.nav.helse.testhelpers.juni
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
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

    @Test
    fun `Flere inntekter fra samme arbeidsgiver på samme måned`() {
        nyPeriode(1.januar til 31.januar, a1)

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

        assertEquals(300000.årlig, a1Inspektør.vilkårsgrunnlag(0).beregnetÅrsinntektFraInntektskomponenten)
    }

    private fun inntektperioder(block: Inntektperioder.() -> Unit) = Inntektperioder(block).toMap()

    private class Inntektperioder(block: Inntektperioder.() -> Unit) {
        private val map = mutableMapOf<YearMonth, MutableList<Pair<String, Inntekt>>>()

        init {
            block()
        }

        internal fun toMap(): Map<YearMonth, List<Pair<String, Inntekt>>> = map

        internal infix fun Periode.inntekter(block: Inntekter.() -> Unit) =
            this.map(YearMonth::from)
                .distinct()
                .forEach { yearMonth -> map.getOrPut(yearMonth) { mutableListOf() }.addAll(Inntekter(block).toList()) }

        internal class Inntekter(block: Inntekter.() -> Unit) {
            private val liste = mutableListOf<Pair<String, Inntekt>>()

            init {
                block()
            }

            internal fun toList() = liste.toList()

            infix fun String.inntekt(inntekt: Int) = liste.add(this to inntekt.månedlig)
        }

        internal infix fun String.inntekt(inntekt: Int) = this to inntekt.månedlig

        internal infix fun Pair<String, Inntekt>.perioder(block: Perioder.() -> Unit) =
            Perioder(block)
                .toList()
                .flatten()
                .map(YearMonth::from)
                .distinct()
                .forEach { yearMonth -> map.getOrPut(yearMonth) { mutableListOf() }.add(this) }

        internal class Perioder(block: Perioder.() -> Unit) {
            private val perioder = mutableListOf<Periode>()

            init {
                block()
            }

            internal fun toList() = perioder.toList()

            internal infix fun LocalDate.til(til: LocalDate) = perioder.add(Periode(this, til))
        }
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


    private infix fun LocalDate.til(other: LocalDate) = Periode(this, other)
}
