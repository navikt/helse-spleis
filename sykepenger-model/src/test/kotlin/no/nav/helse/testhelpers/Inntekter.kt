package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
import no.nav.helse.hendelser.Inntektsvurdering.Inntekttype.LØNNSINNTEKT
import no.nav.helse.hendelser.Inntektsvurdering.MånedligInntekt
import no.nav.helse.hendelser.Periode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import java.time.YearMonth

internal fun inntektperioder(block: Inntektperioder.() -> Unit) = Inntektperioder(block).inntekter()

internal class Inntektperioder(block: Inntektperioder.() -> Unit) {
    private val liste = mutableListOf<MånedligInntekt>()

    init {
        block()
    }

    internal fun inntekter(): List<MånedligInntekt> = liste

    internal infix fun Periode.inntekter(block: Inntekter.() -> Unit) =
        this.map(YearMonth::from)
            .distinct()
            .flatMap { yearMonth ->
                Inntekter(block).toList().map { (arbeidsgiver, inntekt) ->
                    MånedligInntekt(
                        yearMonth,
                        arbeidsgiver,
                        inntekt,
                        LØNNSINNTEKT,
                        SAMMENLIGNINGSGRUNNLAG
                    )
                }
            }.also { liste.addAll(it) }

    internal class Inntekter(block: Inntekter.() -> Unit) {
        private val liste = mutableListOf<Pair<String, Inntekt>>()

        init {
            block()
        }

        internal fun toList() = liste.toList()

        infix fun String.inntekt(inntekt: Int) = this inntekt inntekt.månedlig
        infix fun String.inntekt(inntekt: Inntekt) = liste.add(this to inntekt)
    }
}
