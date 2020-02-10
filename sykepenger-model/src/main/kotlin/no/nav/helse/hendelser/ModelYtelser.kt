package no.nav.helse.hendelser

import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.VedtaksperiodeHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ModelYtelser(
    hendelseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val sykepengehistorikk: ModelSykepengehistorikk,
    private val foreldrepenger: ModelForeldrepenger,
    private val rapportertdato: LocalDateTime,
    aktivitetslogger: Aktivitetslogger
) : ArbeidstakerHendelse(hendelseId, Hendelsestype.Ytelser, aktivitetslogger), VedtaksperiodeHendelse {
    internal companion object {
        fun lagBehov(
            vedtaksperiodeId: UUID,
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String,
            utgangspunktForBeregningAvYtelse: LocalDate
        ): Behov {
            val params = mutableMapOf(
                "utgangspunktForBeregningAvYtelse" to utgangspunktForBeregningAvYtelse
            )

            return Behov.nyttBehov(
                hendelsestype = Hendelsestype.Ytelser,
                behov = listOf(Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                additionalParams = params
            )
        }

    }

    internal fun sykepengehistorikk() = sykepengehistorikk

    internal fun foreldrepenger() = foreldrepenger



    internal fun addInntekter(inntekthistorikk: Inntekthistorikk) {
        sykepengehistorikk().addInntekter(this.hendelseId(), inntekthistorikk)
    }

    override fun rapportertdato(): LocalDateTime {
        return rapportertdato
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

    override fun vedtaksperiodeId(): String {
        return vedtaksperiodeId
    }

    internal fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Ytelser")
    }
}
