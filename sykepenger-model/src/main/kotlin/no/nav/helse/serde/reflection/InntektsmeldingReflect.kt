package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelInntektsmelding
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InntektsmeldingReflect(inntektsmelding: ModelInntektsmelding) {
    private val hendelseId: UUID = inntektsmelding.hendelseId()
    private val refusjon: ModelInntektsmelding.Refusjon = inntektsmelding.getProp("refusjon")
    private val orgnummer: String = inntektsmelding.getProp("orgnummer")
    private val fødselsnummer: String = inntektsmelding.getProp("fødselsnummer")
    private val aktørId: String = inntektsmelding.getProp("aktørId")
    private val mottattDato: LocalDateTime = inntektsmelding.getProp("mottattDato")
    private val førsteFraværsdag: LocalDate = inntektsmelding.getProp("førsteFraværsdag")
    private val beregnetInntekt: Double = inntektsmelding.getProp("beregnetInntekt")
    private val originalJson: String = inntektsmelding.getProp("originalJson")
    private val arbeidsgiverperioder: List<ModelInntektsmelding.Periode.Arbeidsgiverperiode> =
        inntektsmelding.getProp("arbeidsgiverperioder")
    private val ferieperioder: List<ModelInntektsmelding.Periode.Ferieperiode> =
        inntektsmelding.getProp("ferieperioder")

    fun toMap() = mutableMapOf<String, Any?>(
        "hendelseId" to hendelseId,
        "refusjon" to refusjon,
        "orgnummer" to orgnummer,
        "fødselsnummer" to fødselsnummer,
        "aktørId" to aktørId,
        "mottattDato" to mottattDato,
        "førsteFraværsdag" to førsteFraværsdag,
        "beregnetInntekt" to beregnetInntekt,
        "originalJson" to originalJson,
        "arbeidsgiverperioder" to arbeidsgiverperioder.map(::periodeToMap),
        "ferieperioder" to ferieperioder.map(::periodeToMap)
    )

    private fun periodeToMap(it: ModelInntektsmelding.Periode) = mapOf(
        "fom" to it.fom,
        "tom" to it.tom
    )
}
