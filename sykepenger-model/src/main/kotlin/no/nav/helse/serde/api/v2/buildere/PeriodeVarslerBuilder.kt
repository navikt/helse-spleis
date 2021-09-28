package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.AktivitetsloggVisitor
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.serde.api.AktivitetDTO
import java.util.*

internal class VedtaksperiodeVarslerBuilder(
    private val vedtaksperiodeId: UUID,
    aktivitetsloggForPeriode: Aktivitetslogg,
    aktivitetsloggForVilkårsprøving: Aktivitetslogg,
    vilkårsgrunnlag: IVilkårsgrunnlag?
) {
    private val meldingsreferanseId = (vilkårsgrunnlag as? SpleisGrunnlag)?.meldingsreferanseId
    private val varslerForPeriode = PeriodeVarslerBuilder(aktivitetsloggForPeriode).build()
    private val varslerForVilkårsprøving = meldingsreferanseId?.let {
        VilkårsgrunnlagVarslerBuilder(meldingsreferanseId, vedtaksperiodeId, aktivitetsloggForVilkårsprøving).build()
    } ?: emptyList()

    internal fun build() = (varslerForPeriode + varslerForVilkårsprøving).distinctBy { it.melding }

    private class PeriodeVarslerBuilder(aktivitetslogg: Aktivitetslogg): AktivitetsloggVisitor {
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

        fun build(): List<AktivitetDTO> = varsler.distinctBy { it.melding }
    }

    private class VilkårsgrunnlagVarslerBuilder(
        private val vilkårsgrunnlagId: UUID,
        private val vedtaksperiodeId: UUID,
        aktivitetslogg: Aktivitetslogg
    ): AktivitetsloggVisitor {
        private val varsler = mutableListOf<AktivitetDTO>()
        init {
            aktivitetslogg.accept(this)
        }

        private fun harVilkårsgrunnlagVarsler(vilkårsgrunnlagId: UUID, kontekster: List<SpesifikkKontekst>) =
            kontekster
                .filter { it.kontekstType == "Vilkårsgrunnlag" }
                .mapNotNull { it.kontekstMap["meldingsreferanseId"] }
                .map(UUID::fromString)
                .any { it == vilkårsgrunnlagId }

        override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) {
            if (harVilkårsgrunnlagVarsler(vilkårsgrunnlagId, kontekster)) {
                varsler.add(AktivitetDTO(vedtaksperiodeId, "W", melding, tidsstempel))
            }
        }

        fun build(): List<AktivitetDTO> = varsler.distinctBy { it.melding }
    }
}


