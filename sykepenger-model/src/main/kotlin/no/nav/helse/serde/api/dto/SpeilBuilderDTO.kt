package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.forrigeDag
import no.nav.helse.nesteDag
import no.nav.helse.serde.api.speil.builders.IVilkårsgrunnlagHistorikk
import no.nav.helse.utbetalingstidslinje.Begrunnelse

data class PersonDTO(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverDTO>,
    val dødsdato: LocalDate?,
    val versjon: Int,
    val vilkårsgrunnlag: Map<UUID, Vilkårsgrunnlag>
)

data class AktivitetDTO(
    val vedtaksperiodeId: UUID,
    val alvorlighetsgrad: String,
    val melding: String,
    val tidsstempel: String
)

data class ArbeidsgiverDTO(
    val organisasjonsnummer: String,
    val id: UUID,
    val generasjoner: List<Generasjon>,
    val ghostPerioder: List<GhostPeriodeDTO> = emptyList()
) {
    private companion object {
        fun List<ArbeidsgiverDTO>.sykefraværstilfeller() = this
            .flatMap { arbeidsgiver ->
                arbeidsgiver.generasjoner.firstOrNull()?.perioder?.map { periode ->
                    periode.skjæringstidspunkt to (periode.fom .. periode.tom)
                } ?: emptyList()
            }.groupBy({ it.first }) { it.second }
    }
    internal fun erTom(vilkårsgrunnlagHistorikk: IVilkårsgrunnlagHistorikk) = ghostPerioder.isEmpty()
            && generasjoner.isEmpty()
            && vilkårsgrunnlagHistorikk.inngårIkkeISammenligningsgrunnlag(organisasjonsnummer)

    internal fun medGhostperioder(vilkårsgrunnlagHistorikk: IVilkårsgrunnlagHistorikk, arbeidsgivere: List<ArbeidsgiverDTO>): ArbeidsgiverDTO {
        val sykefraværstilfeller = arbeidsgivere.sykefraværstilfeller()
        val ghostsperioder = vilkårsgrunnlagHistorikk
            .potensielleGhostsperioder(organisasjonsnummer, sykefraværstilfeller)
            .flatMap { ghostperiode -> fjernDagerMedSykdom(ghostperiode) }
            .onEach { vilkårsgrunnlagHistorikk.leggIBøtta(it.vilkårsgrunnlagHistorikkInnslagId, it.vilkårsgrunnlagId) }

        return copy(ghostPerioder = ghostsperioder)
    }

    private fun fjernDagerMedSykdom(ghostperiode: GhostPeriodeDTO): List<GhostPeriodeDTO> {
        if (generasjoner.isEmpty()) return listOf(ghostperiode)
        val skjæringstidspunkt = ghostperiode.skjæringstidspunkt
        val tidslinjeperioderFraNyesteGenerasjon = generasjoner
            .first()
            .perioder
            .filter { it.skjæringstidspunkt == skjæringstidspunkt }

        return tidslinjeperioderFraNyesteGenerasjon.fold(listOf(ghostperiode)) { resultat, vedtaksperiode ->
            val tidligereGhostperioder = resultat.dropLast(1)
            val sisteGhostperiode = resultat.lastOrNull()
            val tidslinjeperiode = vedtaksperiode.fom..vedtaksperiode.tom
            tidligereGhostperioder + (sisteGhostperiode?.brytOpp(tidslinjeperiode) ?: emptyList())
        }
    }
}

data class GhostPeriodeDTO(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val vilkårsgrunnlagHistorikkInnslagId: UUID?,
    val vilkårsgrunnlagId: UUID?,
    val vilkårsgrunnlag: Vilkårsgrunnlag? = null,
    val deaktivert: Boolean
) {

    internal fun brytOpp(tidslinjeperiode: ClosedRange<LocalDate>) = when {
        tidslinjeperiode.erInni(this) -> listOf(this.til(tidslinjeperiode), this.fra(tidslinjeperiode))
        tidslinjeperiode.overlapperMedHale(this) -> listOf(this.til(tidslinjeperiode))
        tidslinjeperiode.overlapperMedSnute(this) -> listOf(this.fra(tidslinjeperiode))
        else -> emptyList()
    }

    internal fun til(other: ClosedRange<LocalDate>) = copy(
        id = UUID.randomUUID(),
        tom = other.start.forrigeDag
    )
    internal fun fra(other: ClosedRange<LocalDate>) = copy(
        id = UUID.randomUUID(),
        fom = other.endInclusive.nesteDag
    )

    private fun ClosedRange<LocalDate>.erInni(other: GhostPeriodeDTO) =
        this.start > other.fom && this.endInclusive < other.tom
    private fun ClosedRange<LocalDate>.overlapperMedHale(other: GhostPeriodeDTO) =
        this.start > other.fom && this.endInclusive >= other.tom

    private fun ClosedRange<LocalDate>.overlapperMedSnute(other: GhostPeriodeDTO) =
        this.start <= other.fom && this.endInclusive < other.tom
}

enum class BegrunnelseDTO {
    SykepengedagerOppbrukt,
    SykepengedagerOppbruktOver67,
    MinimumInntekt,
    MinimumInntektOver67,
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad,
    EtterDødsdato,
    ManglerMedlemskap,
    ManglerOpptjening,
    Over70;

    internal companion object {
        fun fraBegrunnelse(begrunnelse: Begrunnelse) = when (begrunnelse) {
            is Begrunnelse.SykepengedagerOppbrukt -> SykepengedagerOppbrukt
            is Begrunnelse.SykepengedagerOppbruktOver67 -> SykepengedagerOppbruktOver67
            is Begrunnelse.MinimumSykdomsgrad -> MinimumSykdomsgrad
            is Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode -> EgenmeldingUtenforArbeidsgiverperiode
            is Begrunnelse.MinimumInntekt -> MinimumInntekt
            is Begrunnelse.MinimumInntektOver67 -> MinimumInntektOver67
            is Begrunnelse.EtterDødsdato -> EtterDødsdato
            is Begrunnelse.ManglerMedlemskap -> ManglerMedlemskap
            is Begrunnelse.ManglerOpptjening -> ManglerOpptjening
            is Begrunnelse.Over70 -> Over70
            is Begrunnelse.NyVilkårsprøvingNødvendig -> SykepengedagerOppbrukt // TODO: Map til NyVilkårsprøvingNødvendig
        }
    }
}

