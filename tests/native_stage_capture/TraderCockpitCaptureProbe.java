package SQ.CustomAnalysis;

import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.file.FileHandler;
import java.io.FileOutputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Bounded native acceptance probe, not installed or enabled by the product. */
public class TraderCockpitCaptureProbe extends CustomAnalysisMethod {
    public TraderCockpitCaptureProbe() { super("TraderCockpitCaptureProbe", TYPE_PROCESS_DATABANK); }
    private static String digest(Path file) throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (java.io.InputStream in = Files.newInputStream(file)) {
            byte[] bytes = new byte[65536]; int n;
            while ((n = in.read(bytes)) != -1) hash.update(bytes, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : hash.digest()) out.append(String.format("%02x", b & 255));
        return out.toString();
    }
    private static void write(Path file, Properties data) throws Exception {
        Path pending = file.resolveSibling(file.getFileName().toString() + ".pending");
        try (FileOutputStream out = new FileOutputStream(pending.toFile())) {
            data.storeToXML(out, "Native capture probe", "UTF-8"); out.getFD().sync();
        }
        Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE);
    }
    @Override public ArrayList<ResultsGroup> processDatabank(String project, String task, String bank,
            ArrayList<ResultsGroup> records) throws Exception {
        String configured = System.getenv("TRADERCOCKPIT_CAPTURE_PROBE_ROOT");
        if (configured == null) throw new IllegalStateException("Capture root is not configured");
        Path root = Paths.get(configured).toAbsolutePath().normalize();
        if (!root.equals(root.toRealPath()) || !Files.isDirectory(root)) throw new IllegalStateException("Invalid capture root");
        Properties graphs = new Properties();
        try (java.io.InputStream in = Files.newInputStream(root.resolve("graphs.xml"))) { graphs.loadFromXML(in); }
        String expected = graphs.getProperty(project);
        if (expected == null || !project.matches("TraderCockpit-StageProbe-[a-z0-9-]+")) throw new IllegalStateException("Unbound project");
        Path graph = Paths.get("user", "projects", project, "project.cfx").toAbsolutePath().normalize();
        if (!graph.equals(graph.toRealPath()) || !expected.equals(digest(graph))) throw new IllegalStateException("Capture graph changed");
        String checkpoint = getInputArgs();
        if (checkpoint == null || !checkpoint.matches("[a-z]+")) throw new IllegalStateException("Invalid checkpoint id");
        String prefix = project + "." + checkpoint + ".";
        String entry = graphs.getProperty(prefix + "entry");
        String run = graphs.getProperty(project + ".run");
        if (entry == null || !entry.matches("CustomAnalysis-Task[0-9]+\\.xml")
                || !task.equals(graphs.getProperty(prefix + "title"))
                || !bank.equals(graphs.getProperty(prefix + "bank"))
                || run == null || !UUID.fromString(run).toString().equals(run))
            throw new IllegalStateException("Unbound checkpoint task, bank or run");
        Path parent = root.resolve(checkpoint);
        if (!parent.equals(parent.toRealPath()) || !Files.isDirectory(parent)) throw new IllegalStateException("Checkpoint destination unavailable");
        Path visit = Files.createDirectory(parent.resolve(UUID.randomUUID().toString()));
        Properties manifest = new Properties();
        manifest.setProperty("schema", "tc.native-capture-probe.v2");
        manifest.setProperty("run", run); manifest.setProperty("task_entry", entry);
        manifest.setProperty("project", project); manifest.setProperty("task", task); manifest.setProperty("databank", bank);
        manifest.setProperty("graph_sha256", expected); manifest.setProperty("checkpoint", checkpoint);
        manifest.setProperty("visit", visit.getFileName().toString()); manifest.setProperty("started", Instant.now().toString());
        manifest.setProperty("count", Integer.toString(records.size())); write(visit.resolve("started.xml"), manifest);
        try {
            for (int i = 0; i < records.size(); i++) {
                Path target = visit.resolve(i + ".sqx");
                new FileHandler().saveFile(records.get(i).clone(), target.toString(), new HashMap<String, String[]>());
                try (java.nio.channels.FileChannel ch = java.nio.channels.FileChannel.open(target, StandardOpenOption.WRITE)) { ch.force(true); }
                manifest.setProperty("artifact." + i + ".name", records.get(i).getName());
                manifest.setProperty("artifact." + i + ".sha256", digest(target));
                manifest.setProperty("artifact." + i + ".bytes", Long.toString(Files.size(target)));
            }
            manifest.setProperty("completed", Instant.now().toString()); write(visit.resolve("completed.xml"), manifest);
        } catch (Exception error) {
            manifest.remove("completed");
            manifest.setProperty("failed", Instant.now().toString()); manifest.setProperty("error_type", error.getClass().getName());
            try { write(visit.resolve("failed.xml"), manifest); } catch (Exception secondary) { error.addSuppressed(secondary); }
            throw error;
        }
        return records;
    }
}
