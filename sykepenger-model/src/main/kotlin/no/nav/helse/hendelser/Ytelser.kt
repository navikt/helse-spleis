package no.nav.helse.hendelser


import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import java.time.LocalDate
import java.util.*

@Deprecated("Sykepengehistorikk og foreldrepenger sendes som to parametre til modellen")
class Ytelser(
    private val meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    internal val vedtaksperiodeId: String,
    private val utbetalingshistorikk: Utbetalingshistorikk,
    private val foreldrepermisjon: Foreldrepermisjon,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(aktivitetslogg) {
    internal fun utbetalingshistorikk() = utbetalingshistorikk

    internal fun foreldrepenger() = foreldrepermisjon

    fun valider(førsteFraværsdag: LocalDate? = null) = utbetalingshistorikk.valider(førsteFraværsdag, Alder(fødselsnummer), ArbeidsgiverRegler.Companion.NormalArbeidstaker)

    internal fun addInntekter(inntekthistorikk: Inntekthistorikk) {
        utbetalingshistorikk().addInntekter(this.meldingsreferanseId, inntekthistorikk)
    }

    override fun aktørId(): String {
        return aktørId
    }

    override fun fødselsnummer(): String {
        return fødselsnummer
    }

    override fun organisasjonsnummer(): String {
        return organisasjonsnummer
    }
}
