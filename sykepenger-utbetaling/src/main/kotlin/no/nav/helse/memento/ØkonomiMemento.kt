package no.nav.helse.memento

data class ØkonomiMemento(
    val grad: ProsentdelMemento,
    val totalGrad: ProsentdelMemento,
    val arbeidsgiverRefusjonsbeløp: InntektMemento,
    val aktuellDagsinntekt: InntektMemento,
    val beregningsgrunnlag: InntektMemento,
    val dekningsgrunnlag: InntektMemento,
    val grunnbeløpgrense: InntektMemento?,
    val arbeidsgiverbeløp: InntektMemento?,
    val personbeløp: InntektMemento?,
    val er6GBegrenset: Boolean?
)
data class ProsentdelMemento(val prosent: Double)