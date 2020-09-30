package no.nav.helse.hendelser


import no.nav.helse.person.*
import java.util.*

class Ytelser(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    internal val vedtaksperiodeId: String,
    private val utbetalingshistorikk: Utbetalingshistorikk,
    private val foreldrepermisjon: Foreldrepermisjon,
    private val pleiepenger: Pleiepenger,
    private val institusjonsopphold: Institusjonsopphold,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(meldingsreferanseId, aktivitetslogg) {
    internal fun utbetalingshistorikk() = utbetalingshistorikk

    internal fun foreldrepenger() = foreldrepermisjon

    internal fun pleiepenger() = pleiepenger

    internal fun institusjonsopphold() = institusjonsopphold

    internal fun valider(periode: Periode, periodetype: Periodetype) =
        utbetalingshistorikk.valider(periode, periodetype)

    internal fun addInntekt(organisasjonsnummer: String, inntektshistorikk: Inntektshistorikk) {
        utbetalingshistorikk().addInntekter(meldingsreferanseId(), organisasjonsnummer, inntektshistorikk)
    }

    internal fun addInntekter(person: Person) {
        utbetalingshistorikk.addInntekter(person, this)
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
