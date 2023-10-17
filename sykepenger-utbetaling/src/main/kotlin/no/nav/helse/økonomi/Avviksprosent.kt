package no.nav.helse.økonomi

import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class Avviksprosent private constructor(private val desimal: Double) : Comparable<Avviksprosent> {

    companion object {
        private const val EPSILON = 0.000001
        private val MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT = Avviksprosent(0.25)

        private fun ratio(ratio: Double) = Avviksprosent(ratio)

        fun avvik(a: Double, b: Double) =
            if (b == 0.0) ratio(1.0)
            else ratio((a - b).absoluteValue / b)
    }

    override fun equals(other: Any?) = other is Avviksprosent && this.equals(other)

    private fun equals(other: Avviksprosent) =
        (this.desimal - other.desimal).absoluteValue < EPSILON

    override fun hashCode() = (desimal / EPSILON).roundToLong().hashCode()

    override fun compareTo(other: Avviksprosent) =
        if (this.equals(other)) 0
        else this.desimal.compareTo(other.desimal)

    fun loggInntektsvurdering(aktivitetslogg: IAktivitetslogg) {
        if (!harAkseptabeltAvvik()) return aktivitetslogg.info("Har mer enn %d.0 %% avvik i inntekt (%.2f %%)", MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.toInt(), this.prosent())
        aktivitetslogg.info("Har %d.0 %% eller mindre avvik i inntekt (%.2f %%)", MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.toInt(), this.prosent())
    }

    fun subsummér(omregnetÅrsinntekt: Inntekt, sammenligningsgrunnlag: Inntekt, subsumsjonObserver: SubsumsjonObserver) {
        subsumsjonObserver.`§ 8-30 ledd 2 punktum 1`(
            maksimaltTillattAvvikPåÅrsinntekt = MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.toInt(),
            grunnlagForSykepengegrunnlagÅrlig = omregnetÅrsinntekt.reflection { årlig, _, _, _ -> årlig },
            sammenligningsgrunnlag = sammenligningsgrunnlag.reflection { årlig, _, _, _ -> årlig },
            avvik = this.prosent()
        )
    }

    fun harAkseptabeltAvvik() = this <= MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT

    override fun toString() = "${toInt()}%"

    fun ratio() = desimal

    fun rundTilToDesimaler() = (desimal * 10000).roundToInt() / 100.0

    fun prosent() = desimal * 100.0

    private fun toInt() = prosent().toInt()
}
