# 🌿 Carbon Credit–Governed VM Migration System

> A simulation-based sustainable cloud computing framework that reduces datacenter carbon emissions by **55.3%** through stateful credit-based VM migration scheduling — outperforming greedy carbon minimisation by **17 percentage points** across a 6-policy benchmark on 17,419 real UK National Grid readings.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Key Results](#key-results)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [How It Works](#how-it-works)
- [ML Decision Component](#ml-decision-component)
- [Dashboard](#dashboard)
- [Policy Benchmark](#policy-benchmark)
- [Patent](#patent)
- [Author](#author)

---

## Overview

This project simulates VM migration scheduling across **3 UK regional datacenters** (London, Wales, Scotland) using real carbon intensity data from the UK National Grid. A carbon credit–based governance mechanism tracks cumulative emission behaviour over time, enabling smarter, stateful migration decisions rather than reacting to instantaneous signals alone.

The system integrates:
-  **CloudSim-based simulation** for multi-VM, multi-region workload scheduling
-  **6-policy benchmark** comparing static, random, CPU-threshold, greedy carbon, credit-aware, and ML-driven strategies
-  **Random Forest classifier** trained on real carbon × CPU × credit signals
-  **Live dashboard** connected to the UK Carbon Intensity API for real-time decisions

---

## Key Results

### 6-Policy Emission Benchmark (17,419 timesteps, 20 VMs)

| Policy | Total Emissions (gCO2) | Reduction vs Static | Migrations | Efficiency (gCO2/mig) |
|---|---|---|---|---|
| Static (Baseline) | 13,548,223 | 0.0% | 0 | — |
| Random | 10,148,411 | 25.1% | 58,263 | 58.4 |
| CPU-Threshold | 6,911,831 | 49.0% | 82,073 | 80.9 |
| Greedy Carbon | 8,363,235 | 38.3% | 51,876 | 100.0 |
| **Credit-Aware (Proposed)** | **6,056,898** | **55.3%** | 156,624 | **47.8** |
| RF ML Model | 9,041,888 | 33.3% | 37,663 | 119.7 |

### ML Classifier Performance

| Metric | Value |
|---|---|
| Test Accuracy | 87.54% |
| ROC-AUC | 0.803 |
| 5-Fold CV Mean | 91.62% |
| CV Std Dev | 0.0098 |

### Feature Importance

| Feature | Importance |
|---|---|
| 🏆 Carbon Credit State | 61.34% |
| Carbon Intensity | 26.92% |
| CPU Utilisation | 11.74% |

> **Key insight:** Carbon credit state — a temporal, stateful signal — is the dominant migration predictor, outweighing instantaneous carbon intensity and CPU load combined.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Monitoring Component                    │
│         CPU Utilisation · Carbon Intensity · Credit      │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                  Decision Component                      │
│     Random Forest · 4-tier recommendation output        │
│   MIGRATE_NOW · MIGRATE_SOON · MONITOR · WAIT           │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│            Carbon Credit Governance Component            │
│     Stateful credit tracking · Temporal awareness       │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│              Cost-Based Evaluation Component             │
│  Energy impact · Migration overhead · Credit constraints │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                  Execution Component                     │
│       VM allocation across London · Wales · Scotland     │
└─────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
Hypervisor-Carbon-Credits/
│
├── README.md                          # You are here
├── carbon_dashboard.html              # Real-time decision dashboard
├── carbon_ml_train.py                 # Random Forest ML pipeline + 6-policy benchmark + dashboard data generation
├── dashboard_data.json               # Simulation output for dashboard
│
└── CarbonAwareCloudMigration/
└── src/examples/org/cloudbus/cloudsim/examples/
│
├── CarbonLedger/              # Carbon credit tracking logic
├── network/                   # Network configuration
├── power/                     # Power model components
│
├── CarbonAwareMigration.java          # Main simulation entry point
├── CarbonAwareVmAllocationPolicy.java # Credit-governed VM placement
├── CarbonTraceLoader.java             # UK National Grid data loader
```

---

## Getting Started

### Prerequisites

```bash
# Java (CloudSim)
Java 11+
CloudSim 3.0

# Python (ML pipeline)
Python 3.9+
pip install scikit-learn pandas numpy imbalanced-learn matplotlib seaborn
```

### Run the Simulation

```bash
# Compile and run the Java CloudSim simulation
javac -cp cloudsim-3.0.jar CarbonAwareSimulation.java
java -cp .:cloudsim-3.0.jar CarbonAwareSimulation
```

### Run the ML Pipeline

```bash
python ml/carbon_ml_train.py
```

This will:
1. Load simulation output data
2. Apply SMOTE to handle class imbalance (8.5:1 ratio)
3. Train Random Forest classifier
4. Output accuracy, ROC-AUC, feature importance
5. Save model as `carbon_scheduler_model.pkl`
6. Generate all output plots

### Launch the Dashboard

```bash
# Serve locally (required to avoid CORS issues)
python -m http.server 8000

# Open in browser
http://localhost:8000/dashboard/carbon_dashboard.html
```

---

## How It Works

### Carbon Credit Mechanism

The credit system acts as a **memory signal** — it tracks cumulative emission behaviour over time rather than reacting to instantaneous carbon intensity alone.

```
Credit increases → when running in low-carbon conditions  
Credit decreases → when running in high-carbon conditions  
Migration triggered → when credit falls below threshold  
```

This prevents both over-migration (thrashing) and under-migration (ignoring sustained high-carbon periods).

### Migration Thresholds (derived from 2023 UK trace)

| Threshold | Value |
|---|---|
| High Carbon (75th percentile) | 203.0 gCO2/kWh |
| Low Carbon (25th percentile) | 98.0 gCO2/kWh |
| Migration energy cost | 8.0 kWh (250W × 115.2s) |

### Regional Setup

| Region | Carbon Multiplier | VM Capacity |
|---|---|---|
| London | ×1.0 | Unlimited |
| Wales | ×0.6 | 8 VMs |
| Scotland | ×0.3 | 4 VMs |

---

## ML Decision Component

The Random Forest classifier predicts migration decisions from three signals:

- `carbonIntensity` — current grid carbon reading (gCO2/kWh)
- `credit` — current carbon credit balance (range: −30 to +40)
- `cpu` — current VM CPU utilisation (0–1)

### 4-Tier Recommendation Output

| Recommendation | Avg Carbon | Avg Wait | Count |
|---|---|---|---|
| MIGRATE_NOW | 205.7 gCO2/kWh | 0 mins | 1,149 |
| MIGRATE_SOON | 205.1 gCO2/kWh | 23 mins | 2,573 |
| MONITOR | 144.5 gCO2/kWh | 42 mins | 5,426 |
| WAIT | 133.4 gCO2/kWh | 77 mins | 8,271 |

### Handling Class Imbalance

The dataset has an 8.5:1 class imbalance (no-migration vs migration). SMOTE was applied to the **training set only**, expanding it from 12,193 to 21,696 samples while preserving the natural distribution in the test set.

---

## Dashboard

The real-time dashboard integrates live data from the [UK Carbon Intensity API](https://carbonintensity.org.uk/) and displays:

- 🔴 Live carbon intensity (gCO2/kWh)
- 💳 Carbon credit state (Healthy / Depleting / Critical)
- 💻 CPU utilisation
- 🤖 ML migration recommendation + confidence score
- 📈 48-hour carbon intensity timeline
- ⚡ Best time to run workload (1hr / 2hr / 4hr / 8hr jobs)
- 📊 6-policy emission comparison

**Data input modes:** Manual entry · CSV upload · API endpoint

---

## Policy Benchmark

Six scheduling policies were benchmarked head-to-head:

1. **Static** — no migration, always runs in place
2. **Random** — migrates randomly
3. **CPU-Threshold** — migrates when CPU exceeds mean fleet utilisation
4. **Greedy Carbon** — always migrates to lowest carbon region available
5. **Credit-Aware** *(proposed)* — stateful credit-governed migration
6. **RF ML Model** — Random Forest–driven migration decisions

The credit-aware policy achieved the **lowest total emissions** and the **best migration efficiency** (47.8 gCO2/migration) across all strategies.

---

## Patent

This system is the subject of a filed Invention Disclosure (IDF-B) at VIT Vellore under the title:

> *"Carbon Credit–Governed VM Migration System"*

**Protected claims include:**
- Stateful carbon emission governance — a credit-based mechanism that tracks and regulates cumulative carbon output over time, preventing both emission spikes and unnecessary migration overhead
- Empirically validated 55.3% emission reduction — demonstrated across 17,419 real UK National Grid readings, outperforming the industry-standard greedy carbon minimisation approach by 17 percentage points
- Carbon credit as dominant migration signal — empirical proof (61.34% feature importance) that temporal emission state predicts sustainable scheduling decisions more accurately than instantaneous carbon intensity or CPU load
- Migration efficiency optimisation — lowest gCO2 per migration (47.8 gCO2/mig) across all 6 evaluated policies, ensuring environmental benefit justifies infrastructure cost
- Real-time sustainable scheduling — live integration with UK National Grid Carbon Intensity API enabling continuously updated green workload placement decisions
- Generalised green cloud architecture — modular framework applicable across any regional datacenter configuration with pluggable carbon data sources

---

## Author

**Ananya Shah**  
BTech Computer Science, VIT Vellore (2027)  

> *"The carbon credit mechanism outperforms greedy carbon minimisation because it carries memory — it knows not just where the grid is now, but where it's been."*
