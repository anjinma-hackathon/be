package org.example.com.anjinma.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.example.com.anjinma.dto.TranslateRequest;
import org.example.com.anjinma.dto.TranslateResponse;
import org.example.com.anjinma.service.PdfService;
import org.example.com.anjinma.service.OcrService;
import org.example.com.anjinma.translation.Language;
import org.example.com.anjinma.translation.OllamaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Translation", description = "PDF 텍스트 청크 번역 API (exaone3.5) - Stateless")
public class TranslationController {

    private final OllamaService ollamaService;
    private final PdfService pdfService;
    private final OcrService ocrService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @PostMapping(value = "/translate/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "generate 번역", description = "언어와 텍스트 청크를 받아 단일 번역을 반환합니다.")
    public Mono<ResponseEntity<TranslateResponse>> translateWithGenerate(@RequestBody TranslateRequest req) {
        Language lang = toLanguage(req.getLanguage());
        return ollamaService.translateWithGenerate(req.getText(), lang)
            .map(result -> ResponseEntity.ok(TranslateResponse.builder()
                .lang(lang.code())
                .mode("generate")
                .content(result)
                .build()));
    }

    @PostMapping(value = "/translate/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "chat 번역", description = "system prompt 기반 고품질 단일 번역을 반환합니다.")
    public Mono<ResponseEntity<TranslateResponse>> translateWithChat(@RequestBody TranslateRequest req) {
        Language lang = toLanguage(req.getLanguage());
        return ollamaService.translateWithChat(req.getText(), lang)
            .map(result -> ResponseEntity.ok(TranslateResponse.builder()
                .lang(lang.code())
                .mode("chat")
                .content(result)
                .build()));
    }

    private Language toLanguage(String code) {
        if (code == null || code.isBlank()) return Language.EN;
        try { return Language.fromCode(code); } catch (IllegalArgumentException e) { return Language.EN; }
    }

    // ---- PDF 다운로드 (stateless, no roomId) ----
    

    // ---- 단일 엔드포인트: PDF 업로드 → 텍스트 추출(텍스트 또는 OCR) → 번역 → 이미지 원위치 유지하여 PDF 반환 ----
    @PostMapping(value = "/translate/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "PDF 업로드 번역(이미지 원위치 보존 + OCR)", description = "업로드한 PDF에서 텍스트를 추출(없으면 OCR)하여 번역하고, 원본 이미지 컨텍스트 위치를 보존해 PDF로 반환합니다.")
    @ApiResponse(responseCode = "200", description = "PDF file",
        content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    public Mono<ResponseEntity<byte[]>> uploadAndTranslatePdf(
        @RequestPart("file") MultipartFile file,
        @RequestPart(name = "language", required = false) String language,
        @RequestPart(name = "mode", required = false) String mode,
        @RequestPart(name = "filename", required = false) String filename,
        @RequestPart(name = "progressToken", required = false) String progressToken
    ) {
        final Language lang = toLanguage(language);
        final boolean useChat = (mode == null || mode.isBlank() || "chat".equalsIgnoreCase(mode));
        try {
            byte[] src = file.getBytes();
            notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                .type("started").message("Upload received").build());
            // 1) Extract text per page; if empty, OCR that page image
            java.util.List<String> pagesText = pdfService.extractPages(new java.io.ByteArrayInputStream(src));
            boolean hasAnyText = pagesText.stream().anyMatch(s -> s != null && !s.isBlank());
            if (!hasAnyText) {
                // OCR fallback
                java.util.List<java.awt.image.BufferedImage> pageImgs = pdfService.renderPageImages(new java.io.ByteArrayInputStream(src), 200f);
                pagesText = new java.util.ArrayList<>();
                int idx = 0;
                int total = pageImgs.size();
                notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                    .type("ocr_started").message("OCR fallback").total(total).current(0).build());
                for (java.awt.image.BufferedImage im : pageImgs) {
                    String txt = ocrService.ocr(im, lang.code());
                    pagesText.add(txt);
                    idx++;
                    notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                        .type("ocr_page").message("OCR page " + idx).total(total).current(idx).build());
                }
            }
            if (pagesText.isEmpty()) return Mono.just(ResponseEntity.badRequest().build());

            // 2) Prefer layout-preserving line extraction
            java.util.List<org.example.com.anjinma.service.PdfService.TextLine> srcLines = pdfService.collectTextLines(src);
            final boolean useLineMode = (srcLines != null && !srcLines.isEmpty());
            final int total = useLineMode ? srcLines.size() : pagesText.size();
            java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger(0);
            notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                .type("translate_started").message(useLineMode?"Translate lines":"Translate pages").total(total).current(0).build());
            if (useLineMode) {
                return reactor.core.publisher.Flux.fromIterable(srcLines)
                    .index()
                    .flatMap(tuple -> {
                        long idx = tuple.getT1();
                        var tl = tuple.getT2();
                        Mono<String> translated = useChat ? ollamaService.translateWithChat(tl.text, lang)
                                                          : ollamaService.translateWithGenerate(tl.text, lang);
                        return translated.map(t -> {
                            int cur = done.incrementAndGet();
                            notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                                .type("translate_page").message("Translated line " + (idx + 1))
                                .total(total).current(cur).build());
                            return new java.util.AbstractMap.SimpleEntry<Long, org.example.com.anjinma.service.PdfService.TextLine>(idx,
                                new org.example.com.anjinma.service.PdfService.TextLine(tl.page, tl.x, tl.y, tl.w, tl.h, t));
                        });
                    }, 2)
                    .collectList()
                    .map(list -> {
                        list.sort(java.util.Comparator.comparingLong(java.util.Map.Entry::getKey));
                        java.util.List<org.example.com.anjinma.service.PdfService.TextLine> ordered = new java.util.ArrayList<>();
                        for (var e : list) ordered.add(e.getValue());
                        notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                            .type("overlay").message("Compose (layout)").build());
                        // Build from original: remove all text, keep graphics/images; then overlay translated lines
                        byte[] base = pdfService.stripTextKeepingGraphics(src);
                        byte[] pdf = pdfService.overlayTranslatedLines(base, ordered);
                        final String langCode = lang.code();
                        String out = (filename == null || filename.isBlank()) ?
                            ("translated-" + langCode + ".pdf") : (filename + ".pdf");
                        notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                            .type("completed").message("PDF ready").build());
                        return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header("Content-Disposition", "attachment; filename=\"" + out + "\"")
                            .body(pdf);
                    });
            } else {
                return reactor.core.publisher.Flux.fromIterable(pagesText)
                    .index()
                    .flatMap(tuple -> {
                        long idx = tuple.getT1();
                        String text = tuple.getT2();
                        Mono<String> translated = useChat ? ollamaService.translateWithChat(text, lang)
                                                          : ollamaService.translateWithGenerate(text, lang);
                        return translated.map(t -> {
                            int cur = done.incrementAndGet();
                            notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                                .type("translate_page").message("Translated page " + (idx + 1))
                                .total(total).current(cur).build());
                            return new java.util.AbstractMap.SimpleEntry<Long, String>(idx, t);
                        });
                    }, 2)
                    .collectList()
                    .map(list -> {
                        list.sort(java.util.Comparator.comparingLong(java.util.Map.Entry::getKey));
                        java.util.List<String> ordered = new java.util.ArrayList<>();
                        for (var e : list) ordered.add(e.getValue());
                        notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                            .type("overlay").message("Compose PDF").build());
                        byte[] pdf = pdfService.overlayTextOnOriginal(src, ordered);
                        final String langCode = lang.code();
                        String out = (filename == null || filename.isBlank()) ?
                            ("translated-" + langCode + ".pdf") : (filename + ".pdf");
                        notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                            .type("completed").message("PDF ready").build());
                        return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header("Content-Disposition", "attachment; filename=\"" + out + "\"")
                            .body(pdf);
                    });
            }
        } catch (Exception e) {
            notify(progressToken, org.example.com.anjinma.dto.ProgressEvent.builder()
                .type("error").message("Processing failed").build());
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    private void notify(String token, org.example.com.anjinma.dto.ProgressEvent evt) {
        if (token == null || token.isBlank()) return;
        try {
            messagingTemplate.convertAndSend("/sub/translate/" + token, evt);
        } catch (Exception ignored) {}
    }

}
