import pandas as pd
import numpy as np
import joblib
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (accuracy_score, classification_report,
                             confusion_matrix, roc_curve, auc,
                             precision_recall_curve, average_precision_score)
import matplotlib.pyplot as plt
import seaborn as sns

# ── Load Dataset ──────────────────────────────────────────
data = pd.read_csv("carbon_trace_dataset.csv", header=None)
data.columns = ["cpu", "carbonIntensity", "credit",
                "migrationFlag", "recommendation", "waitMinutes"]

print("=== Dataset Info ===")
print(f"Total rows: {len(data)}")
print("\nClass distribution:")
print(data["migrationFlag"].value_counts())
print(f"\nMigration rate: {data['migrationFlag'].mean()*100:.1f}%")

print("\n=== Real Data Verification ===")
print(f"carbonIntensity range: {data['carbonIntensity'].min():.1f} - {data['carbonIntensity'].max():.1f} gCO2/kWh")
print(f"Expected real range: ~26-309 gCO2/kWh (UK 2023)")

# ── Recommendation Distribution ───────────────────────────
print("\n=== Recommendation Distribution ===")
print(data["recommendation"].value_counts())
print(f"\nAverage wait time when MONITOR     : {data[data['recommendation']=='MONITOR']['waitMinutes'].mean():.0f} mins")
print(f"Average wait time when MIGRATE_SOON: {data[data['recommendation']=='MIGRATE_SOON']['waitMinutes'].mean():.0f} mins")

# ── Features & Label ──────────────────────────────────────
X = data[["cpu", "carbonIntensity", "credit"]]
y = data["migrationFlag"]

# ── Train / Test Split ────────────────────────────────────
X_train, X_test, y_train, y_test = train_test_split(
    X, y,
    test_size=0.3,
    random_state=42,
    stratify=y
)

# ── SMOTE: Fix class imbalance ────────────────────────────
try:
    from imblearn.over_sampling import SMOTE
    smote = SMOTE(random_state=42)
    X_train_sm, y_train_sm = smote.fit_resample(X_train, y_train)
    print(f"\nSMOTE applied: {len(X_train_sm)} training samples (was {len(X_train)})")
except ImportError:
    print("\nSMOTE not available. Run: pip install imbalanced-learn")
    X_train_sm, y_train_sm = X_train, y_train

# ── Model ─────────────────────────────────────────────────
model = RandomForestClassifier(
    n_estimators=150,
    max_depth=5,
    min_samples_split=20,
    class_weight="balanced",
    random_state=42
)

model.fit(X_train_sm, y_train_sm)
y_pred = model.predict(X_test)
y_prob = model.predict_proba(X_test)[:, 1]

# ── Performance ───────────────────────────────────────────
print("\n===== Test Set Performance =====")
print(f"Accuracy: {accuracy_score(y_test, y_pred):.4f}")
print(classification_report(y_test, y_pred,
      target_names=["No Migration", "Migration"]))

# ── Feature Importance ────────────────────────────────────
print("===== Feature Importance =====")
features = ["cpu", "carbonIntensity", "credit"]
for feat, imp in zip(features, model.feature_importances_):
    print(f"  {feat:20s}: {imp:.4f}")

# ── Cross Validation ──────────────────────────────────────
print("\n===== Cross Validation (5-Fold) =====")
cv_scores = cross_val_score(model, X, y, cv=5, scoring="accuracy")
print(f"CV Scores : {cv_scores}")
print(f"Mean      : {cv_scores.mean():.4f}")
print(f"Std Dev   : {cv_scores.std():.4f}")

# ── Sample Recommendations ────────────────────────────────
print("\n===== Sample Migration Recommendations =====")
print(f"{'Carbon':>10} {'CPU':>6} {'Credit':>8} {'Recommendation':>15} {'Wait':>6}")
print("-" * 55)

sample = data.sample(15, random_state=42).sort_values(
    "carbonIntensity", ascending=False)
for _, row in sample.iterrows():
    carbon_label = "HIGH" if row['carbonIntensity'] > 190 else "LOW"
    print(f"{row['carbonIntensity']:>8.1f} "
          f"({carbon_label}) "
          f"{row['cpu']:>6.2f} "
          f"{row['credit']:>8.2f} "
          f"{row['recommendation']:>15} "
          f"{int(row['waitMinutes']):>5}m")

# ── Wait Time Analysis ────────────────────────────────────
print("\n===== Wait Time Analysis =====")
for rec in ["MIGRATE_NOW", "MIGRATE_SOON", "MONITOR", "WAIT"]:
    subset = data[data["recommendation"] == rec]
    if len(subset) > 0:
        print(f"{rec:15}: count={len(subset):5d} | "
              f"avg_wait={subset['waitMinutes'].mean():5.0f} mins | "
              f"avg_carbon={subset['carbonIntensity'].mean():6.1f} gCO2/kWh")

# ══════════════════════════════════════════════════════════
# PLOTS
# ══════════════════════════════════════════════════════════

# ── Plot 1: Confusion Matrix ──────────────────────────────
cm = confusion_matrix(y_test, y_pred)
plt.figure(figsize=(6, 5))
sns.heatmap(cm, annot=True, fmt="d", cmap="Blues",
            xticklabels=["No Migration", "Migration"],
            yticklabels=["No Migration", "Migration"])
plt.title("Confusion Matrix — Carbon-Aware VM Migration")
plt.ylabel("Actual")
plt.xlabel("Predicted")
plt.tight_layout()
plt.savefig("confusion_matrix.png", dpi=150)
plt.show()
print("Saved: confusion_matrix.png")

# ── Plot 2: ROC Curve ─────────────────────────────────────
fpr, tpr, _ = roc_curve(y_test, y_prob)
roc_auc = auc(fpr, tpr)

plt.figure(figsize=(7, 5))
plt.plot(fpr, tpr, color="#2196F3", lw=2,
         label=f"Random Forest (AUC = {roc_auc:.3f})")
plt.plot([0,1], [0,1], 'k--', lw=1, label="Random Baseline")
plt.xlabel("False Positive Rate")
plt.ylabel("True Positive Rate")
plt.title("ROC Curve — Carbon-Aware VM Migration Classifier")
plt.legend(loc="lower right")
plt.grid(alpha=0.3)
plt.tight_layout()
plt.savefig("roc_curve.png", dpi=150)
plt.show()
print("Saved: roc_curve.png")

# ── Plot 3: Precision-Recall Curve ───────────────────────
prec, rec, _ = precision_recall_curve(y_test, y_prob)
ap = average_precision_score(y_test, y_prob)
baseline = y_test.mean()

plt.figure(figsize=(7, 5))
plt.plot(rec, prec, color="#FF5722", lw=2,
         label=f"Random Forest (AP = {ap:.3f})")
plt.axhline(y=baseline, color='k', linestyle='--', lw=1,
            label=f"Baseline (AP = {baseline:.3f})")
plt.xlabel("Recall")
plt.ylabel("Precision")
plt.title("Precision-Recall Curve — Migration Detection")
plt.legend(loc="upper right")
plt.grid(alpha=0.3)
plt.tight_layout()
plt.savefig("precision_recall_curve.png", dpi=150)
plt.show()
print("Saved: precision_recall_curve.png")

# ── Plot 4: Feature Importance ────────────────────────────
plt.figure(figsize=(7, 4))
importances = model.feature_importances_
colors_feat = ["#4CAF50", "#2196F3", "#FF5722"]
bars = plt.barh(features, importances, color=colors_feat)
plt.xlabel("Importance Score")
plt.title("Feature Importance — Carbon Credit Scheduler\n(Random Forest, Real UK Grid Data 2023)")

for bar, val in zip(bars, importances):
    plt.text(bar.get_width() + 0.005, bar.get_y() + bar.get_height()/2,
             f'{val:.4f}', va='center', fontsize=10)

plt.xlim(0, max(importances) + 0.1)
plt.tight_layout()
plt.savefig("feature_importance.png", dpi=150)
plt.show()
print("Saved: feature_importance.png")

# ── Plot 5: Carbon Intensity Distribution ─────────────────
plt.figure(figsize=(8, 4))
plt.hist(data[data["migrationFlag"]==0]["carbonIntensity"],
         bins=40, alpha=0.6, color="#2196F3", label="No Migration")
plt.hist(data[data["migrationFlag"]==1]["carbonIntensity"],
         bins=40, alpha=0.6, color="#FF5722", label="Migration")
plt.xlabel("Carbon Intensity (gCO2/kWh)")
plt.ylabel("Frequency")
plt.title("Carbon Intensity Distribution by Migration Decision\n(Real UK Grid Data 2023)")
plt.legend()
plt.grid(alpha=0.3)
plt.tight_layout()
plt.savefig("carbon_intensity_distribution.png", dpi=150)
plt.show()
print("Saved: carbon_intensity_distribution.png")

# ── Plot 6: Wait Time by Recommendation ──────────────────
rec_order = ["MIGRATE_NOW", "MIGRATE_SOON", "MONITOR", "WAIT"]
rec_colors = ["#F44336", "#FF9800", "#FFC107", "#4CAF50"]
rec_data = []
rec_labels = []

for rec in rec_order:
    subset = data[data["recommendation"] == rec]["waitMinutes"]
    if len(subset) > 0:
        rec_data.append(subset.values)
        rec_labels.append(rec)

plt.figure(figsize=(9, 5))
bp = plt.boxplot(rec_data, labels=rec_labels, patch_artist=True)
for patch, color in zip(bp['boxes'], rec_colors):
    patch.set_facecolor(color)
    patch.set_alpha(0.7)

plt.xlabel("Recommendation")
plt.ylabel("Wait Time (minutes)")
plt.title("Wait Time Distribution by Migration Recommendation\n(Real UK Grid Data 2023)")
plt.grid(alpha=0.3)
plt.tight_layout()
plt.savefig("wait_time_distribution.png", dpi=150)
plt.show()
print("Saved: wait_time_distribution.png")

# ── Plot 7: Carbon Intensity Over Time (sample) ───────────
plt.figure(figsize=(12, 4))
sample_size = 500
time_sample = data.head(sample_size)

colors_map = {
    "MIGRATE_NOW":  "#F44336",
    "MIGRATE_SOON": "#FF9800",
    "MONITOR":      "#FFC107",
    "WAIT":         "#4CAF50"
}

plt.plot(range(sample_size),
         time_sample["carbonIntensity"],
         color="gray", lw=1, alpha=0.7, label="Carbon Intensity")

for rec, color in colors_map.items():
    mask = time_sample["recommendation"] == rec
    plt.scatter(
        time_sample.index[mask] - time_sample.index[0],
        time_sample[mask]["carbonIntensity"],
        color=color, s=15, label=rec, zorder=5
    )

plt.xlabel("Time Step (each = 30 mins)")
plt.ylabel("Carbon Intensity (gCO2/kWh)")
plt.title("Carbon Intensity Trace with Migration Recommendations\n(First 500 readings — Real UK Grid 2023)")
plt.legend(loc="upper right", fontsize=8)
plt.grid(alpha=0.3)
plt.tight_layout()
plt.savefig("carbon_trace_recommendations.png", dpi=150)
plt.show()
print("Saved: carbon_trace_recommendations.png")

# ── Save Model ────────────────────────────────────────────
joblib.dump(model, "carbon_scheduler_model.pkl")
print("\nModel saved as carbon_scheduler_model.pkl")
print("\n=== All done! ===")
print(f"Accuracy: {accuracy_score(y_test, y_pred)*100:.2f}%")
print(f"AUC:      {roc_auc:.3f}")

import json

# ── Export real data for dashboard ──────────────────────
print("\nGenerating dashboard_data.json...")

# Take last 96 rows = 48 hours of real data
dash_data = data.tail(96).copy()

# Get ML model predictions on this slice
X_dash = dash_data[["cpu", "carbonIntensity", "credit"]]
dash_data["ml_prediction"] = model.predict(X_dash)
dash_data["ml_probability"] = model.predict_proba(X_dash)[:, 1]

# Build JSON
dashboard_json = {
    "model_metrics": {
        "accuracy":  round(accuracy_score(y_test, y_pred), 4),
        "cv_mean":   round(cv_scores.mean(), 4),
        "cv_std":    round(cv_scores.std(), 4),
        "total_rows": len(data)
    },
    "feature_importance": {
        "credit":          round(model.feature_importances_[2], 4),
        "carbonIntensity": round(model.feature_importances_[1], 4),
        "cpu":             round(model.feature_importances_[0], 4)
    },
    "recommendation_counts": data["recommendation"].value_counts().to_dict(),
    "wait_time_averages": {
        rec: round(data[data["recommendation"]==rec]["waitMinutes"].mean(), 1)
        for rec in ["MIGRATE_NOW","MIGRATE_SOON","MONITOR","WAIT"]
        if rec in data["recommendation"].values
    },
    "timeline": [
        {
            "carbonIntensity": round(row["carbonIntensity"], 1),
            "credit":          round(row["credit"], 2),
            "cpu":             round(row["cpu"], 3),
            "recommendation":  row["recommendation"],
            "waitMinutes":     int(row["waitMinutes"]),
            "migrationFlag":   int(row["migrationFlag"]),
            "ml_prediction":   int(row["ml_prediction"]),
            "ml_probability":  round(row["ml_probability"], 3)
        }
        for _, row in dash_data.iterrows()
    ]
}

with open("dashboard_data.json", "w") as f:
    json.dump(dashboard_json, f, indent=2)

print("Saved: dashboard_data.json")
print(f"Dashboard ready — open carbon_dashboard.html in browser")
