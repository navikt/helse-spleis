package no.nav.helse.spleis.e2e

import no.nav.helse.person.*
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal enum class Kilde {
    SKATT, INFOTRYGD, INNTEKTSMELDING, SAKSBEHANDLER
}

internal class InntektshistorikkVol2Inspektør(person: Person, orgnummer: String) : ArbeidsgiverVisitor {
    private class HentArbeidsgiver(person: Person, private val orgnummer: String) : PersonVisitor {
        lateinit var arbeidsgiver: Arbeidsgiver

        init {
            person.accept(this)
        }

        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            if (organisasjonsnummer == orgnummer) this.arbeidsgiver = arbeidsgiver
        }

    }

    private val innslag = mutableListOf<List<Opplysning>>()
    private val inntektsopplysninger = mutableListOf<Opplysning>()

    class Opplysning(
        val dato: LocalDate,
        val sykepengegrunnlag: Inntekt?,
        val sammenligningsgrunnlag: Inntekt?,
        val kilde: Kilde
    )

    init {
        HentArbeidsgiver(person, orgnummer).also { results ->
            results.arbeidsgiver.accept(this)
        }
    }

    val antallInnslag get() = innslag.size
    internal val sisteInnslag get() = innslag.firstOrNull()

    override fun preVisitInnslag(innslag: InntektshistorikkVol2.Innslag) {
        inntektsopplysninger.clear()
    }

    override fun postVisitInnslag(innslag: InntektshistorikkVol2.Innslag) {
        this.innslag.add(inntektsopplysninger.toList())
    }

    override fun visitInntektsmelding(
        inntektsmelding: InntektshistorikkVol2.Inntektsmelding,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysninger.add(
            Opplysning(
                dato,
                inntektsmelding.grunnlagForSykepengegrunnlag(dato),
                inntektsmelding.grunnlagForSammenligningsgrunnlag(dato),
                Kilde.INNTEKTSMELDING
            )
        )
    }

    override fun visitInfotrygd(
        infotrygd: InntektshistorikkVol2.Infotrygd,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        inntektsopplysninger.add(
            Opplysning(
                dato,
                infotrygd.grunnlagForSykepengegrunnlag(dato),
                infotrygd.grunnlagForSammenligningsgrunnlag(dato),
                Kilde.INFOTRYGD
            )
        )
    }

    private lateinit var skattedato: LocalDate

    override fun visitSkattSykepengegrunnlag(
        sykepengegrunnlag: InntektshistorikkVol2.Skatt.Sykepengegrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: InntektshistorikkVol2.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        skattedato = dato
    }

    override fun visitSkattSammenligningsgrunnlag(
        sammenligningsgrunnlag: InntektshistorikkVol2.Skatt.Sammenligningsgrunnlag,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: InntektshistorikkVol2.Skatt.Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime
    ) {
        skattedato = dato
    }

    override fun postVisitSkatt(skattComposite: InntektshistorikkVol2.SkattComposite) {
        inntektsopplysninger.add(
            Opplysning(
                skattedato,
                skattComposite.grunnlagForSykepengegrunnlag(skattedato),
                skattComposite.grunnlagForSammenligningsgrunnlag(skattedato),
                Kilde.SKATT
            )
        )
    }
}
