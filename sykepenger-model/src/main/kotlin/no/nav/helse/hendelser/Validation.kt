package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.*
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import java.time.LocalDate

internal class Validation(private val hendelse: ArbeidstakerHendelse) {
    private lateinit var errorBlock: ErrorBlock

    internal fun onError(block: ErrorBlock) {
        errorBlock = block
    }

    internal fun valider(block: ValiderBlock) {
        if (hendelse.hasErrors() || hendelse.hasNeeds()) return
        val steg = block()
        if (steg.isValid()) return
        steg.feilmelding()?.also { hendelse.error(it) }
        errorBlock()
    }

    internal fun onSuccess(successBlock: SuccessBlock) {
        if (!hendelse.hasErrors() && !hendelse.hasNeeds()) successBlock()
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
        !hendelse.valider().let { it.hasErrors() || it.hasNeeds() }
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
    private val arbeidsgiver: Arbeidsgiver,
    private val person: Person
) : Valideringssteg {
    override fun isValid(): Boolean {
        hendelse.fortsettÅBehandle(arbeidsgiver, person)  // Double dispatch to invoke correct method
        return !hendelse.hasErrors()
    }

    override fun feilmelding() = "Feil under hendelseshåndtering"
}

internal class Overlappende(
    private val sykdomsperiode: Periode,
    private val foreldrepenger: ModelForeldrepenger
) : Valideringssteg {
    override fun isValid() = !foreldrepenger.overlapper(sykdomsperiode)
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
    private val ytelser: ModelYtelser,
    private val alder: Alder
) : Valideringssteg {
    private lateinit var engine: ArbeidsgiverUtbetalinger
    override fun isValid(): Boolean {
        val aktivitetslogger = Aktivitetslogger()
        engine = ArbeidsgiverUtbetalinger(
            tidslinjer = tidslinjer,
            historiskTidslinje = ytelser.sykepengehistorikk().utbetalingslinjer().utbetalingstidslinje(),
            periode = periode,
            alder = alder,
            arbeidsgiverRegler = NormalArbeidstaker,
            aktivitetslogger = aktivitetslogger
        ).also { engine ->
            engine.beregn()
        }
        ytelser.addAll(aktivitetslogger, "utbetalingstidslinje validering")
        return !ytelser.hasErrors()
    }

    internal fun maksdato() = engine.maksdato()

    override fun feilmelding() = "Feil ved kalkulering av utbetalingstidslinjer"
}

internal class ByggUtbetalingslinjer(private val ytelser: ModelYtelser,
                                     private val utbetalingstidslinje: Utbetalingstidslinje
) : Valideringssteg {
    private lateinit var utbetalingslinjer: List<Utbetalingslinje>

    internal fun utbetalingslinjer() = utbetalingslinjer

    override fun isValid(): Boolean {
        utbetalingslinjer = UtbetalingslinjeBuilder(utbetalingstidslinje).result()
        if (utbetalingslinjer.isEmpty())
            ytelser.error("Ingen utbetalingslinjer bygget")
        else
            ytelser.info("Utbetalingslinjer bygget vellykket")
        return !ytelser.hasErrors() && utbetalingslinjer.isNotEmpty()
    }

    override fun feilmelding()   = "Feil ved kalkulering av utbetalingslinjer"

}
