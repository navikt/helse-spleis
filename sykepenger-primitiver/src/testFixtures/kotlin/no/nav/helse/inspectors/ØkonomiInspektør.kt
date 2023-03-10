package no.nav.helse.inspectors

import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

val Økonomi.inspektør get() = ØkonomiInspektør(this)

class ØkonomiInspektør(økonomi: Økonomi) {
    val grad = økonomi.medData { grad, _, _, _, _, _, _, _ -> grad.prosent }
    val arbeidsgiverRefusjonsbeløp = økonomi.medData { _, arbeidsgiverRefusjonsbeløp, _, _, _, _, _, _ -> arbeidsgiverRefusjonsbeløp }.daglig
    val dekningsgrunnlag = økonomi.medData { _, _, dekningsgrunnlag, _, _, _, _, _ -> dekningsgrunnlag }.daglig
    val totalGrad = økonomi.medData { _, _, _, totalGrad, _, _, _, _ -> totalGrad }.prosent
    val aktuellDagsinntekt = økonomi.medData { _, _, _, _, aktuellDagsinntekt, _, _, _ -> aktuellDagsinntekt }.daglig
    val arbeidsgiverbeløp = økonomi.medData { _, _, _, _, _, arbeidsgiverbeløp, _, _ -> arbeidsgiverbeløp }?.daglig
    val personbeløp = økonomi.medData { _, _, _, _, _, _, peronbeløp, _ -> peronbeløp }?.daglig
    val er6GBegrenset = økonomi.medData { _, _, _, _, _, _, _, er6GBegrenset -> er6GBegrenset }
}