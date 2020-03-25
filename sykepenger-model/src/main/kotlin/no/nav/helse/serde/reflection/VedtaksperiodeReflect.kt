package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.api.mapDataForVilkårsvurdering
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeReflect(vedtaksperiode: Vedtaksperiode) {
    private val id: UUID = vedtaksperiode["id"]
    private val aktørId: String = vedtaksperiode["aktørId"]
    private val fødselsnummer: String = vedtaksperiode["fødselsnummer"]
    private val organisasjonsnummer: String = vedtaksperiode["organisasjonsnummer"]
    private val maksdato: LocalDate? = vedtaksperiode["maksdato"]
    private val forbrukteSykedager: Int? = vedtaksperiode["forbrukteSykedager"]
    private val godkjentAv: String? = vedtaksperiode["godkjentAv"]
    private val godkjenttidspunkt: LocalDateTime? = vedtaksperiode["godkjenttidspunkt"]
    private val utbetalingsreferanse: String? = vedtaksperiode["utbetalingsreferanse"]
    private val førsteFraværsdag:LocalDate? = vedtaksperiode["førsteFraværsdag"]
    private val dataForVilkårsvurdering: Map<String, Any>? = vedtaksperiode.get<Vedtaksperiode, Vilkårsgrunnlag.Grunnlagsdata?>("dataForVilkårsvurdering")?.let {
        mapOf(
            "erEgenAnsatt" to it.erEgenAnsatt,
            "beregnetÅrsinntektFraInntektskomponenten" to it.beregnetÅrsinntektFraInntektskomponenten,
            "avviksprosent" to it.avviksprosent,
            "antallOpptjeningsdagerErMinst" to it.antallOpptjeningsdagerErMinst,
            "harOpptjening" to it.harOpptjening
        )
    }

    internal fun toMap() = mutableMapOf<String, Any?>(
        "id" to id,
        "maksdato" to maksdato,
        "forbrukteSykedager" to forbrukteSykedager,
        "godkjentAv" to godkjentAv,
        "godkjenttidspunkt" to godkjenttidspunkt,
        "utbetalingsreferanse" to utbetalingsreferanse,
        "førsteFraværsdag" to førsteFraværsdag,
        "dataForVilkårsvurdering" to dataForVilkårsvurdering
    )

    internal fun toSpeilMap(arbeidsgiver: Arbeidsgiver) = mutableMapOf<String, Any?>(
        "id" to id,
        "maksdato" to maksdato,
        "forbrukteSykedager" to forbrukteSykedager,
        "godkjentAv" to godkjentAv,
        "godkjenttidspunkt" to godkjenttidspunkt,
        "utbetalingsreferanse" to utbetalingsreferanse,
        "førsteFraværsdag" to førsteFraværsdag,
        "inntektFraInntektsmelding" to førsteFraværsdag?.let { arbeidsgiver.inntekt(it)?.toDouble() }
    )
}
