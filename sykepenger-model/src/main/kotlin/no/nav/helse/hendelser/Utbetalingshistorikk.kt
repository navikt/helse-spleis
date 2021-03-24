package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Utbetalingshistorikk(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    arbeidskategorikoder: Map<String, LocalDate>,
    harStatslønn: Boolean,
    perioder: List<Infotrygdperiode>,
    inntektshistorikk: List<Inntektsopplysning>,
    ugyldigePerioder: List<Pair<LocalDate?, LocalDate?>>,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
    besvart: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, aktivitetslogg) {
    private val element = InfotrygdhistorikkElement.opprett(
        oppdatert = besvart,
        hendelseId = meldingsreferanseId(),
        perioder = perioder,
        inntekter = inntektshistorikk,
        arbeidskategorikoder = arbeidskategorikoder,
        ugyldigePerioder = ugyldigePerioder,
        harStatslønn = harStatslønn
    )

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer

    internal fun oppdaterHistorikk(historikk: Infotrygdhistorikk) {
        historikk.oppdaterHistorikk(element)
    }

    internal fun erRelevant(vedtaksperiodeId: UUID) =
        vedtaksperiodeId.toString() == this.vedtaksperiodeId
}
