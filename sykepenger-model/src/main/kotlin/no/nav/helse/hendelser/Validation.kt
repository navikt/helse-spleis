package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
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

internal class ValiderYtelser(
    private val periode: Periode,
    private val ytelser: Ytelser
) : Valideringssteg {
    override fun isValid(): Boolean {
        return !ytelser.valider(periode).hasErrors()
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
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val alder: Alder,
    private val førsteFraværsdag: LocalDate
) : Valideringssteg {
    private lateinit var engine: ArbeidsgiverUtbetalinger
    override fun isValid(): Boolean {
        engine = ArbeidsgiverUtbetalinger(
            tidslinjer = tidslinjer,
            personTidslinje = ytelser.utbetalingshistorikk().utbetalingstidslinje(førsteFraværsdag),
            periode = periode,
            alder = alder,
            arbeidsgiverRegler = NormalArbeidstaker,
            aktivitetslogg = ytelser.aktivitetslogg,
            organisasjonsnummer = organisasjonsnummer,
            fødselsnummer = fødselsnummer
        ).also { engine ->
            engine.beregn()
        }
        return !ytelser.hasErrors()
    }

    internal fun maksdato() = engine.maksdato()
    internal fun gjenståendeSykedager() = engine.gjenståendeSykedager()
    internal fun forbrukteSykedager() = engine.forbrukteSykedager()
    override fun feilmelding() = "Feil ved kalkulering av utbetalingstidslinjer"
}
