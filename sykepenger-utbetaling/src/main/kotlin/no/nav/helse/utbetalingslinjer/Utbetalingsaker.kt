package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode

/**
 * en utbetalingsak er den logiske perioden som binder
 * sammen flere vedtaksperioder mot oppdragsystemet.
 *
 * startperiode vil være arbeidsgiverperioden for spleis-saker,
 * eller siste utbetalte dag i Infotrygd for saker som enten:
 * - begynner i infotrygd
 * - er overgang fra infotrygd
 */
data class Utbetalingsak(
    val startperiode: LocalDate,
    val vedtaksperioder: List<Periode>
) {
    init {
        check(vedtaksperioder.isNotEmpty())
    }
    val omsluttendePeriode = vedtaksperioder.periode()!!.oppdaterFom(startperiode)
}
