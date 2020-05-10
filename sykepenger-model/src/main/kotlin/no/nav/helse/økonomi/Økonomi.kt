package no.nav.helse.økonomi

internal class Økonomi private constructor(private val beløp: Double, private val grad: Grad) {

    companion object {

        internal fun lønn(beløp: Number, grad: Grad) = Økonomi(beløp.toDouble(), grad)

        internal fun samletGrad(økonomier: List<Økonomi>) =
            Grad.vektlagtGjennomsnitt(økonomier.map { it.grad to it.beløp })
    }
}

internal fun List<Økonomi>.samletGrad() = Økonomi.samletGrad(this)

