package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelSykepengehistorikk
import java.time.LocalDate

internal class SykepengehistorikkReflect(sykepengehistorikk: ModelSykepengehistorikk) {
    private val utbetalinger: List<ModelSykepengehistorikk.Periode> = sykepengehistorikk.getProp("utbetalinger")
    private val inntektshistorikk: List<ModelSykepengehistorikk.Inntektsopplysning> =
        sykepengehistorikk.getProp("inntektshistorikk")

    internal fun toMap() = mutableMapOf<String, Any?>(
        "utbetalinger" to utbetalinger.map {
            mutableMapOf<String, Any?>(
                "fom" to it.fom,
                "tom" to it.tom,
                "dagsats" to it.dagsats
            )
        },
        "inntektshistorikk" to inntektshistorikk.map { InntektsopplysningReflect(it).toMap() }
    )

    private class InntektsopplysningReflect(inntektsopplysning: ModelSykepengehistorikk.Inntektsopplysning) {
        private val sykepengerFom: LocalDate = inntektsopplysning.getProp("sykepengerFom")
        private val inntektPerMåned: Int = inntektsopplysning.getProp("inntektPerMåned")
        private val orgnummer: String = inntektsopplysning.getProp("orgnummer")

        internal fun toMap() = mutableMapOf<String, Any?>(
            "sykepengerFom" to sykepengerFom,
            "inntektPerMåned" to inntektPerMåned,
            "orgnummer" to orgnummer
        )
    }
}
