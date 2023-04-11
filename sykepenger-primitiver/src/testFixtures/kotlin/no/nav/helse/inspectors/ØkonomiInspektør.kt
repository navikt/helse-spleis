package no.nav.helse.inspectors

import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import no.nav.helse.økonomi.ØkonomiVisitor

val Økonomi.inspektør get() = ØkonomiInspektørBuilder(this).build()

private class ØkonomiInspektørBuilder(økonomi: Økonomi) : ØkonomiVisitor {
    private var økonomi: ØkonomiInspektør? = null
    init {
        økonomi.accept(this)
    }
    override fun visitØkonomi(
        grad: Double,
        arbeidsgiverRefusjonsbeløp: Double,
        dekningsgrunnlag: Double,
        totalGrad: Double,
        aktuellDagsinntekt: Double,
        arbeidsgiverbeløp: Double?,
        personbeløp: Double?,
        er6GBegrenset: Boolean?
    ) {
        økonomi = ØkonomiInspektør(
            grad.prosent,
            arbeidsgiverRefusjonsbeløp.daglig,
            dekningsgrunnlag.daglig,
            totalGrad.prosent,
            aktuellDagsinntekt.daglig,
            arbeidsgiverbeløp?.daglig,
            personbeløp?.daglig,
            er6GBegrenset
        )
    }

    fun build() = økonomi!!
}

class ØkonomiInspektør(
    val grad: Prosentdel,
    val arbeidsgiverRefusjonsbeløp: Inntekt,
    val dekningsgrunnlag: Inntekt,
    val totalGrad: Prosentdel,
    val aktuellDagsinntekt: Inntekt,
    val arbeidsgiverbeløp: Inntekt?,
    val personbeløp: Inntekt?,
    val er6GBegrenset: Boolean?
)

class ØkonomiAsserter(
    private val assertions: (grad: Double,
                             arbeidsgiverRefusjonsbeløp: Double,
                             dekningsgrunnlag: Double,
                             totalGrad: Double,
                             aktuellDagsinntekt: Double,
                             arbeidsgiverbeløp: Double?,
                             personbeløp: Double?,
                             er6GBegrenset: Boolean?) -> Unit
): ØkonomiVisitor {
    override fun visitØkonomi(
        grad: Double,
        arbeidsgiverRefusjonsbeløp: Double,
        dekningsgrunnlag: Double,
        totalGrad: Double,
        aktuellDagsinntekt: Double,
        arbeidsgiverbeløp: Double?,
        personbeløp: Double?,
        er6GBegrenset: Boolean?
    ) {
        assertions(grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, totalGrad, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, er6GBegrenset)
    }
}
class AvrundetØkonomiAsserter(
    private val assertions: (grad: Int,
                             arbeidsgiverRefusjonsbeløp: Int,
                             dekningsgrunnlag: Int,
                             totalGrad: Int,
                             aktuellDagsinntekt: Int,
                             arbeidsgiverbeløp: Int?,
                             personbeløp: Int?,
                             er6GBegrenset: Boolean?) -> Unit
): ØkonomiVisitor {
    override fun visitAvrundetØkonomi(
        grad: Int,
        arbeidsgiverRefusjonsbeløp: Int,
        dekningsgrunnlag: Int,
        totalGrad: Int,
        aktuellDagsinntekt: Int,
        arbeidsgiverbeløp: Int?,
        personbeløp: Int?,
        er6GBegrenset: Boolean?
    ) {
        assertions(grad, arbeidsgiverRefusjonsbeløp, dekningsgrunnlag, totalGrad, aktuellDagsinntekt, arbeidsgiverbeløp, personbeløp, er6GBegrenset)
    }
}