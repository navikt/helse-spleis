package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID

internal class VedtaksperiodeVenter private constructor(
    private val vedtaksperiodeId: UUID,
    private val ventetSiden: LocalDateTime,
    private val venterTil: LocalDateTime,
    private val venterPå: VenterPå,
    private val organisasjonsnummer: String,
    private val hendelseIder: Set<UUID>) {

    internal fun event(aktørId: String, fødselsnummer: String) =
        PersonObserver.VedtaksperiodeVenterEvent(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            ventetSiden = ventetSiden,
            venterTil = venterTil,
            venterPå = venterPå.event(),
            hendelser = hendelseIder
    )

    internal class Builder {
        private lateinit var vedtaksperiodeId: UUID
        private lateinit var ventetSiden: LocalDateTime
        private lateinit var venterTil: LocalDateTime
        private lateinit var orgnanisasjonsnummer : String
        private lateinit var venterPå: VenterPå
        private val hendelseIder = mutableSetOf<UUID>()

        internal fun venter(vedtaksperiodeId: UUID, orgnummer: String, ventetSiden: LocalDateTime, venterTil: LocalDateTime) {
            this.vedtaksperiodeId = vedtaksperiodeId
            this.ventetSiden = ventetSiden
            this.venterTil = venterTil
            this.orgnanisasjonsnummer = orgnummer
        }
        internal fun hendelseIder(hendelseIder: Set<UUID>) {
            this.hendelseIder.addAll(hendelseIder)
        }

        internal fun venterPå(vedtaksperiodeId: UUID, orgnummer: String, venteÅrsak: Venteårsak) {
            venterPå = VenterPå(vedtaksperiodeId, orgnummer, venteÅrsak)
        }

        internal fun build() =
            VedtaksperiodeVenter(vedtaksperiodeId, ventetSiden, venterTil, venterPå, orgnanisasjonsnummer, hendelseIder.toSet())
    }
}

internal class VenterPå(
    private val vedtaksperiodeId: UUID,
    private val organisasjonsnummer: String,
    private val venteårsak: Venteårsak
) {
    internal fun event() = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = organisasjonsnummer,
        venteårsak = venteårsak.event()
    )
    override fun toString() =
        "vedtaksperiode $vedtaksperiodeId for arbeidsgiver $organisasjonsnummer som venter på $venteårsak"
}

internal class Venteårsak private constructor(
    private val hva: Hva,
    private val hvorfor: Hvorfor?,
){
    internal fun event() = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
        hva = hva.name,
        hvorfor = hvorfor?.name
    )
    override fun toString() =
        hva.name + if(hvorfor == null) "" else " fordi ${hvorfor.name}"
    enum class Hva {
        GODKJENNING,
        SØKNAD,
        INNTEKTSMELDING,
        BEREGNING,
        UTBETALING,
        HJELP
    }

    enum class Hvorfor {
        MANGLER_TILSTREKKELIG_INFORMASJON_TIL_UTBETALING_SAMME_ARBEIDSGIVER,
        MANGLER_TILSTREKKELIG_INFORMASJON_TIL_UTBETALING_ANDRE_ARBEIDSGIVERE,
        HAR_SYKMELDING_SOM_OVERLAPPER_PÅ_ANDRE_ARBEIDSGIVERE,
        MANGLER_INNTEKT_FOR_VILKÅRSPRØVING_PÅ_ANDRE_ARBEIDSGIVERE,
        MANGLER_REFUSJONSOPPLYSNINGER_PÅ_ANDRE_ARBEIDSGIVERE,
        OVERSTYRING_IGANGSATT
    }

    internal companion object {
        internal infix fun Hva.fordi(hvorfor: Hvorfor) = Venteårsak(this, hvorfor)
        internal val Hva.utenBegrunnelse get() = Venteårsak(this, null)
    }
}

