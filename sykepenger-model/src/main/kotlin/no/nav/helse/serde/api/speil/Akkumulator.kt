package no.nav.helse.serde.api.speil

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.speil.IUtbetaling.Companion.leggTil
import no.nav.helse.serde.api.speil.builders.BeregningId
import no.nav.helse.serde.api.speil.builders.GenerasjonIder
import no.nav.helse.serde.api.speil.builders.KorrelasjonsId
import no.nav.helse.serde.api.speil.builders.SykdomshistorikkId

internal class ForkastetVedtaksperiodeAkkumulator : VedtaksperiodeVisitor {
    private val forkastedeVedtaksperioderIder = mutableListOf<UUID>()

    internal fun leggTil(vedtaksperiode: Vedtaksperiode) {
        vedtaksperiode.accept(this)
    }

    internal fun toList() = forkastedeVedtaksperioderIder.toList()

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
        forkastedeVedtaksperioderIder.add(id)
    }
}

internal class VedtaksperiodeAkkumulator {
    private val vedtaksperioder = mutableListOf<IVedtaksperiode>()

    internal fun leggTil(vedtaksperiode: IVedtaksperiode) {
        vedtaksperioder.add(vedtaksperiode)
    }

    internal fun supplerMedAnnulleringer(annulleringer: AnnulleringerAkkumulator) {
        vedtaksperioder.forEach { periode ->
            periode.håndterAnnullering(annulleringer)
        }
    }

    internal fun toList() = vedtaksperioder.toList()
}

internal class GenerasjonIderAkkumulator {
    private val generasjonIder = mutableMapOf<BeregningId, GenerasjonIder>()

    internal fun leggTil(beregningId: BeregningId, generasjonIder: GenerasjonIder) {
        this.generasjonIder.putIfAbsent(beregningId, generasjonIder)
    }

    internal fun toList() = generasjonIder.values.toList()
}

internal class AnnulleringerAkkumulator {
    private var annulleringer = mapOf<KorrelasjonsId, IUtbetaling>()

    internal fun leggTil(utbetaling: IUtbetaling) {
        annulleringer = annulleringer.leggTil(utbetaling)
    }
    internal fun fjerne(utbetalingerErstattet: Set<UUID>) {
        annulleringer = annulleringer.filterNot { (_, annulleringen) -> annulleringen.id in utbetalingerErstattet }
    }

    internal fun finnAnnullering(utbetaling: IUtbetaling) = annulleringer.values.firstOrNull { it.hørerSammen(utbetaling) }
}

internal class SykdomshistorikkAkkumulator {
    private val elementer = mutableMapOf<SykdomshistorikkId, List<Sykdomstidslinjedag>>()

    internal fun leggTil(historikkId: UUID, dager: List<Sykdomstidslinjedag>) {
        elementer.putIfAbsent(historikkId, dager)
    }

    internal fun finnTidslinje(sykdomshistorikkId: SykdomshistorikkId): List<Sykdomstidslinjedag>? {
        return elementer[sykdomshistorikkId]
    }
}
