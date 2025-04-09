package no.nav.helse.person

import java.util.UUID
import no.nav.helse.dto.deserialisering.ForkastetVedtaksperiodeInnDto
import no.nav.helse.dto.serialisering.ForkastetVedtaksperiodeUtDto
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetaling

internal class ForkastetVedtaksperiode(
    private val vedtaksperiode: Vedtaksperiode
) {

    fun view() = vedtaksperiode.view()

    internal companion object {
        private fun Iterable<ForkastetVedtaksperiode>.vedtaksperioder() = map { it.vedtaksperiode }

        internal fun Iterable<ForkastetVedtaksperiode>.perioder() = map { it.vedtaksperiode.periode }

        internal fun List<ForkastetVedtaksperiode>.overlapperMed(dagerFraInntektsmelding: DagerFraInntektsmelding) =
            vedtaksperioder().any { dagerFraInntektsmelding.overlapperMed(it.periode) }

        internal fun harNyereForkastetPeriode(forkastede: Iterable<ForkastetVedtaksperiode>, vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) =
            Vedtaksperiode.harNyereForkastetPeriode(forkastede.vedtaksperioder(), vedtaksperiode, aktivitetslogg)

        internal fun harOverlappendeForkastetPeriode(forkastede: Iterable<ForkastetVedtaksperiode>, vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) =
            Vedtaksperiode.harOverlappendeForkastetPeriode(forkastede.vedtaksperioder(), vedtaksperiode, aktivitetslogg)

        internal fun forlengerForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, aktivitetslogg: IAktivitetslogg, vedtaksperiode: Vedtaksperiode) =
            Vedtaksperiode.forlengerForkastet(forkastede.vedtaksperioder(), aktivitetslogg, vedtaksperiode)

        internal fun harKortGapTilForkastet(forkastede: Iterable<ForkastetVedtaksperiode>, aktivitetslogg: IAktivitetslogg, vedtaksperiode: Vedtaksperiode) =
            Vedtaksperiode.harKortGapTilForkastet(forkastede.vedtaksperioder(), aktivitetslogg, vedtaksperiode)

        internal fun gjenopprett(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            dto: ForkastetVedtaksperiodeInnDto,
            regelverkslogg: Regelverkslogg,
            grunnlagsdata: Map<UUID, VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement>,
            utbetalinger: Map<UUID, Utbetaling>
        ): ForkastetVedtaksperiode {
            return ForkastetVedtaksperiode(
                vedtaksperiode = Vedtaksperiode.gjenopprett(
                    person = person,
                    arbeidsgiver = arbeidsgiver,
                    dto = dto.vedtaksperiode,
                    regelverkslogg = regelverkslogg,
                    grunnlagsdata = grunnlagsdata,
                    utbetalinger = utbetalinger
                )
            )
        }
    }

    internal fun dto() = ForkastetVedtaksperiodeUtDto(vedtaksperiode.dto(null))
}
