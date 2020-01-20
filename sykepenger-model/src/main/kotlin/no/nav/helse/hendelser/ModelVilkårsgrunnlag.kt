package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.IAktivitetslogger
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.math.absoluteValue

class ModelVilkårsgrunnlag(
    hendelseId: UUID,
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val rapportertDato: LocalDateTime,
    private val inntektsmåneder: List<Måned>,
    internal val erEgenAnsatt: Boolean,
    private val aktivitetslogger: Aktivitetslogger
) : ArbeidstakerHendelse(hendelseId, Hendelsestype.Vilkårsgrunnlag), IAktivitetslogger by aktivitetslogger {

    override fun rapportertdato() = rapportertDato
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer
    override fun toJson() = TODO()

    private fun beregnetÅrsInntekt(): Double {
        return inntektsmåneder
            .flatMap { it.inntektsliste }
            .sumByDouble { it.beløp }
    }

    private fun differanseInntekt(månedsinntektFraInntektsmelding: Double) =
        ((månedsinntektFraInntektsmelding * 12) - beregnetÅrsInntekt()).absoluteValue / beregnetÅrsInntekt()

    internal fun harAvvikIOppgittInntekt(månedsinntektFraInntektsmelding: Double) =
        differanseInntekt(månedsinntektFraInntektsmelding) > 0.25

    data class Måned(
        val årMåned: YearMonth,
        val inntektsliste: List<Inntekt>
    )

    data class Inntekt(
        val beløp: Double
    )
}

