package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import java.time.LocalDate

internal class SykepengehistorikkReflect(utbetalingshistorikk: Utbetalingshistorikk) {
    private val utbetalinger: List<Utbetalingshistorikk.Periode> = utbetalingshistorikk["utbetalinger"]
    private val inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning> =
        utbetalingshistorikk["inntektshistorikk"]
    private val aktivitetslogger: Aktivitetslogger = utbetalingshistorikk["aktivitetslogger"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "utbetalinger" to utbetalinger.map {
            mutableMapOf<String, Any?>(
                "fom" to it.fom,
                "tom" to it.tom,
                "dagsats" to it.dagsats,
                "type" to when (it) {
                    is Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver -> "RefusjonTilArbeidsgiver"
                    is Utbetalingshistorikk.Periode.ReduksjonMedlem -> "ReduksjonMedlem"
                    is Utbetalingshistorikk.Periode.Etterbetaling -> "Etterbetaling"
                    is Utbetalingshistorikk.Periode.KontertRegnskap -> "KontertRegnskap"
                    is Utbetalingshistorikk.Periode.ReduksjonArbeidsgiverRefusjon -> "ReduksjonArbeidsgiverRefusjon"
                    is Utbetalingshistorikk.Periode.Tilbakeført -> "Tilbakeført"
                    is Utbetalingshistorikk.Periode.Konvertert -> "Konvertert"
                    is Utbetalingshistorikk.Periode.Ferie -> "Ferie"
                    is Utbetalingshistorikk.Periode.Opphold -> "Opphold"
                    is Utbetalingshistorikk.Periode.Sanksjon -> "Sanksjon"
                    is Utbetalingshistorikk.Periode.Ukjent -> "Ukjent"
                }
            )
        },
        "inntektshistorikk" to inntektshistorikk.map { InntektsopplysningReflect(it).toMap() },
        "aktivitetslogger" to AktivitetsloggerReflect(aktivitetslogger).toMap()
    )

    private class InntektsopplysningReflect(inntektsopplysning: Utbetalingshistorikk.Inntektsopplysning) {
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
