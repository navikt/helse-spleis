package no.nav.helse.inspectors

import no.nav.helse.økonomi.Økonomi

internal val Økonomi.inspektør get() = ØkonomiInspektør(this)

internal class ØkonomiInspektør(økonomi: Økonomi) {
    internal val grad = økonomi.medData { grad, _, _, _, _, _, _, _, _ -> grad }
    internal val arbeidsgiverRefusjonsbeløp = økonomi.medData { _, arbeidsgiverRefusjonsbeløp, _, _, _, _, _, _, _ -> arbeidsgiverRefusjonsbeløp }
    internal val dekningsgrunnlag = økonomi.medData { _, _, dekningsgrunnlag, _, _, _, _, _, _ -> dekningsgrunnlag }
    internal val skjæringstidspunkt = økonomi.medData { _, _, _, skjæringstidspunkt, _, _, _, _, _ -> skjæringstidspunkt }
    internal val totalGrad = økonomi.medData { _, _, _, _, totalGrad, _, _, _, _ -> totalGrad }
    internal val aktuellDagsinntekt = økonomi.medData { _, _, _, _, _, aktuellDagsinntekt, _, _, _ -> aktuellDagsinntekt }
    internal val arbeidsgiverbeløp = økonomi.medData { _, _, _, _, _, _, arbeidsgiverbeløp, _, _ -> arbeidsgiverbeløp }
    internal val peronbeløp = økonomi.medData { _, _, _, _, _, _, _, peronbeløp, _ -> peronbeløp }
    internal val er6GBegrenset = økonomi.medData { _, _, _, _, _, _, _, _, er6GBegrenset -> er6GBegrenset }
}