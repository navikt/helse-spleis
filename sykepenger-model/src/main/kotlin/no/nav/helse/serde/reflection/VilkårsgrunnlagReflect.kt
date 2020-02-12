package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Måned
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse.Hendelsestype
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagReflect(vilkårsgrunnlag: Vilkårsgrunnlag) {
    private val hendelseId: UUID = vilkårsgrunnlag.hendelseId()
    private val hendelsestype: Hendelsestype = vilkårsgrunnlag.hendelsestype()
    private val vedtaksperiodeId: String = vilkårsgrunnlag["vedtaksperiodeId"]
    private val aktørId: String = vilkårsgrunnlag["aktørId"]
    private val fødselsnummer: String = vilkårsgrunnlag["fødselsnummer"]
    private val orgnummer: String = vilkårsgrunnlag["orgnummer"]
    private val rapportertDato: LocalDateTime = vilkårsgrunnlag["rapportertDato"]
    private val inntektsmåneder: List<Måned> = vilkårsgrunnlag["inntektsmåneder"]
    private val arbeidsforhold: Vilkårsgrunnlag.MangeArbeidsforhold? = vilkårsgrunnlag["arbeidsforhold"]
    private val erEgenAnsatt: Boolean = vilkårsgrunnlag["erEgenAnsatt"]
    private val aktivitetslogger: Aktivitetslogger = vilkårsgrunnlag["aktivitetslogger"]

    fun toMap() = mutableMapOf<String, Any?>(
        "type" to hendelsestype.name,
        "data" to mutableMapOf<String, Any?>(
            "hendelseId" to hendelseId,
            "vedtaksperiodeId" to vedtaksperiodeId,
            //"aktørId" to aktørId, // TODO ?
            //"fødselsnummer" to fødselsnummer, // TODO ?
            "orgnummer" to orgnummer,
            "rapportertDato" to rapportertDato,
            "inntektsmåneder" to inntektsmåneder.map { it.toMap() },
            "arbeidsforhold" to arbeidsforhold?.let { ModelArbeidsforholdReflect(it).toList() },
            "erEgenAnsatt" to erEgenAnsatt,
            "aktivitetslogger" to AktivitetsloggerReflect(aktivitetslogger).toMap()
        )
    )

    internal fun toSpeilMap() = mutableMapOf<String, Any?>(
        "type" to hendelsestype.name,
        "hendelseId" to hendelseId,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "orgnummer" to orgnummer,
        "rapportertDato" to rapportertDato,
        "inntektsmåneder" to inntektsmåneder.map { it.toMap() },
        "erEgenAnsatt" to erEgenAnsatt
    )

    private fun Måned.toMap(): MutableMap<String, Any> = mutableMapOf(
        "årMåned" to this.årMåned,
        "inntektsliste" to this.inntektsliste.map { it.toString() }
    )
}
