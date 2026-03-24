# Histogram Equalization — Parallel Processing in Java

**Course:** Sistemas Multinúcleo e Distribuídos  
**Program:** Mestrado em Engenharia Informática — ISEP  
**Deadline:** 12th May 2026

---

## What This Project Does

This project takes a **low-contrast grayscale image** and makes it sharper by redistributing its pixel intensities — a technique called **histogram equalization**.

The same algorithm is implemented **6 different ways**, from a simple sequential version to fully parallel ones, so we can compare their performance and understand the trade-offs of each concurrency approach.

---

## The Algorithm (3 Steps)

Every implementation follows the same 3 stages:

```
Step 1 — Build the histogram
         For every pixel, compute its luminosity (0–255)
         Count how many pixels have each luminosity value
         Result: int[256] histogram

Step 2 — Build the cumulative histogram
         Each entry i = number of pixels with luminosity ≤ i
         Result: int[256] cumulativeHistogram
         (This step is always sequential — each value depends on the previous)

Step 3 — Remap every pixel
         newLuminosity = 255 × (cumulativeHist[originalLuminosity] / totalPixels)
         Set pixel R, G, B = newLuminosity (grayscale)
         Result: processed image with increased contrast
```

Steps 1 and 3 are done on every pixel independently — this is where parallelism helps the most.

---

## Project Structure

```
HistogramEQ/
├── src/
│   ├── image/              ← image loading/saving API (provided by teacher)
│   ├── filter/             ← one file per implementation
│   │   ├── SequentialFilter.java
│   │   ├── ThreadedFilter.java
│   │   ├── ThreadPoolFilter.java
│   │   ├── ForkJoinFilter.java
│   │   └── CompletableFilter.java
│   ├── benchmark/          ← measures execution time, CPU, memory
│   └── Main.java           ← runs all implementations
├── images/                 ← input images
├── output/                 ← processed images saved here
├── results/                ← benchmark logs and charts
├── lib/                    ← external .jar files if needed
└── README.md
```

---

## How to Compile and Run

From the project root in PowerShell:

```powershell
.\run.ps1
```

This script automatically compiles all `.java` files under `src/` and runs `Main.java`.

> Make sure you have a JDK installed and `javac` is available on your PATH.

---

## The 6 Implementations

| # | Name | Concurrency Strategy |
|---|------|----------------------|
| 1 | Sequential | No concurrency — baseline for comparison |
| 2 | Manual Threads | Explicitly created threads, manual synchronization |
| 3 | Thread Pool | `ExecutorService` with fixed thread pool |
| 4 | Fork/Join | Recursive task splitting via `ForkJoinPool` |
| 5 | CompletableFuture | Async, non-blocking task composition |
| 6 | GC Tuning | JVM garbage collector configuration for better memory performance |

---

## Key Concurrency Challenge

Steps 1 and 3 run across many threads simultaneously, but they all write to **the same shared histogram array**.  
Without proper handling, two threads can corrupt each other's data (a **race condition**).

Each implementation solves this differently:

- **Manual Threads** → `synchronized` blocks
- **Thread Pool** → `AtomicIntegerArray`
- **Fork/Join** → each task builds a partial histogram, merged at the end

---

## Performance Metrics Collected

For each implementation:

- **Execution time** (milliseconds)
- **CPU usage** (%)
- **Memory usage** (MB)
- **Speedup** over the sequential baseline
- Results tested across different image sizes and thread counts

---

## What to Expect Next

1. Add starter code files into `src/image/`
2. Implement and verify `SequentialFilter.java` first
3. Build each parallel version one at a time, verifying output correctness before benchmarking
4. Run benchmarks and generate comparison charts for the report