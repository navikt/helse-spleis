package no.nav.helse.hendelser


import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
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

    internal fun valider(periode: Periode) =
        utbetalingshistorikk.valider(periode)

    internal fun addInntekt(organisasjonsnummer: String, inntekthistorikk: Inntekthistorikk) {
        utbetalingshistorikk().addInntekter(this.meldingsreferanseId, organisasjonsnummer, inntekthistorikk)
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
