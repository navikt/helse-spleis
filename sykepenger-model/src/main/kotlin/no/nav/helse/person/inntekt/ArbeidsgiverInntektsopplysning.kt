package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.*
import no.nav.helse.dto.deserialisering.ArbeidsgiverInntektsopplysningInnDto
import no.nav.helse.dto.serialisering.ArbeidsgiverInntektsopplysningUtDto
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-15`
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger.KorrigertArbeidsgiverInntektsopplysning
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.person.ArbeidstakerOpptjening
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.builders.UtkastTilVedtakBuilder
import no.nav.helse.person.inntekt.Skatteopplysning.Companion.subsumsjonsformat
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

internal data class ArbeidsgiverInntektsopplysning(
    val orgnummer: String,
    val faktaavklartInntekt: ArbeidstakerFaktaavklartInntekt,
    val korrigertInntekt: Saksbehandler?,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsatt?
) {
    val omregnetÅrsinntekt = korrigertInntekt?.inntektsdata ?: faktaavklartInntekt.inntektsdata
    private val fastsattÅrsinntektInntektsdata = skjønnsmessigFastsatt?.inntektsdata ?: omregnetÅrsinntekt
    val fastsattÅrsinntekt = fastsattÅrsinntektInntektsdata.beløp

    internal fun gjelder(organisasjonsnummer: String) = organisasjonsnummer == orgnummer

    private fun håndterArbeidstakerFaktaavklartInntekt(organisasjonsnummer: String, førsteFraværsdag: LocalDate, skjæringstidspunkt: LocalDate, arbeidstakerFaktaavklartInntekt: ArbeidstakerFaktaavklartInntekt): Utfall {
        check(arbeidstakerFaktaavklartInntekt.inntektsopplysningskilde is Arbeidstakerinntektskilde.Arbeidsgiver) { "Hva holder du på med? Du skal ikke sende inntekter med kilde ${arbeidstakerFaktaavklartInntekt.inntektsopplysningskilde::class.simpleName} hit!" }

        if (this.orgnummer != organisasjonsnummer) return Utfall.Uendret(this)

        // Samme inntekt vi allerede har lagt til grunn
        if (this.faktaavklartInntekt.id == arbeidstakerFaktaavklartInntekt.id) return Utfall.Uendret(this)

        val liggerSkattTilGrunnNå = this.faktaavklartInntekt.inntektsopplysningskilde is Arbeidstakerinntektskilde.AOrdningen

        // Om arbeidsgiver er syk i annen måned enn skjæringstidspunktet og det ligger skatt til grunn skal vi beholde skatt.
        if (førsteFraværsdag.yearMonth != skjæringstidspunkt.yearMonth && liggerSkattTilGrunnNå) return Utfall.Uendret(this)

        // Samme beløp som vi allerede har, men nå går fra å ha lagt skatt til grunn til å ha lagt arbeidsgivers inntekt til grunn
        if (omregnetÅrsinntekt.beløp == arbeidstakerFaktaavklartInntekt.inntektsdata.beløp && liggerSkattTilGrunnNå) return Utfall.EndretKilde(copy(
            faktaavklartInntekt = arbeidstakerFaktaavklartInntekt,
            korrigertInntekt = null
        ))

        // En ny arbeidstakerFaktaavklartInntekt som sier det samme som før
        if (omregnetÅrsinntekt.beløp == arbeidstakerFaktaavklartInntekt.inntektsdata.beløp) return Utfall.Uendret(this)

        return Utfall.EndretBeløp(copy(
            faktaavklartInntekt = arbeidstakerFaktaavklartInntekt,
            korrigertInntekt = null,
            skjønnsmessigFastsatt = null
        ))
    }

    private fun håndterKorrigerteInntekter(korrigerteInntekter: List<KorrigertArbeidsgiverInntektsopplysning>): Utfall {
        val korrigertInntekt = korrigerteInntekter.singleOrNull { it.organisasjonsnummer == this.orgnummer }?.korrigertInntekt ?: return Utfall.Uendret(this)
        // bare sett inn ny inntekt hvis beløp er ulikt (speil sender inntekt- og refusjonoverstyring i samme melding)
        if (korrigertInntekt.inntektsdata.beløp == omregnetÅrsinntekt.beløp) return Utfall.Uendret(this)
        return Utfall.EndretBeløp(copy(
            korrigertInntekt = korrigertInntekt,
            skjønnsmessigFastsatt = null
        ))
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
        val inntekterSisteTreMåneder = when (faktaavklartInntekt.inntektsopplysningskilde) {
            is Arbeidstakerinntektskilde.AOrdningen -> faktaavklartInntekt.inntektsopplysningskilde.inntektsopplysninger
                Arbeidstakerinntektskilde.Arbeidsgiver,
                Arbeidstakerinntektskilde.Infotrygd -> emptyList()
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
        internal fun List<ArbeidsgiverInntektsopplysning>.rullTilbakeEventuellSkjønnsmessigFastsettelse() =
            map { it.copy(skjønnsmessigFastsatt = null) }

        internal fun List<ArbeidsgiverInntektsopplysning>.validerSkjønnsmessigAltEllerIntet() {
            check(all { it.skjønnsmessigFastsatt == null } || all { it.skjønnsmessigFastsatt != null }) { "Enten så må alle inntektsopplysninger var skjønnsmessig fastsatt, eller så må ingen være det" }
        }

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

        internal fun List<ArbeidsgiverInntektsopplysning>.håndterArbeidstakerFaktaavklartInntekt(
            organisasjonsnummer: String,
            førsteFraværsdag: LocalDate,
            skjæringstidspunkt: LocalDate,
            arbeidstakerFaktaavklartInntekt: ArbeidstakerFaktaavklartInntekt
        ) = this.map { arbeidsgiverInntektsopplysning -> arbeidsgiverInntektsopplysning.håndterArbeidstakerFaktaavklartInntekt(
            organisasjonsnummer = organisasjonsnummer,
            førsteFraværsdag = førsteFraværsdag,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidstakerFaktaavklartInntekt = arbeidstakerFaktaavklartInntekt
        ) }


        internal fun List<ArbeidsgiverInntektsopplysning>.håndterKorrigerteInntekter(
            korrigerteInntekter: List<KorrigertArbeidsgiverInntektsopplysning>
        ) = this.map { arbeidsgiverInntektsopplysning -> arbeidsgiverInntektsopplysning.håndterKorrigerteInntekter(korrigerteInntekter) }

        internal fun List<ArbeidsgiverInntektsopplysning>.skjønnsfastsett(other: List<SkjønnsmessigFastsettelse.SkjønnsfastsattInntekt>): List<ArbeidsgiverInntektsopplysning> {
            check(this.size == other.size) { "alle inntektene må skjønnsfastsettes" }
            return this.map { inntekt -> inntekt.skjønnsfastsett(other) }
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.vurderArbeidsgivere(
            aktivitetslogg: IAktivitetslogg,
            opptjening: ArbeidstakerOpptjening,
            orgnummer: String
        ) {
            if (!harInntekt(orgnummer)) return aktivitetslogg.varsel(Varselkode.TilkommenInntekt.`Søknad fra arbeidsgiver som ikke er i sykepengegrunnlaget`)
            vurderSkifteAvArbeidsgiver(aktivitetslogg, opptjening, orgnummer)
        }

        // Denne funksjonen skjønner jeg ingenting av, ikke spør meg om den, hilsen Maxi
        private fun List<ArbeidsgiverInntektsopplysning>.vurderSkifteAvArbeidsgiver(
            aktivitetslogg: IAktivitetslogg,
            opptjening: ArbeidstakerOpptjening,
            orgnummer: String
        ) {
            val arbeidsforholdAktivePåSkjæringstidspunktet = singleOrNull { opptjening.ansattVedSkjæringstidspunkt(it.orgnummer) } ?: return
            if (arbeidsforholdAktivePåSkjæringstidspunktet.orgnummer == orgnummer) return
            aktivitetslogg.varsel(Varselkode.RV_VV_8)
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.måHaRegistrertOpptjeningForArbeidsgivere(
            aktivitetslogg: IAktivitetslogg,
            opptjening: ArbeidstakerOpptjening
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
                        if (arbeidsgiver.skjønnsmessigFastsatt != null || arbeidsgiver.korrigertInntekt != null) {
                            Inntektskilde.Saksbehandler
                        } else when (arbeidsgiver.faktaavklartInntekt.inntektsopplysningskilde) {
                            is Arbeidstakerinntektskilde.AOrdningen -> Inntektskilde.AOrdningen
                            Arbeidstakerinntektskilde.Arbeidsgiver -> Inntektskilde.Arbeidsgiver
                            Arbeidstakerinntektskilde.Infotrygd -> Inntektskilde.Arbeidsgiver
                        }
                )
            }

        internal fun List<ArbeidsgiverInntektsopplysning>.harInntekt(organisasjonsnummer: String) =
            singleOrNull { it.orgnummer == organisasjonsnummer } != null

        internal fun List<ArbeidsgiverInntektsopplysning>.fastsattÅrsinntekt() =
            fold(INGEN) { acc, item -> acc + item.fastsattÅrsinntekt }

        internal fun List<ArbeidsgiverInntektsopplysning>.totalOmregnetÅrsinntekt() =
            fold(INGEN) { acc, item -> acc + item.omregnetÅrsinntekt.beløp }

        internal fun gjenopprett(dto: ArbeidsgiverInntektsopplysningInnDto): ArbeidsgiverInntektsopplysning {
            return ArbeidsgiverInntektsopplysning(
                orgnummer = dto.orgnummer,
                faktaavklartInntekt = ArbeidstakerFaktaavklartInntekt.gjenopprett(dto.faktaavklartInntekt),
                korrigertInntekt = dto.korrigertInntekt?.let { Saksbehandler.gjenopprett(it) },
                skjønnsmessigFastsatt = dto.skjønnsmessigFastsatt?.let { SkjønnsmessigFastsatt.gjenopprett(it) }
            )
        }
    }

    internal fun dto() = ArbeidsgiverInntektsopplysningUtDto(
        orgnummer = this.orgnummer,
        faktaavklartInntekt = this.faktaavklartInntekt.dto(),
        korrigertInntekt = this.korrigertInntekt?.dto(),
        skjønnsmessigFastsatt = skjønnsmessigFastsatt?.dto()
    )

    internal sealed interface Utfall {
        val arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning
        data class Uendret(override val arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning): Utfall
        data class EndretBeløp(override val arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning): Utfall
        data class EndretKilde(override val arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning): Utfall
    }
}

