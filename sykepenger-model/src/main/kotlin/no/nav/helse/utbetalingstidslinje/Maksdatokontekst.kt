package no.nav.helse.utbetalingstidslinje

import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.plus
import no.nav.helse.ukedager

internal data class Maksdatokontekst(
    val regler: MaksimumSykepengedagerregler,
    val sekstisyvårsdagen: LocalDate,
    val vurdertTilOgMed: LocalDate,
    val startdatoSykepengerettighet: LocalDate,
    val startdatoTreårsvindu: LocalDate,
    val betalteDager: Set<LocalDate>,
    val oppholdsdager: Set<LocalDate>,
    val avslåtteDager: Set<LocalDate>,
    val begrunnelser: Map<LocalDate, Begrunnelse>
) {
    companion object {
        fun tomKontekst(regler: MaksimumSykepengedagerregler, sekstisyvårsdagen: LocalDate) =
            Maksdatokontekst(
                regler = regler,
                sekstisyvårsdagen = sekstisyvårsdagen,
                vurdertTilOgMed = LocalDate.MIN,
                startdatoSykepengerettighet = LocalDate.MIN,
                startdatoTreårsvindu = LocalDate.MIN,
                betalteDager = emptySet(),
                oppholdsdager = emptySet(),
                avslåtteDager = emptySet(),
                begrunnelser = emptyMap()
            )
    }

    internal val oppholdsteller = oppholdsdager.size
    internal val betalteDagerOver67 = betalteDager.filter { it > sekstisyvårsdagen }.toSet()

    internal val forbrukteDager = betalteDager.size
    internal val forbrukteDagerOver67 = betalteDagerOver67.size

    internal val gjenståendeDagerUnder67År = regler.maksSykepengedager() - forbrukteDager
    internal val gjenståendeDagerOver67År = regler.maksSykepengedagerOver67() - forbrukteDagerOver67

    internal val erDagerUnder67ÅrForbrukte = gjenståendeDagerUnder67År == 0
    internal val erDagerOver67ÅrForbrukte = gjenståendeDagerOver67År == 0

    internal fun harNåddMaks(vedtaksperiode: Periode) =
        avslåtteDager.any { it in vedtaksperiode }

    internal fun datoForTilstrekkeligOppholdOppnådd(tilstrekkeligOpphold: Int) =
        oppholdsdager.sorted().getOrNull(tilstrekkeligOpphold - 1)

    internal fun fremdelesSykEtterTilstrekkeligOpphold(vedtaksperiode: Periode, tilstrekkeligOpphold: Int): Boolean {
        val datoForTilstrekkeligOpphold = datoForTilstrekkeligOppholdOppnådd(tilstrekkeligOpphold) ?: return false
        return avslåtteDager.any { it > datoForTilstrekkeligOpphold && it in vedtaksperiode }
    }

    internal fun avgrensTil(vurderingTilOgMed: LocalDate): Maksdatokontekst {
        /** etter at vi har telt ferdig så skal hver vedtaksperiode få et maksdatoresultat som tar utgangspunkt i
         *  siste dag i sin periode. enten så "spoler vi tilbake" saken, eller så vyer vi fremover.
         */
        if (vurdertTilOgMed == vurderingTilOgMed) return this // 😌
        if (vurdertTilOgMed > vurderingTilOgMed) return spolTilbake(vurderingTilOgMed)
        return vyFremover(vurderingTilOgMed) // 😏
    }

    private fun spolTilbake(vurderingTilOgMed: LocalDate) = copy(
        vurdertTilOgMed = vurderingTilOgMed,
        betalteDager = betalteDager.filter { it <= vurderingTilOgMed }.toSet(),
        oppholdsdager = oppholdsdager.filter { it <= vurderingTilOgMed }.toSet(),
        avslåtteDager = avslåtteDager.filter { it <= vurderingTilOgMed }.toSet(),
        begrunnelser = begrunnelser.filterKeys { it <= vurderingTilOgMed }
    )

    private fun vyFremover(vurderingTilOgMed: LocalDate) = copy(
        vurdertTilOgMed = vurderingTilOgMed
    )

    internal fun tilbakestill() = copy(
        vurdertTilOgMed = LocalDate.MIN,
        startdatoSykepengerettighet = LocalDate.MIN,
        startdatoTreårsvindu = LocalDate.MIN,
        betalteDager = emptySet(),
        oppholdsdager = emptySet(),
        avslåtteDager = emptySet(),
        begrunnelser = emptyMap()
    )

    internal fun nyMaksdatosak(dagen: LocalDate, startdatoTreårsvindu: LocalDate): Maksdatokontekst {
        return this
            .copy(startdatoSykepengerettighet = dagen)
            .medNyStartdatoTreårsvindu(startdatoTreårsvindu)
            .inkrementer(dagen)
    }

    internal fun inkrementer(dato: LocalDate) = copy(
        vurdertTilOgMed = dato,
        betalteDager = betalteDager.plus(dato),
        oppholdsdager = emptySet()
    )

    internal fun dekrementer(dato: LocalDate, nyStartdatoTreårsvindu: LocalDate) =
        this
            .medNyStartdatoTreårsvindu(nyStartdatoTreårsvindu)
            .inkrementer(dato)

    // tilgir forbrukte dager som følge av at treårsvinduet forskyves
    private fun medNyStartdatoTreårsvindu(nyStartdatoTreårsvindu: LocalDate) = copy(
        startdatoTreårsvindu = nyStartdatoTreårsvindu,
        betalteDager = betalteDager.filter { it >= nyStartdatoTreårsvindu }.toSet()
    )

    internal fun medOppholdsdag(dato: LocalDate) = copy(
        vurdertTilOgMed = dato,
        oppholdsdager = oppholdsdager + dato
    )

    internal fun medAvslåttDag(dato: LocalDate, begrunnelse: Begrunnelse) = copy(
        vurdertTilOgMed = dato,
        avslåtteDager = this.avslåtteDager + dato,
        oppholdsdager = oppholdsdager + dato,
        begrunnelser = this.begrunnelser.plus(dato to begrunnelse)
    )

    internal fun beregnMaksdato(syttiårsdagen: LocalDate, dødsdato: LocalDate?): Maksdatoresultat {
        fun LocalDate.forrigeVirkedagFør() = minusDays(
            when (dayOfWeek) {
                SUNDAY -> 2
                MONDAY -> 3
                else -> 1
            }
        )

        fun LocalDate.sisteVirkedagInklusiv() = when (dayOfWeek) {
            SATURDAY -> minusDays(1)
            SUNDAY -> minusDays(2)
            else -> this
        }

        val harNåddMaks = gjenståendeDagerOver67År == 0 || gjenståendeDagerUnder67År == 0
        val forrigeMaksdato = if (harNåddMaks) betalteDager.last() else null
        val forrigeVirkedag = forrigeMaksdato ?: vurdertTilOgMed.sisteVirkedagInklusiv()

        val maksdatoOrdinærRett = forrigeVirkedag + gjenståendeDagerUnder67År.ukedager
        val maksdatoBegrensetRett = maxOf(forrigeVirkedag, sekstisyvårsdagen.sisteVirkedagInklusiv()) + gjenståendeDagerOver67År.ukedager

        val hjemmelsbegrunnelse: Maksdatoresultat.Bestemmelse
        val maksdato: LocalDate
        val gjenståendeDager: Int
        // maksdato er den dagen som først inntreffer blant ordinær kvote, 67-års-kvoten og 70-årsdagen,
        // med mindre man allerede har brukt opp alt tidligere
        when {
            maksdatoOrdinærRett <= maksdatoBegrensetRett -> {
                maksdato = listOfNotNull(maksdatoOrdinærRett, dødsdato).min()
                gjenståendeDager = ukedager(forrigeVirkedag, maksdato)
                hjemmelsbegrunnelse = Maksdatoresultat.Bestemmelse.ORDINÆR_RETT
            }

            maksdatoBegrensetRett <= syttiårsdagen.forrigeVirkedagFør() -> {
                maksdato = listOfNotNull(maksdatoBegrensetRett, dødsdato).min()
                gjenståendeDager = ukedager(forrigeVirkedag, maksdato)
                hjemmelsbegrunnelse = Maksdatoresultat.Bestemmelse.BEGRENSET_RETT
            }

            else -> {
                maksdato = syttiårsdagen.forrigeVirkedagFør()
                gjenståendeDager = ukedager(forrigeVirkedag, maksdato)
                hjemmelsbegrunnelse = Maksdatoresultat.Bestemmelse.SYTTI_ÅR
            }
        }

        return Maksdatoresultat(
            vurdertTilOgMed = vurdertTilOgMed,
            bestemmelse = hjemmelsbegrunnelse,
            startdatoTreårsvindu = startdatoTreårsvindu,
            startdatoSykepengerettighet = startdatoSykepengerettighet.takeUnless { it == LocalDate.MIN },
            forbrukteDager = betalteDager.grupperSammenhengendePerioder(),
            oppholdsdager = oppholdsdager.grupperSammenhengendePerioder(),
            avslåtteDager = avslåtteDager.grupperSammenhengendePerioder(),
            maksdato = maksdato,
            gjenståendeDager = gjenståendeDager
        )
    }
}
