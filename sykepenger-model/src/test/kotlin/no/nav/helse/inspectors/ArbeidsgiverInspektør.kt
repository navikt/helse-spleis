package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk

internal val Arbeidsgiver.inspektør get() = ArbeidsgiverInspektør(this)

internal class ArbeidsgiverInspektør(arbeidsgiver: Arbeidsgiver): ArbeidsgiverVisitor {
    private val vedtaksperioder: MutableMap<UUID, Vedtaksperiode> = mutableMapOf()
    private var aktiveVedtaksperioder: List<Vedtaksperiode> = emptyList()
    private val sisteVedtaksperiodeTilstander: MutableMap<UUID, TilstandType> = mutableMapOf()
    private var sisteInntektshistorikk: Inntektshistorikk? = null

    internal lateinit var sykdomshistorikk: Sykdomshistorikk
        private set

    internal lateinit var arbeidsforholdhistorikk: Arbeidsforholdhistorikk
        private set

    init {
        arbeidsgiver.accept(this)
    }

    internal fun aktiveVedtaksperioder() = aktiveVedtaksperioder
    internal fun sisteVedtaksperiodeTilstander() = sisteVedtaksperiodeTilstander
    internal val inntektshistorikk get() = sisteInntektshistorikk!!

    override fun preVisitArbeidsforholdhistorikk(arbeidsforholdhistorikk: Arbeidsforholdhistorikk) {
        this.arbeidsforholdhistorikk = arbeidsforholdhistorikk
    }

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

    override fun preVisitInntekthistorikk(inntektshistorikk: Inntektshistorikk) {
        this.sisteInntektshistorikk = inntektshistorikk
    }

    override fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {
        this.sykdomshistorikk = sykdomshistorikk
    }
}