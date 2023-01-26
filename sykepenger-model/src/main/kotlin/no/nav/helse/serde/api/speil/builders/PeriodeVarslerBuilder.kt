package no.nav.helse.serde.api.speil.builders

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.hendelser.til
import no.nav.helse.november
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.AktivitetsloggVisitor
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.Varselkode
import no.nav.helse.serde.api.dto.AktivitetDTO

internal class PeriodeVarslerBuilder(
    aktivitetslogg: Aktivitetslogg
) : AktivitetsloggVisitor {

    private val varsler = mutableListOf<AktivitetDTO>()

    init {
        aktivitetslogg.accept(this)
    }

    override fun visitVarsel(id: UUID, kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Varsel, kode: Varselkode?, melding: String, tidsstempel: String) {
        kontekster.find { it.kontekstType == "Vedtaksperiode" }
            ?.let { it.kontekstMap["vedtaksperiodeId"] }
            ?.let(UUID::fromString)
            ?.also { vedtaksperiodeId ->
                varsler.add(AktivitetDTO(vedtaksperiodeId, "W", melding, tidsstempel))
            }
    }


    fun build(): List<AktivitetDTO> {
        val varsler = varsler.distinctBy { it.melding }
        val outOfOrderVarsel = "Saken må revurderes fordi det har blitt behandlet en tidligere periode som kan ha betydning."
        val tidsrom = 20.november(2022) til 30.november(2022)
        val harFeilAktigVarsel = varsler.find {
            it.melding == outOfOrderVarsel && LocalDateTime.parse(
                it.tidsstempel,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            ).toLocalDate() in tidsrom
        } != null

        return if (harFeilAktigVarsel) varsler.filter { it.melding != outOfOrderVarsel } else varsler
    }
}


