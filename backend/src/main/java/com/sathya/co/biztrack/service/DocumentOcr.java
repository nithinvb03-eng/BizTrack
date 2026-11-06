package com.sathya.co.biztrack.service;

import java.awt.image.BufferedImage;
import net.sourceforge.tess4j.TesseractException;

public interface DocumentOcr {
    String recognize(BufferedImage image) throws TesseractException;
}
