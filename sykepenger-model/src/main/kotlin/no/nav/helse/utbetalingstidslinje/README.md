#Creating Utbetalingstidslinjer (Payment time lines) and Utbetalingslinjer (Payment lines)

The creation of an array of **Utbetalingslinjer** (payment lines) is a multistep
process involving both validation and morphing of information. The challenge is even
greater because some of the validation and morphing occurs at the Person level, and
some at the Arbeidsgiver (company) level.

This design guide applies to multiple Arbeidsgivere; initial implementations will be
scaled back to a single Arbeidsgiver.

The Utbetalingslinje itself is a structure representing a payment amount and a range
of dates  over which the payment applies. This range should covers weekends (which are
ignored by subsequent NAV systems) if payment days exist on Fredag and Mandag around
the weekend.

And finally, we are only interested in the Utbetalingslinjer for a particular
Vedtaksperiode. This means that we can constrain our calculations to dates before
the end date of the Vedtaksperiode, even if we must consider other Vedtaksperioder
and other Arbeidsgivere.

##Steps

###0. All Information Collected

This process starts when we have received prior utbetalinger from the legacy
system. These are all the HistorikUtbetalinger associated with the *Person*, not
just those associated with a given Arbeidsgiver.

At this point, we have also built a Sykdomstidslinje for the Vedtaksperiode,
and may or may not have other Vedtaksperioder in various states of progress.

Additionally, we may or may not have other Arbeidsgiverer with their own
Vedtaksperioder, with each of these in various states.

###1. Creation of Utbetalingstidslinje for Each Arbeidsgiver

Using the various HistorikUtbetalinger, we need to calculate a Utbetalingstidslinje
for each Arbeidsgivere:

* Consolidate the Sykdomstidslinjer for each Vedtaksperiode in time order. *Plus*
is already defined on Sykdomstidslinje for this purpose. Note that these
Sykdomstidslinje will never overlap.
* Determine if there is a need to allocated some of the Sykdag as paid by the
Arbeidsgiver. This is done by finding the closest prior HistorikUtbetalinger,
and determining is a sufficient gap (generally 16 dager) to require the Arbeidsgiver
must pay for the sickness.
* Generate the Utbetalingstidslinje from the Sykdomstidslinje, taking into account
whether the Arbeidsgiver has already covered their utbetaling obligation or not. A
specific UtbetalingBuilder walks through each dag in the Sykdomstidslinje,
assisted by its internal State Machine.

So while we are interested in payments for a single Arbeidsgiver, we need the
Utbetalingstidslinje for all the other Arbeidsgivere for the subsequent steps.
After we have manipulated all these Utbetalingstidslinjer, we will push each
one onto a stack of Utbetalingstidslinjer kept for each Arbeidsgiver.

###2. Application of Sickness Grade

NAV payment will not be made if the overall *Sickness Grade* is less than 20%.
The overall sickness grade is calculated as the weighted average (by inntekt) of
the degree of sickness for each Arbeidsgiver.

In order to calculate the overall sickness grade, the Utbetalingstidslinje for each
Arbeidsgiver (with their corresponding sickness grade and inntekt) must be consolidated.
For any NavDag (NAV paymnent days) where the overall sickeness grade falls
below 20% needs to be marked as not payable. Each NavDag for each Utbetalingstidslinje
of the Arbeidsgiver is replaced with a AvvistDag to mark non-payment.

We are now ready for the next filtering.

###3. Filtering on Income Level

The next filtering is for insufficient income level. Across all sources of income,
no sickness payments are made unless total income is 1/2G (or 2G for persons 67
or older). The Alder class can be interrogated to determine which limit applies
for any given day.

Inntekt is calculated from monthly inntekt from all sources (employers, self-employed,
unemployment, parental leave, etc.), converted to an annual inntekt, and then
checked against this minimum limit. For any days below minimum income, a NavDag
for that day in any Utbetalingstidslinje is changed to a AvvistDag.

###4. Validating Sickness Day Limits

Depending on the Arbeidsgiver and the age of the applicant, there are limits to
the number of sickness days we will pay.

By law, sickness benefits run out after certain limits or events. These limits are
captured in the UtbetalingTeller class which also uses the age of the claimant (
Alder class). The following summarizes the rules:

* Benefits are not paid on the 70th birthday or later
* Only 60 days of benefits are paid after the 67th birthday
* Otherwise maximum benefits depend upon the type of employment and whether NAV
insurance has been purchased. This is captured in ArbeidsgiverRegler (EmployerRules)
  * 248 days are typical with arbeidsgivere, and is the only ArbeidsgiverRegler currently
  implemented
  * When conflicts exist with which ArbeidsgiverRegler to use (amongst different
  inntekt sources), a primary ArbeidsgiverRegler is selected for limit analysis.

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
a *visitor* (GoF Visitor Pattern) across each specific dager of Utbetalingstidslinje,
allowing it to assess each dag in turn, counting each possible limit. The result of
this analysis is another specific set of AvvistDager identifying days that were originally
marked for payment by NAV, but because of the limits, should not be paid.

The identified AvvistDager are then merged back into each Utbetalingstidslinje for
each Arbeidsgiver. Now we are ready to allocate payments.

###5. Allocation of Payments

With the revised Utbetalingstidslinje for the Arbeidsgiver from the previous step,
we need to check for the 6G (maximum allowed daily payment) limit. Several factors
are at play here:

* The 6G limit is adjusted by the overall sickness grade. The full 6G payment is
only awarded for 100% sick.
* The revised 6G limit is then to be distributed across all arbeidsgivere:
  * Maximum repayment is the lesser of the usual inntekt for that day, or revised
  6G limit.
  * Only arbeidsgivere who paid employees directly on a particular day are entitled
  for repayment.
  * Repayment to a particular arbeidsgiver is limited to the payment they made that
  day to the sick employee.
  * When multiple arbeidsgivere are entitled to rebates, the rebate is proportioned
  by daily inntekt for each arbeidsgiver.
  * Any remaining portion of the partial 6G limit is given directly to the claimant.
* Any reductions in the daily utbetaling are reflected in an updated Utbetalingstidslinje
for that Arbeidsgiver.

We now have the final, calculated Utbetalingstidslinje for each Arbeidsgiver. These
revised Utbetalingstidslinje are pushed onto the stack of prior Utbetalingstidslinje
for the Arbeidsgiver. A future Epic addresses the behavior when revised payments are
indicated for prior Vedtaksperioder.

###6. Generation of Utbetalingslinjer (Payment Lines)

Using just the subset of the last Utbetalingstidslinje for the Arbeidsgiver of the
relevant Vedtaksperiode, another visitor (GoF Visitor Pattern) is spawned and walks
the dager of the Utbetalingstidslinje -- a UtbetalingslinjeBuilder class. Using yet
another state machine (GoF State Pattern), utbetalingslinjer are generated across
the period, including imbedded weekends as appropriate.

This completes the process.

One further future refinement will be necessary at some point: *Identification of
which part of the payment should go to the arbeidsgiver, and which part to the
claimant. Probably two sets of Utbetalingslinjer will need to be generated.*






