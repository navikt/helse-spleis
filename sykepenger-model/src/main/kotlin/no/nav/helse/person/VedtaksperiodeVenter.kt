package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.VedtaksperiodeVenterDto
import no.nav.helse.dto.VenterPåDto
import no.nav.helse.dto.VenteårsakDto
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.MeldingsreferanseId

internal class VedtaksperiodeVenter private constructor(
    private val vedtaksperiodeId: UUID,
    private val behandlingId: UUID,
    private val skjæringstidspunkt: LocalDate,
    private val ventetSiden: LocalDateTime,
    private val venterTil: LocalDateTime,
    private val venterPå: VenterPå,
    private val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
    private val hendelseIder: Set<UUID>
) {

    internal fun event() =
        PersonObserver.VedtaksperiodeVenterEvent(
            yrkesaktivitetssporing = yrkesaktivitetssporing,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            ventetSiden = ventetSiden,
            venterTil = venterTil,
            venterPå = venterPå.event(),
            hendelser = hendelseIder
        )

    fun dto() = VedtaksperiodeVenterDto(
        ventetSiden = ventetSiden,
        venterTil = venterTil,
        venterPå = venterPå.dto()
    )

    internal class Builder {
        private lateinit var vedtaksperiodeId: UUID
        private lateinit var behandlingId: UUID
        private lateinit var skjæringstidspunkt: LocalDate
        private lateinit var ventetSiden: LocalDateTime
        private lateinit var venterTil: LocalDateTime
        private lateinit var yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet
        private lateinit var venterPå: VenterPå
        private val hendelseIder = mutableSetOf<UUID>()

        internal fun venter(vedtaksperiodeId: UUID, skjæringstidspunkt: LocalDate, yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, ventetSiden: LocalDateTime, venterTil: LocalDateTime) {
            this.vedtaksperiodeId = vedtaksperiodeId
            this.skjæringstidspunkt = skjæringstidspunkt
            this.ventetSiden = ventetSiden
            this.venterTil = venterTil
            this.yrkesaktivitetssporing = yrkesaktivitetssporing
        }

        internal fun behandlingVenter(behandlingId: UUID) {
            this.behandlingId = behandlingId
        }

        internal fun hendelseIder(hendelseIder: Set<MeldingsreferanseId>) {
            this.hendelseIder.addAll(hendelseIder.map { it.id })
        }

        internal fun venterPå(vedtaksperiodeId: UUID, skjæringstidspunkt: LocalDate, yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet, venteÅrsak: Venteårsak) {
            venterPå = VenterPå(vedtaksperiodeId, skjæringstidspunkt, yrkesaktivitetssporing, venteÅrsak)
        }

        internal fun build() =
            VedtaksperiodeVenter(vedtaksperiodeId, behandlingId, skjæringstidspunkt, ventetSiden, venterTil, venterPå, yrkesaktivitetssporing, hendelseIder.toSet())
    }
}

internal class VenterPå(
    private val vedtaksperiodeId: UUID,
    private val skjæringstidspunkt: LocalDate,
    private val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
    private val venteårsak: Venteårsak
) {
    fun dto() = VenterPåDto(
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = when (yrkesaktivitetssporing) {
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> "ARBEIDSLEDIG"
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> yrkesaktivitetssporing.organisasjonsnummer
            Behandlingsporing.Yrkesaktivitet.Frilans -> "FRILANDS"
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> "SELVSTENDIG"
        },
        venteårsak = venteårsak.dto()
    )

    internal fun event() = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
        vedtaksperiodeId = vedtaksperiodeId,
        skjæringstidspunkt = skjæringstidspunkt,
        yrkesaktivitetssporing = yrkesaktivitetssporing,
        venteårsak = venteårsak.event()
    )

    override fun toString() =
        "vedtaksperiode $vedtaksperiodeId med skjæringstidspunkt $skjæringstidspunkt for arbeidsgiver $yrkesaktivitetssporing som venter på $venteårsak"
}

internal class Venteårsak private constructor(
    private val hva: Hva,
    private val hvorfor: Hvorfor?,
) {
    fun dto() = VenteårsakDto(hva.name, hvorfor?.name)

    internal fun event() = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
        hva = hva.name,
        hvorfor = hvorfor?.name
    )

    override fun toString() =
        hva.name + if (hvorfor == null) "" else " fordi ${hvorfor.name}"

    enum class Hva {
        GODKJENNING,
        SØKNAD,
        INNTEKTSMELDING,
        BEREGNING,
        UTBETALING,
        HJELP
    }

    enum class Hvorfor {
        OVERSTYRING_IGANGSATT,
        VIL_OMGJØRES
    }

    internal companion object {
        internal infix fun Hva.fordi(hvorfor: Hvorfor) = Venteårsak(this, hvorfor)
        internal val Hva.utenBegrunnelse get() = Venteårsak(this, null)
    }
}

