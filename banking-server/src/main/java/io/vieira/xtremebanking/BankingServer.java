package io.vieira.xtremebanking;

import io.vieira.xtremebanking.funds.InMemoryFundsManager;
import io.vieira.xtremebanking.loan.DefaultLoansBuffer;
import io.vieira.xtremebanking.time.TimeConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan(basePackageClasses = {
        TimeConfiguration.class,
        DefaultLoansBuffer.class,
        LoanController.class,
        InMemoryFundsManager.class
})
public class BankingServer {

    public static void main(String[] args) {
        new SpringApplication(BankingServer.class).run(args);
    }
}
