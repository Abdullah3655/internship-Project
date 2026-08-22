package com.recruitment.candidateservice.service;

import com.recruitment.candidateservice.dto.ParsedCvData;
import com.recruitment.candidateservice.exception.InvalidFileException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CvParseService {

    private static final Pattern EMAIL = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PHONE = Pattern.compile(
            "(?:\\+|00)?\\d[\\d\\s().-]{7,}\\d"
    );
    private static final List<String> SKILL_KEYWORDS = List.of(
            "java", "spring", "python", "javascript", "typescript", "react", "angular",
            "node", "mysql", "postgresql", "mongodb", "docker", "kubernetes", "aws",
            "azure", "git", "kafka", "redis", "hibernate", "junit", "sql"
    );
    private static final int PREVIEW_LIMIT = 400;

    public ParsedCvData parse(MultipartFile file) {
        if (file == null) {
            throw new InvalidFileException("A CV file is required");
        }
        String original = file.getOriginalFilename() == null ? "cv" : file.getOriginalFilename();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new InvalidFileException("Could not read CV file");
        }
        if (bytes.length == 0) {
            throw new InvalidFileException(
                    "CV file is empty (0 bytes): " + original
                            + ". In Postman use Body → form-data → key 'files' with type File (not Text), then Select Files."
            );
        }
        return parseBytes(bytes, extensionOf(original));
    }

    public ParsedCvData parseBytes(byte[] bytes, String extension) {
        String text = extractText(bytes, extension);
        String email = firstMatch(EMAIL, text);
        String phone = normalizePhone(firstMatch(PHONE, text));
        String[] name = guessName(text, email, phone);
        List<String> tags = detectSkills(text);
        String preview = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (preview.length() > PREVIEW_LIMIT) {
            preview = preview.substring(0, PREVIEW_LIMIT) + "...";
        }
        return new ParsedCvData(
                name[0],
                name[1],
                email == null ? null : email.toLowerCase(Locale.ROOT),
                phone,
                tags,
                preview
        );
    }

    private String extractText(byte[] bytes, String extension) {
        try {
            return switch (extension) {
                case "pdf" -> extractPdf(bytes);
                case "docx" -> extractDocx(bytes);
                case "txt" -> new String(bytes, StandardCharsets.UTF_8);
                case "doc" -> "";
                default -> throw new InvalidFileException("CV must be a pdf, doc, docx, or txt file");
            };
        } catch (InvalidFileException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidFileException("Could not parse CV content");
        }
    }

    private static String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String extractDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String[] guessName(String text, String email, String phone) {
        if (text == null || text.isBlank()) {
            return new String[]{null, null};
        }
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (email != null && line.toLowerCase(Locale.ROOT).contains(email.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (phone != null && line.replaceAll("\\s", "").contains(phone.replaceAll("\\s", ""))) {
                continue;
            }
            if (EMAIL.matcher(line).find() || PHONE.matcher(line).find()) {
                continue;
            }
            String cleaned = line.replaceAll("[^\\p{L}\\s'-]", " ").trim().replaceAll("\\s+", " ");
            if (cleaned.isBlank()) {
                continue;
            }
            String[] parts = cleaned.split(" ");
            if (parts.length == 1) {
                return new String[]{parts[0], "Unknown"};
            }
            return new String[]{parts[0], parts[parts.length - 1]};
        }
        return new String[]{null, null};
    }

    private static List<String> detectSkills(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Set<String> found = new LinkedHashSet<>();
        for (String skill : SKILL_KEYWORDS) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(skill) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(lower).find()) {
                found.add(skill);
            }
        }
        return new ArrayList<>(found);
    }

    private static String firstMatch(Pattern pattern, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group().trim() : null;
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String compact = phone.replaceAll("[^\\d+]", "");
        return compact.isBlank() ? null : compact;
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
