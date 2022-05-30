package no.nav.helse.serde.api.speil

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.serde.api.speil.builders.BeregningId
import no.nav.helse.serde.api.speil.builders.FagsystemId
import no.nav.helse.serde.api.speil.builders.GenerasjonIder
import no.nav.helse.serde.api.speil.builders.InntektsmeldingId
import no.nav.helse.serde.api.speil.builders.SykdomshistorikkId
import no.nav.helse.serde.api.dto.Refusjon
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag

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
        periodetype: () -> Periodetype,
        skjæringstidspunkt: () -> LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
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
    private val annulleringer = mutableMapOf<FagsystemId, IUtbetaling>()

    internal fun leggTil(utbetaling: IUtbetaling) {
        annulleringer.putIfAbsent(utbetaling.fagsystemId(), utbetaling)
    }

    internal fun finnAnnullering(fagsystemId: String) = annulleringer[fagsystemId]
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

internal class RefusjonerAkkumulator {
    private val refusjoner = mutableMapOf<InntektsmeldingId, Refusjon>()

    internal fun leggTil(refusjoner: Map<InntektsmeldingId, Refusjon>) {
        this.refusjoner.putAll(refusjoner)
    }

    internal fun getRefusjoner(): Map<InntektsmeldingId, Refusjon> = refusjoner
}
