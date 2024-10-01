package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode

internal val Arbeidsgiver.inspektør get() = ArbeidsgiverInspektør(this)

internal class ArbeidsgiverInspektør(arbeidsgiver: Arbeidsgiver): ArbeidsgiverVisitor {
    private val view = arbeidsgiver.view()

    private val vedtaksperioder: MutableMap<UUID, Vedtaksperiode> = mutableMapOf()
    private var aktiveVedtaksperioder: List<Vedtaksperiode> = emptyList()
    private val sisteVedtaksperiodeTilstander: MutableMap<UUID, TilstandType> = mutableMapOf()

    val organisasjonsnummer = view.organisasjonsnummer

    val refusjonshistorikk = view.refusjonshistorikk.inspektør

    val sykdomshistorikk = arbeidsgiver.view().sykdomshistorikk.inspektør

    init {
        arbeidsgiver.accept(this)
    }

    internal fun aktiveVedtaksperioder() = aktiveVedtaksperioder
    internal fun forkastedeVedtaksperioder() = vedtaksperioder.values - aktiveVedtaksperioder
    internal fun sisteVedtaksperiodeTilstander() = sisteVedtaksperiodeTilstander

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        hendelseIder: Set<Dokumentsporing>,
        egenmeldingsperioder: List<Periode>
    ) {
        vedtaksperioder[id] = vedtaksperiode
        sisteVedtaksperiodeTilstander[id] = tilstand.type
    }

    override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        aktiveVedtaksperioder = vedtaksperioder
    }
}