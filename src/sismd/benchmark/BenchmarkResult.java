package sismd.benchmark;

public class BenchmarkResult {

    private final String name;
    private final long avgTimeMs;
    private final long avgMemoryBytes;

    public BenchmarkResult(String name, long avgTimeMs, long avgMemoryBytes) {
        this.name           = name;
        this.avgTimeMs      = avgTimeMs;
        this.avgMemoryBytes = avgMemoryBytes;
    }

    public String getName()         { return name; }
    public long getAvgTimeMs()      { return avgTimeMs; }
    public long getAvgMemoryBytes() { return avgMemoryBytes; }
    public double getAvgMemoryMB()  { return avgMemoryBytes / (1024.0 * 1024.0); }
}
