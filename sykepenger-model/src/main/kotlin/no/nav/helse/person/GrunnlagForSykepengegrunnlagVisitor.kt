package no.nav.helse.person

import no.nav.helse.person.Inntekthistorikk.Inntektsendring
import no.nav.helse.person.Inntekthistorikk.Inntektsendring.Kilde.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class GrunnlagForSykepengegrunnlagVisitor(private val dato: LocalDate) : InntekthistorikkVisitor {
    private var tilstand: Tilstand = Tilstand.Start
    private val inntekter = mutableListOf<Inntektsendring>()

    internal fun sykepengegrunnlag() = tilstand.sykepengegrunnlag(this)

    private fun addInntekt(inntektsendring: Inntektsendring) {
        inntekter.add(inntektsendring)
    }

    private fun clear() {
        inntekter.clear()
    }

    private fun tilstand(tilstand: Tilstand) {
        this.tilstand = tilstand
    }

    private fun tilstand(tilstand: Tilstand, inntektsendring: Inntektsendring) {
        clear()
        addInntekt(inntektsendring)
        this.tilstand = tilstand
    }

    override fun visitInntekt(
        inntektsendring: Inntektsendring,
        id: UUID,
        kilde: Inntektsendring.Kilde,
        fom: LocalDate
    ) {
        if (fom != dato) return
        if (kilde == INFOTRYGD) return tilstand.addInfotrygdinntekt(this, inntektsendring)
        if (kilde == INNTEKTSMELDING) return tilstand.addInntektsmeldinginntekt(this, inntektsendring)
    }

    override fun visitInntektSkatt(
        inntektsendring: Inntektsendring.Skatt,
        id: UUID,
        kilde: Inntektsendring.Kilde,
        fom: LocalDate
    ) {
        if (YearMonth.from(fom) !in YearMonth.from(dato).let { it.minusMonths(3)..it.minusMonths(1) }) return
        if (kilde == SKATT_SYKEPENGEGRUNNLAG) return tilstand.addSkatteinntekt(this, inntektsendring)
    }

    override fun visitInntektSaksbehandler(
        inntektsendring: Inntektsendring.Saksbehandler,
        id: UUID,
        kilde: Inntektsendring.Kilde,
        fom: LocalDate
    ) {
        if (fom != dato) return
        tilstand.addSaksbehandlerinntekt(this, inntektsendring)
    }

    private sealed class Tilstand {
        open fun sykepengegrunnlag(visitor: GrunnlagForSykepengegrunnlagVisitor): Inntekt = INGEN

        open fun addSkatteinntekt(
            visitor: GrunnlagForSykepengegrunnlagVisitor,
            inntektsendring: Inntektsendring.Skatt
        ) {
        }

        open fun addInfotrygdinntekt(
            visitor: GrunnlagForSykepengegrunnlagVisitor,
            inntektsendring: Inntektsendring
        ) {
        }

        open fun addInntektsmeldinginntekt(
            visitor: GrunnlagForSykepengegrunnlagVisitor,
            inntektsendring: Inntektsendring
        ) {
        }

        open fun addSaksbehandlerinntekt(
            visitor: GrunnlagForSykepengegrunnlagVisitor,
            inntektsendring: Inntektsendring
        ) {
        }

        object Start : Tilstand() {
            override fun addSkatteinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring.Skatt
            ) {
                visitor.addInntekt(inntektsendring)
                visitor.tilstand(SkatteinntektTilstand)
            }

            override fun addInfotrygdinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.addInntekt(inntektsendring)
                visitor.tilstand(InfotrygdinntektTilstand)
            }

            override fun addInntektsmeldinginntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.addInntekt(inntektsendring)
                visitor.tilstand(InntektsmeldinginntektTilstand)
            }

            override fun addSaksbehandlerinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.addInntekt(inntektsendring)
                visitor.tilstand(SaksbehandlerinntektTilstand)
            }
        }

        object SkatteinntektTilstand : Tilstand() {
            override fun sykepengegrunnlag(visitor: GrunnlagForSykepengegrunnlagVisitor) =
                visitor.inntekter.map { it.inntekt() }.summer() / 3

            override fun addSkatteinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring.Skatt
            ) {
                if (inntektsendring.isBefore(visitor.inntekter.last())) return
                if (inntektsendring.isAfter(visitor.inntekter.last())) visitor.clear()
                visitor.addInntekt(inntektsendring)
            }

            override fun addInfotrygdinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.tilstand(InfotrygdinntektTilstand, inntektsendring)
            }

            override fun addInntektsmeldinginntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.tilstand(InntektsmeldinginntektTilstand, inntektsendring)
            }

            override fun addSaksbehandlerinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.tilstand(SaksbehandlerinntektTilstand, inntektsendring)
            }
        }

        object InfotrygdinntektTilstand : Tilstand() {
            override fun sykepengegrunnlag(visitor: GrunnlagForSykepengegrunnlagVisitor) =
                visitor.inntekter.last().inntekt()

            override fun addInfotrygdinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.addInntekt(inntektsendring)
            }

            override fun addInntektsmeldinginntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.tilstand(InntektsmeldinginntektTilstand, inntektsendring)
            }

            override fun addSaksbehandlerinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.tilstand(SaksbehandlerinntektTilstand, inntektsendring)
            }
        }

        object InntektsmeldinginntektTilstand : Tilstand() {
            override fun sykepengegrunnlag(visitor: GrunnlagForSykepengegrunnlagVisitor) =
                visitor.inntekter.last().inntekt()

            override fun addInntektsmeldinginntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.addInntekt(inntektsendring)
            }

            override fun addSaksbehandlerinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.tilstand(SaksbehandlerinntektTilstand, inntektsendring)
            }
        }

        object SaksbehandlerinntektTilstand : Tilstand() {
            override fun addSaksbehandlerinntekt(
                visitor: GrunnlagForSykepengegrunnlagVisitor,
                inntektsendring: Inntektsendring
            ) {
                visitor.addInntekt(inntektsendring)
            }
        }
    }
}
