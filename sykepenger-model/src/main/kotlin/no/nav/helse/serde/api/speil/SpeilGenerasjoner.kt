package no.nav.helse.serde.api.speil

import java.util.UUID
import no.nav.helse.serde.api.dto.AnnullertPeriode
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.SpeilGenerasjonDTO
import no.nav.helse.serde.api.dto.SpeilTidslinjeperiode
import no.nav.helse.serde.api.dto.SpeilTidslinjeperiode.Companion.utledPeriodetyper
import no.nav.helse.serde.api.dto.UberegnetPeriode
import no.nav.helse.serde.api.dto.UberegnetVilkårsprøvdPeriode

class SpeilGenerasjoner {
    private var kildeTilGenerasjon: UUID? = null
    private val nåværendeGenerasjon = mutableListOf<SpeilTidslinjeperiode>()
    private val generasjoner = mutableListOf<SpeilGenerasjonDTO>()
    private var tilstand: Byggetilstand = Byggetilstand.Initiell

    internal fun build(): List<SpeilGenerasjonDTO> {
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

    private fun byggGenerasjon(periodene: List<SpeilTidslinjeperiode>) {
        if (periodene.isEmpty()) return
        generasjoner.add(0, SpeilGenerasjonDTO(UUID.randomUUID(), periodene.utledPeriodetyper(), kildeTilGenerasjon!!))
    }

    private fun leggTilNyRad(kilde: SpeilTidslinjeperiode) {
        byggGenerasjon(nåværendeGenerasjon.filterNot { it.venter() })
        kildeTilGenerasjon = kilde.kilde
    }

    private fun leggTilNyRadOgPeriode(periode: SpeilTidslinjeperiode, nesteTilstand: Byggetilstand) {
        leggTilNyRad(periode)
        leggTilNyPeriode(periode, nesteTilstand)
    }

    private fun leggTilNyPeriode(periode: SpeilTidslinjeperiode, nesteTilstand: Byggetilstand? = null) {
        if (kildeTilGenerasjon == null) kildeTilGenerasjon = periode.kilde
        val index = nåværendeGenerasjon.indexOfFirst { other -> periode.erSammeVedtaksperiode(other) }
        if (index >= 0) nåværendeGenerasjon[index] = periode
        else nåværendeGenerasjon.add(periode)
        nesteTilstand?.also { this.tilstand = nesteTilstand }
    }

    private interface Byggetilstand {

        fun uberegnetPeriode(generasjoner: SpeilGenerasjoner, periode: UberegnetPeriode) {
            generasjoner.leggTilNyPeriode(periode, AktivGenerasjon(periode))
        }
        fun uberegnetVilkårsprøvdPeriode(generasjoner: SpeilGenerasjoner, periode: UberegnetVilkårsprøvdPeriode) {
            generasjoner.leggTilNyPeriode(periode, AktivGenerasjon(periode))
        }
        fun utbetaltPeriode(generasjoner: SpeilGenerasjoner, periode: BeregnetPeriode) {
            generasjoner.leggTilNyPeriode(periode, AktivGenerasjon(periode))
        }
        fun annullertPeriode(generasjoner: SpeilGenerasjoner, periode: AnnullertPeriode) {
            generasjoner.leggTilNyRadOgPeriode(periode.somBeregnetPeriode(), AnnullertGenerasjon)
        }
        fun revurdertPeriode(generasjoner: SpeilGenerasjoner, periode: BeregnetPeriode) {
            generasjoner.leggTilNyRadOgPeriode(periode, RevurdertGenerasjon(periode))
        }

        object Initiell : Byggetilstand {
            override fun revurdertPeriode(generasjoner: SpeilGenerasjoner, periode: BeregnetPeriode) =
                error("forventet ikke en revurdert periode i tilstand ${this::class.simpleName}!")
        }

        class AktivGenerasjon(private val forrigeBeregnet: SpeilTidslinjeperiode) : Byggetilstand {
            override fun utbetaltPeriode(generasjoner: SpeilGenerasjoner, periode: BeregnetPeriode) {
                // en tidligere utbetalt periode vil bety at en tidligere uberegnet periode er omgjort/eller er out of order
                if (periode < forrigeBeregnet) return generasjoner.leggTilNyRadOgPeriode(periode, EndringITidligerePeriodeGenerasjon())
                generasjoner.leggTilNyPeriode(periode, AktivGenerasjon(periode))
            }

            override fun uberegnetPeriode(generasjoner: SpeilGenerasjoner, periode: UberegnetPeriode) {
                if (periode > forrigeBeregnet) return generasjoner.leggTilNyPeriode(periode)
                generasjoner.leggTilNyRadOgPeriode(periode, EndringITidligerePeriodeGenerasjon())
            }

            override fun uberegnetVilkårsprøvdPeriode(generasjoner: SpeilGenerasjoner, periode: UberegnetVilkårsprøvdPeriode) {
                if (periode > forrigeBeregnet) return generasjoner.leggTilNyPeriode(periode)
                generasjoner.leggTilNyRadOgPeriode(periode, EndringITidligerePeriodeGenerasjon())
            }
        }

        // dersom det kommer en endring på en tidligere periode så er det garantert å komme revurderinger etterpå;
        // derfor lages det ikke en ny rad ved første revurdering
        class EndringITidligerePeriodeGenerasjon() : Byggetilstand {
            override fun revurdertPeriode(generasjoner: SpeilGenerasjoner, periode: BeregnetPeriode) {
                generasjoner.leggTilNyPeriode(periode, RevurdertGenerasjon(periode))
            }
        }

        object AnnullertGenerasjon : Byggetilstand {
            override fun annullertPeriode(generasjoner: SpeilGenerasjoner, periode: AnnullertPeriode) {
                generasjoner.leggTilNyPeriode(periode.somBeregnetPeriode())
            }
        }

        class RevurdertGenerasjon(private val revurderingen: BeregnetPeriode) : Byggetilstand {
            override fun revurdertPeriode(generasjoner: SpeilGenerasjoner, periode: BeregnetPeriode) {
                if (periode.ingenEndringerMellom(revurderingen)) return generasjoner.leggTilNyPeriode(periode, RevurdertGenerasjon(periode))
                generasjoner.leggTilNyRadOgPeriode(periode, RevurdertGenerasjon(periode))
            }

            override fun utbetaltPeriode(generasjoner: SpeilGenerasjoner, periode: BeregnetPeriode) {
                if (periode > revurderingen) return generasjoner.leggTilNyPeriode(periode, RevurdertGenerasjon(revurderingen))
                generasjoner.leggTilNyRadOgPeriode(periode, AktivGenerasjon(periode))
            }
            override fun uberegnetVilkårsprøvdPeriode(generasjoner: SpeilGenerasjoner, periode: UberegnetVilkårsprøvdPeriode) {
                if (periode > revurderingen) return generasjoner.leggTilNyPeriode(periode, RevurdertGenerasjon(revurderingen))
                generasjoner.leggTilNyRadOgPeriode(periode, EndringITidligerePeriodeGenerasjon())
            }

            override fun uberegnetPeriode(generasjoner: SpeilGenerasjoner, periode: UberegnetPeriode) {
                if (periode > revurderingen) return generasjoner.leggTilNyPeriode(periode, RevurdertGenerasjon(revurderingen))
                generasjoner.leggTilNyRadOgPeriode(periode, EndringITidligerePeriodeGenerasjon())
            }
        }
    }
}
