package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.utenPerioder
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel

internal fun List<Arbeidsgiverberegning>.avvisMaksimumSykepengerdager(maksdatoberegning: Maksdatoberegning): List<Arbeidsgiverberegning> {
    val vurderinger = maksdatoberegning.beregn(this)

    /** går gjennom alle maksdato-sakene og avslår dager. EGENTLIG er det nok å avslå dagene
     *  fra sisteVurdering, men det er noen enhetstester som tester veldig lange
     *  tidslinjer og de forventer at alle maksdatodager avslås, uavhengig av maksdatosak
     */
    val begrunnelser = vurderinger
        .flatMap { maksdatosak ->
            maksdatosak.begrunnelser.map { (begrunnelse, dato) -> dato to begrunnelse }
        }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })

    val avvisteTidslinjer = begrunnelser.entries.fold(this) { result, (begrunnelse, dager) ->
        result.avvis(dager.grupperSammenhengendePerioder(), begrunnelse)
    }

    return avvisteTidslinjer
}

internal fun List<Arbeidsgiverberegning>.maksimumUtbetalingsberegning(sykepengegrunnlagBegrenset6G: Inntekt, andreYtelser: (dato: LocalDate) -> Prosentdel): List<Arbeidsgiverberegning> {
    val betalteTidslinjer = Utbetalingstidslinje
        .betale(sykepengegrunnlagBegrenset6G, this.map { it.samletTidslinje }, andreYtelser)
        .zip(this) { beregnetTidslinje, arbeidsgiver ->
            arbeidsgiver.copy(
                vedtaksperioder = arbeidsgiver.vedtaksperioder.map { vedtaksperiode ->
                    vedtaksperiode.copy(
                        utbetalingstidslinje = beregnetTidslinje.subset(vedtaksperiode.periode)
                    )
                }
            )
        }
    return betalteTidslinjer
}

internal fun List<Arbeidsgiverberegning>.avvisMinsteinntekt(sekstisyvårsdagen: LocalDate, erUnderMinsteinntektskravTilFylte67: Boolean, erUnderMinsteinntektEtterFylte67: Boolean): List<Arbeidsgiverberegning> {
    fun List<Arbeidsgiverberegning>.avvisMinsteinntektTilFylte67(): List<Arbeidsgiverberegning> {
        if (!erUnderMinsteinntektskravTilFylte67) return this
        return avvis(listOf(LocalDate.MIN til sekstisyvårsdagen), Begrunnelse.MinimumInntekt)
    }

    fun List<Arbeidsgiverberegning>.avvisMinsteinntektEtterFylte67(): List<Arbeidsgiverberegning> {
        if (!erUnderMinsteinntektEtterFylte67) return this
        return avvis(listOf(sekstisyvårsdagen.nesteDag til LocalDate.MAX), Begrunnelse.MinimumInntektOver67)
    }
    return this
        .avvisMinsteinntektTilFylte67()
        .avvisMinsteinntektEtterFylte67()
}

internal fun List<Arbeidsgiverberegning>.avvisOpptjening(harOpptjening: Boolean): List<Arbeidsgiverberegning> {
    if (harOpptjening) return this
    return this.avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerOpptjening)
}

internal fun List<Arbeidsgiverberegning>.avvisMedlemskap(erMedlemAvFolketrygden: Boolean): List<Arbeidsgiverberegning> {
    if (erMedlemAvFolketrygden) return this
    return this.avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerMedlemskap)
}

internal fun List<Arbeidsgiverberegning>.sykdomsgradsberegning(perioderMedMinimumSykdomsgradVurdertOK: Set<Periode>): List<Arbeidsgiverberegning> {
    fun List<Arbeidsgiverberegning>.totalSykdomsgradsberegning(): List<Arbeidsgiverberegning> {
        return Utbetalingstidslinje.totalSykdomsgrad(this.map { it.samletTidslinje })
            .zip(this) { beregnetTidslinje, arbeidsgiver ->
                arbeidsgiver.copy(
                    vedtaksperioder = arbeidsgiver.vedtaksperioder.map { vedtaksperiodeberegning ->
                        vedtaksperiodeberegning.copy(
                            utbetalingstidslinje = beregnetTidslinje.subset(vedtaksperiodeberegning.periode)
                        )
                    },
                    ghostOgAndreInntektskilder = arbeidsgiver.ghostOgAndreInntektskilder.map {
                        beregnetTidslinje.subset(it.periode())
                    }
                )
            }
    }

    fun List<Arbeidsgiverberegning>.avvisSykdomsgradUnderGrense(): List<Arbeidsgiverberegning> {
        val tentativtAvvistePerioder = Utbetalingsdag.dagerUnderGrensen(this.map { it.samletVedtaksperiodetidslinje })
        val avvistePerioder = tentativtAvvistePerioder.utenPerioder(perioderMedMinimumSykdomsgradVurdertOK)

        val avvisteTidslinjer = this.avvis(avvistePerioder, Begrunnelse.MinimumSykdomsgrad)
        return avvisteTidslinjer
    }

    return this
        .totalSykdomsgradsberegning()
        .avvisSykdomsgradUnderGrense()
}
