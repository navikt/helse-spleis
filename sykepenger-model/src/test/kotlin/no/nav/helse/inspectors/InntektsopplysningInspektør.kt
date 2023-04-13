package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.inntekt.InntektsopplysningVisitor
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

internal val Inntektsopplysning.inspektør get() = InntektsopplysningInspektør(this)

internal class InntektsopplysningInspektør(inntektsopplysning: Inntektsopplysning) : InntektsopplysningVisitor {

    internal lateinit var beløp: Inntekt
        private set
    internal lateinit var hendelseId: UUID
        private set
    internal lateinit var tidsstempel: LocalDateTime
        private set

    private var tilstand: Tilstand = Tilstand.FangeInntekt

    init {
        inntektsopplysning.accept(this)
    }

    override fun preVisitSaksbehandler(
        saksbehandler: Saksbehandler,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        forklaring: String?,
        subsumsjon: Subsumsjon?,
        tidsstempel: LocalDateTime
    ) {
        this.tilstand.lagreInntekt(this, beløp, hendelseId, tidsstempel)
    }

    override fun visitInntektsmelding(
        inntektsmelding: Inntektsmelding,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.tilstand.lagreInntekt(this, beløp, hendelseId, tidsstempel)
    }

    override fun visitIkkeRapportert(id: UUID, hendelseId: UUID, dato: LocalDate, tidsstempel: LocalDateTime) {
        this.tilstand.lagreInntekt(this, INGEN, hendelseId, tidsstempel)
    }

    override fun visitInfotrygd(
        infotrygd: Infotrygd,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.tilstand.lagreInntekt(this, beløp, hendelseId, tidsstempel)
    }

    override fun preVisitSkattSykepengegrunnlag(
        skattSykepengegrunnlag: SkattSykepengegrunnlag,
        id: UUID,
        hendelseId: UUID,
        dato: LocalDate,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.tilstand.lagreInntekt(this, beløp, hendelseId, tidsstempel)
    }

    private sealed interface Tilstand {
        fun lagreInntekt(inspektør: InntektsopplysningInspektør, beløp: Inntekt, hendelseId: UUID, tidsstempel: LocalDateTime)
        object FangeInntekt : Tilstand {
            override fun lagreInntekt(inspektør: InntektsopplysningInspektør, beløp: Inntekt, hendelseId: UUID, tidsstempel: LocalDateTime) {
                inspektør.beløp = beløp
                inspektør.hendelseId = hendelseId
                inspektør.tidsstempel = tidsstempel
                inspektør.tilstand = HarFangetInntekt
            }
        }
        object HarFangetInntekt : Tilstand {
            override fun lagreInntekt(
                inspektør: InntektsopplysningInspektør,
                beløp: Inntekt,
                hendelseId: UUID,
                tidsstempel: LocalDateTime
            ) {}
        }
    }
}