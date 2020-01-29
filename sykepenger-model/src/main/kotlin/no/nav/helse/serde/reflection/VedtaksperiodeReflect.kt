package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Vedtaksperiode
import java.time.LocalDate
import java.util.*
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

internal class VedtaksperiodeReflect(vedtaksperiode: Vedtaksperiode) {
    private val id: UUID = vedtaksperiode["id"]
    private val aktørId: String = vedtaksperiode["aktørId"]
    private val fødselsnummer: String = vedtaksperiode["fødselsnummer"]
    private val organisasjonsnummer: String = vedtaksperiode["organisasjonsnummer"]
    private val maksdato: LocalDate? = vedtaksperiode["maksdato"]
    private val godkjentAv: String? = vedtaksperiode["godkjentAv"]
    private val utbetalingsreferanse: String? = vedtaksperiode["utbetalingsreferanse"]
    private val førsteFraværsdag:LocalDate? = vedtaksperiode["førsteFraværsdag"]
    private val inntektFraInntektsmelding: Double? = vedtaksperiode["inntektFraInntektsmelding"]
    private val aktivitetslogger: Aktivitetslogger = vedtaksperiode["aktivitetslogger"]
    private val dataForVilkårsvurdering: Map<String, Any>? = vedtaksperiode.get<Vedtaksperiode, ModelVilkårsgrunnlag
        .Grunnlagsdata?>("dataForVilkårsvurdering")?.let {
        mapOf(
            "erEgenAnsatt" to it.erEgenAnsatt,
            "beregnetÅrsinntektFraInntektskomponenten" to it.beregnetÅrsinntektFraInntektskomponenten,
            "avviksprosent" to it.avviksprosent
        )
    }

    internal fun toMap() = mutableMapOf<String, Any?>(
        "id" to id,
        "maksdato" to maksdato,
        "godkjentAv" to godkjentAv,
        "utbetalingsreferanse" to utbetalingsreferanse,
        "førsteFraværsdag" to førsteFraværsdag,
        "inntektFraInntektsmelding" to inntektFraInntektsmelding,
        "dataForVilkårsvurdering" to dataForVilkårsvurdering,
        "aktivitetslogger" to AktivitetsloggerReflect(aktivitetslogger).toMap()
    )
}
