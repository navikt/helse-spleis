package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagReflect(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
    private val hendelseId: UUID = vilkårsgrunnlag.hendelseId()
    private val vedtaksperiodeId: String = vilkårsgrunnlag.getProp("vedtaksperiodeId")
    private val aktørId: String = vilkårsgrunnlag.getProp("aktørId")
    private val fødselsnummer: String = vilkårsgrunnlag.getProp("fødselsnummer")
    private val orgnummer: String = vilkårsgrunnlag.getProp("orgnummer")
    private val rapportertDato: LocalDateTime = vilkårsgrunnlag.getProp("rapportertDato")
    private val inntektsmåneder: List<ModelVilkårsgrunnlag.Måned> = vilkårsgrunnlag.getProp("inntektsmåneder")
    private val erEgenAnsatt: Boolean = vilkårsgrunnlag.getProp("erEgenAnsatt")
    private val originalJson: String = vilkårsgrunnlag.getProp("originalJson")

    fun toMap() = mutableMapOf<String, Any?>(
        "hendelseId" to hendelseId,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "aktørId" to aktørId,
        "fødselsnummer" to fødselsnummer,
        "orgnummer" to orgnummer,
        "rapportertDato" to rapportertDato,
        "inntektsmåneder" to inntektsmåneder.map { InntektsmånederReflect(it).toMap() },
        "erEgenAnsatt" to erEgenAnsatt,
        "originalJson" to originalJson
    )

    private class InntektsmånederReflect(private val måned: ModelVilkårsgrunnlag.Måned) {
        internal fun toMap() = mutableMapOf<String, Any?>(
            "årMåned" to måned.årMåned,
            "inntektsliste" to måned.inntektsliste.map { mutableMapOf("beløp" to it.beløp) }
        )
    }
}
