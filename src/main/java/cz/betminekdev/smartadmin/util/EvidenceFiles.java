package cz.betminekdev.smartadmin.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EvidenceFiles {
    private EvidenceFiles() {
    }

    public static Path write(Path folder, String player, List<String> lines) throws IOException {
        Files.createDirectories(folder);
        String safeName = player.replaceAll("[^A-Za-z0-9_-]", "_");
        // Atomic unique creation prevents overwriting reports generated in the same instant.
        Path output = Files.createTempFile(folder, safeName + "-evidence-", ".txt");
        try {
            Files.write(output, lines, StandardCharsets.UTF_8);
            return output;
        } catch (IOException exception) {
            Files.deleteIfExists(output);
            throw exception;
        }
    }

    public static String plainText(String value) {
        return value.replaceAll("(?i)[&\u00a7][0-9a-fk-orx]", "")
                .replaceAll("[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]", " ").trim();
    }
}
