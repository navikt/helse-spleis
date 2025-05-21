package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.math.MathContext

internal class V327ProsenterSomTallMellomNullOgEn : JsonMigration(327) {
    override val description = "migrerer alle prosenter fra tall mellom 0 og 100 til desimaltall mellom 0 og 1"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("sykdomshistorikk").forEach { it -> migrerSykdomshistorikk(it) }
            arbeidsgiver.path("utbetalinger").forEach { it -> migrerUtbetaling(it) }
            arbeidsgiver.path("vedtaksperioder").forEach { migrerVedtaksperiode(it) }
            arbeidsgiver.path("forkastede").forEach { migrerVedtaksperiode(it.path("vedtaksperiode")) }
        }
    }

    private fun migrerSykdomshistorikk(sykdomshistorikkElement: JsonNode) {
        migrerSykdomstidslinje(sykdomshistorikkElement.path("hendelseSykdomstidslinje"))
        migrerSykdomstidslinje(sykdomshistorikkElement.path("beregnetSykdomstidslinje"))
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                migrerSykdomstidslinje(endring.path("sykdomstidslinje"))
                migrerUtbetalingstidslinje(endring.path("utbetalingstidslinje"))
            }
        }
    }

    private fun migrerUtbetaling(utbetaling: JsonNode) {
        migrerUtbetalingstidslinje(utbetaling.path("utbetalingstidslinje"))
    }

    private fun migrerUtbetalingstidslinje(utbetalingstidslinje: JsonNode) {
        utbetalingstidslinje.path("dager").forEach { dag ->
            dag as ObjectNode
            dag.migrer("grad")
            dag.migrer("totalGrad")
            dag.migrer("utbetalingsgrad")
            dag.migrer("dekningsgrad")
        }
    }

    private fun migrerSykdomstidslinje(sykdomstidslinje: JsonNode) {
        sykdomstidslinje.path("dager").forEach { dag ->
            dag as ObjectNode
            dag.migrer("grad")
        }
    }

    private fun ObjectNode.migrer(navn: String) {
        val verdi = this.path(navn)
        check(verdi.isNumber) { "Kan ikke migrere grad fordi $navn ikke er tall (er ${verdi.nodeType})" }
        this.put(navn, verdi.migrerGrad())
    }
}

private val enkle = mapOf(
    "0.0" to 0.0,
    "1.0" to 0.01,
    "2.0" to 0.02,
    "3.0" to 0.03,
    "4.0" to 0.04,
    "5.0" to 0.05,
    "6.0" to 0.06,
    "7.0" to 0.07,
    "8.0" to 0.08,
    "9.0" to 0.09,
    "10.0" to 0.1,
    "11.0" to 0.11,
    "12.0" to 0.12,
    "13.0" to 0.13,
    "14.0" to 0.14,
    "15.0" to 0.15,
    "16.0" to 0.16,
    "17.0" to 0.17,
    "18.0" to 0.18,
    "19.0" to 0.19,
    "20.0" to 0.2,
    "21.0" to 0.21,
    "22.0" to 0.22,
    "23.0" to 0.23,
    "24.0" to 0.24,
    "25.0" to 0.25,
    "26.0" to 0.26,
    "27.0" to 0.27,
    "28.0" to 0.28,
    "29.0" to 0.29,
    "30.0" to 0.3,
    "31.0" to 0.31,
    "32.0" to 0.32,
    "33.0" to 0.33,
    "34.0" to 0.34,
    "35.0" to 0.35,
    "36.0" to 0.36,
    "37.0" to 0.37,
    "38.0" to 0.38,
    "39.0" to 0.39,
    "40.0" to 0.4,
    "41.0" to 0.41,
    "42.0" to 0.42,
    "43.0" to 0.43,
    "44.0" to 0.44,
    "45.0" to 0.45,
    "46.0" to 0.46,
    "47.0" to 0.47,
    "48.0" to 0.48,
    "49.0" to 0.49,
    "50.0" to 0.5,
    "51.0" to 0.51,
    "52.0" to 0.52,
    "53.0" to 0.53,
    "54.0" to 0.54,
    "55.0" to 0.55,
    "56.0" to 0.56,
    "57.0" to 0.57,
    "58.0" to 0.58,
    "59.0" to 0.59,
    "60.0" to 0.6,
    "61.0" to 0.61,
    "62.0" to 0.62,
    "63.0" to 0.63,
    "64.0" to 0.64,
    "65.0" to 0.65,
    "66.0" to 0.66,
    "67.0" to 0.67,
    "68.0" to 0.68,
    "69.0" to 0.69,
    "70.0" to 0.7,
    "71.0" to 0.71,
    "72.0" to 0.72,
    "73.0" to 0.73,
    "74.0" to 0.74,
    "75.0" to 0.75,
    "76.0" to 0.76,
    "77.0" to 0.77,
    "78.0" to 0.78,
    "79.0" to 0.79,
    "80.0" to 0.8,
    "81.0" to 0.81,
    "82.0" to 0.82,
    "83.0" to 0.83,
    "84.0" to 0.84,
    "85.0" to 0.85,
    "86.0" to 0.86,
    "87.0" to 0.87,
    "88.0" to 0.88,
    "89.0" to 0.89,
    "90.0" to 0.9,
    "91.0" to 0.91,
    "92.0" to 0.92,
    "93.0" to 0.93,
    "94.0" to 0.94,
    "95.0" to 0.95,
    "96.0" to 0.96,
    "97.0" to 0.97,
    "98.0" to 0.98,
    "99.0" to 0.99,
    "100.0" to 1.0
)

private val mc = MathContext.DECIMAL128
private val HUNDRE_PROSENT = 100.0.toBigDecimal(mc)
private fun JsonNode.migrerGrad(): Double {
    return enkle[this.asText()] ?: this.asDouble().toBigDecimal(mc).divide(HUNDRE_PROSENT, mc).toDouble()
}
