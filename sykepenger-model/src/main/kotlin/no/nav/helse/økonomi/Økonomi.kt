package no.nav.helse.økonomi

import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY

internal class Økonomi private constructor(
    private val grad: Prosentdel,
    private val arbeidsgiverBetalingProsent: Prosentdel,
    private var lønn: Double? = null,
    private var tilstand: Tilstand = Tilstand.KunGrad()
) {

    companion object {
        private val GRENSE = 20.prosent

        internal fun sykdomsgrad(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(grad, arbeidsgiverBetalingProsent)

        internal fun arbeidshelse(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(!grad, arbeidsgiverBetalingProsent)

        internal fun samletGrad(økonomiList: List<Økonomi>) =
            Prosentdel.vektlagtGjennomsnitt(økonomiList.map { it.grad to it.lønn() })
    }

    internal fun erUnderGrensen() = grad.compareTo(GRENSE) < 0

    internal fun lønn(beløp: Number): Økonomi {
        beløp.toDouble().also {
            require(it >= 0) { "lønn kan ikke være negativ" }
            require(it !in listOf(
                POSITIVE_INFINITY,
                NEGATIVE_INFINITY,
                NaN
            )) { "lønn må være gyldig positivt nummer" }
            tilstand.lønn(this, it)
        }
        return this
    }

    internal fun toMap(): Map<String, Any> = tilstand.toMap(this)

    private fun lønn() = lønn ?: throw IllegalStateException("Lønn er ikke satt ennå")

    private abstract sealed class Tilstand {
        internal open fun lønn(økonomi: Økonomi, beløp: Double) {
            throw IllegalStateException("Forsøk å stille lønn igjen")
        }

        internal open fun toMap(økonomi: Økonomi): Map<String, Any> = mapOf(
            "grad" to økonomi.grad.toDouble(),
            "arbeidsgiverBetalingProsent" to økonomi.arbeidsgiverBetalingProsent.toDouble()
        )

        internal class KunGrad: Tilstand() {

            override fun lønn(økonomi: Økonomi, beløp: Double) {
                økonomi.lønn = beløp
                økonomi.tilstand = HaLønn()
            }
        }

        internal class HaLønn: Tilstand() {

            override fun toMap(økonomi: Økonomi) = super.toMap(økonomi) + mapOf(
                "lønn" to økonomi.lønn()
            )
        }

    }
}

internal fun List<Økonomi>.samletGrad(): Prosentdel = Økonomi.samletGrad(this)
