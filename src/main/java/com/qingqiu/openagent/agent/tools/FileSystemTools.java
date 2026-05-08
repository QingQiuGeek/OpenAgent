package com.qingqiu.openagent.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 10:32
 * @description: FileSystemTools
 */

@Slf4j
public class FileSystemTools implements ITool {

    private static final String BASE_DIRECTORY = System.getProperty("user.dir");

    @Override
    public String getName() {
        return "fileSystemTool";
    }

    @Override
    public String getDescription() {
        return "File system operations";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

//    @Tool(name = "readFile", value = "Read the text content of a file under the working directory")
    public String readFile(
            @P(value = "Relative path to the file under the working directory, e.g. data/notes.txt")
            String filePath) {
        try {
            Path path = validateAndResolvePath(filePath);
            if (!Files.exists(path)) {
                return "Error: file not found.";
            }
            if (!Files.isRegularFile(path)) {
                return "Error: path is not a file.";
            }
            return "File content:\n" + Files.readString(path);
        } catch (Exception e) {
            log.error("readFile failed", e);
            return "Error: " + e.getMessage();
        }
    }

//    @Tool(name = "writeFile", value = "Write text content to a file (overwrite). Creates parent directories if missing.")
    public String writeFile(
            @P(value = "Relative path to the target file under the working directory")
            String filePath,
            @P(value = "Text content to write; existing file will be overwritten")
            String content) {
        try {
            Path path = validateAndResolvePath(filePath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content == null ? "" : content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return "OK";
        } catch (Exception e) {
            log.error("writeFile failed", e);
            return "Error: " + e.getMessage();
        }
    }

//    @Tool(name = "appendToFile", value = "Append text content to the end of a file. Creates the file if it does not exist.")
    public String appendToFile(
            @P(value = "Relative path to the target file under the working directory")
            String filePath,
            @P(value = "Text content to append at the end of the file")
            String content) {
        try {
            Path path = validateAndResolvePath(filePath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content == null ? "" : content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return "OK";
        } catch (Exception e) {
            log.error("appendToFile failed", e);
            return "Error: " + e.getMessage();
        }
    }

//    @Tool(name = "listFiles", value = "List the entries (files and subdirectories) under the given directory")
    public String listFiles(
            @P(value = "Relative directory path under the working directory; pass empty string to list the working directory itself")
            String directoryPath) {
        try {
            Path path = (directoryPath == null || directoryPath.isBlank())
                    ? Paths.get(BASE_DIRECTORY)
                    : validateAndResolvePath(directoryPath);

            if (!Files.exists(path)) {
                return "Error: directory not found.";
            }
            if (!Files.isDirectory(path)) {
                return "Error: path is not a directory.";
            }

            List<String> items = Files.list(path)
                    .map(p -> Files.isDirectory(p) ? "[DIR] " + p.getFileName() : "[FILE] " + p.getFileName())
                    .sorted()
                    .collect(Collectors.toList());

            return items.isEmpty() ? "(empty)" : String.join("\n", items);
        } catch (Exception e) {
            log.error("listFiles failed", e);
            return "Error: " + e.getMessage();
        }
    }

//    @Tool(name = "deleteFile", value = "Delete a file, or recursively delete a directory and its contents")
    public String deleteFile(
            @P(value = "Relative path of the file or directory to delete, under the working directory")
            String path) {
        try {
            Path filePath = validateAndResolvePath(path);
            if (!Files.exists(filePath)) {
                return "Error: path not found.";
            }

            if (Files.isDirectory(filePath)) {
                try (Stream<Path> paths = Files.walk(filePath)) {
                    paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }
            } else {
                Files.delete(filePath);
            }
            return "OK";
        } catch (Exception e) {
            log.error("deleteFile failed", e);
            return "Error: " + e.getMessage();
        }
    }

//    @Tool(name = "createDirectory", value = "Create a directory (recursively, including any missing parent directories)")
    public String createDirectory(
            @P(value = "Relative directory path under the working directory to create")
            String directoryPath) {
        try {
            Path path = validateAndResolvePath(directoryPath);
            if (Files.exists(path) && !Files.isDirectory(path)) {
                return "Error: path exists but not directory.";
            }
            Files.createDirectories(path);
            return "OK";
        } catch (Exception e) {
            log.error("createDirectory failed", e);
            return "Error: " + e.getMessage();
        }
    }

    private Path validateAndResolvePath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("path cannot be empty");
        }

        Path basePath = Paths.get(BASE_DIRECTORY).toAbsolutePath().normalize();
        Path resolvedPath = basePath.resolve(filePath).toAbsolutePath().normalize();
        if (!resolvedPath.startsWith(basePath)) {
            throw new SecurityException("path traversal is not allowed");
        }
        return resolvedPath;
    }
}
