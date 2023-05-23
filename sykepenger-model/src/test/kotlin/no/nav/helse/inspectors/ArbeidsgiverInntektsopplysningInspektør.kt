package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningVisitor
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.IkkeRapportert
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import no.nav.helse.økonomi.Inntekt

internal val ArbeidsgiverInntektsopplysning.inspektør get() = ArbeidsgiverInntektsopplysningInspektør(this)

internal class ArbeidsgiverInntektsopplysningInspektør(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning) :
    ArbeidsgiverInntektsopplysningVisitor {
    internal lateinit var orgnummer: String
        private set
    internal lateinit var inntektsopplysning: Inntektsopplysning
        private set

    internal lateinit var refusjonsopplysninger: Refusjonsopplysninger
        private set

    private var tilstand: Tilstand = Tilstand.FangeInntekt

    init {
        arbeidsgiverInntektsopplysning.accept(this)
    }

    override fun preVisitArbeidsgiverInntektsopplysning(
        arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning,
        orgnummer: String
    ) {
        this.orgnummer = orgnummer
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
        this.tilstand.lagreInntekt(this, saksbehandler)
    }

    override fun preVisitSkjønnsmessigFastsatt(
        saksbehandler: SkjønnsmessigFastsatt,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        forklaring: String?,
        subsumsjon: Subsumsjon?,
        tidsstempel: LocalDateTime
    ) {
        this.tilstand.lagreInntekt(this, saksbehandler)
    }

    override fun visitInntektsmelding(
        inntektsmelding: Inntektsmelding,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.tilstand.lagreInntekt(this, inntektsmelding)
    }

    override fun visitIkkeRapportert(
        ikkeRapportert: IkkeRapportert,
        id: UUID,
        hendelseId: UUID,
        dato: LocalDate,
        tidsstempel: LocalDateTime
    ) {
        this.tilstand.lagreInntekt(this, IkkeRapportert(id, hendelseId, dato, tidsstempel))
    }

    override fun visitInfotrygd(
        infotrygd: Infotrygd,
        id: UUID,
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.tilstand.lagreInntekt(this, infotrygd)
    }

    override fun preVisitSkattSykepengegrunnlag(
        skattSykepengegrunnlag: SkattSykepengegrunnlag,
        id: UUID,
        hendelseId: UUID,
        dato: LocalDate,
        beløp: Inntekt,
        tidsstempel: LocalDateTime
    ) {
        this.tilstand.lagreInntekt(this, skattSykepengegrunnlag)
    }

    override fun preVisitRefusjonsopplysninger(refusjonsopplysninger: Refusjonsopplysninger) {
        this.refusjonsopplysninger = refusjonsopplysninger
    }

    private sealed interface Tilstand {
        fun lagreInntekt(inspektør: ArbeidsgiverInntektsopplysningInspektør, inntektsopplysning: Inntektsopplysning)
        object FangeInntekt : Tilstand {
            override fun lagreInntekt(inspektør: ArbeidsgiverInntektsopplysningInspektør, inntektsopplysning: Inntektsopplysning) {
                inspektør.inntektsopplysning = inntektsopplysning
                inspektør.tilstand = HarFangetInntekt
            }
        }
        object HarFangetInntekt : Tilstand {
            override fun lagreInntekt(
                inspektør: ArbeidsgiverInntektsopplysningInspektør,
                inntektsopplysning: Inntektsopplysning
            ) {}
        }
    }
}