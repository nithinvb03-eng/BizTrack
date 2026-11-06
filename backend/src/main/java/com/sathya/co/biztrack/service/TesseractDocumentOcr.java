package com.sathya.co.biztrack.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import net.sourceforge.tess4j.util.LoadLibs;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TesseractDocumentOcr implements DocumentOcr {

    private final String dataPath;

    public TesseractDocumentOcr(@Value("${app.receipts.tesseract-data-path:tessdata}") String dataPath) {
        this.dataPath = dataPath;
    }

    @Override
    public String recognize(BufferedImage image) throws TesseractException {
        ITesseract tesseract = new Tesseract();
        File tessDataDirectory = dataPath.isBlank()
                ? LoadLibs.extractTessResources("tessdata")
                : new File(dataPath);
        if (tessDataDirectory == null || !new File(tessDataDirectory, "eng.traineddata").isFile()) {
            throw new TesseractException(
                    "English Tesseract language data was not found. Configure app.receipts.tesseract-data-path.");
        }
        tesseract.setDatapath(tessDataDirectory.getAbsolutePath());
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT);
        return tesseract.doOCR(preprocess(image));
    }

    private BufferedImage preprocess(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = width < 1200 ? Math.min(2.0, 1200.0 / width) : 1.0;
        BufferedImage prepared = new BufferedImage(
                (int) Math.ceil(width * scale),
                (int) Math.ceil(height * scale),
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = prepared.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, prepared.getWidth(), prepared.getHeight(), null);
        graphics.dispose();
        return prepared;
    }
}
