package no.nav.helse.serde.api.sporing

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.api.builders.BuilderState
import no.nav.helse.sykdomstidslinje.erRettFør
import java.util.*

internal class ArbeidsgiverBuilder(
    private val organisasjonsnummer: String
) : BuilderState() {
    private val perioderBuilder = VedtaksperioderBuilder()
    private val forkastetPerioderBuilder = VedtaksperioderBuilder(byggerForkastedePerioder = true)

    internal fun build(): ArbeidsgiverDTO {
        val perioder = (perioderBuilder.build() + forkastetPerioderBuilder.build()).sortedBy { it.fom }.toMutableList()
        return ArbeidsgiverDTO(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperioder = perioder.onEachIndexed { index, denne ->
                val erForlengelse = if (index > 0) perioder[index - 1].forlenger(denne) else false
                val erForlenget = if (perioder.size > (index + 1)) denne.forlenger(perioder[index + 1]) else false
                perioder[index] = denne.copy(periodetype = when {
                    !erForlengelse && !erForlenget -> PeriodetypeDTO.GAP_SISTE
                    !erForlengelse && erForlenget -> PeriodetypeDTO.GAP
                    erForlengelse && !erForlenget -> PeriodetypeDTO.FORLENGELSE_SISTE
                    else -> PeriodetypeDTO.FORLENGELSE
                })
            }
        )
    }

    private fun VedtaksperiodeDTO.forlenger(other: VedtaksperiodeDTO) = this.tom.erRettFør(other.fom)

    override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        pushState(perioderBuilder)
    }

    override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        pushState(forkastetPerioderBuilder)
    }

    override fun postVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
        popState()
    }
}
