package no.nav.helse.serde.api.speil

import java.util.UUID
import no.nav.helse.serde.api.dto.AnnullertPeriode
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.GenerasjonDTO
import no.nav.helse.serde.api.dto.Tidslinjeperiode
import no.nav.helse.serde.api.dto.Tidslinjeperiode.Companion.sorterEtterPeriode
import no.nav.helse.serde.api.dto.UberegnetPeriode
import no.nav.helse.serde.api.dto.UberegnetVilkårsprøvdPeriode

class Generasjoner {
    private val nåværendeGenerasjon = mutableListOf<Tidslinjeperiode>()
    private val generasjoner = mutableListOf<GenerasjonDTO>()
    private var tilstand: Byggetilstand = Byggetilstand.Initiell

    internal fun build(): List<GenerasjonDTO> {
        byggGenerasjon(nåværendeGenerasjon)
        return this.generasjoner.toList()
    }

    internal fun revurdertPeriode(periode: BeregnetPeriode) {
        tilstand.revurdertPeriode(this, periode)
    }

    internal fun annullertPeriode(periode: AnnullertPeriode) {
        tilstand.annullertPeriode(this, periode)
    }

    internal fun uberegnetPeriode(uberegnetPeriode: UberegnetPeriode) {
        tilstand.uberegnetPeriode(this, uberegnetPeriode)
    }

    internal fun uberegnetVilkårsprøvdPeriode(uberegnetVilkårsprøvdPeriode: UberegnetVilkårsprøvdPeriode) {
        tilstand.uberegnetVilkårsprøvdPeriode(this, uberegnetVilkårsprøvdPeriode)
    }

    internal fun utbetaltPeriode(beregnetPeriode: BeregnetPeriode) {
        tilstand.utbetaltPeriode(this, beregnetPeriode)
    }

    private fun byggGenerasjon(periodene: List<Tidslinjeperiode>) {
        if (periodene.isEmpty()) return
        generasjoner.add(0, GenerasjonDTO(UUID.randomUUID(), periodene.sorterEtterPeriode()))
    }

    private fun leggTilNyRad() {
        byggGenerasjon(nåværendeGenerasjon.filterNot { it.venter() })
    }

    private fun leggTilNyRadOgPeriode(periode: Tidslinjeperiode, nesteTilstand: Byggetilstand) {
        leggTilNyRad()
        leggTilNyPeriode(periode, nesteTilstand)
    }

    private fun leggTilNyPeriode(periode: Tidslinjeperiode, nesteTilstand: Byggetilstand? = null) {
        val index = nåværendeGenerasjon.indexOfFirst { other -> periode.erSammeVedtaksperiode(other) }
        if (index >= 0) nåværendeGenerasjon[index] = periode
        else nåværendeGenerasjon.add(periode)
        nesteTilstand?.also { this.tilstand = nesteTilstand }
    }

    private interface Byggetilstand {

        fun uberegnetPeriode(generasjoner: Generasjoner, periode: UberegnetPeriode) {
            generasjoner.leggTilNyPeriode(periode)
        }
        fun uberegnetVilkårsprøvdPeriode(generasjoner: Generasjoner, periode: UberegnetVilkårsprøvdPeriode) {
            generasjoner.leggTilNyPeriode(periode)
        }
        fun utbetaltPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
            generasjoner.leggTilNyPeriode(periode, AktivGenerasjon(periode))
        }
        fun annullertPeriode(generasjoner: Generasjoner, periode: AnnullertPeriode) {
            generasjoner.leggTilNyRadOgPeriode(periode.somBeregnetPeriode(), AnnullertGenerasjon)
        }
        fun revurdertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
            generasjoner.leggTilNyRadOgPeriode(periode, RevurdertGenerasjon(periode))
        }

        object Initiell : Byggetilstand {
            override fun revurdertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) =
                error("forventet ikke en revurdert periode i tilstand ${this::class.simpleName}!")
        }

        class AktivGenerasjon(private val forrigeBeregnet: BeregnetPeriode) : Byggetilstand {
            override fun utbetaltPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
                // en tidligere utbetalt periode vil bety at en tidligere uberegnet periode er omgjort/eller er out of order
                if (periode < forrigeBeregnet) return generasjoner.leggTilNyPeriode(periode, EndringITidligerePeriodeGenerasjon(periode))
                generasjoner.leggTilNyPeriode(periode, AktivGenerasjon(periode))
            }

            override fun uberegnetVilkårsprøvdPeriode(generasjoner: Generasjoner, periode: UberegnetVilkårsprøvdPeriode) {
                if (periode > forrigeBeregnet) return generasjoner.leggTilNyPeriode(periode, AktivGenerasjon(forrigeBeregnet))
                generasjoner.leggTilNyRadOgPeriode(periode, UberegnetVilkårsprøvdPeriodeGenerasjon())
            }
        }

        class UberegnetVilkårsprøvdPeriodeGenerasjon() : Byggetilstand {
            override fun revurdertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
                generasjoner.leggTilNyPeriode(periode, RevurdertGenerasjon(periode))
            }
        }

        class EndringITidligerePeriodeGenerasjon(private val outOfOrderPeriode: BeregnetPeriode) : Byggetilstand {
            override fun revurdertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
                val perioder = generasjoner.nåværendeGenerasjon.filter { it >= outOfOrderPeriode && it < periode }
                generasjoner.nåværendeGenerasjon.removeAll(perioder)
                generasjoner.leggTilNyRad()
                perioder.forEach { generasjoner.leggTilNyPeriode(it) }
                generasjoner.leggTilNyPeriode(periode, RevurdertGenerasjon(periode))
            }
        }

        object AnnullertGenerasjon : Byggetilstand {
            override fun annullertPeriode(generasjoner: Generasjoner, periode: AnnullertPeriode) {
                generasjoner.leggTilNyPeriode(periode.somBeregnetPeriode())
            }
        }

        class RevurdertGenerasjon(private val revurderingen: BeregnetPeriode) : Byggetilstand {
            override fun revurdertPeriode(generasjoner: Generasjoner, periode: BeregnetPeriode) {
                if (periode.ingenEndringerMellom(revurderingen)) return generasjoner.leggTilNyPeriode(periode, RevurdertGenerasjon(periode))
                generasjoner.leggTilNyRadOgPeriode(periode, RevurdertGenerasjon(periode))
            }
            override fun uberegnetVilkårsprøvdPeriode(generasjoner: Generasjoner, periode: UberegnetVilkårsprøvdPeriode) {
                if (periode > revurderingen) return generasjoner.leggTilNyPeriode(periode, RevurdertGenerasjon(revurderingen))
                generasjoner.leggTilNyRadOgPeriode(periode, UberegnetVilkårsprøvdPeriodeGenerasjon())
            }
        }
    }
}
