package com.lekhanditsove;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.google.zxing.WriterException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.text.PDFTextStripper;


public class VfdIntegrator{
    public static void main(String[] args) throws IOException, WriterException, InterruptedException {

        // -- Missing Function to Watch for Directory
        VfdIntegrator vfdIntegrator = new VfdIntegrator();

        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path directory = Path.of("C:/samples/input/");
        WatchKey watchKey = directory.register(watchService, ENTRY_CREATE);

        while(true){
            for (WatchEvent<?> event : watchKey.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path fileName = pathEvent.context();

                if(kind == StandardWatchEventKinds.ENTRY_CREATE){

                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        String inputFilePath = "C:/samples/input/"+fileName;
                        File file = new File(inputFilePath);
                        PDDocument doc = PDDocument.load(file);

                        String invoiceNumber = vfdIntegrator.invoiceNumberExtractor(doc);
                        // -- Missing Function to Query Invoice Info from CVBS

                        String verificationCode = vfdIntegrator.generateReceiptVerificationCode(invoiceNumber);
                        // -- Missing Function to Authenticate and Send Invoice Info to TRA

                        PDPage page = doc.getPage(0);
                        BufferedImage qrCodeImage = vfdIntegrator.qrCodeGenerator(verificationCode);
                        vfdIntegrator.printQRCodeAndVerificationCode(doc, page, qrCodeImage);
                        String outPutFilePath = "C:/samples/output/" + fileName;
                        doc.save(new File(outPutFilePath));
                        doc.close();
                        Files.deleteIfExists(file.toPath());
                }
            }
            boolean valid = watchKey.reset();
            if (!valid) {
                break;
            }

        }

    }

    public String invoiceNumberExtractor(PDDocument doc) throws IOException {
        // Read text from the doc
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(doc);

        // Get the first Index of Invoice
        int firstIndex = text.indexOf("Invoice:");

        // Get the Invoice number
        String invoiceNumber = text.substring(firstIndex + 9,firstIndex + 18);
        // Code to extract  number;

       return invoiceNumber;
    }

    //public void queryInvoiceInfoFromCVBS(){}; -- Not Implemented for now.

    public String generateReceiptVerificationCode(String invoiceNumber){

        // Code to generate Verification Code
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String verificationCode = invoiceNumber + ""+ dtf.format(now);
        return verificationCode;
    }

   // public void sendReceiptInfoToTRA(){}; -- Not Implemented for now.

   public void printQRCodeAndVerificationCode(PDDocument doc, PDPage page, BufferedImage bImage) throws IOException {

       //PDFont font = PDType1Font.HELVETICA;
       PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true);
       PDImageXObject image = JPEGFactory.createFromImage(doc, bImage);
       contentStream.drawImage(image, 170, 502, 75, 75);
       contentStream.close();
   }
    public BufferedImage qrCodeGenerator(String verificationCode) throws UnsupportedEncodingException, WriterException {

        //Encoding charset to be used
        String charset = "UTF-8";
        Map<EncodeHintType, ErrorCorrectionLevel> hashMap = new HashMap<EncodeHintType, ErrorCorrectionLevel>();

        //generates QR code with Low level(L) error correction capability
        hashMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

        BitMatrix matrix = new MultiFormatWriter().encode(
                new String(verificationCode.getBytes("UTF-8"), "UTF-8"),
                BarcodeFormat.QR_CODE, 100, 100, hashMap);

        MatrixToImageConfig config = new MatrixToImageConfig(0xFF000001, 0xFFFFFFFF);
        BufferedImage bImage = MatrixToImageWriter.toBufferedImage(matrix, config);

        return bImage;
    }


}
