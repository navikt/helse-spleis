package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.VedtaksperiodeVenterDto
import no.nav.helse.dto.VenterPåDto
import no.nav.helse.dto.VenteårsakDto
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.person.PersonObserver.VedtaksperiodeVenterEvent

internal data class VedtaksperiodeVenter(
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val skjæringstidspunkt: LocalDate,
    val ventetSiden: LocalDateTime,
    val venterTil: LocalDateTime,
    val venterPå: VenterPå,
    val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
    val hendelseIder: Set<UUID>
) {

    fun venterPå() = when (venterPå) {
        is VenterPå.AnnenPeriode -> {
            var faktiskVenterPå: Pair<VedtaksperiodeVenterEvent.VenterPå, LocalDateTime>? = null
            var peker: VedtaksperiodeVenter? = this
            while (peker != null) {
                when (peker.venterPå) {
                    is VenterPå.AnnenPeriode -> peker = peker.venterPå.vedtaksperiode.vedtaksperiodeVenter
                    VenterPå.Nestemann -> {
                        error("støtter ikke at annen periode venter på nestemann!")
                    }
                    is VenterPå.SegSelv -> {
                        faktiskVenterPå = Pair(VedtaksperiodeVenterEvent.VenterPå(
                            vedtaksperiodeId = peker.vedtaksperiodeId,
                            skjæringstidspunkt = peker.skjæringstidspunkt,
                            yrkesaktivitetssporing = peker.yrkesaktivitetssporing,
                            venteårsak = peker.venterPå.venteårsak.event()
                        ), peker.venterTil)
                        peker = null
                    }
                }
            }

            checkNotNull(faktiskVenterPå) {
                "Venter på annen periode, men annen periode venter ikke på noe!"
            }
        }

        // nestemann venter på nestemann ...
        VenterPå.Nestemann -> null

        is VenterPå.SegSelv -> VedtaksperiodeVenterEvent.VenterPå(
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            yrkesaktivitetssporing = yrkesaktivitetssporing,
            venteårsak = venterPå.venteårsak.event()
        ) to venterTil
    }

    fun event(nestemann: VedtaksperiodeVenter): VedtaksperiodeVenterEvent? {
        val (venterPåEvent, venterTil) = when (venterPå) {
            VenterPå.Nestemann -> when {
                // vedtaksperioden venter på nestemann, og den er nestemann ...
                nestemann.vedtaksperiodeId == this.vedtaksperiodeId -> return null
                else -> nestemann.venterPå()
            }
            else -> this.venterPå()
        } ?: return null

        return VedtaksperiodeVenterEvent(
            yrkesaktivitetssporing = yrkesaktivitetssporing,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            hendelser = hendelseIder,
            ventetSiden = ventetSiden,
            venterTil = minOf(this.venterTil, venterTil),
            venterPå = venterPåEvent
        )
    }

    fun dto(nestemann: VedtaksperiodeVenter) = event(nestemann)?.let {
        VedtaksperiodeVenterDto(
            ventetSiden = it.ventetSiden,
            venterTil = it.venterTil,
            venterPå = VenterPåDto(
                vedtaksperiodeId = it.venterPå.vedtaksperiodeId,
                organisasjonsnummer = when (it.venterPå.yrkesaktivitetssporing) {
                    Behandlingsporing.Yrkesaktivitet.Arbeidsledig -> "ARBEIDSLEDIG"
                    is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> it.venterPå.yrkesaktivitetssporing.organisasjonsnummer
                    Behandlingsporing.Yrkesaktivitet.Frilans -> "FRILANDS"
                    Behandlingsporing.Yrkesaktivitet.Selvstendig -> "SELVSTENDIG"
                    Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker -> "SELVSTENDIG_JORDBRUKER"
                    Behandlingsporing.Yrkesaktivitet.SelvstendigFisker -> "SELVSTENDIG_FISKER"
                    Behandlingsporing.Yrkesaktivitet.SelvstendigDagmamma -> "SELVSTENDIG_DAGMAMMA"
                },
                venteårsak = VenteårsakDto(it.venterPå.venteårsak.hva, it.venterPå.venteårsak.hvorfor)
            )
        )
    }
}

internal sealed interface VenterPå {
    data class SegSelv(val venteårsak: Venteårsak) : VenterPå
    data class AnnenPeriode(val vedtaksperiode: Vedtaksperiode) : VenterPå
    data object Nestemann : VenterPå
}

internal class Venteårsak private constructor(
    private val hva: Hva,
    private val hvorfor: Hvorfor?,
) {
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

