package com.coussy.modelisation.many.to.many;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity(name = "stressTestScenario")
@Table(name = "portfolio_stress_test_scenario", indexes = {
  @Index(name = "stress_test_by_scenario", columnList = "scenario_id", unique = true)
})
public class StressTestScenario {

  @Id
  @GeneratedValue
  private UUID                             uuid;

}


