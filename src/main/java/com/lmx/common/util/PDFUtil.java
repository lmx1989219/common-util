package com.lmx.common.util;

import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

/**
 * @author : lucas
 * @date : 2020/08/15 10:03
 */
public class PDFUtil {


    public static String readPDF(String fileName) {
        File file = new File(fileName);
        try {
            // 新建一个PDF解析器对象
            PDFParser parser = new PDFParser(new RandomAccessFile(file, "r"));
            // 对PDF文件进行解析
            parser.parse();
            // 获取解析后得到的PDF文档对象
            PDDocument document = parser.getPDDocument();
            // 新建一个PDF文本剥离器
            PDFTextStripper stripper = new PDFTextStripper();
            // 从PDF文档对象中剥离文本
            return stripper.getText(document);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
