# Hypervisor Carbon Credits Scheduler

## Current System Scenario

The project currently implements a **carbon-aware VM migration simulation framework** using:

* Real UK National Grid 2023 carbon trace dataset
* Random Forest based scheduler model
* Carbon credit abstraction layer
* Trend-aware recommendation engine
* CloudSim simulation environment

The system already supports prediction of migration decisions:

* MIGRATE_NOW
* MIGRATE_SOON
* MONITOR
* WAIT

These recommendations are generated based on:

* CPU utilisation
* Carbon intensity trace stream
* Credit state variable

---

## What Exists in the Codebase

Currently the system contains:

1. ML Scheduler Layer (Python)

   * Model training pipeline
   * Dashboard export logic
   * Dataset processing

2. Simulation Layer (Java CloudSim Framework)

   * VM simulation environment
   * Carbon trend analyzer
   * Recommendation inference

3. Visualization Layer

   * Dashboard analytics interface

---

## What Needs to Be Added (Cost Optimization Feature)

Your task is to implement **cost-aware VM migration scheduling** inside the Java simulation framework.
---

### Step 1. Create Cost Optimizer Module

Inside:

```
src/
```

Create file:

```
CarbonCostOptimizer.java
```

Implement a multi-objective cost function.

The cost model should consider:

* Energy consumption cost
* Migration transfer overhead penalty
* Workload utilisation cost
* Carbon credit state penalty

---

### Step 2. Modify Main Simulation Driver

Open:

```
CarbonAwareMigration.java
```

Inside the time iteration loop:

Locate recommendation parsing logic.

Immediately after recommendation extraction, add cost evaluation call:

```
CarbonCostOptimizer.computeCost(...)
```

---

### Step 3. Replace Random Migration Flag Logic

Current code uses probabilistic migration flags.

Replace it with deterministic cost-aware scheduling:

* Compute system cost score
* Compare against optimisation threshold
* Trigger migration decision accordingly

---

### Step 4. Scheduler Integration

If scheduler modules exist inside:

```
scheduler/
vmMigration/
```

Modify migration decision pipeline to follow:

```
Trend Analyzer → Recommendation Engine → Cost Optimizer → Migration Scheduler

## Testing Procedure

After coding:

1. Compile CloudSim project
2. Run CarbonAwareMigration.java
3. Observe:

   * Migration triggers
   * Cost computation logs
   * Scheduler decision outputs

---

## Development Philosophy

This project follows a **stateful carbon governance scheduling architecture** rather than stateless threshold triggering.

The credit abstraction layer maintains temporal carbon awareness.

---

