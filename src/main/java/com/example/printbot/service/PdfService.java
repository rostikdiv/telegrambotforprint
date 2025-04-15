package com.example.printbot.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    public int getPageCount(File file) {
        log.info("Start getPageCount for file: {}", file.getName());
        PDDocument document = null;
        try {
            document = PDDocument.load(file);
            int pageCount = document.getNumberOfPages();
            log.info("File {} has {} pages", file.getName(), pageCount);
            return pageCount;
        } catch (IOException e) {
            log.error("Error while getting page count for file: {}", file.getName(), e);
            return 0;
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    log.error("Error while closing document: {}", file.getName(), e);
                }
            }
            log.info("End getPageCount for file: {}", file.getName());
        }
    }
}