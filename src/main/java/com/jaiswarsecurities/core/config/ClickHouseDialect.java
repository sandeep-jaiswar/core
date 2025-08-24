package com.jaiswarsecurities.core.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.function.CommonFunctionFactory;

/**
 * Custom Hibernate Dialect for ClickHouse
 */
public class ClickHouseDialect extends MySQLDialect {

	public ClickHouseDialect() {
		super();
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		// Register functions commonly used in ClickHouse
		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.concat();
		functionFactory.coalesce();
		functionFactory.currentUtcdatetimetimestamp();
		functionFactory.yearMonthDay();
		functionFactory.hourMinuteSecond();

		functionContributions.getFunctionRegistry().registerPattern("toDate", "toDate(?1)");

		functionContributions.getFunctionRegistry().registerPattern("today", "today()");

		functionContributions.getFunctionRegistry().registerPattern("now", "now()");
		// Example custom mapping
		functionContributions.getFunctionRegistry().registerPattern("toUnixTimestamp", "toUnixTimestamp(?1)");
	}
}
