package no.nav.helse.serde.api.sporing

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.serde.api.builders.BuilderState
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperioderBuilder(private val byggerForkastedePerioder: Boolean = false) : BuilderState() {
    private val perioder = mutableListOf<VedtaksperiodeDTO>()

    fun build() = perioder

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
        perioder.add(VedtaksperiodeDTO(
            id = id,
            fom = periode.start,
            tom = periode.endInclusive,
            periodetype = PeriodetypeDTO.GAP,
            forkastet = byggerForkastedePerioder
        ))
    }

    override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        popState()
    }

    override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        popState()
    }
}
