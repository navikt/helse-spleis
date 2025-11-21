package no.nav.helse.hendelser

import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel

class InntekterForBeregning(val inntektsperioder: List<Inntektsperiode>) {

    sealed interface Inntektsperiode {
        val inntektskilde: String
        val periode: Periode
        data class Beløp(override val inntektskilde: String, override val periode: Periode, val beløp: Inntekt): Inntektsperiode
        data class AndelAvSykepengegrunnlag(override val inntektskilde: String, override val periode: Periode, val andel: Prosentdel): Inntektsperiode
    }
}
