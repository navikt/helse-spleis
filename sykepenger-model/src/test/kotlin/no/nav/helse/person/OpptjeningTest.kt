package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDate.EPOCH
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.a1
import no.nav.helse.etterlevelse.Ledd.Companion.ledd
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_2
import no.nav.helse.etterlevelse.Subsumsjon
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.lørdag
import no.nav.helse.mai
import no.nav.helse.mandag
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold
import no.nav.helse.person.ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag.Arbeidsforhold.Companion.opptjeningsperiode
import no.nav.helse.søndag
import no.nav.helse.tirsdag
import no.nav.helse.torsdag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OpptjeningTest {

    @Test
    fun `konkret opptjeningsperiode`() {
        val arbeidsforhold = listOf(
            Arbeidsforhold(EPOCH, torsdag den 28.desember(2017), false),
            Arbeidsforhold(mandag den 1.januar, fredag den 5.januar, false),
            Arbeidsforhold(mandag den 8.januar, lørdag den 13.januar, false),
            Arbeidsforhold(mandag den 15.januar, søndag den 21.januar, false),
            Arbeidsforhold(mandag den 22.januar, søndag den 28.januar, false)
        )

        assertEquals(1.januar til 7.januar, arbeidsforhold.opptjeningsperiode(8.januar))
        assertEquals(1.januar til 28.januar, arbeidsforhold.opptjeningsperiode(29.januar))
        assertEquals((mandag den 29.januar).somPeriode(), arbeidsforhold.opptjeningsperiode(tirsdag den 30.januar))
    }

    @Test
    fun `deaktivert arbeidsforhold`() {
        val arbeidsforhold = listOf(
            Arbeidsforhold(EPOCH, null, true),
        )
        assertEquals(7.januar.somPeriode(), arbeidsforhold.opptjeningsperiode(8.januar))
    }

    @Test
    fun `uendelig opptjeningsperiode`() {
        val arbeidsforhold = listOf(
            Arbeidsforhold(EPOCH, null, false),
        )
        assertEquals(EPOCH til 9.oktober, arbeidsforhold.opptjeningsperiode(10.oktober))
    }

    @Test
    fun `ingen opptjeningsperiode`() {
        val arbeidsforhold = listOf(
            Arbeidsforhold(8.januar, null, false)
        )

        assertEquals(7.januar.somPeriode(), emptyList<Arbeidsforhold>().opptjeningsperiode(8.januar))
        assertEquals(7.januar.somPeriode(), arbeidsforhold.opptjeningsperiode(8.januar))
    }

    @Test
    fun `startdato for manglende arbeidsforhold`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "a1", listOf(
                Arbeidsforhold(1.januar, null, false)
            )
            )
        )
        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.mars)
        assertNull(opptjening.startdatoFor("a2"))
    }

    @Test
    fun `startdato for deaktivert arbeidsforhold`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "a1", listOf(
                Arbeidsforhold(1.januar, null, true)
            )
            )
        )
        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.mars)
        assertEquals(28.februar, opptjening.startdatoFor("a1"))
    }

    @Test
    fun `startdato for aktivt arbeidsforhold`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "a1", listOf(
                Arbeidsforhold(1.februar, null, false),
                Arbeidsforhold(1.januar, 31.januar, false),
            )
            )
        )
        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.mars)
        assertEquals(1.januar, opptjening.startdatoFor("a1"))
    }

    @Test
    fun `Tom liste med arbeidsforhold betyr at du ikke oppfyller opptjeningskrav`() {
        val arbeidsforhold = emptyList<ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag>()
        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.januar)

        assertFalse(opptjening.harTilstrekkeligAntallOpptjeningsdager())
    }

    @Test
    fun `Én dags opptjening oppfyller ikke krav til opptjening`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(
                    Arbeidsforhold(
                        ansattFom = 1.januar,
                        ansattTom = null,
                        deaktivert = false
                    )
                )
            )
        )
        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 2.januar)

        assertFalse(opptjening.harTilstrekkeligAntallOpptjeningsdager())
    }

    @Test
    fun `27 dager opptjening oppfyller ikke krav til opptjening`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(
                    Arbeidsforhold(
                        ansattFom = 1.januar,
                        ansattTom = null,
                        deaktivert = false
                    )
                )
            )
        )
        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.januar.plusDays(27))

        assertFalse(opptjening.harTilstrekkeligAntallOpptjeningsdager())
    }

    @Test
    fun `28 dager opptjening oppfyller krav til opptjening`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(
                    Arbeidsforhold(
                        ansattFom = 1.januar,
                        ansattTom = null,
                        deaktivert = false
                    )
                )
            )
        )
        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.januar.plusDays(28))

        assertTrue(opptjening.harTilstrekkeligAntallOpptjeningsdager())
    }

    @Test
    fun `Opptjening skal ikke bruke deaktiverte arbeidsforhold`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(
                    Arbeidsforhold(
                        ansattFom = 1.januar,
                        ansattTom = null,
                        deaktivert = true
                    )
                )
            )
        )
        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.januar.plusDays(28))

        assertFalse(opptjening.harTilstrekkeligAntallOpptjeningsdager())
    }

    @Test
    fun `Opptjening skal ikke koble sammen om deaktiverte arbeidsforhold fører til gap`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(
                    Arbeidsforhold(
                        ansattFom = 1.januar,
                        ansattTom = 10.januar,
                        deaktivert = false
                    ),
                    Arbeidsforhold(
                        ansattFom = 11.januar,
                        ansattTom = 14.januar,
                        deaktivert = true
                    ),
                    Arbeidsforhold(ansattFom = 15.januar, ansattTom = null, deaktivert = false)
                )
            )
        )

        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.januar.plusDays(28))

        assertFalse(opptjening.harTilstrekkeligAntallOpptjeningsdager())
    }

    @Test
    fun `to tilstøtende arbeidsforhold`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer",
                listOf(
                    Arbeidsforhold(
                        ansattFom = 1.januar,
                        ansattTom = 10.januar,
                        deaktivert = false
                    ),
                    Arbeidsforhold(ansattFom = 11.januar, ansattTom = null, deaktivert = false)
                )
            )
        )

        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.januar.plusDays(28))

        assertTrue(opptjening.harTilstrekkeligAntallOpptjeningsdager())
    }

    @Test
    fun `Opptjening kobler sammen gap selvom rekkefølgen ikke er kronologisk`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                "orgnummer", listOf(
                Arbeidsforhold(ansattFom = 1.januar, ansattTom = 10.januar, deaktivert = false),
                Arbeidsforhold(ansattFom = 15.januar, ansattTom = null, deaktivert = false),
                Arbeidsforhold(ansattFom = 11.januar, ansattTom = 14.januar, deaktivert = false)
            )
            )
        )

        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.januar.plusDays(28))

        assertTrue(opptjening.harTilstrekkeligAntallOpptjeningsdager())
    }

    @Test
    fun `slutter på lørdag, starter på mandag`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag("a1", listOf(Arbeidsforhold(ansattFom = søndag den 1.oktober(2017), ansattTom = lørdag den 30.april(2022), deaktivert = false))),
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag("a2", listOf(Arbeidsforhold(ansattFom = mandag den 2.mai(2022), ansattTom = null, deaktivert = false)))
        )

        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 2.mai(2022))

        assertTrue(opptjening.harTilstrekkeligAntallOpptjeningsdager())
        assertEquals(1.oktober(2017), opptjening.opptjeningFom())
    }

    @Test
    fun `slutter på fredag, starter på mandag`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag("a1", listOf(Arbeidsforhold(ansattFom = søndag den 1.oktober(2017), ansattTom = fredag den 29.april(2022), deaktivert = false))),
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag("a2", listOf(Arbeidsforhold(ansattFom = mandag den 2.mai(2022), ansattTom = null, deaktivert = false)))
        )

        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 3.mai(2022))

        assertTrue(opptjening.harTilstrekkeligAntallOpptjeningsdager())
        assertEquals(1.oktober(2017), opptjening.opptjeningFom())
    }

    @Test
    fun `slutter på torsdag, starter på mandag`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag("a1", listOf(Arbeidsforhold(ansattFom = søndag den 1.oktober(2017), ansattTom = torsdag den 28.april(2022), deaktivert = false))),
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag("a2", listOf(Arbeidsforhold(ansattFom = mandag den 2.mai(2022), ansattTom = null, deaktivert = false)))
        )

        val opptjening = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, tirsdag den 3.mai(2022))

        assertFalse(opptjening.harTilstrekkeligAntallOpptjeningsdager())
        assertEquals(2.mai(2022), opptjening.opptjeningFom())
    }

    @Test
    fun `§ 8-2 ledd 1 - opptjeningstid tilfredstilt`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                a1,
                listOf(Arbeidsforhold(4.desember(2017), 31.januar, deaktivert = false))
            )
        )
        val subsumsjon = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.januar).subsumsjon
        assertEquals(
            Subsumsjon.enkelSubsumsjon(
                lovverk = "folketrygdloven",
                utfall = VILKAR_OPPFYLT,
                versjon = LocalDate.of(2020, 6, 12),
                paragraf = PARAGRAF_8_2,
                ledd = 1.ledd,
                input = mapOf(
                    "skjæringstidspunkt" to 1.januar,
                    "tilstrekkeligAntallOpptjeningsdager" to 28,
                    "arbeidsforhold" to listOf(
                        mapOf(
                            "orgnummer" to a1,
                            "fom" to 4.desember(2017),
                            "tom" to 31.januar
                        )
                    )
                ),
                output = mapOf("antallOpptjeningsdager" to 28)
            ), subsumsjon
        )
    }

    @Test
    fun `§ 8-2 ledd 1 - opptjeningstid ikke tilfredstilt`() {
        val arbeidsforhold = listOf(
            ArbeidstakerOpptjening.ArbeidsgiverOpptjeningsgrunnlag(
                a1,
                listOf(Arbeidsforhold(5.desember(2017), 31.januar, deaktivert = false))
            )
        )
        val subsumsjon = ArbeidstakerOpptjening.nyOpptjening(arbeidsforhold, 1.januar).subsumsjon
        assertEquals(
            Subsumsjon.enkelSubsumsjon(
                lovverk = "folketrygdloven",
                utfall = VILKAR_IKKE_OPPFYLT,
                versjon = LocalDate.of(2020, 6, 12),
                paragraf = PARAGRAF_8_2,
                ledd = 1.ledd,
                input = mapOf(
                    "skjæringstidspunkt" to 1.januar,
                    "tilstrekkeligAntallOpptjeningsdager" to 28,
                    "arbeidsforhold" to listOf(
                        mapOf(
                            "orgnummer" to a1,
                            "fom" to 5.desember(2017),
                            "tom" to 31.januar
                        )
                    )
                ),
                output = mapOf("antallOpptjeningsdager" to 27)
            ), subsumsjon
        )
    }
}
