package no.nav.helse.inspectors

import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

val Økonomi.inspektør get() = ØkonomiInspektørBuilder(this).build()

private class ØkonomiInspektørBuilder(økonomi: Økonomi) {
    private val inspektøren = ØkonomiInspektør(
        økonomi.grad.toDouble(),
        økonomi.arbeidsgiverRefusjonsbeløp,
        økonomi.dekningsgrunnlag,
        økonomi.totalGrad.toDouble().toInt(),
        økonomi.aktuellDagsinntekt,
        økonomi.arbeidsgiverbeløp,
        økonomi.personbeløp
    )
    fun build() = inspektøren
}

class ØkonomiInspektør(
    val gradProsent: Double,
    val arbeidsgiverRefusjonsbeløp: Inntekt,
    val dekningsgrunnlag: Inntekt,
    val totalGrad: Int,
    val aktuellDagsinntekt: Inntekt,
    val arbeidsgiverbeløp: Inntekt?,
    val personbeløp: Inntekt?
) {
    val grad get() = gradProsent.prosent
}
