package no.nav.helse.person

import no.nav.helse.person.InntektshistorikkVol2.Inntektsopplysning
import no.nav.helse.person.InntektshistorikkVol2.Inntektsopplysning.Kilde.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class GrunnlagForSykepengegrunnlagVisitor(private val dato: LocalDate) : InntekthistorikkVisitor {
    private var tilstand: Tilstand = Tilstand.Start
    private val inntekter = mutableListOf<Inntektsopplysning>()

    internal fun sykepengegrunnlag() = tilstand.sykepengegrunnlag(this)

    private fun addInntekt(inntektsopplysning: Inntektsopplysning) {
        inntekter.add(inntektsopplysning)
    }

    private fun clear() {
        inntekter.clear()
    }

    private fun tilstand(tilstand: Tilstand) {
        this.tilstand = tilstand
    }

    private fun tilstand(tilstand: Tilstand, inntektsopplysning: Inntektsopplysning) {
        clear()
        addInntekt(inntektsopplysning)
        this.tilstand = tilstand
    }

    override fun visitInntektVol2(
        inntektsopplysning: Inntektsopplysning,
        id: UUID,
        kilde: Inntektsopplysning.Kilde,
        fom: LocalDate,
        tidsstempel: LocalDateTime
    ) {
        if (fom != dato) return
        if (kilde == INFOTRYGD) return tilstand.addInfotrygdinntekt(this, inntektsopplysning)
        if (kilde == INNTEKTSMELDING) return tilstand.addInntektsmeldinginntekt(this, inntektsopplysning)
    }

    override fun visitInntektSkattVol2(
        inntektsopplysning: Inntektsopplysning.Skatt,
        id: UUID,
        kilde: Inntektsopplysning.Kilde,
        fom: LocalDate,
        tidsstempel: LocalDateTime
    ) {
        if (YearMonth.from(fom) !in YearMonth.from(dato).let { it.minusMonths(3)..it.minusMonths(1) }) return
        if (kilde == SKATT_SYKEPENGEGRUNNLAG) return tilstand.addSkatteinntekt(this, inntektsopplysning)
    }

    override fun visitInntektSaksbehandlerVol2(
        inntektsopplysning: Inntektsopplysning.Saksbehandler,
        id: UUID,
        kilde: Inntektsopplysning.Kilde,
        fom: LocalDate,
        tidsstempel: LocalDateTime
    ) {
        if (fom != dato) return
        tilstand.addSaksbehandlerinntekt(this, inntektsopplysning)
    }

    private sealed class Tilstand {
        open fun sykepengegrunnlag(visitor: GrunnlagForSykepengegrunnlagVisitor): Inntekt = INGEN

        open fun addSkatteinntekt(
            visitor: GrunnlagForSykepengegrunnlagVisitor,
            inntektsopplysning: Inntektsopplysning.Skatt
        ) {
        }

        open fun addInfotrygdinntekt(
            visitor: GrunnlagForSykepengegrunnlagVisitor,
            inntektsopplysning: Inntektsopplysning
        ) {
        }

        open fun addInntektsmeldinginntekt(
            visitor: GrunnlagForSykepengegrunnlagVisitor,
            inntektsopplysning: Inntektsopplysning
        ) {
        }

        open fun addSaksbehandlerinntekt(
            visitor: GrunnlagForSykepengegrunnlagVisitor,
            inntektsopplysning: Inntektsopplysning
        ) {
        }

        object Start : Tilstand() {
            override fun addSkatteinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning.Skatt
            ) {
                visitor.addInntekt(inntektsopplysning)
                visitor.tilstand(SkatteinntektTilstand)
            }

            override fun addInfotrygdinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.addInntekt(inntektsopplysning)
                visitor.tilstand(InfotrygdinntektTilstand)
            }

            override fun addInntektsmeldinginntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.addInntekt(inntektsopplysning)
                visitor.tilstand(InntektsmeldinginntektTilstand)
            }

            override fun addSaksbehandlerinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.addInntekt(inntektsopplysning)
                visitor.tilstand(SaksbehandlerinntektTilstand)
            }
        }

        object SkatteinntektTilstand : Tilstand() {
            override fun sykepengegrunnlag(visitor: GrunnlagForSykepengegrunnlagVisitor) =
                visitor.inntekter.map { it.inntekt() }.summer() / 3

            override fun addSkatteinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning.Skatt
            ) {
                if (inntektsopplysning.isBefore(visitor.inntekter.last())) return
                if (inntektsopplysning.isAfter(visitor.inntekter.last())) visitor.clear()
                visitor.addInntekt(inntektsopplysning)
            }

            override fun addInfotrygdinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.tilstand(InfotrygdinntektTilstand, inntektsopplysning)
            }

            override fun addInntektsmeldinginntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.tilstand(InntektsmeldinginntektTilstand, inntektsopplysning)
            }

            override fun addSaksbehandlerinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.tilstand(SaksbehandlerinntektTilstand, inntektsopplysning)
            }
        }

        object InfotrygdinntektTilstand : Tilstand() {
            override fun sykepengegrunnlag(visitor: GrunnlagForSykepengegrunnlagVisitor) =
                visitor.inntekter.last().inntekt()

            override fun addInfotrygdinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.addInntekt(inntektsopplysning)
            }

            override fun addInntektsmeldinginntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.tilstand(InntektsmeldinginntektTilstand, inntektsopplysning)
            }

            override fun addSaksbehandlerinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.tilstand(SaksbehandlerinntektTilstand, inntektsopplysning)
            }
        }

        object InntektsmeldinginntektTilstand : Tilstand() {
            override fun sykepengegrunnlag(visitor: GrunnlagForSykepengegrunnlagVisitor) =
                visitor.inntekter.last().inntekt()

            override fun addInntektsmeldinginntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.addInntekt(inntektsopplysning)
            }

            override fun addSaksbehandlerinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.tilstand(SaksbehandlerinntektTilstand, inntektsopplysning)
            }
        }

        object SaksbehandlerinntektTilstand : Tilstand() {
            override fun addSaksbehandlerinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsopplysning: Inntektsopplysning
            ) {
                visitor.addInntekt(inntektsopplysning)
            }
        }
    }
}
