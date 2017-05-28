package io.vieira.xtremebanking.loan.payment;

import io.vieira.xtremebanking.funds.FundsManager;
import io.vieira.xtremebanking.models.LoanRequest;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DefaultLoanPayer implements LoanPayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLoanPayer.class);
    private final FundsManager fundsManager;
    private final Double baseRate;
    private final Double specialRate;
    private final Double insuranceFee;
    private final Double rateMultiplier;

    public DefaultLoanPayer(FundsManager fundsManager,
                            @Value("${xtreme-banking.base-rate:0.15}") Double baseRate,
                            @Value("${xtreme-banking.special-rate:0.2}") Double specialRate,
                            @Value("${xtreme-banking.insurance-fee:250}") Double insuranceFee) {
        this.fundsManager = fundsManager;
        this.baseRate = baseRate;
        this.specialRate = specialRate;
        this.insuranceFee = insuranceFee;
        this.rateMultiplier = 1 + this.baseRate;
    }

    @Override
    public Publisher<Double> staggerPaymentForDayAndRequest(int yearNumber, LoanRequest request) {
        Publisher<Double> toReturn;

        switch(yearNumber) {
            case 1:
                toReturn = Mono
                        .just(request.getOffer())
                        .delayElement(yearDuration)
                        .map(offer -> offer * rateMultiplier)
                        .doOnNext(loanRevenue -> LOGGER.info("Buyer '{}' just won {} with his successful loan.", request.getBuyer(), loanRevenue));
                break;
            case 2:
                toReturn = Mono.just(request.getOffer())
                        .repeat(monthsInAYear)
                        .delayElements(monthDuration)
                        .map(offer -> offer * (this.baseRate / monthsInAYear));
                break;
            case 3:
            case 4:
            case 5:
                toReturn = Mono.just(request.getOffer())
                        .repeat(daysInAYear)
                        .delayElements(dayDuration)
                        .map(offer -> offer * ((yearNumber == 5 ? this.baseRate + this.specialRate : this.baseRate) / daysInAYear))
                        .doOnNext(amountToAdd -> {
                            if(yearNumber == 4 && !this.fundsManager.hasEnoughFunds(request.getBuyer(), 100000)) {
                                // Fine him.
                                // TODO : variabilize the amount of the fine ?
                                LOGGER.warn("Buyer '{}' is not being honest with the regulations, and has been fined.", request.getBuyer());
                                this.fundsManager.spend(request.getBuyer(), 5000);
                            }
                        });
                break;
            case 6:
                toReturn = Mono.just(request.getOffer())
                        .repeat(monthsInAYear)
                        .delayElements(monthDuration)
                        .map(offer -> offer * (this.baseRate / monthsInAYear))
                        // Insurance
                        .doOnNext(amountToAdd -> {
                            this.fundsManager.spend(request.getBuyer(), this.insuranceFee);
                            LOGGER.info("Buyer '{}' has paid his monthly insurance of {} for his loan.", request.getBuyer(), this.insuranceFee);
                        });
                break;
            default:
                toReturn = Mono.empty();
                break;
        }

        return toReturn;
    }
}
