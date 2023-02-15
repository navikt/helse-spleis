package no.nav.helse.serde.api.sporing

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.api.BuilderState

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
        skjæringstidspunkt: () -> LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: () -> Inntektskilde
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
