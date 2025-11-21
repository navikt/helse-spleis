package no.nav.helse.hendelser

import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

class InntekterForBeregning(val inntektsperioder: List<Inntektsperiode>) {

    sealed interface Inntektsperiode {
        val inntektskilde: String
        val periode: Periode
        data class Beløp(override val inntektskilde: String, override val periode: Periode, val beløp: Inntekt): Inntektsperiode
        data class AndelAvSykepengegrunnlag(override val inntektskilde: String, override val periode: Periode, val andel: Prosentdel): Inntektsperiode {
            init {
                check(andel in 1.prosent .. 100.prosent) { "Hva i huleste heita er detta? $andel% av sykepengegrunnlaget?!?"}
            }
        }
    }

    data class Inntektsperioder(
        val kilde: Kilde,
        val inntektsperioder: List<Inntektsperiode>
    )
}
