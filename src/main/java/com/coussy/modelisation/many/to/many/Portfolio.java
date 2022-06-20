package com.coussy.modelisation.many.to.many;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity(name = "portfolio")
@Table(name = "portfolio_portfolio", indexes = {
		@Index(name = "portfolio_uniqueness", columnList = "customer_code, reference_month, code", unique = true),
		@Index(name = "portfolio_foreignkey_aifm_uuid", columnList = "aifm_uuid", unique = false) })
public class Portfolio {

	@Id
	@GeneratedValue
	private UUID uuid;

	@ManyToMany
	@JoinTable(name = "portfolio_stress_test_scenario_many2many", inverseJoinColumns = {
			@JoinColumn(name = "stress_test_scenario_uuid") }, joinColumns = {
					@JoinColumn(name = "portfolio_uuid", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, foreignKeyDefinition = "FOREIGN KEY (portfolio_uuid) REFERENCES portfolio_portfolio ON DELETE CASCADE")) }, uniqueConstraints = {
							@UniqueConstraint(columnNames = { "portfolio_uuid", "stress_test_scenario_uuid" }) })
	@MapKey(name = "scenarioId")
	private Map<String, StressTestScenario> stressTestScenarios;

	public Map<String, StressTestScenario> getStressTestScenarios() {
		if (stressTestScenarios == null) {
			stressTestScenarios = new HashMap<>();
		}
		return stressTestScenarios;
	}

	public void setStressTestScenarios(final Map<String, StressTestScenario> stressTestScenarios) {
		this.stressTestScenarios = stressTestScenarios;
	}

}
