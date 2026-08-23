package com.abhiai.abhiai_backend.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import com.abhiai.abhiai_backend.exception.InvalidMediaException;

@Service
public class DocumentExtractionService {

    private static final int MAX_PAGES = 50;
    private static final int MAX_OCR_PAGES = 10;
    private static final int MAX_EXTRACTED_CHARACTERS = 200_000;
    private static final Duration OCR_TIMEOUT = Duration.ofSeconds(30);

    public String extractPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw new InvalidMediaException("Password-protected PDF documents are not supported");
            }
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new InvalidMediaException("PDF documents may contain at most " + MAX_PAGES + " pages");
            }

            String text = new PDFTextStripper().getText(document).trim();
            if (text.length() < 40) {
                text = extractWithOcr(document);
            }
            if (text.isBlank()) {
                throw new InvalidMediaException("No readable text could be extracted from this document");
            }
            return text.length() > MAX_EXTRACTED_CHARACTERS
                    ? text.substring(0, MAX_EXTRACTED_CHARACTERS)
                    : text;
        } catch (IOException exception) {
            throw new InvalidMediaException("The PDF document could not be processed");
        }
    }

    private String extractWithOcr(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder result = new StringBuilder();
        int pages = Math.min(document.getNumberOfPages(), MAX_OCR_PAGES);
        for (int page = 0; page < pages; page++) {
            BufferedImage image = renderer.renderImageWithDPI(page, 200, ImageType.RGB);
            result.append(runTesseract(image)).append('\n');
        }
        return result.toString().trim();
    }

    private String runTesseract(BufferedImage image) throws IOException {
        Process process;
        try {
            process = new ProcessBuilder("tesseract", "stdin", "stdout", "-l", "eng", "--psm", "6")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException exception) {
            throw new InvalidMediaException("OCR is not available on this server");
        }

        try (var output = process.getOutputStream()) {
            ImageIO.write(image, "png", output);
        }
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        process.getInputStream().transferTo(response);
        try {
            if (!process.waitFor(OCR_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new InvalidMediaException("OCR processing timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new InvalidMediaException("OCR processing was interrupted");
        }
        if (process.exitValue() != 0) {
            throw new InvalidMediaException("OCR could not read the scanned document");
        }
        return response.toString(StandardCharsets.UTF_8);
    }
}
