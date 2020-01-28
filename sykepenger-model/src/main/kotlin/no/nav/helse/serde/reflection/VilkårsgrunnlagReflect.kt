package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import no.nav.helse.person.ArbeidstakerHendelse.Hendelsestype
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagReflect(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
    private val hendelseId: UUID = vilkårsgrunnlag.hendelseId()
    private val hendelsestype: Hendelsestype = vilkårsgrunnlag.hendelsetype()
    private val vedtaksperiodeId: String = vilkårsgrunnlag["vedtaksperiodeId"]
    private val aktørId: String = vilkårsgrunnlag["aktørId"]
    private val fødselsnummer: String = vilkårsgrunnlag["fødselsnummer"]
    private val orgnummer: String = vilkårsgrunnlag["orgnummer"]
    private val rapportertDato: LocalDateTime = vilkårsgrunnlag["rapportertDato"]
    private val inntektsmåneder: List<ModelVilkårsgrunnlag.Måned> = vilkårsgrunnlag["inntektsmåneder"]
    private val erEgenAnsatt: Boolean = vilkårsgrunnlag["erEgenAnsatt"]

    fun toMap() = mutableMapOf<String, Any?>(
        "hendelseId" to hendelseId,
        "hendelsetype" to hendelsestype.name,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "aktørId" to aktørId,
        "fødselsnummer" to fødselsnummer,
        "orgnummer" to orgnummer,
        "rapportertDato" to rapportertDato,
        "inntektsmåneder" to inntektsmåneder.map { InntektsmånederReflect(it).toMap() },
        "erEgenAnsatt" to erEgenAnsatt
    )

    private class InntektsmånederReflect(private val måned: ModelVilkårsgrunnlag.Måned) {
        internal fun toMap() = mutableMapOf<String, Any?>(
            "årMåned" to måned.årMåned,
            "inntektsliste" to måned.inntektsliste.map { mutableMapOf("beløp" to it.beløp) }
        )
    }
}
