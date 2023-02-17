package no.nav.helse.inspectors

import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

internal val Økonomi.inspektør get() = ØkonomiInspektør(this)

internal class ØkonomiInspektør(økonomi: Økonomi) {
    internal val grad = økonomi.medData { grad, _, _, _, _, _, _, _ -> grad.prosent }
    internal val arbeidsgiverRefusjonsbeløp = økonomi.medData { _, arbeidsgiverRefusjonsbeløp, _, _, _, _, _, _ -> arbeidsgiverRefusjonsbeløp }.daglig
    internal val dekningsgrunnlag = økonomi.medData { _, _, dekningsgrunnlag, _, _, _, _, _ -> dekningsgrunnlag }.daglig
    internal val totalGrad = økonomi.medData { _, _, _, totalGrad, _, _, _, _ -> totalGrad }.prosent
    internal val aktuellDagsinntekt = økonomi.medData { _, _, _, _, aktuellDagsinntekt, _, _, _ -> aktuellDagsinntekt }.daglig
    internal val arbeidsgiverbeløp = økonomi.medData { _, _, _, _, _, arbeidsgiverbeløp, _, _ -> arbeidsgiverbeløp }?.daglig
    internal val personbeløp = økonomi.medData { _, _, _, _, _, _, peronbeløp, _ -> peronbeløp }?.daglig
    internal val er6GBegrenset = økonomi.medData { _, _, _, _, _, _, _, er6GBegrenset -> er6GBegrenset }
}