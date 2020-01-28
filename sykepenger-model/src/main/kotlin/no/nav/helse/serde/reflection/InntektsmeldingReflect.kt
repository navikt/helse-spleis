package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.person.ArbeidstakerHendelse.Hendelsestype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InntektsmeldingReflect(inntektsmelding: ModelInntektsmelding) {
    private val hendelseId: UUID = inntektsmelding.hendelseId()
    private val hendelsetype: Hendelsestype = inntektsmelding.hendelsetype()
    private val refusjon: ModelInntektsmelding.Refusjon = inntektsmelding["refusjon"]
    private val orgnummer: String = inntektsmelding["orgnummer"]
    private val fødselsnummer: String = inntektsmelding["fødselsnummer"]
    private val aktørId: String = inntektsmelding["aktørId"]
    private val mottattDato: LocalDateTime = inntektsmelding["mottattDato"]
    private val førsteFraværsdag: LocalDate = inntektsmelding["førsteFraværsdag"]
    private val beregnetInntekt: Double = inntektsmelding["beregnetInntekt"]
    private val originalJson: String = inntektsmelding["originalJson"]
    private val arbeidsgiverperioder: List<ModelInntektsmelding.InntektsmeldingPeriode.Arbeidsgiverperiode> =
        inntektsmelding["arbeidsgiverperioder"]
    private val ferieperioder: List<ModelInntektsmelding.InntektsmeldingPeriode.Ferieperiode> =
        inntektsmelding["ferieperioder"]

    fun toMap() = mutableMapOf<String, Any?>(
        "hendelseId" to hendelseId,
        "hendelsetype" to hendelsetype.name,
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

    private fun periodeToMap(periode: ModelInntektsmelding.InntektsmeldingPeriode) = mutableMapOf<String, Any?>(
        "fom" to periode.fom,
        "tom" to periode.tom
    )
}
