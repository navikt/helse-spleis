package no.nav.helse.serde.api.speil

import java.util.UUID
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.speil.IUtbetaling.Companion.leggTil
import no.nav.helse.serde.api.speil.builders.BeregningId
import no.nav.helse.serde.api.speil.builders.GenerasjonIder
import no.nav.helse.serde.api.speil.builders.KorrelasjonsId
import no.nav.helse.serde.api.speil.builders.SykdomshistorikkId

internal class ForkastetVedtaksperiodeAkkumulator {
    private val forkastedeVedtaksperioderIder = mutableListOf<UUID>()

    internal fun leggTil(vedtaksperiodeId: UUID) {
        forkastedeVedtaksperioderIder.add(vedtaksperiodeId)
    }

    internal fun toList() = forkastedeVedtaksperioderIder.toList()
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
