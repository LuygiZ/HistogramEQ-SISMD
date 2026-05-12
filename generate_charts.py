"""Generate benchmark charts for the SISMD project report."""

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import os

OUT = "images/charts"
os.makedirs(OUT, exist_ok=True)

BLUE   = "#2563EB"
GREEN  = "#16A34A"
ORANGE = "#EA580C"
PURPLE = "#7C3AED"
RED    = "#DC2626"
GRAY   = "#6B7280"

plt.rcParams.update({
    "font.family": "DejaVu Sans",
    "font.size": 11,
    "axes.titlesize": 13,
    "axes.titleweight": "bold",
    "axes.spines.top": False,
    "axes.spines.right": False,
    "figure.dpi": 150,
})

# ── 1. Speedup vs thread count ────────────────────────────────────────────────
# Data: src.jpg 860×1276 (1.1M pixels), default GC run
threads = [1, 2, 4, 8, 22]
speedup_mt = [1.38, 2.06, 3.67, 5.50, 4.71]
speedup_tp = [1.38, 2.36, 3.67, 3.67, 4.71]
speedup_cf = [1.43, 2.36, 3.30, 4.71, 5.50]
# Amdahl upper bound (sequential fraction ~5%)
f_seq = 0.05
ideal = [1 / (f_seq + (1 - f_seq) / t) for t in threads]

fig, ax = plt.subplots(figsize=(8, 5))
ax.plot(threads, speedup_mt, "o-", color=BLUE,   label="Multithreaded", lw=2, ms=7)
ax.plot(threads, speedup_tp, "s-", color=GREEN,  label="Thread Pool",   lw=2, ms=7)
ax.plot(threads, speedup_cf, "^-", color=ORANGE, label="CompletableFuture", lw=2, ms=7)
ax.plot(threads, ideal,      "--", color=GRAY,   label="Amdahl (fseq=5%)", lw=1.5, alpha=0.7)
ax.axhline(1.0, color="black", lw=0.8, ls=":", alpha=0.4)
ax.set_xlabel("Número de threads")
ax.set_ylabel("Speedup (× Sequential)")
ax.set_title("Speedup por número de threads\n(imagem 860×1276 – 1,1M píxeis)")
ax.set_xticks(threads)
ax.legend(frameon=False)
ax.set_ylim(0, 7)
ax.yaxis.set_minor_locator(ticker.MultipleLocator(0.5))
fig.tight_layout()
fig.savefig(f"{OUT}/chart_01_speedup_threads.png")
plt.close(fig)
print("chart_01_speedup_threads.png")

# ── 2. Sequential time vs image size ─────────────────────────────────────────
sizes  = ["512×512\n(262K px)", "860×1276\n(1,1M px)", "1920×1080\n(2,1M px)", "3840×2160\n(8,3M px)"]
seq_ms = [9, 33, 54, 216]

fig, ax = plt.subplots(figsize=(8, 5))
bars = ax.bar(sizes, seq_ms, color=[BLUE, GREEN, ORANGE, RED], width=0.5, zorder=3)
for bar, val in zip(bars, seq_ms):
    ax.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 3,
            f"{val} ms", ha="center", va="bottom", fontsize=11, fontweight="bold")
ax.set_ylabel("Tempo sequencial (ms)")
ax.set_title("Tempo de execução sequencial por dimensão de imagem")
ax.set_ylim(0, 250)
ax.yaxis.grid(True, linestyle="--", alpha=0.5, zorder=0)
fig.tight_layout()
fig.savefig(f"{OUT}/chart_02_time_by_size.png")
plt.close(fig)
print("chart_02_time_by_size.png")

# ── 3. Best speedup per implementation × image size ──────────────────────────
labels = ["512×512", "860×1276", "1920×1080", "3840×2160"]
impl_names = ["Multithreaded", "Thread Pool", "Fork/Join", "CompletableFuture"]
# best speedup at any thread count for each (impl, size) combo
data = {
    "Multithreaded":      [2.25, 5.50, 3.00, 2.63],
    "Thread Pool":        [3.00, 4.71, 2.84, 2.73],
    "Fork/Join":          [1.80, 4.71, 1.80, 2.63],
    "CompletableFuture":  [3.00, 5.50, 2.25, 2.70],
}
colors = [BLUE, GREEN, PURPLE, ORANGE]
x = np.arange(len(labels))
width = 0.2

fig, ax = plt.subplots(figsize=(9, 5))
for i, (name, vals) in enumerate(data.items()):
    offset = (i - 1.5) * width
    bars = ax.bar(x + offset, vals, width, label=name, color=colors[i], zorder=3)
ax.axhline(1.0, color="black", lw=0.8, ls=":", alpha=0.4)
ax.set_xlabel("Dimensão da imagem")
ax.set_ylabel("Speedup máximo (×)")
ax.set_title("Melhor speedup por implementação e dimensão de imagem")
ax.set_xticks(x)
ax.set_xticklabels(labels)
ax.legend(frameon=False, loc="upper right")
ax.set_ylim(0, 7)
ax.yaxis.grid(True, linestyle="--", alpha=0.5, zorder=0)
fig.tight_layout()
fig.savefig(f"{OUT}/chart_03_speedup_by_size.png")
plt.close(fig)
print("chart_03_speedup_by_size.png")

# ── 4. GC comparison: sequential vs best parallel (1920×1080) ────────────────
gc_names  = ["SerialGC", "ParallelGC", "G1GC", "ZGC"]
gc_seq    = [43, 41, 62, 71]
gc_best   = [8,  10, 24, 26]
x = np.arange(len(gc_names))
width = 0.35

fig, ax = plt.subplots(figsize=(8, 5))
b1 = ax.bar(x - width/2, gc_seq,  width, label="Sequencial",      color=BLUE,  zorder=3)
b2 = ax.bar(x + width/2, gc_best, width, label="Melhor paralelo", color=GREEN, zorder=3)
for bar, val in zip(b1, gc_seq):
    ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 1,
            f"{val}", ha="center", va="bottom", fontsize=10)
for bar, val in zip(b2, gc_best):
    ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 1,
            f"{val}", ha="center", va="bottom", fontsize=10)
ax.set_ylabel("Tempo (ms)")
ax.set_title("Impacto do Garbage Collector no desempenho\n(imagem 1920×1080 – 2,1M píxeis)")
ax.set_xticks(x)
ax.set_xticklabels(gc_names)
ax.legend(frameon=False)
ax.set_ylim(0, 85)
ax.yaxis.grid(True, linestyle="--", alpha=0.5, zorder=0)
fig.tight_layout()
fig.savefig(f"{OUT}/chart_04_gc_timing.png")
plt.close(fig)
print("chart_04_gc_timing.png")

# ── 5. GC pause statistics ────────────────────────────────────────────────────
gc_names_pause = ["SerialGC", "ParallelGC", "G1GC", "ZGC\n(concurrent)"]
gc_avg_pause   = [32.0, 12.6,  7.9, 0.0]
gc_max_pause   = [69.1, 40.9, 10.8, 0.0]
x = np.arange(len(gc_names_pause))
width = 0.35

fig, ax = plt.subplots(figsize=(8, 5))
b1 = ax.bar(x - width/2, gc_avg_pause, width, label="Pausa média (ms)", color=ORANGE, zorder=3)
b2 = ax.bar(x + width/2, gc_max_pause, width, label="Pausa máxima (ms)", color=RED, zorder=3, alpha=0.85)
for bar, val in zip(b1, gc_avg_pause):
    if val > 0:
        ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.5,
                f"{val:.1f}", ha="center", va="bottom", fontsize=10)
for bar, val in zip(b2, gc_max_pause):
    if val > 0:
        ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.5,
                f"{val:.1f}", ha="center", va="bottom", fontsize=10)
ax.set_ylabel("Pausa (ms)")
ax.set_title("Estatísticas de pausa GC por coletor\n(benchmark completo, 1920×1080)")
ax.set_xticks(x)
ax.set_xticklabels(gc_names_pause)
ax.legend(frameon=False)
ax.set_ylim(0, 80)
ax.yaxis.grid(True, linestyle="--", alpha=0.5, zorder=0)
ax.text(x[-1], 4, "sem STW", ha="center", va="bottom",
        fontsize=9, color=GRAY, style="italic")
fig.tight_layout()
fig.savefig(f"{OUT}/chart_05_gc_pauses.png")
plt.close(fig)
print("chart_05_gc_pauses.png")

# ── 6. Thread count vs time for all parallel implementations (1920×1080) ─────
threads6 = [1, 2, 4, 8, 22]
mt_ms   = [56, 39, 24, 19, 18]
tp_ms   = [55, 41, 28, 20, 19]
fj_ms   = [30]   # single point
cf_ms   = [74, 52, 33, 25, 24]
seq_ms6 = 54

fig, ax = plt.subplots(figsize=(8, 5))
ax.axhline(seq_ms6, color=GRAY, lw=1.5, ls="--", label=f"Sequential ({seq_ms6} ms)", alpha=0.8)
ax.plot(threads6, mt_ms, "o-", color=BLUE,   label="Multithreaded", lw=2, ms=7)
ax.plot(threads6, tp_ms, "s-", color=GREEN,  label="Thread Pool",   lw=2, ms=7)
ax.plot(threads6, cf_ms, "^-", color=ORANGE, label="CompletableFuture", lw=2, ms=7)
ax.plot([8], fj_ms, "D",        color=PURPLE, label="Fork/Join (auto)", ms=9, zorder=5)
ax.set_xlabel("Número de threads")
ax.set_ylabel("Tempo médio (ms)")
ax.set_title("Tempo de execução por número de threads\n(imagem 1920×1080 – 2,1M píxeis)")
ax.set_xticks(threads6)
ax.legend(frameon=False)
ax.set_ylim(0, 90)
ax.yaxis.grid(True, linestyle="--", alpha=0.5, zorder=0)
fig.tight_layout()
fig.savefig(f"{OUT}/chart_06_time_threads_fhd.png")
plt.close(fig)
print("chart_06_time_threads_fhd.png")

print("\nAll charts saved to", OUT)
