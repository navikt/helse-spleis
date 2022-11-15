package no.nav.helse.spleis.graphql.dto

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.speil.builders.SykepengegrunnlagsgrenseDTO
import no.nav.helse.spleis.graphql.mapVilkårsgrunnlagHistorikk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY

internal class GraphQLVilkårsgrunnlagTest {

    @Test
    fun `mapper vilkårsgrunnlag med normal avviksprosent`() {
        val graphQlGrunnlag = mapVilkårsgrunnlagHistorikk(UUID.randomUUID(), listOf(spleisVilkårsgrunnlag(
            avviksprosent = 25.0
        )))
        assertAvviksprosent(25.0, graphQlGrunnlag)
    }

    @Test
    fun `mapper vilkårsgrunnlag med Infinity avviksprosent til 100`() {
        val graphQlGrunnlag = mapVilkårsgrunnlagHistorikk(UUID.randomUUID(), listOf(spleisVilkårsgrunnlag(
            avviksprosent = POSITIVE_INFINITY
        )))
        assertAvviksprosent(100.0, graphQlGrunnlag)
    }

    @Test
    fun `mapper vilkårsgrunnlag med -Infinity avviksprosent til 100`() {
        val graphQlGrunnlag = mapVilkårsgrunnlagHistorikk(UUID.randomUUID(), listOf(spleisVilkårsgrunnlag(
            avviksprosent = NEGATIVE_INFINITY
        )))
        assertAvviksprosent(100.0, graphQlGrunnlag)
    }

    @Test
    fun `mapper vilkårsgrunnlag med NaN avviksprosent til 100`() {
        val graphQlGrunnlag = mapVilkårsgrunnlagHistorikk(UUID.randomUUID(), listOf(spleisVilkårsgrunnlag(
            avviksprosent = NaN
        )))
        assertAvviksprosent(100.0, graphQlGrunnlag)
    }

    @Test
    fun `mapper vilkårsgrunnlag med avviksprosent null`() {
        val graphQlGrunnlag = mapVilkårsgrunnlagHistorikk(UUID.randomUUID(), listOf(spleisVilkårsgrunnlag(
            avviksprosent = null
        )))
        assertAvviksprosent(null, graphQlGrunnlag)
    }

    private fun spleisVilkårsgrunnlag(
        skjæringstidpunkt: LocalDate = 1.januar(2018),
        avviksprosent: Double?) = SpleisVilkårsgrunnlag(
        skjæringstidspunkt = skjæringstidpunkt,
        omregnetÅrsinntekt = 500000.0,
        sammenligningsgrunnlag = 500000.0,
        sykepengegrunnlag = 500000.0,
        inntekter = emptyList(),
        refusjonsopplysninger = emptyList(),
        avviksprosent = avviksprosent,
        grunnbeløp = 500000,
        sykepengegrunnlagsgrense = SykepengegrunnlagsgrenseDTO(500000, 500000, skjæringstidpunkt),
        antallOpptjeningsdagerErMinst = 10,
        opptjeningFra = skjæringstidpunkt,
        oppfyllerKravOmMinstelønn = true,
        oppfyllerKravOmOpptjening = true,
        oppfyllerKravOmMedlemskap = true
    )

    private fun assertAvviksprosent(avviksprosent: Double?, historikk: GraphQLVilkarsgrunnlaghistorikk){
        assertEquals(avviksprosent, historikk.grunnlag.filterIsInstance<GraphQLSpleisVilkarsgrunnlag>().first().avviksprosent)
    }
}