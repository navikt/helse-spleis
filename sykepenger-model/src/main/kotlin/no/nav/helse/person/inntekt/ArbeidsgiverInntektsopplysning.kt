package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.dto.deserialisering.ArbeidsgiverInntektsopplysningInnDto
import no.nav.helse.dto.serialisering.ArbeidsgiverInntektsopplysningUtDto
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-15`
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger.KorrigertArbeidsgiverInntektsopplysning
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Opptjening
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.builders.UtkastTilVedtakBuilder
import no.nav.helse.person.inntekt.Skatteopplysning.Companion.subsumsjonsformat
import no.nav.helse.utbetalingstidslinje.VilkårsprøvdSkjæringstidspunkt
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

internal data class ArbeidsgiverInntektsopplysning(
    val orgnummer: String,
    val gjelder: Periode,
    val faktaavklartInntekt: FaktaavklartInntekt,
    val korrigertInntekt: Saksbehandler?,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsatt?
) {
    val omregnetÅrsinntekt = korrigertInntekt?.inntektsdata ?: faktaavklartInntekt.inntektsdata
    private val fastsattÅrsinntektInntektsdata = skjønnsmessigFastsatt?.inntektsdata ?: omregnetÅrsinntekt
    val fastsattÅrsinntekt = fastsattÅrsinntektInntektsdata.beløp

    private fun gjelderPåSkjæringstidspunktet(skjæringstidspunkt: LocalDate) =
        skjæringstidspunkt == gjelder.start

    private fun fastsattÅrsinntekt(acc: Inntekt, skjæringstidspunkt: LocalDate): Inntekt {
        return acc + beregningsgrunnlag(skjæringstidspunkt)
    }
    private fun omregnetÅrsinntekt(acc: Inntekt, skjæringstidspunkt: LocalDate): Inntekt {
        return acc + omregnetÅrsinntekt(skjæringstidspunkt)
    }

    private fun beregningsgrunnlag(skjæringstidspunkt: LocalDate): Inntekt {
        if (!gjelderPåSkjæringstidspunktet(skjæringstidspunkt)) return INGEN
        return fastsattÅrsinntekt
    }

    private fun omregnetÅrsinntekt(skjæringstidspunkt: LocalDate): Inntekt {
        if (!gjelderPåSkjæringstidspunktet(skjæringstidspunkt)) return INGEN
        return omregnetÅrsinntekt.beløp
    }

    internal fun gjelder(organisasjonsnummer: String) = organisasjonsnummer == orgnummer

    private fun overstyrMedInntektsmelding(organisasjonsnummer: String, nyInntekt: FaktaavklartInntekt): ArbeidsgiverInntektsopplysning {
        if (this.orgnummer != organisasjonsnummer) return this
        if (nyInntekt.inntektsdata.dato.yearMonth != this.omregnetÅrsinntekt.dato.yearMonth) return this
        return copy(
            faktaavklartInntekt = nyInntekt,
            korrigertInntekt = null
        )
    }

    private fun overstyrMedSaksbehandler(overstyringer: List<KorrigertArbeidsgiverInntektsopplysning>): ArbeidsgiverInntektsopplysning {
        val korrigering = overstyringer.singleOrNull { it.organisasjonsnummer == this.orgnummer } ?: return this
        // bare sett inn ny inntekt hvis beløp er ulikt (speil sender inntekt- og refusjonoverstyring i samme melding)
        if (korrigering.inntektsdata.beløp == omregnetÅrsinntekt.beløp && this.gjelder == korrigering.gjelder) return this
        return copy(
            gjelder = korrigering.gjelder,
            korrigertInntekt = Saksbehandler(
                id = UUID.randomUUID(),
                inntektsdata = korrigering.inntektsdata
            ),
            skjønnsmessigFastsatt = null
        )
    }

    private fun skjønnsfastsett(fastsettelser: List<SkjønnsmessigFastsettelse.SkjønnsfastsattInntekt>): ArbeidsgiverInntektsopplysning {
        val fastsettelse = fastsettelser.single { it.orgnummer == this.orgnummer }
        return copy(
            skjønnsmessigFastsatt = SkjønnsmessigFastsatt(
                id = UUID.randomUUID(),
                inntektsdata = fastsettelse.inntektsdata
            )
        )
    }

    private fun deaktiver(
        forklaring: String,
        oppfylt: Boolean,
        subsumsjonslogg: Subsumsjonslogg
    ): ArbeidsgiverInntektsopplysning {
        val inntekterSisteTreMåneder = when (faktaavklartInntekt.inntektsopplysning) {
            is Inntektsopplysning.Arbeidstaker -> when (faktaavklartInntekt.inntektsopplysning.kilde) {
                is Arbeidstakerinntektskilde.AOrdningen -> faktaavklartInntekt.inntektsopplysning.kilde.inntektsopplysninger
                Arbeidstakerinntektskilde.Arbeidsgiver,
                Arbeidstakerinntektskilde.Infotrygd -> emptyList()
            }
        }
        subsumsjonslogg.logg(
            `§ 8-15`(
                skjæringstidspunkt = omregnetÅrsinntekt.dato,
                organisasjonsnummer = orgnummer,
                inntekterSisteTreMåneder = if (korrigertInntekt == null) inntekterSisteTreMåneder.subsumsjonsformat() else emptyList(),
                forklaring = forklaring,
                oppfylt = oppfylt
            )
        )

        return this
    }

    internal companion object {
        private fun List<ArbeidsgiverInntektsopplysning>.rullTilbakeEventuellSkjønnsmessigFastsettelse() =
            map { it.copy(skjønnsmessigFastsatt = null) }

        internal fun List<ArbeidsgiverInntektsopplysning>.faktaavklarteInntekter() = this
            .map { arbeidsgiverInntektsopplysning ->
                VilkårsprøvdSkjæringstidspunkt.FaktaavklartInntekt(
                    organisasjonsnummer = arbeidsgiverInntektsopplysning.orgnummer,
                    fastsattÅrsinntekt = arbeidsgiverInntektsopplysning.fastsattÅrsinntekt
                )
            }

        private fun ArbeidsgiverInntektsopplysning.kilde() = Kilde(
            avsender = if (korrigertInntekt != null || skjønnsmessigFastsatt != null) Avsender.SAKSBEHANDLER else Avsender.ARBEIDSGIVER,
            meldingsreferanseId = fastsattÅrsinntektInntektsdata.hendelseId,
            tidsstempel = fastsattÅrsinntektInntektsdata.tidsstempel
        )

        internal fun List<ArbeidsgiverInntektsopplysning>.beverte(builder: InntekterForBeregning.Builder) {
            forEach { arbeidsgiverInntektsopplysning ->
                builder.fraInntektsgrunnlag(arbeidsgiverInntektsopplysning.orgnummer, arbeidsgiverInntektsopplysning.fastsattÅrsinntekt, arbeidsgiverInntektsopplysning.kilde())
            }
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.beverteDeaktiverte(builder: InntekterForBeregning.Builder) {
            forEach { deaktivertArbeidsgiverInntektsopplysning ->
                builder.deaktivertFraInntektsgrunnlag(deaktivertArbeidsgiverInntektsopplysning.orgnummer, deaktivertArbeidsgiverInntektsopplysning.kilde())
            }
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.validerSkjønnsmessigAltEllerIntet() {
            check(all { it.skjønnsmessigFastsatt == null } || all { it.skjønnsmessigFastsatt != null }) { "Enten så må alle inntektsopplysninger var skjønnsmessig fastsatt, eller så må ingen være det" }
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.finn(orgnummer: String) =
            firstOrNull { it.gjelder(orgnummer) }

        internal fun List<ArbeidsgiverInntektsopplysning>.deaktiver(
            deaktiverte: List<ArbeidsgiverInntektsopplysning>,
            orgnummer: String,
            forklaring: String,
            subsumsjonslogg: Subsumsjonslogg
        ): Pair<List<ArbeidsgiverInntektsopplysning>, List<ArbeidsgiverInntektsopplysning>> {
            // Om inntektene i sykepengegrunnlaget var skjønnsmessig fastsatt før _deaktivering_ må vi først
            // rulle tilbake eventuell skjønnsmessig fastsettelse ettersom en skjønnsmessig fastsettelse blir gjort
            // for hele sykepengegrunnlaget, og når noe _deaktiveres_ kan ikke den skjønnsmessige fastsettelsen gjelde lenger
            // og det må eventuelt gjøres en ny skjønnsmessig fastsettelse gitt arbeidsgiverne som nå er aktive.
            return this.rullTilbakeEventuellSkjønnsmessigFastsettelse().fjernInntekt(deaktiverte.rullTilbakeEventuellSkjønnsmessigFastsettelse(), orgnummer, forklaring, true, subsumsjonslogg)
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.aktiver(
            aktiveres: List<ArbeidsgiverInntektsopplysning>,
            orgnummer: String,
            forklaring: String,
            subsumsjonslogg: Subsumsjonslogg
        ): Pair<List<ArbeidsgiverInntektsopplysning>, List<ArbeidsgiverInntektsopplysning>> {
            // Om inntektene i sykepengegrunnlaget var skjønnsmessig fastsatt før _aktivering_ må vi først
            // rulle tilbake eventuell skjønnsmessig fastsettelse ettersom en skjønnsmessig fastsettelse blir gjort
            // for hele sykepengegrunnlaget, og når noe _aktiveres_ kan ikke den skjønnsmessige fastsettelsen gjelde lenger
            // og det må eventuelt gjøres en ny skjønnsmessig fastsettelse gitt arbeidsgiverne som nå er aktive.
            return this.rullTilbakeEventuellSkjønnsmessigFastsettelse().fjernInntekt(aktiveres.rullTilbakeEventuellSkjønnsmessigFastsettelse(), orgnummer, forklaring, false, subsumsjonslogg)
        }

        // flytter inntekt for *orgnummer* fra *this* til *deaktiverte*
        // aktive.deaktiver(deaktiverte, orgnummer) er direkte motsetning til deaktiverte.deaktiver(aktive, orgnummer)
        private fun List<ArbeidsgiverInntektsopplysning>.fjernInntekt(
            deaktiverte: List<ArbeidsgiverInntektsopplysning>,
            orgnummer: String,
            forklaring: String,
            oppfylt: Boolean,
            subsumsjonslogg: Subsumsjonslogg
        ): Pair<List<ArbeidsgiverInntektsopplysning>, List<ArbeidsgiverInntektsopplysning>> {
            val inntektsopplysning = checkNotNull(this.singleOrNull { it.orgnummer == orgnummer }) {
                "Kan ikke overstyre arbeidsforhold for en arbeidsgiver vi ikke kjenner til"
            }.deaktiver(forklaring, oppfylt, subsumsjonslogg)
            val aktive = this.filterNot { it === inntektsopplysning }
            return aktive to (deaktiverte + listOfNotNull(inntektsopplysning))
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.overstyrMedInntektsmelding(
            organisasjonsnummer: String,
            nyInntekt: FaktaavklartInntekt
        ): List<ArbeidsgiverInntektsopplysning> {
            val endringen = this.map { inntekt -> inntekt.overstyrMedInntektsmelding(organisasjonsnummer, nyInntekt) }
            if (skalSkjønnsmessigFastsattRullesTilbake(endringen)) {
                return endringen.rullTilbakeEventuellSkjønnsmessigFastsettelse()
            }
            return endringen
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.overstyrMedSaksbehandler(
            other: List<KorrigertArbeidsgiverInntektsopplysning>
        ): List<ArbeidsgiverInntektsopplysning> {
            val endringen = this.map { inntekt -> inntekt.overstyrMedSaksbehandler(other) }
            if (skalSkjønnsmessigFastsattRullesTilbake(endringen)) {
                return endringen.rullTilbakeEventuellSkjønnsmessigFastsettelse()
            }
            return endringen
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.skjønnsfastsett(other: List<SkjønnsmessigFastsettelse.SkjønnsfastsattInntekt>): List<ArbeidsgiverInntektsopplysning> {
            check(this.size == other.size) { "alle inntektene må skjønnsfastsettes" }
            return this.map { inntekt -> inntekt.skjønnsfastsett(other) }
        }

        private fun List<ArbeidsgiverInntektsopplysning>.skalSkjønnsmessigFastsattRullesTilbake(etter: List<ArbeidsgiverInntektsopplysning>) =
            this.zip(etter) { gammelOpplysning, nyOpplysning ->
                gammelOpplysning.omregnetÅrsinntekt.beløp != nyOpplysning.omregnetÅrsinntekt.beløp
            }.any { it }

        internal fun List<ArbeidsgiverInntektsopplysning>.sjekkForNyArbeidsgiver(
            aktivitetslogg: IAktivitetslogg,
            opptjening: Opptjening,
            orgnummer: String
        ) {
            val arbeidsforholdAktivePåSkjæringstidspunktet =
                singleOrNull { opptjening.ansattVedSkjæringstidspunkt(it.orgnummer) } ?: return
            if (arbeidsforholdAktivePåSkjæringstidspunktet.orgnummer == orgnummer) return
            aktivitetslogg.varsel(Varselkode.RV_VV_8)
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.måHaRegistrertOpptjeningForArbeidsgivere(
            aktivitetslogg: IAktivitetslogg,
            opptjening: Opptjening
        ) {
            if (none { opptjening.startdatoFor(it.orgnummer) == null }) return
            aktivitetslogg.varsel(Varselkode.RV_VV_1)
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.berik(builder: UtkastTilVedtakBuilder) = this
            .forEach { arbeidsgiver ->
                builder.arbeidsgiverinntekt(
                    arbeidsgiver = arbeidsgiver.orgnummer,
                    omregnedeÅrsinntekt = arbeidsgiver.omregnetÅrsinntekt.beløp,
                    skjønnsfastsatt = arbeidsgiver.skjønnsmessigFastsatt?.inntektsdata?.beløp,
                    inntektskilde =
                        if (arbeidsgiver.skjønnsmessigFastsatt != null || arbeidsgiver.korrigertInntekt != null) Inntektskilde.Saksbehandler
                        else when (arbeidsgiver.faktaavklartInntekt.inntektsopplysning) {
                            is Inntektsopplysning.Arbeidstaker -> when (arbeidsgiver.faktaavklartInntekt.inntektsopplysning.kilde) {
                                is Arbeidstakerinntektskilde.AOrdningen -> Inntektskilde.AOrdningen
                                Arbeidstakerinntektskilde.Arbeidsgiver -> Inntektskilde.Arbeidsgiver
                                Arbeidstakerinntektskilde.Infotrygd -> Inntektskilde.Arbeidsgiver
                            }
                        }
                )
            }

        internal fun List<ArbeidsgiverInntektsopplysning>.harInntekt(organisasjonsnummer: String) =
            singleOrNull { it.orgnummer == organisasjonsnummer } != null

        internal fun List<ArbeidsgiverInntektsopplysning>.fastsattÅrsinntekt(skjæringstidspunkt: LocalDate) =
            fold(INGEN) { acc, item -> item.fastsattÅrsinntekt(acc, skjæringstidspunkt) }

        internal fun List<ArbeidsgiverInntektsopplysning>.totalOmregnetÅrsinntekt(skjæringstidspunkt: LocalDate) =
            fold(INGEN) { acc, item -> item.omregnetÅrsinntekt(acc, skjæringstidspunkt) }

        internal fun List<ArbeidsgiverInntektsopplysning>.finnEndringsdato(
            other: List<ArbeidsgiverInntektsopplysning>
        ): LocalDate? {
            val endringsDatoer = this.mapNotNull { ny ->
                val gammel = other.singleOrNull { it.orgnummer == ny.orgnummer }
                when {
                    gammel == null -> ny.gjelder.start
                    gammel.harFunksjonellEndring(ny) -> minOf(ny.gjelder.start, gammel.gjelder.start)
                    else -> null
                }
            }
            return endringsDatoer.minOrNull()
        }

        private fun ArbeidsgiverInntektsopplysning.harFunksjonellEndring(other: ArbeidsgiverInntektsopplysning): Boolean {
            if (this.gjelder != other.gjelder) return true
            if (this.skjønnsmessigFastsatt != other.skjønnsmessigFastsatt) return true
            if (!this.faktaavklartInntekt.funksjoneltLik(other.faktaavklartInntekt)) return true
            return this.korrigertInntekt != other.korrigertInntekt
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.harGjenbrukbarInntekt(organisasjonsnummer: String) =
            singleOrNull { it.orgnummer == organisasjonsnummer }
                ?.faktaavklartInntekt
                ?.inntektsopplysning?.let { it as? Inntektsopplysning.Arbeidstaker }
                ?.kilde is Arbeidstakerinntektskilde.Arbeidsgiver

        internal fun List<ArbeidsgiverInntektsopplysning>.lagreTidsnæreInntekter(
            skjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            aktivitetslogg: IAktivitetslogg,
            nyArbeidsgiverperiode: Boolean
        ) {
            this.forEach {
                val tidsnær = (it.faktaavklartInntekt.inntektsopplysning as? Inntektsopplysning.Arbeidstaker)?.kilde as? Arbeidstakerinntektskilde.Arbeidsgiver
                if (tidsnær != null) {
                    arbeidsgiver.lagreTidsnærInntektsmelding(
                        skjæringstidspunkt = skjæringstidspunkt,
                        orgnummer = it.orgnummer,
                        arbeidsgiverinntekt = FaktaavklartInntekt(
                            id = UUID.randomUUID(),
                            inntektsdata = it.faktaavklartInntekt.inntektsdata.copy(beløp = it.omregnetÅrsinntekt.beløp),
                            inntektsopplysning = Inntektsopplysning.Arbeidstaker(Arbeidstakerinntektskilde.Arbeidsgiver)
                        ),
                        aktivitetslogg = aktivitetslogg,
                        nyArbeidsgiverperiode = nyArbeidsgiverperiode
                    )
                }
            }
        }

        internal fun gjenopprett(dto: ArbeidsgiverInntektsopplysningInnDto): ArbeidsgiverInntektsopplysning {
            return ArbeidsgiverInntektsopplysning(
                orgnummer = dto.orgnummer,
                gjelder = Periode.gjenopprett(dto.gjelder),
                faktaavklartInntekt = FaktaavklartInntekt.gjenopprett(dto.faktaavklartInntekt),
                korrigertInntekt = dto.korrigertInntekt?.let { Saksbehandler.gjenopprett(it) },
                skjønnsmessigFastsatt = dto.skjønnsmessigFastsatt?.let { SkjønnsmessigFastsatt.gjenopprett(it) }
            )
        }
    }

    internal fun dto() = ArbeidsgiverInntektsopplysningUtDto(
        orgnummer = this.orgnummer,
        gjelder = this.gjelder.dto(),
        faktaavklartInntekt = this.faktaavklartInntekt.dto(),
        korrigertInntekt = this.korrigertInntekt?.dto(),
        skjønnsmessigFastsatt = skjønnsmessigFastsatt?.dto()
    )
}
