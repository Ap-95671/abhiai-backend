package com.abhiai.abhiai_backend.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class DocumentExtractionServiceTest {

    @Test
    void extractsUtf8PlainText() {
        String text = new DocumentExtractionService().extractText(
                "AbhiAI understands UTF-8 text documents. ✓".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertEquals("AbhiAI understands UTF-8 text documents. ✓", text);
    }

    @Test
    void extractsTextFromPdf() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText("AbhiAI private document retrieval works correctly.");
                content.endText();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            pdf = output.toByteArray();
        }

        String text = new DocumentExtractionService().extractPdf(pdf);

        assertTrue(text.contains("AbhiAI private document retrieval works correctly."));
    }
}
