package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.*
import no.nav.helse.desember
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.a4
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.saksbehandler
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning
import no.nav.helse.utbetalingstidslinje.Vedtaksperiodeberegning
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InntekterForBeregningTest {

    @Test
    fun `lager ghosttidslinjer for dagene vi ikke har beregnet utbetalingstidslinje`() {
        val inntekterForBeregning = inntekterForBeregning(1.januar til 28.februar) {
            fraInntektsgrunnlag(a1, 1000.daglig)
            fraInntektsgrunnlag(a2, 1000.daglig)
        }

        val a1 = Arbeidsgiverberegning(
            orgnummer = "a1",
            vedtaksperioder = listOf(
                Vedtaksperiodeberegning(UUID.randomUUID(), tidslinjeOf(16.AP, 8.NAV, startDato = 1.januar)), // 1.-24.jan
                Vedtaksperiodeberegning(UUID.randomUUID(), tidslinjeOf(11.NAV, startDato = 7.februar)) // 7.-17.feb
            ),
            ghostOgAndreInntektskilder = emptyList()
        )
        val a2 = Arbeidsgiverberegning(
            orgnummer = "a2",
            vedtaksperioder = listOf(
                Vedtaksperiodeberegning(UUID.randomUUID(), tidslinjeOf(16.AP, 6.NAV, startDato = 20.januar)), // 20.jan-10.feb
                Vedtaksperiodeberegning(UUID.randomUUID(), tidslinjeOf(11.NAV, startDato = 18.februar)) // 18.feb - 28.feb
            ),
            ghostOgAndreInntektskilder = emptyList()
        )
        inntekterForBeregning.hensyntattAlleInntektskilder(listOf(a1, a2)).also { result ->
            assertEquals(2, result.size)
            result.first().also {
                assertEquals("a1", it.orgnummer)
                assertEquals(2, it.vedtaksperioder.size)
                it.vedtaksperioder[0].also {
                    assertEquals("PPPPPPP PPPPPPP PPNNNHH NNN", it.utbetalingstidslinje.toString())
                    assertEquals(1.januar til 24.januar, it.periode)
                }
                it.vedtaksperioder[1].also {
                    assertEquals("NNNHH NNNNNH", it.utbetalingstidslinje.toString())
                    assertEquals(7.februar til 17.februar, it.periode)
                }
                assertEquals(2, it.ghostOgAndreInntektskilder.size)
                it.ghostOgAndreInntektskilder[0].also {
                    assertEquals("AAFF AAAAAFF AA", it.toString())
                    assertEquals(25.januar til 6.februar, it.periode())
                }
                it.ghostOgAndreInntektskilder[1].also {
                    assertEquals("F AAAAAFF AAA", it.toString())
                    assertEquals(18.februar til 28.februar, it.periode())
                }
                assertEquals("PPPPPPP PPPPPPP PPNNNHH NNNAAFF AAAAAFF AANNNHH NNNNNHF AAAAAFF AAA", it.samletTidslinje.toString())
                assertEquals(listOf(
                    1.januar til 26.januar,
                    29.januar til 2.februar,
                    5.februar til 17.februar,
                    19.februar til 23.februar,
                    26.februar til 28.februar
                ), it.samletTidslinje.inspektør.perioderMedBeløp)
            }
            result.last().also {
                assertEquals("a2", it.orgnummer)
                assertEquals(2, it.vedtaksperioder.size)
                it.vedtaksperioder[0].also {
                    assertEquals("PP PPPPPPP PPPPPPP NNNNNH", it.utbetalingstidslinje.toString())
                    assertEquals(20.januar til 10.februar, it.periode)
                }
                it.vedtaksperioder[1].also {
                    assertEquals("H NNNNNHH NNN", it.utbetalingstidslinje.toString())
                    assertEquals(18.februar til 28.februar, it.periode)
                }
                assertEquals(2, it.ghostOgAndreInntektskilder.size)
                it.ghostOgAndreInntektskilder[0].also {
                    assertEquals("AAAAAFF AAAAAFF AAAAA", it.toString())
                    assertEquals(1.januar til 19.januar, it.periode())
                }
                it.ghostOgAndreInntektskilder[1].also {
                    assertEquals("F AAAAAF", it.toString())
                    assertEquals(11.februar til 17.februar, it.periode())
                }
                assertEquals("AAAAAFF AAAAAFF AAAAAPP PPPPPPP PPPPPPP NNNNNHF AAAAAFH NNNNNHH NNN", it.samletTidslinje.toString())
                assertEquals(listOf(
                    1.januar til 5.januar,
                    8.januar til 12.januar,
                    15.januar til 10.februar,
                    12.februar til 16.februar,
                    18.februar til 28.februar
                ), it.samletTidslinje.inspektør.perioderMedBeløp)
            }
        }
    }

    @Test
    fun `begrenser inntektsjusteringer til en periode`() {
        val inntekterForBeregning = inntekterForBeregning(1.januar til 31.januar) {
            fraInntektsgrunnlag(a1, 500.daglig)
            deaktivertFraInntektsgrunnlag(a2)
            inntektsendringer(Inntektskilde(a3), 10.januar, 15.januar, 250.daglig)
        }

        // Hele perioden
        with(inntekterForBeregning.inntektsjusteringer(1.januar til 31.januar)) {
            assertEquals(setOf(Inntektskilde(a3)), keys)
            assertBeløpstidslinje(SYSTEM.beløpstidslinje(10.januar til 15.januar, 250.daglig), getValue(Inntektskilde(a3)), ignoreMeldingsreferanseId = true)
        }

        // Uten snute & hale
        with(inntekterForBeregning.inntektsjusteringer(11.januar til 14.januar)) {
            assertEquals(setOf(Inntektskilde(a3)), keys)
            assertBeløpstidslinje(SYSTEM.beløpstidslinje(11.januar til 14.januar, 250.daglig), getValue(Inntektskilde(a3)), ignoreMeldingsreferanseId = true)
        }


        // Etter inntektsendringen
        with(inntekterForBeregning.inntektsjusteringer(16.januar til 31.januar)) {
            assertEquals(emptySet<Inntektskilde>(), keys)
        }

        // Utenfor beregningsperioden
        assertThrows<IllegalStateException> { inntekterForBeregning.inntektsjusteringer(31.desember(2017) til 31.januar) }
        assertThrows<IllegalStateException> { inntekterForBeregning.inntektsjusteringer(1.januar til 1.februar) }
    }

    @Test
    fun `perioder med 0 kroner i beløp`() {
        val inntekterForBeregning = inntekterForBeregning(1.januar til 31.januar) {
            fraInntektsgrunnlag(a1, 0.daglig)
            fraInntektsgrunnlag(a2, 1000.daglig)
            inntektsendringer(Inntektskilde(a2), 2.januar, 30.januar, 0.daglig)
            deaktivertFraInntektsgrunnlag(a3)
            inntektsendringer(Inntektskilde(a4), 10.januar, 15.januar, 0.daglig)
        }

        assertEquals(Beløpstidslinje(), inntekterForBeregning.tilBeregning(a1).second)
        assertBeløpstidslinje(SYSTEM.beløpstidslinje(2.januar til 30.januar, 0.daglig), inntekterForBeregning.tilBeregning(a2).second, ignoreMeldingsreferanseId = true)
        assertEquals(Beløpstidslinje(), inntekterForBeregning.tilBeregning(a3).second)
        assertBeløpstidslinje(SYSTEM.beløpstidslinje(10.januar til 15.januar, 0.daglig), inntekterForBeregning.tilBeregning(a4).second, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `kan endre inntekt på skjæringstidspunktet for en arbeidsgiver som finnes i inntektsgrunnlaget ved hjelp av en inntektsendring`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            fraInntektsgrunnlag(a1, INNTEKT)
            inntektsendringer(Inntektskilde(a1), 1.januar, 31.januar, INNTEKT * 2)
        }

        val (_, inntektstidslinje) = inntekterForBeregning.tilBeregning(a1)

        val forventetTidslinje = SYSTEM.beløpstidslinje(1.januar til 31.januar, INNTEKT * 2)
        assertBeløpstidslinje(forventetTidslinje, inntektstidslinje, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `kan sette inntekt på skjæringstidspunktet for en arbeidsgiver som ikke finnes i inntektsgrunnlaget`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            fraInntektsgrunnlag(a1, INNTEKT)
            inntektsendringer(Inntektskilde(a2), 1.januar, 31.januar, INNTEKT * 2)
        }

        val (_, inntektstidslinjeForA1) = inntekterForBeregning.tilBeregning(a1)
        val (_, inntektstidslinjeForA2) = inntekterForBeregning.tilBeregning(a2)

        val forventetTidslinjeA2 = SYSTEM.beløpstidslinje(januar, INNTEKT * 2)

        assertEquals(Beløpstidslinje(), inntektstidslinjeForA1)
        assertBeløpstidslinje(forventetTidslinjeA2, inntektstidslinjeForA2, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `kan endre inntekt på skjæringstidspunktet for en deaktivert arbeidsgiver i inntektsgrunnlaget ved hjelp av en inntektsendring`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            fraInntektsgrunnlag(a1, INNTEKT)
            deaktivertFraInntektsgrunnlag(a2)
            inntektsendringer(Inntektskilde(a2), 1.januar, 31.januar, INNTEKT * 2)
        }

        val (_, inntektstidslinjeForA1) = inntekterForBeregning.tilBeregning(a1)
        val (_, inntektstidslinjeForA2) = inntekterForBeregning.tilBeregning(a2)

        val forventetTidslinjeA2 = SYSTEM.beløpstidslinje(januar, INNTEKT * 2)

        assertEquals(Beløpstidslinje(), inntektstidslinjeForA1)
        assertBeløpstidslinje(forventetTidslinjeA2, inntektstidslinjeForA2, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `kan sende inn inntektsendringer før inntekter fra inntektsgrunnlaget`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            inntektsendringer(Inntektskilde(a1), 1.januar, 31.januar, INNTEKT * 2)
            fraInntektsgrunnlag(a1, INNTEKT)
        }
        val (_, inntektstidslinje) = inntekterForBeregning.tilBeregning(a1)
        val forventetTidslinje = SYSTEM.beløpstidslinje(1.januar til 31.januar, INNTEKT * 2)
        assertBeløpstidslinje(forventetTidslinje, inntektstidslinje, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `inntekter for beregning uten noen inntekter`() {
        val inntekterForBeregning = InntekterForBeregning.Builder(1.januar til 16.januar).build()
        val inntekterForPeriode = inntekterForBeregning.inntektsjusteringer(1.januar til 16.januar)
        assertEquals(emptyMap<Inntektskilde, Beløpstidslinje>(), inntekterForPeriode)
    }

    private fun inntekterForBeregning(periode: Periode, block: InntekterForBeregning.Builder.() -> Unit) = with(InntekterForBeregning.Builder(periode)) {
        block()
        build()
    }

    private fun InntekterForBeregning.Builder.fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, meldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())) =
        fraInntektsgrunnlag(organisasjonsnummer, fastsattÅrsinntekt, meldingsreferanseId.id.arbeidsgiver)

    private fun InntekterForBeregning.Builder.deaktivertFraInntektsgrunnlag(organisasjonsnummer: String,meldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())) =
        deaktivertFraInntektsgrunnlag(organisasjonsnummer, meldingsreferanseId.id.saksbehandler)

    private fun InntekterForBeregning.Builder.inntektsendringer(inntektskilde: Inntektskilde, fom: LocalDate, tom: LocalDate?, inntekt: Inntekt, meldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())) =
        inntektsendringer(inntektskilde, fom, tom, inntekt, meldingsreferanseId)
}
