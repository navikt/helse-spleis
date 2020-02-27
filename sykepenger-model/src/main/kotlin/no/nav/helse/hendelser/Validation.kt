package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import java.time.LocalDate

internal class Validation(private val hendelse: ArbeidstakerHendelse) {
    private lateinit var errorBlock: ErrorBlock

    internal fun onError(block: ErrorBlock) {
        errorBlock = block
    }

    internal fun valider(block: ValiderBlock) {
        if (hendelse.hasErrorsOld() || hendelse.hasNeedsOld()) return
        val steg = block()
        if (steg.isValid()) return
        steg.feilmelding()?.also {
            hendelse.errorOld(it)
            hendelse.error(it)
        }
        errorBlock()
    }

    internal fun onSuccess(successBlock: SuccessBlock) {
        if (!hendelse.hasErrorsOld() && !hendelse.hasNeedsOld()) successBlock()
    }
}

internal typealias ErrorBlock = () -> Unit
internal typealias SuccessBlock = () -> Unit
internal typealias ValiderBlock = () -> Valideringssteg

internal interface Valideringssteg {
    fun isValid(): Boolean
    fun feilmelding(): String? = null
}

// Invoke internal validation of a Hendelse
internal class ValiderSykdomshendelse(private val hendelse: SykdomstidslinjeHendelse) : Valideringssteg {
    override fun isValid() =
        !hendelse.valider().let { it.hasErrorsOld() || it.hasNeedsOld() }
}

internal class ValiderYtelser(private val ytelser: Ytelser) : Valideringssteg {
    override fun isValid() =
        !ytelser.valider().let { it.hasErrorsOld() || it.hasNeedsOld() }
}

// Confirm that only one Arbeidsgiver exists for a Person (temporary; remove in Epic 7)
internal class ValiderKunEnArbeidsgiver(
    private val arbeidsgivere: List<Arbeidsgiver>
) : Valideringssteg {
    override fun isValid() = arbeidsgivere.size == 1
    override fun feilmelding() = "Bruker har mer enn én arbeidsgiver"
}

// Continue processing Hendelse with appropriate Arbeidsgiver
internal class ArbeidsgiverHåndterHendelse(
    private val hendelse: SykdomstidslinjeHendelse,
    private val arbeidsgiver: Arbeidsgiver
) : Valideringssteg {
    override fun isValid(): Boolean {
        hendelse.fortsettÅBehandle(arbeidsgiver)  // Double dispatch to invoke correct method
        return !hendelse.hasErrorsOld()
    }

    override fun feilmelding() = String.format("Feil under håndtering av %s", hendelse::class.simpleName)
}

internal class Overlappende(
    private val sykdomsperiode: Periode,
    private val foreldrepermisjon: Foreldrepermisjon
) : Valideringssteg {
    override fun isValid() = !foreldrepermisjon.overlapper(sykdomsperiode)
    override fun feilmelding() = "Har overlappende foreldrepengeperioder med syketilfelle"
}

internal class GapPå26Uker(
    private val tidslinje: ConcreteSykdomstidslinje,
    private val sisteHistoriskeSykedag: LocalDate?
) : Valideringssteg {
    override fun isValid() =
        sisteHistoriskeSykedag.let { it == null || it.plusWeeks(26) < tidslinje.førsteDag() }

    override fun feilmelding() = "Har historikk innenfor 26 uker"
}

internal class HarInntektshistorikk(
    private val arbeidsgiver: Arbeidsgiver,
    private val førsteDag: LocalDate
) : Valideringssteg {
    override fun isValid() = arbeidsgiver.inntekt(førsteDag) != null
    override fun feilmelding() = "Vi har ikke inntektshistorikken vi trenger"
}

internal class HarArbeidsgivertidslinje(private val arbeidsgiver: Arbeidsgiver) : Valideringssteg {
    override fun isValid() = arbeidsgiver.sykdomstidslinje() != null
    override fun feilmelding() = "Arbeidsgiver har ikke en sykdomstidslinje"
}

internal class ByggUtbetalingstidlinjer(
    private val tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>,
    private val periode: Periode,
    private val ytelser: Ytelser,
    private val alder: Alder
) : Valideringssteg {
    private lateinit var engine: ArbeidsgiverUtbetalinger
    override fun isValid(): Boolean {
        val aktivitetslogger = Aktivitetslogger()
        engine = ArbeidsgiverUtbetalinger(
            tidslinjer = tidslinjer,
            historiskTidslinje = ytelser.sykepengehistorikk().utbetalingstidslinje(),
            periode = periode,
            alder = alder,
            arbeidsgiverRegler = NormalArbeidstaker,
            aktivitetslogger = aktivitetslogger
        ).also { engine ->
            engine.beregn()
        }
        ytelser.addAll(aktivitetslogger, "utbetalingstidslinje validering")
        return !ytelser.hasErrorsOld()
    }

    internal fun maksdato() = engine.maksdato()
    internal fun forbrukteSykedager() = engine.forbrukteSykedager()
    override fun feilmelding() = "Feil ved kalkulering av utbetalingstidslinjer"
}

internal class ByggUtbetalingslinjer(private val ytelser: Ytelser,
                                     private val utbetalingstidslinje: Utbetalingstidslinje
) : Valideringssteg {
    private lateinit var utbetalingslinjer: List<Utbetalingslinje>

    internal fun utbetalingslinjer() = utbetalingslinjer

    override fun isValid(): Boolean {
        utbetalingslinjer = UtbetalingslinjeBuilder(utbetalingstidslinje).result()
        if (utbetalingslinjer.isEmpty())
            ytelser.errorOld("Ingen utbetalingslinjer bygget")
        else
            ytelser.infoOld("Utbetalingslinjer bygget vellykket")
        return !ytelser.hasErrorsOld() && utbetalingslinjer.isNotEmpty()
    }

    override fun feilmelding()   = "Feil ved kalkulering av utbetalingslinjer"

}
