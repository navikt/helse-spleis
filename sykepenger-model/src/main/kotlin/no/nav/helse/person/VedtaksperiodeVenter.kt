package no.nav.helse.person

import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

internal class VedtaksperiodeVenter private constructor(
    private val vedtaksperiodeId: UUID,
    private val ventetSiden: LocalDateTime,
    private val venterTil: LocalDateTime,
    private val venterPå: VenterPå,
    private val organisasjonsnummer: String) {

    override fun toString(): String {
        return "vedtaksperiode $vedtaksperiodeId for arbeidsgiver $organisasjonsnummer venter på $venterPå. Ventet siden $ventetSiden og venter til $venterTil (${Duration.between(ventetSiden, venterTil)})"
    }

    internal class Builder(private val arbeidsgivere: List<Arbeidsgiver>) {
        private lateinit var vedtaksperiodeId: UUID
        private lateinit var ventetSiden: LocalDateTime
        private lateinit var venterTil: LocalDateTime
        private lateinit var orgnanisasjonsnummer : String
        private var venterPå: VenterPå? = null

        internal fun venter(vedtaksperiodeId: UUID, orgnummer: String, ventetSiden: LocalDateTime, venterTil: LocalDateTime) {
            this.vedtaksperiodeId = vedtaksperiodeId
            this.ventetSiden = ventetSiden
            this.venterTil = venterTil
            this.orgnanisasjonsnummer = orgnummer
        }

        internal fun venterPå(vedtaksperiodeId: UUID, orgnummer: String, venteÅrsak: Venteårsak, ) {
            venterPå = VenterPå(vedtaksperiodeId, orgnummer, venteÅrsak)
        }
        internal fun build() = venterPå?.let { venterPå ->
            VedtaksperiodeVenter(vedtaksperiodeId, ventetSiden, venterTil, venterPå, orgnanisasjonsnummer)
        }
    }
}

internal class VenterPå(
    private val vedtaksperiodeId: UUID,
    private val organisasjonsnummer: String,
    private val venteÅrsak: Venteårsak
) {
    override fun toString(): String {
        return "vedtaksperiode $vedtaksperiodeId for arbeidsgiver $organisasjonsnummer med venteårsak ${venteÅrsak.name}"
    }
}

enum class Venteårsak {
    GODKJENNING,
    SØKNAD,
    INNTEKTSMELDING,
    HJELP
}