package no.nav.helse.serde.api.speil.builders

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.AktivitetsloggVisitor
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.serde.api.dto.AktivitetDTO
import java.util.*
import no.nav.helse.serde.api.dto.HendelseDTO

internal class PeriodeVarslerBuilder(
    aktivitetslogg: Aktivitetslogg
) : AktivitetsloggVisitor {

    private val varsler = mutableListOf<AktivitetDTO>()

    init {
        aktivitetslogg.accept(this)
    }

    override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
        kontekster.find { it.kontekstType == "Vedtaksperiode" }
            ?.let { it.kontekstMap["vedtaksperiodeId"] }
            ?.let(UUID::fromString)
            ?.also { vedtaksperiodeId ->
                varsler.add(AktivitetDTO(vedtaksperiodeId, "W", melding, tidsstempel))
            }
    }


    fun build(hendelser: List<HendelseDTO> = emptyList()): List<AktivitetDTO> {
        val varsler = varsler.distinctBy { it.melding }
        val periodeHarEnInntektsmelding = hendelser.count { it.type == "INNTEKTSMELDING" } == 1
        val flereImVarsel = "Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt."
        return if (periodeHarEnInntektsmelding) varsler.filter { it.melding != flereImVarsel } else varsler
    }
}


