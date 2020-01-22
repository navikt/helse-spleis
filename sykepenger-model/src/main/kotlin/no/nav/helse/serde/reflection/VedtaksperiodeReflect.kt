package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelVilkårsgrunnlag
import no.nav.helse.person.Vedtaksperiode
import java.time.LocalDate
import java.util.*

internal class VedtaksperiodeReflect(vedtaksperiode: Vedtaksperiode) {
    private val id: UUID = vedtaksperiode.getProp("id")
    private val aktørId: String = vedtaksperiode.getProp("aktørId")
    private val fødselsnummer: String = vedtaksperiode.getProp("fødselsnummer")
    private val organisasjonsnummer: String = vedtaksperiode.getProp("organisasjonsnummer")
    private val maksdato: LocalDate? = vedtaksperiode.getProp("maksdato")
    private val godkjentAv: String? = vedtaksperiode.getProp("godkjentAv")
    private val utbetalingsreferanse: String? = vedtaksperiode.getProp("utbetalingsreferanse")
    private val førsteFraværsdag:LocalDate? = vedtaksperiode.getProp("førsteFraværsdag")
    private val inntektFraInntektsmelding: Double? = vedtaksperiode.getProp("inntektFraInntektsmelding")
    private val dataForVilkårsvurdering: Map<String, Any>? = vedtaksperiode.getProp<Vedtaksperiode, ModelVilkårsgrunnlag
        .Grunnlagsdata?>("dataForVilkårsvurdering")?.let {
        mapOf(
            "erEgenAnsatt" to it.erEgenAnsatt,
            "beregnetÅrsinntektFraInntektskomponenten" to it.beregnetÅrsinntektFraInntektskomponenten,
            "avviksprosent" to it.avviksprosent
        )
    }

    internal fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "aktørId" to aktørId,
        "fødselsnummer" to fødselsnummer,
        "organisasjonsnummer" to organisasjonsnummer,
        "maksdato" to maksdato,
        "godkjentAv" to godkjentAv,
        "utbetalingsreferanse" to utbetalingsreferanse,
        "førsteFraværsdag" to førsteFraværsdag,
        "inntektFraInntektsmelding" to inntektFraInntektsmelding,
        "dataForVilkårsvurdering" to dataForVilkårsvurdering
    )
}
