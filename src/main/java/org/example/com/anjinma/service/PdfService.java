package org.example.com.anjinma.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PdfService {

    public java.util.List<String> extractPages(java.io.InputStream in) {
        java.util.List<String> pages = new java.util.ArrayList<>();
        try (PDDocument doc = PDDocument.load(in)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            int total = doc.getNumberOfPages();
            for (int i = 1; i <= total; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(doc);
                pages.add(text == null ? "" : text);
            }
        } catch (IOException e) {
            log.error("PDF text extraction failed", e);
        }
        return pages;
    }

    public java.util.List<BufferedImage> renderPageImages(java.io.InputStream in, float dpi) {
        java.util.List<BufferedImage> images = new java.util.ArrayList<>();
        try (PDDocument doc = PDDocument.load(in)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int total = doc.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                BufferedImage bim = renderer.renderImageWithDPI(i, dpi);
                images.add(bim);
            }
        } catch (IOException e) {
            log.error("PDF render to image failed", e);
        }
        return images;
    }

    // ---------- Image positions extraction ----------
    public java.util.List<PdfImageInfo> extractImages(byte[] pdfBytes) {
        java.util.List<PdfImageInfo> list = new java.util.ArrayList<>();
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            PDPageTree pages = doc.getPages();
            int index = 0;
            for (org.apache.pdfbox.pdmodel.PDPage page : pages) {
                float pageHeight = page.getMediaBox().getHeight();
                ImagePositionExtractor extractor = new ImagePositionExtractor(index, pageHeight, list);
                extractor.processPage(page);
                index++;
            }
        } catch (IOException e) {
            log.warn("Extract images failed: {}", e.getMessage());
        }
        return list;
    }

    private static class ImagePositionExtractor extends PDFStreamEngine {
        private final int pageIndex;
        private final float pageHeight;
        private final java.util.List<PdfImageInfo> out;

        ImagePositionExtractor(int pageIndex, float pageHeight, java.util.List<PdfImageInfo> out) {
            this.pageIndex = pageIndex;
            this.pageHeight = pageHeight;
            this.out = out;
        }

        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            String op = operator.getName();
            if ("Do".equals(op)) {
                COSName objectName = (COSName) operands.get(0);
                PDResources resources = getResources();
                if (resources == null) return;
                PDXObject xobject = resources.getXObject(objectName);
                if (xobject instanceof PDImageXObject img) {
                    var ctm = getGraphicsState().getCurrentTransformationMatrix();
                    float x = ctm.getTranslateX();
                    float y = ctm.getTranslateY();
                    float w = ctm.getScalingFactorX();
                    float h = ctm.getScalingFactorY();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(img.getImage(), "png", baos);
                    out.add(new PdfImageInfo(pageIndex, x, y, w, h, baos.toByteArray()));
                }
            } else {
                super.processOperator(operator, operands);
            }
        }
    }

    public static class PdfImageInfo {
        public final int page;
        public final float x, y, w, h;
        public final byte[] bytes;

        public PdfImageInfo(int page, float x, float y, float w, float h, byte[] bytes) {
            this.page = page;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.bytes = bytes;
        }
    }

    public byte[] overlayImages(byte[] basePdf, java.util.List<PdfImageInfo> images) {
        try (PDDocument doc = PDDocument.load(basePdf)) {
            for (PdfImageInfo info : images) {
                if (info.page < 0 || info.page >= doc.getNumberOfPages()) continue;
                org.apache.pdfbox.pdmodel.PDPage page = doc.getPage(info.page);
                PDImageXObject img = PDImageXObject.createFromByteArray(doc, info.bytes, "img");
                try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    cs.drawImage(img, info.x, info.y, info.w, info.h);
                }
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            log.warn("Overlay images failed: {}", e.getMessage());
            return basePdf;
        }
    }

    public byte[] renderTextAsPdf(String text) {
        try (PDDocument doc = new PDDocument()) {
            PDFont font = loadUnicodeFont(doc);
            float fontSize = 11f;
            float leading = 1.5f * fontSize;
            PDRectangle pageSize = PDRectangle.A4;
            float margin = 50f;
            float width = pageSize.getWidth() - 2 * margin;
            float startX = margin;
            float startY = pageSize.getHeight() - margin;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.setFont(font, fontSize);
            cs.setLeading(leading);
            cs.beginText();
            cs.newLineAtOffset(startX, startY);
            float curY = startY;

            // split by line breaks and draw with naive wrapping
            String[] lines = text.split("\r?\n", -1);
            for (String line : lines) {
                java.util.List<String> parts = wrapLine(line, font, fontSize, width);
                if (parts.isEmpty()) parts = java.util.List.of("");
                for (String part : parts) {
                    // Page break check before writing line
                    if (curY - leading <= margin) {
                        cs.endText();
                        cs.close();
                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        cs.setFont(font, fontSize);
                        cs.setLeading(leading);
                        cs.beginText();
                        cs.newLineAtOffset(startX, startY);
                        curY = startY;
                    }
                    cs.showText(part);
                    cs.newLine();
                    curY -= leading;
                }
            }

            cs.endText();
            cs.close();

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            log.error("PDF rendering failed", e);
            return new byte[0];
        }
    }

    // ---------- Layout-preserving text extraction ----------
    public static class TextLine {
        public final int page;
        public final float x;
        public final float y;
        public final float w;
        public final float h;
        public final String text;

        public TextLine(int page, float x, float y, float w, float h, String text) {
            this.page = page;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.text = text;
        }
    }

    public java.util.List<TextLine> collectTextLines(byte[] srcPdf) {
        java.util.List<TextLine> lines = new java.util.ArrayList<>();
        try (PDDocument doc = PDDocument.load(srcPdf)) {
            LineCollector collector = new LineCollector(lines);
            collector.setSortByPosition(true);
            collector.writeText(doc, new java.io.OutputStreamWriter(new java.io.ByteArrayOutputStream()));
        } catch (IOException e) {
            log.warn("Collect text lines failed: {}", e.getMessage());
        }
        return lines;
    }

    private static class LineCollector extends org.apache.pdfbox.text.PDFTextStripper {
        private final java.util.List<TextLine> out;
        private int currentPage = 0;

        LineCollector(java.util.List<TextLine> out) throws IOException {
            this.out = out;
        }

        @Override
        protected void startPage(PDPage page) throws IOException {
            super.startPage(page);
        }

        @Override
        protected void writeString(String text, java.util.List<org.apache.pdfbox.text.TextPosition> positions) throws IOException {
            if (positions == null || positions.isEmpty()) return;
            float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
            float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
            for (var tp : positions) {
                float x = tp.getXDirAdj();
                float y = tp.getYDirAdj();
                float w = tp.getWidthDirAdj();
                float h = tp.getHeightDir();
                minX = Math.min(minX, x);
                minY = Math.min(minY, y - h);
                maxX = Math.max(maxX, x + w);
                maxY = Math.max(maxY, y);
            }
            out.add(new TextLine(getCurrentPageNo() - 1, minX, minY, (maxX - minX), (maxY - minY), text));
        }
    }

    // Overlay translated lines at original positions; auto-scale font to fit width
    public byte[] overlayTranslatedLines(byte[] srcPdf, java.util.List<TextLine> translatedLines) {
        try (PDDocument doc = PDDocument.load(srcPdf)) {
            Fonts fonts = loadPreferredFonts(doc);
            for (TextLine tl : translatedLines) {
                if (tl.page < 0 || tl.page >= doc.getNumberOfPages()) continue;
                PDPage page = doc.getPage(tl.page);
                PDRectangle box = page.getMediaBox();
                float pageBottomMargin = 20f;
                try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    float fontSize = 11f;
                    float maxWidth = Math.max(10f, tl.w);
                    // sanitize text: remove hard line breaks which many fonts can't encode as glyphs
                    String drawText = tl.text == null ? "" : tl.text.replace("\r", "").replace("\n", " ");
                    PDFont font = hasCjk(drawText) ? fonts.cjk : fonts.latin;
                    float textWidth;
                    try {
                        textWidth = Math.max(1f, font.getStringWidth(drawText) / 1000f * fontSize);
                    } catch (IllegalArgumentException encEx) {
                        // Font cannot encode this text (likely CJK with Helvetica). Keep size; fallback scaling disabled.
                        textWidth = maxWidth / 2f;
                    }
                    if (textWidth > maxWidth) {
                        fontSize = Math.max(6f, fontSize * (maxWidth / textWidth));
                    }
                    // Erase original line area, then draw translated text in similar location
                    float pad = Math.max(1f, fontSize * 0.2f);
                    float bgY = Math.max(pageBottomMargin, tl.y - pad);
                    float bgHeight = tl.h + 2 * pad;
                    cs.setNonStrokingColor(Color.WHITE);
                    cs.addRect(tl.x, bgY, Math.max(10f, tl.w), bgHeight);
                    cs.fill();
                    cs.setNonStrokingColor(Color.BLACK);
                    cs.setFont(font, fontSize);
                    cs.beginText();
                    float targetY = tl.y + Math.max(fontSize, tl.h * 0.8f);
                    if (targetY > box.getHeight() - pageBottomMargin) targetY = box.getHeight() - pageBottomMargin;
                    cs.newLineAtOffset(tl.x, targetY);
                    try {
                        cs.showText(drawText);
                    } catch (IllegalArgumentException enc) {
                        // Fallback: replace non-ASCII with '?' and use Helvetica
                        try {
                            String ascii = drawText.replaceAll("[^\\x00-\\x7F]", "?");
                            cs.setFont(PDType1Font.HELVETICA, fontSize);
                            cs.showText(ascii);
                        } catch (Exception ignore) {
                            // give up on this line but keep PDF generation going
                        }
                    }
                    cs.endText();
                }
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            log.error("Overlay translated lines failed", e);
            return srcPdf;
        }
    }

    // Build a brand-new PDF: blank pages with same sizes, original images reinserted, and translated lines drawn.
    public byte[] composeWithImagesAndLines(byte[] srcPdf, java.util.List<TextLine> translatedLines) {
        try (PDDocument src = PDDocument.load(srcPdf); PDDocument out = new PDDocument()) {
            // 1) Prepare pages with same size
            int pageCount = src.getNumberOfPages();
            java.util.List<PDPage> outPages = new java.util.ArrayList<>(pageCount);
            for (int i = 0; i < pageCount; i++) {
                PDRectangle box = src.getPage(i).getMediaBox();
                PDPage newPage = new PDPage(box);
                out.addPage(newPage);
                outPages.add(newPage);
            }
            // 2) Reinsert images at original positions
            java.util.List<PdfImageInfo> imgs = extractImages(srcPdf);
            for (PdfImageInfo info : imgs) {
                if (info.page < 0 || info.page >= out.getNumberOfPages()) continue;
                PDPage page = outPages.get(info.page);
                PDImageXObject img = PDImageXObject.createFromByteArray(out, info.bytes, "img");
                try (PDPageContentStream cs = new PDPageContentStream(out, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    cs.drawImage(img, info.x, info.y, info.w, info.h);
                }
            }
            // 3) Draw translated lines in original boxes (font chosen per line)
            Fonts fonts = loadPreferredFonts(out);
            for (TextLine tl : translatedLines) {
                if (tl.page < 0 || tl.page >= out.getNumberOfPages()) continue;
                PDPage page = outPages.get(tl.page);
                PDRectangle box = page.getMediaBox();
                try (PDPageContentStream cs = new PDPageContentStream(out, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    float fontSize = 11f;
                    float maxWidth = Math.max(10f, tl.w);
                    String drawText = tl.text == null ? "" : tl.text.replace("\r", "").replace("\n", " ");
                    PDFont font = hasCjk(drawText) ? fonts.cjk : fonts.latin;
                    float textWidth;
                    try {
                        textWidth = Math.max(1f, font.getStringWidth(drawText) / 1000f * fontSize);
                    } catch (IllegalArgumentException encEx) {
                        textWidth = maxWidth / 2f;
                    }
                    if (textWidth > maxWidth) {
                        fontSize = Math.max(6f, fontSize * (maxWidth / textWidth));
                    }
                    cs.setNonStrokingColor(Color.BLACK);
                    cs.setFont(font, fontSize);
                    cs.beginText();
                    float baselineY = tl.y + Math.max(fontSize, tl.h * 0.8f);
                    float topLimit = box.getHeight() - 20f;
                    if (baselineY > topLimit) baselineY = topLimit;
                    cs.newLineAtOffset(tl.x, baselineY);
                    cs.showText(drawText);
                    cs.endText();
                }
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                out.save(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            log.error("Compose with images and lines failed", e);
            return srcPdf;
        }
    }

    // Remove all text operators (BT..ET blocks) from the original document while keeping images/vector graphics
    public byte[] stripTextKeepingGraphics(byte[] srcPdf) {
        try (PDDocument doc = PDDocument.load(srcPdf)) {
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDPage page = doc.getPage(i);
                PDFStreamParser parser = new PDFStreamParser(page);
                parser.parse();
                java.util.List<Object> tokens = parser.getTokens();
                java.util.List<Object> newTokens = new java.util.ArrayList<>();
                boolean inText = false;
                for (Object t : tokens) {
                    if (t instanceof Operator op) {
                        String name = op.getName();
                        if ("BT".equals(name)) {
                            inText = true;
                            continue;
                        }
                        if ("ET".equals(name)) {
                            inText = false;
                            continue;
                        }
                        if (inText) {
                            continue;
                        }
                        newTokens.add(op);
                    } else {
                        if (!inText) newTokens.add(t);
                    }
                }
                org.apache.pdfbox.pdmodel.common.PDStream newStream = new org.apache.pdfbox.pdmodel.common.PDStream(doc);
                try (java.io.OutputStream os = newStream.createOutputStream()) {
                    new ContentStreamWriter(os).writeTokens(newTokens);
                }
                page.setContents(newStream);
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            log.warn("Strip text failed: {}", e.getMessage());
            return srcPdf;
        }
    }

    public byte[] renderMixedAsPdf(java.util.List<Item> items) {
        try (PDDocument doc = new PDDocument()) {
            PDFont font = loadUnicodeFont(doc);
            float fontSize = 11f;
            float leading = 1.5f * fontSize;
            PDRectangle pageSize = PDRectangle.A4;
            float margin = 50f;
            float width = pageSize.getWidth() - 2 * margin;
            float startX = margin;
            float startY = pageSize.getHeight() - margin;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.setFont(font, fontSize);
            cs.setLeading(leading);
            cs.beginText();
            cs.newLineAtOffset(startX, startY);
            float curY = startY;

            for (Item it : items) {
                if (it.type == ItemType.TEXT) {
                    String[] lines = it.text.split("\r?\n", -1);
                    for (String line : lines) {
                        java.util.List<String> parts = wrapLine(line, font, fontSize, width);
                        if (parts.isEmpty()) parts = java.util.List.of("");
                        for (String part : parts) {
                            if (curY - leading <= margin) {
                                cs.endText();
                                cs.close();
                                page = new PDPage(pageSize);
                                doc.addPage(page);
                                cs = new PDPageContentStream(doc, page);
                                cs.setFont(font, fontSize);
                                cs.setLeading(leading);
                                cs.beginText();
                                cs.newLineAtOffset(startX, startY);
                                curY = startY;
                            }
                            cs.showText(part);
                            cs.newLine();
                            curY -= leading;
                        }
                    }
                } else if (it.type == ItemType.IMAGE) {
                    // End text stream before drawing image
                    cs.endText();
                    cs.close();

                    PDImageXObject img = PDImageXObject.createFromByteArray(doc, it.imageBytes, "img");
                    float imgW = img.getWidth();
                    float imgH = img.getHeight();
                    float scale = Math.min(width / imgW, 1.0f);
                    float drawW = imgW * scale;
                    float drawH = imgH * scale;

                    if (curY - drawH <= margin) {
                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        curY = startY;
                    }
                    try (PDPageContentStream imgCs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                        float x = startX;
                        float y = curY - drawH;
                        imgCs.drawImage(img, x, y, drawW, drawH);
                    }

                    // Resume text stream below image
                    cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);
                    cs.setFont(font, fontSize);
                    cs.setLeading(leading);
                    cs.beginText();
                    cs.newLineAtOffset(startX, curY - drawH);
                    curY = curY - drawH;
                }
            }

            cs.endText();
            cs.close();

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            log.error("PDF mixed rendering failed", e);
            return new byte[0];
        }
    }

    public enum ItemType {TEXT, IMAGE}

    public static class Item {
        public final ItemType type;
        public final String text;
        public final byte[] imageBytes;

        private Item(ItemType type, String text, byte[] imageBytes) {
            this.type = type;
            this.text = text;
            this.imageBytes = imageBytes;
        }

        public static Item text(String t) {
            return new Item(ItemType.TEXT, t, null);
        }

        public static Item image(byte[] b) {
            return new Item(ItemType.IMAGE, null, b);
        }
    }

    // Overlay translated text onto original PDF pages (preserves original graphics/images)
    public byte[] overlayTextOnOriginal(byte[] srcPdf, java.util.List<String> pageTexts) {
        try (PDDocument doc = PDDocument.load(srcPdf)) {
            Fonts fonts = loadPreferredFonts(doc);
            float fontSize = 11f;
            float leading = 1.5f * fontSize;
            float margin = 50f;

            int pageCount = Math.max(doc.getNumberOfPages(), pageTexts.size());
            int textIndex = 0;
            for (int i = 0; i < pageCount; i++) {
                String text = (textIndex < pageTexts.size()) ? pageTexts.get(textIndex) : "";
                // Ensure page exists
                if (i >= doc.getNumberOfPages()) {
                    doc.addPage(new PDPage());
                }
                PDPage page = doc.getPage(i);
                PDRectangle box = page.getMediaBox();
                float width = box.getWidth() - 2 * margin;
                float startX = margin;
                float startY = box.getHeight() - margin;

                try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    cs.setLeading(leading);
                    cs.beginText();
                    cs.newLineAtOffset(startX, startY);
                    float curY = startY;

                    String[] lines = text.split("\r?\n", -1);
                    for (String line : lines) {
                        java.util.List<String> parts = wrapLine(line, fonts.latin, fontSize, width);
                        if (parts.isEmpty()) parts = java.util.List.of("");
                        for (String part : parts) {
                            if (curY - leading <= margin) {
                                cs.endText();
                                // overflow: add a new blank page and continue
                                PDPage extra = new PDPage(box);
                                doc.addPage(extra);
                                try (PDPageContentStream cs2 = new PDPageContentStream(doc, extra)) {
                                    cs2.setLeading(leading);
                                    cs2.beginText();
                                    cs2.newLineAtOffset(startX, startY);
                                    curY = startY;
                                    PDFont f2 = hasCjk(part) ? fonts.cjk : fonts.latin;
                                    cs2.setFont(f2, fontSize);
                                    try {
                                        cs2.showText(part);
                                    } catch (IllegalArgumentException enc) {
                                        try {
                                            String ascii = part.replaceAll("[^\\x00-\\x7F]", "?");
                                            cs2.setFont(PDType1Font.HELVETICA, fontSize);
                                            cs2.showText(ascii);
                                        } catch (Exception ignore) {}
                                    }
                                    cs2.newLine();
                                }
                                // advance to next page index position for subsequent content
                                // Note: we won't re-open the previous cs; continue writing on following pages via new loop
                                i++; // move index to this new page
                                continue;
                            }
                            PDFont f = hasCjk(part) ? fonts.cjk : fonts.latin;
                            cs.setFont(f, fontSize);
                            try {
                                cs.showText(part);
                            } catch (IllegalArgumentException enc) {
                                try {
                                    String ascii = part.replaceAll("[^\\x00-\\x7F]", "?");
                                    cs.setFont(PDType1Font.HELVETICA, fontSize);
                                    cs.showText(ascii);
                                } catch (Exception ignore) {}
                            }
                            cs.newLine();
                            curY -= leading;
                        }
                    }
                    cs.endText();
                }
                textIndex++;
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            log.error("Overlay text on original PDF failed", e);
            return srcPdf;
        }
    }


    private PDFont loadUnicodeFont(PDDocument doc) {
        try {
            // Try to load a bundled Unicode TTF (optional). If missing, fallback to Helvetica.
            ClassPathResource res = new ClassPathResource("fonts/NotoSansCJK-Regular.ttf");
            if (res.exists()) {
                return PDType0Font.load(doc, res.getInputStream(), true);
            }
            // Try common OS font locations for CJK-capable fonts
            String[] candidates = new String[]{
                    // Windows
                    "C:/Windows/Fonts/malgun.ttf",                // Korean
                    "C:/Windows/Fonts/arialuni.ttf",              // Arial Unicode MS
                    "C:/Windows/Fonts/msyh.ttc",                  // Microsoft YaHei (SC)
                    "C:/Windows/Fonts/simsun.ttc",                // SimSun
                    "C:/Windows/Fonts/YuGothR.ttc",               // Yu Gothic
                    // macOS
                    "/System/Library/Fonts/AppleSDGothicNeo.ttc",
                    "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
                    // Linux common Noto paths
                    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc"
            };
            for (String p : candidates) {
                try {
                    if (Files.exists(Paths.get(p))) {
                        return PDType0Font.load(doc, Files.newInputStream(Paths.get(p)), true);
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (IOException ignored) {
        }
        // Final fallback: Helvetica (will not render CJK; callers must handle encoding issues)
        return PDType1Font.HELVETICA;
    }

    // New: load Latin + CJK fonts, with classpath and OS fallbacks
    private Fonts loadPreferredFonts(PDDocument doc) {
        PDFont latin = null;
        PDFont cjk = null;
        // 1) Classpath first
        try {
            ClassPathResource latinRes = new ClassPathResource("fonts/NotoSans-Regular.ttf");
            if (latinRes.exists()) {
                latin = PDType0Font.load(doc, latinRes.getInputStream(), true);
                log.info("Loaded Latin font from classpath: {}", latinRes.getPath());
            }
        } catch (Exception ignore) {
        }
        try {
            // accept multiple resource names for CJK
            String[] cjkRes = new String[]{
                    "fonts/NotoSansCJK-Regular.ttf",
                    "fonts/NotoSansCJK-Regular.ttc",
                    "fonts/NotoSerifCJK.ttc",
                    "fonts/NotoSerifCJK-Regular.ttc",
                    "fonts/NotoSansSC-Regular.otf",
                    "fonts/NotoSansTC-Regular.otf",
                    "fonts/NotoSansKR-Regular.otf",
                    "fonts/NotoSansJP-Regular.otf"
            };
            for (String r : cjkRes) {
                ClassPathResource cjkResCp = new ClassPathResource(r);
                if (cjkResCp.exists()) {
                    cjk = PDType0Font.load(doc, cjkResCp.getInputStream(), true);
                    log.info("Loaded CJK font from classpath: {}", cjkResCp.getPath());
                    break;
                }
            }
        } catch (Exception ignore) {
        }
        // 2) OS fallback if needed
        if (latin == null) {
            String[] latinCand = new String[]{
                    // Common system Latin fonts
                    "C:/Windows/Fonts/arial.ttf",
                    "/System/Library/Fonts/Supplemental/Arial.ttf",
                    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
            };
            for (String p : latinCand) {
                try {
                    if (Files.exists(Paths.get(p))) {
                        latin = PDType0Font.load(doc, Files.newInputStream(Paths.get(p)), true);
                        log.info("Loaded Latin font from OS path: {}", p);
                        break;
                    }
                } catch (Exception ignore) {
                }
            }
        }
        if (cjk == null) {
            String[] cjkCand = new String[]{
                    // Windows CJK
                    "C:/Windows/Fonts/msyh.ttc",      // SC
                    "C:/Windows/Fonts/simsun.ttc",    // SC
                    "C:/Windows/Fonts/malgun.ttf",    // KR
                    "C:/Windows/Fonts/YuGothR.ttc",   // JP
                    // macOS CJK
                    "/System/Library/Fonts/AppleSDGothicNeo.ttc",
                    "/System/Library/Fonts/ヒラギノ角ゴシック W3.ttc",
                    // Linux Noto CJK
                    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc"
            };
            for (String p : cjkCand) {
                try {
                    if (Files.exists(Paths.get(p))) {
                        cjk = PDType0Font.load(doc, Files.newInputStream(Paths.get(p)), true);
                        log.info("Loaded CJK font from OS path: {}", p);
                        break;
                    }
                } catch (Exception ignore) {
                }
            }
        }
        if (latin == null) {
            latin = PDType1Font.HELVETICA;
            log.warn("Latin font not found; falling back to Helvetica");
        }
        if (cjk == null) {
            cjk = latin;
            log.warn("CJK font not found; falling back to Latin font (may cause missing glyphs)");
        }
        return new Fonts(latin, cjk);
    }

    private boolean hasCjk(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            int cp = s.codePointAt(i);
            if (isCjkCodePoint(cp)) return true;
            if (Character.charCount(cp) == 2) i++; // skip surrogate pair extra unit
        }
        return false;
    }

    private boolean isCjkCodePoint(int cp) {
        return // CJK Unified Ideographs + Ext A
                (cp >= 0x4E00 && cp <= 0x9FFF) || (cp >= 0x3400 && cp <= 0x4DBF) ||
                        // Hangul Syllables
                        (cp >= 0xAC00 && cp <= 0xD7AF) ||
                        // Hiragana, Katakana
                        (cp >= 0x3040 && cp <= 0x30FF) ||
                        // CJK Symbols and Punctuation, Halfwidth/Fullwidth Forms (rough)
                        (cp >= 0x3000 && cp <= 0x303F) || (cp >= 0xFF00 && cp <= 0xFFEF);
    }

    private static class Fonts {
        final PDFont latin;
        final PDFont cjk;

        Fonts(PDFont latin, PDFont cjk) {
            this.latin = latin;
            this.cjk = cjk;
        }
    }

    private boolean needsPageBreak(float startY, PDPage page, float leading, float margin) {
        float y = startY - leading; // approximate; PDFBox doesn't expose cursor y easily
        return y <= margin;
    }

    private List<String> wrapLine(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        // Preserve empty lines
        if (text == null || text.isEmpty()) return java.util.List.of("");
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            float w = font.getStringWidth(candidate) / 1000 * fontSize;
            if (w <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (line.length() > 0) parts.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) parts.add(line.toString());
        return parts;
    }
}
