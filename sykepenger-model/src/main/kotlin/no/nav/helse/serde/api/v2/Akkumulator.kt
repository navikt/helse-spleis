package no.nav.helse.serde.api.v2

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.serde.api.SykdomstidslinjedagDTO
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class ForkastetVedtaksperiodeAkkumulator : VedtaksperiodeVisitor {
    private val forkastedeVedtaksperioder = mutableListOf<Vedtaksperiode>()
    private val forkastedeVedtaksperioderIder = mutableListOf<UUID>()

    internal fun leggTil(vedtaksperiode: Vedtaksperiode) {
        forkastedeVedtaksperioder.add(vedtaksperiode)
    }

    internal fun toList(): List<UUID> {
        forkastedeVedtaksperioder.forEach {
            it.accept(this)
        }
        return forkastedeVedtaksperioderIder.toList()
    }

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<UUID>,
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
    private val elementer = mutableMapOf<SykdomshistorikkId, List<SykdomstidslinjedagDTO>>()

    internal fun leggTil(historikkId: UUID, dager: List<SykdomstidslinjedagDTO>) {
        elementer.putIfAbsent(historikkId, dager)
    }

    internal fun finnTidslinje(sykdomshistorikkId: SykdomshistorikkId): List<SykdomstidslinjedagDTO>? {
        return elementer[sykdomshistorikkId]
    }
}
