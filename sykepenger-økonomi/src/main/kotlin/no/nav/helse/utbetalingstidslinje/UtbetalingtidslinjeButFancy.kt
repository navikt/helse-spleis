package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Arbeidsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ForeldetDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavHelgDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag

class UtbetalingtidslinjeButFancy(private val linje: Utbetalingstidslinje) {

    override fun toString() = linje.joinToString(separator = "") {
        (if (it.dato.dayOfWeek == DayOfWeek.MONDAY) " " else "") +
            when (it::class) {
                NavDag::class -> "${ANSI_BRIGHT_WHITE_BACKGROUND}${ANSI_RED}N${ANSI_RESET}"
                NavHelgDag::class -> "${ANSI_CYAN_BACKGROUND}${ANSI_BRIGHT_WHITE}H${ANSI_RESET}"
                Arbeidsdag::class -> "${ANSI_CYAN_BACKGROUND}${ANSI_BRIGHT_WHITE}A${ANSI_RESET}"
                ArbeidsgiverperiodeDag::class -> "${ANSI_RED_BACKGROUND}${ANSI_BRIGHT_WHITE}P${ANSI_RESET}"
                Fridag::class -> "${ANSI_BRIGHT_PURPLE_BACKGROUND}${ANSI_BLACK}F${ANSI_RESET}"
                AvvistDag::class -> "${ANSI_BLACK_BACKGROUND}${ANSI_RED}X${ANSI_RESET}"
                UkjentDag::class -> "${ANSI_BLACK_BACKGROUND}${ANSI_RED}U${ANSI_RESET}"
                ForeldetDag::class -> "${ANSI_RED_BACKGROUND}${ANSI_BLACK}O${ANSI_RESET}"
                else -> "${ANSI_BLUE_BACKGROUND}${ANSI_BLACK}?${ANSI_RESET}"
            }
    }.trim()

    companion object {
        private const val ANSI_RESET: String = "\u001B[0m"
        private const val ANSI_BLACK: String = "\u001B[30m"
        private const val ANSI_RED: String = "\u001B[31m"
        private const val ANSI_GREEN: String = "\u001B[32m"
        private const val ANSI_BRIGHT_GREEN: String = "\u001B[92m"
        private const val ANSI_YELLOW: String = "\u001B[33m"
        private const val ANSI_BLUE: String = "\u001B[34m"
        private const val ANSI_PURPLE: String = "\u001B[35m"
        private const val ANSI_CYAN: String = "\u001B[36m"
        private const val ANSI_BRIGHT_WHITE: String = "\u001B[97m"
        private const val ANSI_BLACK_BACKGROUND: String = "\u001B[40m"
        private const val ANSI_RED_BACKGROUND: String = "\u001B[41m"
        private const val ANSI_GREEN_BACKGROUND: String = "\u001B[42m"
        private const val ANSI_BRIGHT_YELLOW_BACKGROUND: String = "\u001B[93m"
        private const val ANSI_BLUE_BACKGROUND: String = "\u001B[44m"
        private const val ANSI_PURPLE_BACKGROUND: String = "\u001B[45m"
        private const val ANSI_BRIGHT_PURPLE_BACKGROUND: String = "\u001B[105m"
        private const val ANSI_CYAN_BACKGROUND: String = "\u001B[46m"
        private const val ANSI_BRIGHT_WHITE_BACKGROUND: String = "\u001B[107m"
    }
}
