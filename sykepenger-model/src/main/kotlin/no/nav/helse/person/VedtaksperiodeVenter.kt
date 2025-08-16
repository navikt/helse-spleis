package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.VedtaksperiodeVenterDto
import no.nav.helse.dto.VenterPåDto
import no.nav.helse.dto.VenteårsakDto
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.person.PersonObserver.VedtaksperiodeVenterEvent

internal data class VedtaksperiodeVenterdata(
    val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val skjæringstidspunkt: LocalDate,
    val hendelseIder: Set<UUID>,
    val ventetSiden: LocalDateTime,
    val venterTil: LocalDateTime
) {
    fun event(venteårsak: Venteårsak) =
        VedtaksperiodeVenterEvent.VenterPå(
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            yrkesaktivitetssporing = yrkesaktivitetssporing,
            venteårsak = venteårsak.event()
        )
}

internal data class VedtaksperiodeVenter(
    val vedtaksperiodedata: VedtaksperiodeVenterdata,
    val venterPå: VenterPå
) {
    fun event(nestemann: VedtaksperiodeVenter): VedtaksperiodeVenterEvent? {
        val venterPåEvent = when (venterPå) {
            VenterPå.Nestemann -> when {
                // vedtaksperioden venter på nestemann, og den er nestemann ...
                nestemann.vedtaksperiodedata == this.vedtaksperiodedata -> return null
                else -> when (val venterPå = nestemann.venterPå) {
                    is VenterPå.AnnenPeriode -> venterPå.annenPeriode.event(venterPå.venteårsak)
                    // vedtaksperioden venter på nestemann, og nestemann er nestemann ...
                    VenterPå.Nestemann -> return null
                    is VenterPå.SegSelv -> nestemann.vedtaksperiodedata.event(venterPå.venteårsak)
                }
            }

            is VenterPå.AnnenPeriode -> venterPå.annenPeriode.event(venterPå.venteårsak)
            is VenterPå.SegSelv -> vedtaksperiodedata.event(venterPå.venteårsak)
        }

        val venterTil = when (venterPå) {
            is VenterPå.AnnenPeriode -> minOf(vedtaksperiodedata.venterTil, venterPå.annenPeriode.venterTil)
            VenterPå.Nestemann -> minOf(vedtaksperiodedata.venterTil, nestemann.vedtaksperiodedata.venterTil)
            is VenterPå.SegSelv -> vedtaksperiodedata.venterTil
        }

        return VedtaksperiodeVenterEvent(
            yrkesaktivitetssporing = vedtaksperiodedata.yrkesaktivitetssporing,
            vedtaksperiodeId = vedtaksperiodedata.vedtaksperiodeId,
            behandlingId = vedtaksperiodedata.behandlingId,
            skjæringstidspunkt = vedtaksperiodedata.skjæringstidspunkt,
            hendelser = vedtaksperiodedata.hendelseIder,
            ventetSiden = vedtaksperiodedata.ventetSiden,
            venterTil = venterTil,
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
    data class AnnenPeriode(val annenPeriode: VedtaksperiodeVenterdata, val venteårsak: Venteårsak) : VenterPå
    data object Nestemann : VenterPå
}

internal class Venteårsak private constructor(
    private val hva: Hva,
    private val hvorfor: Hvorfor?,
) {
    internal fun event() = VedtaksperiodeVenterEvent.Venteårsak(
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

