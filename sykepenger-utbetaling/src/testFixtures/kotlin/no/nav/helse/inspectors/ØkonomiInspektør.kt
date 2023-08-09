package no.nav.helse.inspectors

import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import no.nav.helse.økonomi.ØkonomiBuilder
import no.nav.helse.økonomi.ØkonomiVisitor

val Økonomi.inspektør get() = ØkonomiInspektørBuilder(this).build()

private class ØkonomiInspektørBuilder(økonomi: Økonomi) : ØkonomiBuilder() {

    init {
        økonomi.builder(this)
    }

    fun build() = ØkonomiInspektør(
        grad.prosent,
        arbeidsgiverRefusjonsbeløp?.daglig ?: INGEN,
        dekningsgrunnlag?.daglig ?: INGEN,
        totalGrad?.toInt() ?: 0,
        aktuellDagsinntekt?.daglig ?: INGEN,
        arbeidsgiverbeløp?.daglig,
        personbeløp?.daglig
    )
}

class ØkonomiInspektør(
    val grad: Prosentdel,
    val arbeidsgiverRefusjonsbeløp: Inntekt,
    val dekningsgrunnlag: Inntekt,
    val totalGrad: Int,
    val aktuellDagsinntekt: Inntekt,
    val arbeidsgiverbeløp: Inntekt?,
    val personbeløp: Inntekt?
)
