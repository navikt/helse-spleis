package no.nav.helse.person.inntekt

import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.beløpstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InntekterForBeregningTest {

    @Test
    fun `kan ikke endre inntekt på skjæringstidspunktet for en arbeidsgiver som finnes i inntektsgrunnlaget ved hjelp av en inntektsendring`() {
        val builder = InntekterForBeregning.Builder(januar, 1.januar)
        builder.fastsattÅrsinntekt(a1, INNTEKT, UUID.randomUUID().arbeidsgiver)
        builder.inntektsendringer(a1, Avsender.SAKSBEHANDLER.beløpstidslinje(januar, INNTEKT * 2))
        builder.medGjeldende6G(Grunnbeløp.`6G`.beløp(1.januar))
        val (_, inntektstidslinje) = builder.build().tilBeregning(a1)

        val forventetTidslinje = Avsender.ARBEIDSGIVER.beløpstidslinje(1.januar.somPeriode(), INNTEKT) + Avsender.SAKSBEHANDLER.beløpstidslinje(2.januar til 31.januar, INNTEKT * 2)
        assertBeløpstidslinje(forventetTidslinje, inntektstidslinje, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `kan sette inntekt på skjæringstidspunktet for en arbeidsgiver som ikke finnes i inntektsgrunnlaget`() {
        val builder = InntekterForBeregning.Builder(januar, 1.januar)
        builder.fastsattÅrsinntekt(a1, INNTEKT, UUID.randomUUID().arbeidsgiver)
        builder.inntektsendringer(a2, Avsender.SAKSBEHANDLER.beløpstidslinje(januar, INNTEKT * 2))
        builder.medGjeldende6G(Grunnbeløp.`6G`.beløp(1.januar))
        val inntekterForBeregning = builder.build()
        val (fastsattÅrsinntektA1, inntektstidslinjeForA1) = inntekterForBeregning.tilBeregning(a1)
        val (fastsattÅrsinntektA2, inntektstidslinjeForA2) = inntekterForBeregning.tilBeregning(a2)

        val forventetTidslinjeA1 = Avsender.ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT)
        val forventetTidslinjeA2 = Avsender.SAKSBEHANDLER.beløpstidslinje(januar, INNTEKT * 2)

        assertBeløpstidslinje(forventetTidslinjeA1, inntektstidslinjeForA1, ignoreMeldingsreferanseId = true)
        assertBeløpstidslinje(forventetTidslinjeA2, inntektstidslinjeForA2, ignoreMeldingsreferanseId = true)
        assertEquals(INNTEKT, fastsattÅrsinntektA1)
        assertEquals(INGEN, fastsattÅrsinntektA2)
    }
}
