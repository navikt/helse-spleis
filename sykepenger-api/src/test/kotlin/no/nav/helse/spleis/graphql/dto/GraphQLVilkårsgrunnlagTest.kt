package no.nav.helse.spleis.graphql.dto

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.speil.builders.SykepengegrunnlagsgrenseDTO
import no.nav.helse.spleis.graphql.mapVilkårsgrunnlag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY

internal class GraphQLVilkårsgrunnlagTest {

    @Test
    fun `ignorerer ei vilkårsgrunnlag med normal avviksprosent`() {
        val graphQlGrunnlag = mapVilkårsgrunnlag(UUID.randomUUID(), listOf(spleisVilkårsgrunnlag(
            avviksprosent = 25.0
        ))).grunnlag

        assertEquals(1, graphQlGrunnlag.size)
    }

    @Test
    fun `ignorerer vilkårsgrunnlag med Infinity avviksprosent`() {
        val graphQlGrunnlag = mapVilkårsgrunnlag(UUID.randomUUID(), listOf(spleisVilkårsgrunnlag(
            avviksprosent = POSITIVE_INFINITY
        ))).grunnlag

        assertEquals(emptyList<GraphQLVilkarsgrunnlag>(), graphQlGrunnlag)
    }

    @Test
    fun `ignorerer vilkårsgrunnlag med -Infinity avviksprosent`() {
        val graphQlGrunnlag = mapVilkårsgrunnlag(UUID.randomUUID(), listOf(spleisVilkårsgrunnlag(
            avviksprosent = NEGATIVE_INFINITY
        ))).grunnlag

        assertEquals(emptyList<GraphQLVilkarsgrunnlag>(), graphQlGrunnlag)
    }

    @Test
    fun `ignorerer vilkårsgrunnlag med NaN avviksprosent`() {
        val graphQlGrunnlag = mapVilkårsgrunnlag(UUID.randomUUID(), listOf(spleisVilkårsgrunnlag(
            avviksprosent = NaN
        ))).grunnlag

        assertEquals(emptyList<GraphQLVilkarsgrunnlag>(), graphQlGrunnlag)
    }

    private fun spleisVilkårsgrunnlag(
        skjæringstidpunkt: LocalDate = 1.januar(2018),
        avviksprosent: Double) = SpleisVilkårsgrunnlag(
        skjæringstidspunkt = skjæringstidpunkt,
        omregnetÅrsinntekt = 500000.0,
        sammenligningsgrunnlag = 500000.0,
        sykepengegrunnlag = 500000.0,
        inntekter = emptyList(),
        avviksprosent = avviksprosent,
        grunnbeløp = 500000,
        sykepengegrunnlagsgrense = SykepengegrunnlagsgrenseDTO(500000, 500000, skjæringstidpunkt),
        antallOpptjeningsdagerErMinst = 10,
        opptjeningFra = skjæringstidpunkt,
        oppfyllerKravOmMinstelønn = true,
        oppfyllerKravOmOpptjening = true,
        oppfyllerKravOmMedlemskap = true
    )
}