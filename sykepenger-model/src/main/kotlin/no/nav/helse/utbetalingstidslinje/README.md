#Creating Utbetalingstidslinjer (Payment time lines) and Utbetalingslinjer (Payment lines)

The creation of an array of  **Utbetalingslinje** (utbetalingslinjer, or payment
lines) is a multistep
process involving both validation and morphing of information. The challenge is even
greater because some of the validation and morphing occurs at the Sak (or more properly
Person) level, and some at the Arbeidsgiver (company) level.

The Utbetalingslinje itself is a structure representing a payment amount and a range
of dates  over which the payment applies. This range should covers weekends (which are
ignored by subsequent NAV systems) if payment days exist on Fredag and Mandag around
the weekend.

And finally, we are only interested in the Utbetalingslinjer for a particular
Vedtaksperiode. This means that we can constrain our calculations to dates before
the end date of the Vedtaksperiode, even if we must consider other Vedtaksperioder
and other Arbeidsgivere.

##Steps

###Validation of Sufficient Inntekt (income)

Across all sources of income, no sickness payments are made unless total income is
1/2G (or 2G) for persons 67 or older. AlderRegler (AgeRules) can be interrogated to
determine which limit applies.

Inntekt is calculated from monthly income from all sources (employers, self-employed,
unemployment, parental leave, etc.), converted to an annual
inntekt, and then checked against this minimum limit.

Hence, these calculations must be performed at the Sak (or Person) level.

###Creating Utbetalingstidslinje for each Arbeidsgiver

For each Arbeidsgiver, a Utbetalingstidslinje is created. It is a day-by-day mapping
of the Sykdomstidslinje for all the Vedataksperioder for each Arbeidsgiver.

In order to run all the calculations needed at both the Sak (Person) and
Arbeidsgiver (Company) levels, the first step is to create a combined Sykdomstidslinje
at the Sak level. So a SakSykdomstidslinje is created, containing a new
ArbeidsgiverSykdomstidslinje for each Arbeidsgiver, which in turn contains
the CompositeSykdomstidslinje for each Vedtaksperiode. We then have a massive
spanning tree of all the Sykdomstidslinjer for the Person.

For each ArbeidsgiverSykdomstidslinje, a Utbetalingstidslinje is generated. This
is a day-by-day mapping, and is performed by the UtbetalingBuilder. Using a state
machine (GoF State Pattern), the UtbetalingBuilder tracks employer periods and
NAV payment obligations. There are rules for how this is done, and these rules
are captured in the ArbeidsgiverRegler class. These rules involve the number of
days an employer must pay, gap periods that reset this count, etc.

Additionally, the UtbetalingBuilder can be seeded with
the number of employer payment days already incurred. This should prove useful
when absorbing legacy payment information. For example, if analysis of the
legacy sickness payments indicates a payment within 16 days of the start of our
ArbeidsgiverSykdomstidslinje, we can assume the employer period requirements have
already been met.

At this point, we now have a valid Utbetalingstidslinje for each Arbeidsgiver. We
make one more change to each: We truncate each Utbelatlingstidslinje at the
period end date. Calculation of anything past this date is not useful.

###Payment Limits

By law, sickness benefits run out after certain limits or events. These limits are
captured in the AlderRegler class which understands the birthdate of the claimant.

* Benefits are not paid on the 70th birthday or later
* Only 60 days of benefits are paid after the 67th birthday
* Otherwise maximum benefits depend upon the type of employment and whether NAV
insurance has been purchased. This is captured in ArbeidsgiverRegler (EmployerRules)
  * 248 days are typical with arbeidsgivere, and is the only ArbeidsgiverRegler currently
  implemented

With the Utbetalingstidslinje already calculated for each Arbeidsgiver, we must merge
these Utbetalingstidslinje for limit analysis. *(This is not implemented yet, and is
not required until support for multiple Arbeitsgivere is needed. But day-by-day
merging and conflict resolution will be required. For example, if one Arbeidsgiver
shows a payment day, and another shows a feriedag, we assume feriedag.)*

The class Utbetalingsavgrenser is responsible for the analysis of payment limits. A
necessary side effect of this calculation is understanding how many sick days have
been paid, and calculation of the *maksdato*, the last day a claimant can receive
sickness benefits if she or he continues to be sick. These calculations are valid
since we have clipped the Utbetalingstidslinje at the last date of the Vedtaksperiode
of interest.

Utbetalingsavgrenser uses its own state machine (another GoF State Pattern) to track
payment limits, and reset payment limits when sufficient time has passed without a
claim (26 weeks, currently). Total paid days and total paid days after age 67 are both
tracked, ensuring that all payment limits are respected. A Utbetalingstidslinje is
a *visitor* (GoF Visitor Pattern) across the specific dager of Utbetalingstidslinje,
allowing it to assess each dag in turn, counting each possible limit. The result of
this analysis is a specific set of AvvistDager identifying days that were originally
marked for payment by NAV, but because of the limits, should not be paid.

The identified AvvistDager are then merged back into just the Arbeidsgiver for the
Vedtaksperiode that should be paid.

###6G Limitations

With the revised Utbetalingstidslinje for the Arbeidsgiver, we need to check for the
6G (maximum allowed daily payment) limit. Several factors are at play here:

* The overall *sickness grade* needs to be calculated. This is a weighted average by
inntekts per abeidsgiver using the sickness grade of each Vedtaksperiode. *(100%
sickness is currently assumed; changes in that assumption go in this step.)*
* If the sickness grade is less than 20% on a particular day, no sickness payment
will be made for that day.
* The 6G limit is adjusted by the overall sickness grade. 6G is only awarded for 100%
sick.
* The revised 6G limit is then to be distributed across all arbeidsgivere:
  * Maximum repayment is the lesser of the usual salary for that day, or revised 6G limit.
  * Only arbeidsgivere who paid employees directly on a particular day are entitled
  for repayment.
  * Repayment to a particular arbeidsgiver is limited to the payment they made that
  day to the sick employee.
  * When multiple arbeidsgivere are entitled to rebates, the rebate is proportioned
  by daily inntekt for each arbeidsgiver.
  * Any remaining portion of the partial 6G limit is given directly to the claimant.
* Any reductions in the daily utbetaling are reflected in an updated Utbetalingstidslinje
for that Arbeidsgiver.

###Restriction of Payment Period

Recall that we are only interested in paying for a particular Vedtaksperiode *although
reconciliation with prior payments, particularly directly to claimants, may be
needed in the future.* So the next step is to constrain the Utbetalingstidslinje to
just the Vedtaksperiode dates of interest. It is a simple subsetting operation on
the dager of the Utbetalingstidslinje.

###Generation of Utbetalingslinjer (Payment Lines)

Using just the subset of the Utbetalingstidslinje, another visitor (GoF Visitor Pattern)
is spawned and walks the dagen of the Utbetalingstidslinje -- a UtbetalingslinjeBuilder
class. Using yet another state machine (GoF State Pattern), utbetalingslinjer are
generated across the period, including imbedded weekends as appropriate.

This complete the process.

One further future refinement will be necessary: *Identification of which part of the
payment should go to the arbeidsgiver, and which part to the claimant. Probably two
sets of Utbetalingslinjer will need to be generated.*






