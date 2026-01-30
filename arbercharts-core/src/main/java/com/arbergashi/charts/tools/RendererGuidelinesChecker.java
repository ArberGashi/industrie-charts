package com.arbergashi.charts.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * RendererGuidelinesChecker: Checks renderer source code for compliance with zero-allocation rules.
 * Focus: Detects "new" allocations within the drawData method.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class RendererGuidelinesChecker {

    private static final Pattern DRAW_DATA_PATTERN = Pattern.compile(
            "void\\s+drawData\\s*\\(",
            Pattern.MULTILINE);

    private static final String FORBIDDEN_NEW = "\\bnew\\b";
    private static final List<String> ALLOWED_EXCEPTIONS = List.of(
            // Exceptions could be added here if necessary
    );

    public static void main(String[] args) {
        String projectRoot = ".";
        if (args.length > 0) {
            projectRoot = args[0];
        }

        File srcRoot = new File(projectRoot, "arbercharts-core/src/main/java/com/arbergashi/charts/render");
        if (!srcRoot.exists()) {
            // try common alternative locations
            File coreJava = new File(projectRoot, "arbercharts-core/src/main/java");
            File found = findDirectoryByName(coreJava, "render");
            if (found == null) {
                // fall back to searching the whole projectRoot
                found = findDirectoryByName(new File(projectRoot), "render");
            }
            if (found != null && found.exists()) {
                srcRoot = found;
            } else {
                System.err.println("Renderer source root not found: expected '.../arbercharts-core/src/main/java/.../render' under " + projectRoot);
                System.exit(1);
            }
        }

        List<File> javaFiles = new ArrayList<>();
        findJavaFiles(srcRoot, javaFiles);

        int violations = 0;
        for (File file : javaFiles) {
            violations += checkFile(file);
        }

        if (violations > 0) {
            System.err.println("\nTotal violations found: " + violations);
            System.exit(2);
        } else {
            System.out.println("Renderer guidelines check passed successfully.");
            System.exit(0);
        }
    }

    private static void findJavaFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                findJavaFiles(f, result);
            } else if (f.getName().endsWith(".java")) {
                result.add(f);
            }
        }
    }

    // neu: rekursive Verzeichnissuche nach Namen
    private static File findDirectoryByName(File root, String name) {
        if (root == null || !root.exists()) return null;
        if (root.isDirectory() && root.getName().equals(name)) return root;
        File[] files = root.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findDirectoryByName(f, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static int checkFile(File file) {
        try {
            String content = Files.readString(file.toPath());
            Matcher matcher = DRAW_DATA_PATTERN.matcher(content);

            int fileViolations = 0;
            while (matcher.find()) {
                int matchStart = matcher.start();

                // find the end of parameter list (matching ')')
                int parenStart = content.indexOf('(', matchStart);
                if (parenStart == -1) continue;
                int parenEnd = findClosingParen(content, parenStart);
                if (parenEnd == -1) continue;

                // find the opening brace '{' after the parameter list (skip whitespace/comments)
                int bodyOpen = findNextCharIndex(content, parenEnd + 1, '{');
                if (bodyOpen == -1) continue;

                int bodyClose = findClosingBraceAware(content, bodyOpen);
                if (bodyClose == -1) continue;

                // scan body for 'new' tokens outside comments/strings
                List<Integer> violations = findNewTokensOutsideComments(content, bodyOpen + 1, bodyClose);
                for (int idx : violations) {
                    int line = getLineNumber(content, idx);
                    String lineText = extractLine(content, idx);
                    System.err.println("Violation in " + file.getPath() + ": 'new' detected in drawData at line " + line);
                    System.err.println("  -> " + lineText.trim());
                    fileViolations++;
                }
            }
            return fileViolations;
        } catch (IOException e) {
            System.err.println("Error reading file " + file.getPath() + ": " + e.getMessage());
            return 0;
        }
    }

    private static int findClosingParen(String content, int startPos) {
        int depth = 0;
        for (int i = startPos; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            } else if (c == '"' || c == '\'') {
                // skip string/char literals
                i = skipStringOrChar(content, i);
            } else if (c == '/') {
                // skip comments
                if (i + 1 < content.length()) {
                    char n = content.charAt(i + 1);
                    if (n == '/') i = skipLineComment(content, i);
                    else if (n == '*') i = skipBlockComment(content, i);
                }
            }
        }
        return -1;
    }

    private static int findNextCharIndex(String content, int from, char target) {
        for (int i = from; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == target) return i;
            if (c == '"' || c == '\'') {
                i = skipStringOrChar(content, i);
            } else if (c == '/') {
                if (i + 1 < content.length()) {
                    char n = content.charAt(i + 1);
                    if (n == '/') i = skipLineComment(content, i);
                    else if (n == '*') i = skipBlockComment(content, i);
                }
            }
            // otherwise skip whitespace and other tokens until brace found
        }
        return -1;
    }

    private static int findClosingBraceAware(String content, int openBraceIndex) {
        int depth = 1;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inString = false;
        boolean inChar = false;

        for (int i = openBraceIndex + 1; i < content.length(); i++) {
            char c = content.charAt(i);
            char prev = i > 0 ? content.charAt(i - 1) : 0;

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                continue;
            }
            if (inBlockComment) {
                if (prev == '*' && c == '/') inBlockComment = false;
                continue;
            }
            if (inString) {
                if (c == '"' && prev != '\\') inString = false;
                continue;
            }
            if (inChar) {
                if (c == '\'' && prev != '\\') inChar = false;
                continue;
            }

            if (c == '/' && i + 1 < content.length()) {
                char n = content.charAt(i + 1);
                if (n == '/') { inLineComment = true; i++; continue; }
                if (n == '*') { inBlockComment = true; i++; continue; }
            } else if (c == '"') {
                inString = true;
            } else if (c == '\'') {
                inChar = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static List<Integer> findNewTokensOutsideComments(String content, int start, int end) {
        List<Integer> result = new ArrayList<>();
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inString = false;
        boolean inChar = false;

        for (int i = start; i < end; i++) {
            char c = content.charAt(i);
            char prev = i > 0 ? content.charAt(i - 1) : 0;

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                continue;
            }
            if (inBlockComment) {
                if (prev == '*' && c == '/') inBlockComment = false;
                continue;
            }
            if (inString) {
                if (c == '"' && prev != '\\') inString = false;
                continue;
            }
            if (inChar) {
                if (c == '\'' && prev != '\\') inChar = false;
                continue;
            }

            if (c == '/' && i + 1 < content.length()) {
                char n = content.charAt(i + 1);
                if (n == '/') { inLineComment = true; i++; continue; }
                if (n == '*') { inBlockComment = true; i++; continue; }
            } else if (c == '"') {
                inString = true;
                continue;
            } else if (c == '\'') {
                inChar = true;
                continue;
            }

            // detect 'new' token
            if (c == 'n' && i + 3 <= end) {
                if (content.startsWith("new", i)) {
                    int before = i - 1;
                    int after = i + 3;
                    boolean okBefore = before < 0 || !Character.isJavaIdentifierPart(content.charAt(before));
                    boolean okAfter = after >= content.length() || !Character.isJavaIdentifierPart(content.charAt(after));
                    if (okBefore && okAfter) {
                        // optional: check allowed exceptions (not implemented patterns)
                        result.add(i);
                    }
                }
            }
        }
        return result;
    }

    private static int skipStringOrChar(String content, int i) {
        char quote = content.charAt(i);
        i++;
        for (; i < content.length(); i++) {
            char c = content.charAt(i);
            char prev = content.charAt(i - 1);
            if (c == quote && prev != '\\') return i;
            if (c == '\\') i++; // skip escaped
        }
        return i;
    }

    private static int skipLineComment(String content, int i) {
        for (; i < content.length(); i++) {
            if (content.charAt(i) == '\n') return i;
        }
        return content.length() - 1;
    }

    private static int skipBlockComment(String content, int i) {
        for (i = i + 2; i < content.length(); i++) {
            char prev = content.charAt(i - 1);
            char c = content.charAt(i);
            if (prev == '*' && c == '/') return i;
        }
        return content.length() - 1;
    }

    private static String extractLine(String content, int index) {
        int start = content.lastIndexOf('\n', index - 1);
        int end = content.indexOf('\n', index);
        if (start == -1) start = 0; else start = start + 1;
        if (end == -1) end = content.length();
        return content.substring(start, end);
    }

    private static int getLineNumber(String content, int index) {
        int line = 1;
        for (int i = 0; i < index && i < content.length(); i++) {
            if (content.charAt(i) == '\n') line++;
        }
        return line;
    }

}
