package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelSykepengehistorikk
import java.time.LocalDate

internal class SykepengehistorikkReflect(sykepengehistorikk: ModelSykepengehistorikk) {
    private val utbetalinger: List<ModelSykepengehistorikk.Periode> = sykepengehistorikk["utbetalinger"]
    private val inntektshistorikk: List<ModelSykepengehistorikk.Inntektsopplysning> =
        sykepengehistorikk["inntektshistorikk"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "utbetalinger" to utbetalinger.map {
            mutableMapOf<String, Any?>(
                "fom" to it.fom,
                "tom" to it.tom,
                "dagsats" to it.dagsats,
                "type" to when(it) {
                    is ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver -> "RefusjonTilArbeidsgiver"
                    is ModelSykepengehistorikk.Periode.ReduksjonMedlem -> "ReduksjonMedlem"
                    is ModelSykepengehistorikk.Periode.Etterbetaling -> "Etterbetaling"
                    is ModelSykepengehistorikk.Periode.KontertRegnskap -> "KontertRegnskap"
                    is ModelSykepengehistorikk.Periode.ReduksjonArbeidsgiverRefusjon -> "ReduksjonArbeidsgiverRefusjon"
                    is ModelSykepengehistorikk.Periode.Tilbakeført -> "Tilbakeført"
                    is ModelSykepengehistorikk.Periode.Konvertert -> "Konvertert"
                    is ModelSykepengehistorikk.Periode.Ferie -> "Ferie"
                    is ModelSykepengehistorikk.Periode.Opphold -> "Opphold"
                    is ModelSykepengehistorikk.Periode.Sanksjon -> "Sanksjon"
                    is ModelSykepengehistorikk.Periode.Ukjent -> "Ukjent"
                }
            )
        },
        "inntektshistorikk" to inntektshistorikk.map { InntektsopplysningReflect(it).toMap() }
    )

    private class InntektsopplysningReflect(inntektsopplysning: ModelSykepengehistorikk.Inntektsopplysning) {
        private val sykepengerFom: LocalDate = inntektsopplysning["sykepengerFom"]
        private val inntektPerMåned: Int = inntektsopplysning["inntektPerMåned"]
        private val orgnummer: String = inntektsopplysning["orgnummer"]

        internal fun toMap() = mutableMapOf<String, Any?>(
            "sykepengerFom" to sykepengerFom,
            "inntektPerMåned" to inntektPerMåned,
            "orgnummer" to orgnummer
        )
    }
}
