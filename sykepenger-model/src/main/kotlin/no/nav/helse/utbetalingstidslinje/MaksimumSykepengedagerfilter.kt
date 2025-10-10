package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.Tidslinjedag
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder.Companion.subsumsjonsformat
import no.nav.helse.etterlevelse.`§ 8-12 ledd 1 punktum 1`
import no.nav.helse.etterlevelse.`§ 8-12 ledd 2`
import no.nav.helse.etterlevelse.`§ 8-3 ledd 1 punktum 2`
import no.nav.helse.etterlevelse.`§ 8-51 ledd 3`
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_9
import no.nav.helse.utbetalingstidslinje.Maksdatoberegning.Companion.TILSTREKKELIG_OPPHOLD_I_SYKEDAGER

internal class MaksimumSykepengedagerfilter(
    private val maksdatoberegning: Maksdatoberegning,
    private val syttiårsdagen: LocalDate,
    private val dødsdato: LocalDate?,
    private val subsumsjonslogg: Subsumsjonslogg,
    private val aktivitetslogg: IAktivitetslogg,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val infotrygdtidslinje: Utbetalingstidslinje
) : UtbetalingstidslinjerFilter {

    private lateinit var beregnetTidslinje: Utbetalingstidslinje
    private lateinit var tidslinjegrunnlag: List<Utbetalingstidslinje>

    private val tidslinjegrunnlagsubsumsjon by lazy { tidslinjegrunnlag.subsumsjonsformat() }
    private val beregnetTidslinjesubsumsjon by lazy { beregnetTidslinje.subsumsjonsformat() }

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
        tidslinjegrunnlag = arbeidsgivere.map { it.samletVedtaksperiodetidslinje } + listOf(infotrygdtidslinje.fremTilOgMed(vedtaksperiode.endInclusive))
        beregnetTidslinje = tidslinjegrunnlag.reduce(Utbetalingstidslinje::plus)

        val vurderinger = maksdatoberegning.beregn(arbeidsgivere)

        // todo: rart å subsummere alle vurderingene siden tidenes morgen?
        vurderinger.forEach { vurdering ->
            // Bare relevant om det er ny rett på sykepenger eller om vilkåret ikke er oppfylt
            val harTilstrekkeligOpphold = vurdering.datoForTilstrekkeligOppholdOppnådd(TILSTREKKELIG_OPPHOLD_I_SYKEDAGER)
            val gjenståendeSykepengedager = vurdering.gjenståendeDagerUnder67År(arbeidsgiverRegler)

            if (harTilstrekkeligOpphold != null || gjenståendeSykepengedager == 0) {
                subsumsjonslogg.logg(
                    `§ 8-12 ledd 2`(
                        oppfylt = harTilstrekkeligOpphold != null,
                        dato = vurdering.vurdertTilOgMed,
                        tilstrekkeligOppholdISykedager = TILSTREKKELIG_OPPHOLD_I_SYKEDAGER,
                        tidslinjegrunnlag = tidslinjegrunnlagsubsumsjon,
                        beregnetTidslinje = beregnetTidslinjesubsumsjon,
                    )
                )
            }
        }

        /** går gjennom alle maksdato-sakene og avslår dager. EGENTLIG er det nok å avslå dagene
         *  fra sisteVurdering, men det er noen enhetstester som tester veldig lange
         *  tidslinjer og de forventer at alle maksdatodager avslås, uavhengig av maksdatosak
         */
        val begrunnelser = vurderinger
            .flatMap { maksdatosak -> maksdatosak.begrunnelseForAvslåtteDager(syttiårsdagen, dødsdato, arbeidsgiverRegler, TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        val avvisteTidslinjer = begrunnelser.entries.fold(arbeidsgivere) { result, (begrunnelse, dager) ->
            result.avvis(dager.grupperSammenhengendePerioder(), begrunnelse)
        }

        val sisteVurdering = vurderinger.last()

        if (sisteVurdering.fremdelesSykEtterTilstrekkeligOpphold(vedtaksperiode, TILSTREKKELIG_OPPHOLD_I_SYKEDAGER)) {
            aktivitetslogg.funksjonellFeil(RV_VV_9)
        }
        if (sisteVurdering.harNåddMaks(vedtaksperiode))
            aktivitetslogg.info("Maks antall sykepengedager er nådd i perioden")
        else
            aktivitetslogg.info("Maksimalt antall sykedager overskrides ikke i perioden")

        return avvisteTidslinjer
    }
}

data class Maksdatovurdering(
    val resultat: Maksdatoresultat,
    val tidslinjegrunnlagsubsumsjon: List<List<Tidslinjedag>>,
    val beregnetTidslinjesubsumsjon: List<Tidslinjedag>,
    val syttiårsdag: LocalDate
) {
    fun subsummer(subsumsjonslogg: Subsumsjonslogg, vedtaksperiode: Periode) {
        val førSyttiårsdagen = fun(subsumsjonslogg: Subsumsjonslogg, utfallTom: LocalDate) {
            subsumsjonslogg.logg(
                `§ 8-3 ledd 1 punktum 2`(
                    oppfylt = true,
                    syttiårsdagen = syttiårsdag,
                    utfallFom = vedtaksperiode.start,
                    utfallTom = utfallTom,
                    tidslinjeFom = vedtaksperiode.start,
                    tidslinjeTom = vedtaksperiode.endInclusive,
                    avvistePerioder = emptyList()
                )
            )
        }

        when (resultat.bestemmelse) {
            Maksdatoresultat.Bestemmelse.IKKE_VURDERT -> error("ugyldig situasjon ${resultat.bestemmelse}")
            Maksdatoresultat.Bestemmelse.ORDINÆR_RETT -> {
                `§ 8-12 ledd 1 punktum 1`(vedtaksperiode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, resultat.gjenståendeDager, resultat.antallForbrukteDager, resultat.maksdato, resultat.startdatoSykepengerettighet ?: LocalDate.MIN).forEach {
                    subsumsjonslogg.logg(it)
                }
                førSyttiårsdagen(subsumsjonslogg, vedtaksperiode.endInclusive)
            }

            Maksdatoresultat.Bestemmelse.BEGRENSET_RETT -> {
                `§ 8-51 ledd 3`(vedtaksperiode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, resultat.gjenståendeDager, resultat.antallForbrukteDager, resultat.maksdato, resultat.startdatoSykepengerettighet ?: LocalDate.MIN).forEach {
                    subsumsjonslogg.logg(it)
                }
                førSyttiårsdagen(subsumsjonslogg, syttiårsdag.forrigeDag)
            }

            Maksdatoresultat.Bestemmelse.SYTTI_ÅR -> {
                if (vedtaksperiode.start < syttiårsdag) {
                    førSyttiårsdagen(subsumsjonslogg, syttiårsdag.forrigeDag)
                }

                val avvisteDagerFraOgMedSøtti = resultat.avslåtteDager.flatten().filter { it >= syttiårsdag }
                if (avvisteDagerFraOgMedSøtti.isNotEmpty()) {
                    subsumsjonslogg.logg(
                        `§ 8-3 ledd 1 punktum 2`(
                            oppfylt = false,
                            syttiårsdagen = syttiårsdag,
                            utfallFom = maxOf(syttiårsdag, vedtaksperiode.start),
                            utfallTom = vedtaksperiode.endInclusive,
                            tidslinjeFom = vedtaksperiode.start,
                            tidslinjeTom = vedtaksperiode.endInclusive,
                            avvistePerioder = avvisteDagerFraOgMedSøtti.grupperSammenhengendePerioder()
                        )
                    )
                }
            }
        }
    }
}
