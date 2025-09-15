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

internal data class Venteårsak(
    private val hva: String,
    private val hvorfor: String?,
) {
    internal fun event() = VedtaksperiodeVenterEvent.Venteårsak(
        hva = hva,
        hvorfor = hvorfor
    )

    override fun toString() =
        hva + if (hvorfor == null) "" else " fordi $hvorfor"

    enum class Hvorfor {
        OVERSTYRING_IGANGSATT,
        VIL_OMGJØRES
    }

    internal companion object {
        val GODKJENNING = Venteårsak("GODKJENNING", null)
        val SØKNAD = Venteårsak("SØKNAD", null)
        val INNTEKTSMELDING = Venteårsak("INNTEKTSMELDING", null)
        val BEREGNING = Venteårsak("BEREGNING", null)
        val UTBETALING = Venteårsak("UTBETALING", null)
        val HJELP = Venteårsak("HJELP", null)

        internal infix fun Venteårsak.fordi(hvorfor: Hvorfor) = copy(hvorfor = hvorfor.name)
    }
}

