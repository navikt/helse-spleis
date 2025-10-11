package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingstidslinje.Maksdatoberegning.State.Død
import no.nav.helse.utbetalingstidslinje.Maksdatoberegning.State.ForGammel
import no.nav.helse.utbetalingstidslinje.Maksdatoberegning.State.Karantene
import no.nav.helse.utbetalingstidslinje.Maksdatoberegning.State.Syk
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag

internal class Maksdatoberegning(
    private val sekstisyvårsdagen: LocalDate,
    private val syttiårsdagen: LocalDate,
    private val dødsdato: LocalDate?,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val infotrygdtidslinje: Utbetalingstidslinje
) {
    internal companion object {
        const val TILSTREKKELIG_OPPHOLD_I_SYKEDAGER = 26 * 7
        private const val HISTORISK_PERIODE_I_ÅR: Long = 3
    }

    private val _maksdatosaker = mutableListOf<Maksdatokontekst>()
    internal var sisteVurdering = Maksdatokontekst.TomKontekst
        private set
    internal val maksdatosaker get() = _maksdatosaker.plusElement(sisteVurdering)

    private var state: State = State.Initiell

    internal fun beregnMaksdatoBegrensetTilPeriode(periode: Periode): Maksdatoresultat {
        return sisteVurdering
            .avgrensTil(periode.endInclusive)
            .beregnMaksdato(sekstisyvårsdagen, syttiårsdagen, dødsdato, arbeidsgiverRegler)
    }

    private fun vurderStopp(dato: LocalDate) {
        when (state) {
            State.Initiell,
            Karantene,
            State.KaranteneTilstrekkeligOppholdNådd,
            State.Opphold,
            State.OppholdFri,
            Syk -> when {
                syttiårsdagen <= dato -> state(ForGammel)
                dødsdato != null && dødsdato < dato -> state(Død)
            }
            ForGammel,
            Død -> {
                // vurderer ikke lenger stopp fordi vi er i en sluttilstand
            }
        }
    }

    fun beregn(arbeidsgivere: List<Arbeidsgiverberegning>): List<Maksdatokontekst> {
        val tidslinjegrunnlag = arbeidsgivere.map { it.samletVedtaksperiodetidslinje }.plusElement(infotrygdtidslinje)
        val beregnetTidslinje = tidslinjegrunnlag.reduce(Utbetalingstidslinje::plus)

        Utbetalingstidslinje.periode(tidslinjegrunnlag)
            ?.forEach { dato ->
                vurderStopp(dato)

                when (val dag = beregnetTidslinje[dato]) {
                    is Utbetalingsdag.Arbeidsdag -> state.oppholdsdag(this, dag.dato)
                    is Utbetalingsdag.ArbeidsgiverperiodeDag -> state.oppholdsdag(this, dag.dato)
                    is Utbetalingsdag.ArbeidsgiverperiodedagNav -> state.oppholdsdag(this, dag.dato)
                    is Utbetalingsdag.AvvistDag -> state.avvistDag(this, dag.dato)
                    is Utbetalingsdag.ForeldetDag -> state.oppholdsdag(this, dag.dato)
                    is Utbetalingsdag.Fridag -> state.fridag(this, dag.dato)
                    is NavDag -> state.betalbarDag(this, dag.dato)
                    is Utbetalingsdag.NavHelgDag -> state.sykdomshelg(this, dag.dato)
                    is UkjentDag -> state.oppholdsdag(this, dag.dato)
                    is Utbetalingsdag.Ventetidsdag -> state.oppholdsdag(this, dag.dato)
                }
            }

        return maksdatosaker
    }

    private fun state(nyState: State) {
        if (this.state == nyState) return
        state.leaving(this)
        state = nyState
        state.entering(this)
    }

    private fun økOppholdstelling(dato: LocalDate) {
        sisteVurdering = sisteVurdering.medOppholdsdag(dato)
    }

    private fun håndterBetalbarDag(dagen: LocalDate) {
        sisteVurdering = sisteVurdering.inkrementer(dagen)
        when {
            sisteVurdering.erDagerUnder67ÅrForbrukte(arbeidsgiverRegler) || sisteVurdering.erDagerOver67ÅrForbrukte(sekstisyvårsdagen, arbeidsgiverRegler) -> state(Karantene)
            else -> state(Syk)
        }
    }

    private fun håndterBetalbarDagEtterFerie(dagen: LocalDate) {
        håndterBetalbarDag(dagen)
    }

    private fun håndterBetalbarDagEtterOpphold(dagen: LocalDate) {
        sisteVurdering = sisteVurdering.dekrementer(dagen, dagen.minusYears(HISTORISK_PERIODE_I_ÅR))
        håndterBetalbarDag(dagen)
    }

    private fun håndterBetalbarDagEtterMaksdato(dag: LocalDate) {
        sisteVurdering = sisteVurdering.medAvslåttDag(dag)
    }

    private sealed interface State {
        fun betalbarDag(avgrenser: Maksdatoberegning, dagen: LocalDate)
        fun avvistDag(avgrenser: Maksdatoberegning, dagen: LocalDate) = oppholdsdag(avgrenser, dagen)
        fun oppholdsdag(avgrenser: Maksdatoberegning, dagen: LocalDate)
        fun sykdomshelg(avgrenser: Maksdatoberegning, dagen: LocalDate)
        fun fridag(avgrenser: Maksdatoberegning, dagen: LocalDate) = oppholdsdag(avgrenser, dagen)
        fun entering(avgrenser: Maksdatoberegning) {}
        fun leaving(avgrenser: Maksdatoberegning) {}

        object Initiell : State {
            override fun entering(avgrenser: Maksdatoberegning) {
                avgrenser._maksdatosaker.add(avgrenser.sisteVurdering)
                avgrenser.sisteVurdering = Maksdatokontekst.TomKontekst
            }
            override fun oppholdsdag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.sisteVurdering = avgrenser.sisteVurdering.copy(vurdertTilOgMed = dagen)
            }
            override fun sykdomshelg(avgrenser: Maksdatoberegning, dagen: LocalDate) {}
            override fun betalbarDag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                /* starter en helt ny maksdatosak 😊 */
                avgrenser.sisteVurdering = Maksdatokontekst(
                    vurdertTilOgMed = dagen,
                    startdatoSykepengerettighet = dagen,
                    startdatoTreårsvindu = dagen.minusYears(HISTORISK_PERIODE_I_ÅR),
                    betalteDager = setOf(dagen),
                    oppholdsdager = emptySet(),
                    avslåtteDager = emptySet()
                )
                avgrenser.state(Syk)
            }
        }

        object Syk : State {
            override fun entering(avgrenser: Maksdatoberegning) {
                check(avgrenser.sisteVurdering.oppholdsteller == 0)
            }

            override fun betalbarDag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.håndterBetalbarDag(dagen)
            }

            override fun sykdomshelg(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                /* verken forbrukt dag eller oppholdsdag 😌 */
            }

            override fun oppholdsdag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                avgrenser.state(Opphold)
            }

            override fun fridag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                avgrenser.state(OppholdFri)
            }
        }

        object Opphold : State {

            override fun betalbarDag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterOpphold(dagen)
            }

            override fun sykdomshelg(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                oppholdsdag(avgrenser, dagen)
            }

            override fun oppholdsdag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                if (avgrenser.sisteVurdering.oppholdsteller < TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) return
                avgrenser.state(Initiell)
            }
        }

        object OppholdFri : State {
            override fun betalbarDag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterFerie(dagen)
            }

            override fun sykdomshelg(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                /* verken forbrukt dag eller oppholdsdag 😌 */
            }

            override fun fridag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                if (avgrenser.sisteVurdering.oppholdsteller < TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) return
                avgrenser.state(Initiell)
            }

            override fun oppholdsdag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                if (avgrenser.sisteVurdering.oppholdsteller < TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) return avgrenser.state(Opphold)
                avgrenser.state(Initiell)
            }
        }

        object Karantene : State {
            override fun entering(avgrenser: Maksdatoberegning) {
                check(avgrenser.sisteVurdering.oppholdsteller == 0)
            }

            override fun betalbarDag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterMaksdato(dagen)
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            override fun avvistDag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterMaksdato(dagen)
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            override fun sykdomshelg(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                /* helg skal ikke medføre ny rettighet */
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            override fun oppholdsdag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                if (avgrenser.sisteVurdering.oppholdsteller < TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) return
                avgrenser.state(Initiell)
            }

            override fun fridag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.økOppholdstelling(dagen)
                /* helg skal ikke medføre ny rettighet */
                vurderTilstrekkeligOppholdNådd(avgrenser)
            }

            private fun vurderTilstrekkeligOppholdNådd(avgrenser: Maksdatoberegning) {
                if (avgrenser.sisteVurdering.oppholdsteller < TILSTREKKELIG_OPPHOLD_I_SYKEDAGER) return
                avgrenser.state(KaranteneTilstrekkeligOppholdNådd)
            }
        }

        object KaranteneTilstrekkeligOppholdNådd : State {
            override fun betalbarDag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avvistDag(avgrenser, dagen)
            }

            override fun avvistDag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterMaksdato(dagen)
            }

            override fun sykdomshelg(avgrenser: Maksdatoberegning, dagen: LocalDate) {}
            override fun oppholdsdag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.state(Initiell)
            }

            override fun fridag(avgrenser: Maksdatoberegning, dagen: LocalDate) {}
        }

        object ForGammel : State {
            override fun betalbarDag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                over70(avgrenser, dagen)
            }

            override fun sykdomshelg(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                over70(avgrenser, dagen)
            }

            override fun oppholdsdag(avgrenser: Maksdatoberegning, dagen: LocalDate) {}
            override fun leaving(avgrenser: Maksdatoberegning) = throw IllegalStateException("Kan ikke gå ut fra state ForGammel")
            private fun over70(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterMaksdato(dagen)
            }
        }
        object Død : State {
            override fun betalbarDag(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                død(avgrenser, dagen)
            }

            override fun sykdomshelg(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                død(avgrenser, dagen)
            }

            override fun oppholdsdag(avgrenser: Maksdatoberegning, dagen: LocalDate) {}
            override fun leaving(avgrenser: Maksdatoberegning) = throw IllegalStateException("Kan ikke gå ut fra state Død")
            private fun død(avgrenser: Maksdatoberegning, dagen: LocalDate) {
                avgrenser.håndterBetalbarDagEtterMaksdato(dagen)
            }
        }
    }
}
