package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingslinjer.UtbetalingslinjeBuilder
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate

internal class Validation(private val hendelse: ArbeidstakerHendelse) {
    private lateinit var errorBlock: ErrorBlock

    internal fun onError(block: ErrorBlock) {
        errorBlock = block
    }

    internal fun valider(block: ValiderBlock) {
        if (hendelse.hasErrors()) return
        val steg = block()
        if (steg.isValid()) return
        steg.feilmelding()?.also { hendelse.error(it) }
        errorBlock()
    }

    internal fun onSuccess(successBlock: SuccessBlock) {
        if (!hendelse.hasBehov()) successBlock()
    }
}

internal typealias ErrorBlock = () -> Unit
internal typealias SuccessBlock = () -> Unit
internal typealias ValiderBlock = () -> Valideringssteg

internal interface Valideringssteg {
    fun isValid(): Boolean
    fun feilmelding(): String? = null
}

internal class ValiderYtelser(private val arbeidsgivertidslinje: Sykdomstidslinje, private val ytelser: Ytelser, private val førsteFraværsdag: LocalDate?) : Valideringssteg {
    override fun isValid(): Boolean {
        if (ytelser.valider(førsteFraværsdag).hasBehov()) return false
        val sisteUtbetalteDag = ytelser.utbetalingshistorikk().sisteUtbetalteDag(førsteFraværsdag) ?: return true
        when {
            sisteUtbetalteDag >= arbeidsgivertidslinje.førsteDag() -> ytelser.error("Hele eller deler av perioden til arbeidsgiver er utbetalt i Infotrygd")
            sisteUtbetalteDag >= arbeidsgivertidslinje.førsteDag().minusDays(18) -> ytelser.error("Har utbetalt periode i Infotrygd nærmere enn 18 dager fra første dag")
        }
        return !ytelser.hasErrors()
    }
}

internal class Overlappende(
    private val sykdomsperiode: Periode,
    private val foreldrepermisjon: Foreldrepermisjon
) : Valideringssteg {
    override fun isValid() = !foreldrepermisjon.overlapper(sykdomsperiode)
    override fun feilmelding() = "Har overlappende foreldrepengeperioder med syketilfelle"
}

internal class HarInntektshistorikk(
    private val arbeidsgiver: Arbeidsgiver,
    private val førsteDag: LocalDate
) : Valideringssteg {
    override fun isValid() = arbeidsgiver.inntekt(førsteDag) != null
    override fun feilmelding() = "Vi har ikke inntektshistorikken vi trenger"
}

internal class ByggUtbetalingstidlinjer(
    private val tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>,
    private val periode: Periode,
    private val ytelser: Ytelser,
    private val alder: Alder,
    private val førsteFraværsdag: LocalDate?
) : Valideringssteg {
    private lateinit var engine: ArbeidsgiverUtbetalinger
    override fun isValid(): Boolean {
        engine = ArbeidsgiverUtbetalinger(
            tidslinjer = tidslinjer,
            historiskTidslinje = ytelser.utbetalingshistorikk().utbetalingstidslinje(førsteFraværsdag),
            periode = periode,
            alder = alder,
            arbeidsgiverRegler = NormalArbeidstaker,
            aktivitetslogg = ytelser.aktivitetslogg
        ).also { engine ->
            engine.beregn()
        }
        return !ytelser.hasErrors()
    }

    internal fun maksdato() = engine.maksdato()
    internal fun forbrukteSykedager() = engine.forbrukteSykedager()
    override fun feilmelding() = "Feil ved kalkulering av utbetalingstidslinjer"
}

internal class ByggUtbetalingslinjer(
    private val ytelser: Ytelser,
    private val vedtaksperiode: Vedtaksperiode,
    private val utbetalingstidslinje: Utbetalingstidslinje
) : Valideringssteg {
    private lateinit var utbetalingslinjer: List<Utbetalingslinje>

    internal fun utbetalingslinjer() = utbetalingslinjer

    override fun isValid(): Boolean {
        utbetalingslinjer = UtbetalingslinjeBuilder(
            utbetalingstidslinje,
            vedtaksperiode.periode()
        ).result()
        if (utbetalingslinjer.isEmpty())
            ytelser.info("Ingen utbetalingslinjer bygget")
        else
            ytelser.info("Utbetalingslinjer bygget vellykket")
        return !ytelser.hasErrors()
    }

    override fun feilmelding()   = "Feil ved kalkulering av utbetalingslinjer"
}
