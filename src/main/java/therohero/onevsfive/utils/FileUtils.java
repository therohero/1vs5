package therohero.onevsfive.utils;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

public class FileUtils {
    public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) throws IOException {
        Files.walk(Paths.get(sourceDirectoryLocation))
                .forEach(source -> {
                    Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                            .substring(sourceDirectoryLocation.length()));
                    try {
                        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            try (Stream<Path> walk = Files.walk(directory.toPath())) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }
}
