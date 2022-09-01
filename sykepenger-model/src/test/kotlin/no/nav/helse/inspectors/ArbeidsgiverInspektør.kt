package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode

internal val Arbeidsgiver.inspektør get() = ArbeidsgiverInspektør(this)

internal class ArbeidsgiverInspektør(arbeidsgiver: Arbeidsgiver): ArbeidsgiverVisitor {
    private val vedtaksperioder: MutableMap<UUID, Vedtaksperiode> = mutableMapOf()
    private var aktiveVedtaksperioder: List<Vedtaksperiode> = emptyList()
    private val sisteVedtaksperiodeTilstander: MutableMap<UUID, TilstandType> = mutableMapOf()

    init {
        arbeidsgiver.accept(this)
    }

    internal fun aktiveVedtaksperioder() = aktiveVedtaksperioder.map { vedtaksperiode -> vedtaksperioder.entries.single { it.value == vedtaksperiode }.key }
    internal fun sisteVedtaksperiodeTilstander() = sisteVedtaksperiodeTilstander

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        periodetype: () -> Periodetype,
        skjæringstidspunkt: () -> LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
        vedtaksperioder[id] = vedtaksperiode
        sisteVedtaksperiodeTilstander[id] = tilstand.type
    }

    override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        aktiveVedtaksperioder = vedtaksperioder
    }
}