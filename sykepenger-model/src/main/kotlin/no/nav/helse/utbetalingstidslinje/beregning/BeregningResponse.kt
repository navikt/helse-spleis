package no.nav.helse.utbetalingstidslinje.beregning

import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.util.UUID

data class BeregningResponse(
    val yrkesaktiviteter: List<BeregnetYrkesaktivitet>
) {
    data class BeregnetYrkesaktivitet(
        val yrkesaktivitet: Yrkesaktivitet,
        val vedtaksperioder: List<BeregnetVedtaksperiode>,
        val ghostOgAndreInntektskilder: List<Utbetalingstidslinje>
    )

    data class BeregnetVedtaksperiode(
        val vedtaksperiodeId: UUID,
        val utbetalingstidslinje: Utbetalingstidslinje,
        val inntektjusteringer: Map<Yrkesaktivitet, Beløpstidslinje>
    ) {
        val periode = utbetalingstidslinje.periode()
    }
}
