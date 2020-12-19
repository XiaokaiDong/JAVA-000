package io.kimmking.dubbo.demo.consumer;

import io.kimmking.entity.Account;
import io.kimmking.service.AccountService;
import io.kimmking.service.ExchangeDealService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@EnableDubbo
@SpringBootApplication
@MapperScan("src/main/java/io/kimmking/mapper")
public class DubboClientApplication {
	@DubboReference(version = "1.0.0", url = "dubbo://127.0.0.1:12345")
	private ExchangeDealService exchangeDealService;

	@DubboReference(version = "1.0.0", url = "dubbo://127.0.0.1:12345")
	private AccountService accountServiceOne;


	@DubboReference(version = "1.0.0", url = "dubbo://127.0.0.1:12346")
	private AccountService accountServiceTwo;

	@Autowired
	private Account accountUSDOne;

	@Autowired
	private Account accountCNYTwo;

	public static void main(String[] args) {

		SpringApplication.run(DubboClientApplication.class).close();

	}

	@Bean
	public ApplicationRunner runner() {
		return args -> {
			exchangeDealService.buyUSDWithCNY(accountUSDOne, accountCNYTwo, 2000);
		};
	}

	@Bean
	Account accountCNYOne(){
		Account accountCNYOne = Account.builder()
				.accountNum(1)
				.accountType("000")
				.amount(0)
				.currencyType("CNY")
				.build();
		accountServiceOne.openAccount(accountCNYOne);
		return accountCNYOne;
	}

	@Bean
	Account accountUSDOne(){
		Account accountUSDOne = Account.builder()
				.accountNum(2)
				.accountType("000")
				.amount(0)
				.currencyType("USD")
				.build();
		accountServiceOne.openAccount(accountUSDOne);
		return accountUSDOne;
	}

	@Bean
	Account accountCNYTwo(){
		Account accountCNYTwo = Account.builder()
				.accountNum(3)
				.accountType("000")
				.amount(0)
				.currencyType("CNY")
				.build();
		accountServiceTwo.openAccount(accountCNYTwo);
		return accountCNYTwo;
	}

	@Bean
	Account accountUSDTwo(){
		Account accountUSDTwo = Account.builder()
				.accountNum(4)
				.accountType("000")
				.amount(0)
				.currencyType("USD")
				.build();
		accountServiceTwo.openAccount(accountUSDTwo);
		return accountUSDTwo;
	}

	@Bean
	Account bglCleaningForCNYOne(){
		Account bglCleaningForCNYOne = Account.builder()
				.accountNum(5)
				.accountType("BGL")
				.amount(0)
				.currencyType("CNY")
				.build();
		accountServiceOne.openAccount(bglCleaningForCNYOne);
		return bglCleaningForCNYOne;
	}

	@Bean
	Account bglCleaningForCNYTwo(){
		Account bglCleaningForCNYTwo = Account.builder()
				.accountNum(6)
				.accountType("BGL")
				.amount(0)
				.currencyType("CNY")
				.build();
		accountServiceTwo.openAccount(bglCleaningForCNYTwo);
		return bglCleaningForCNYTwo;
	}

	@Bean
	Account bglCleaningForUSDOne() {
		Account bglCleaningForUSDOne = Account.builder()
				.accountNum(7)
				.accountType("BGL")
				.amount(0)
				.currencyType("USD")
				.build();
		accountServiceOne.openAccount(bglCleaningForUSDOne);
		return bglCleaningForUSDOne;
	}

	@Bean
	Account bglCleaningForUSDTwo() {
		Account bglCleaningForUSDTwo = Account.builder()
				.accountNum(8)
				.accountType("BGL")
				.amount(0)
				.currencyType("USD")
				.build();
		accountServiceTwo.openAccount(bglCleaningForUSDTwo);
		return bglCleaningForUSDTwo;
	}

}
