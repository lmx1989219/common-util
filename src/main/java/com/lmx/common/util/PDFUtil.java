package com.lmx.common.util;

import com.google.common.collect.Lists;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.util.List;

/**
 * @author : lucas
 * @date : 2020/08/15 10:03
 */
public class PDFUtil {


    public static List<String> readPDF(String fileName) {
        File file = new File(fileName);
        try {
            // 新建一个PDF解析器对象
            PDFParser parser = new PDFParser(new RandomAccessFile(file, "r"));
            // 对PDF文件进行解析
            parser.parse();
            // 获取解析后得到的PDF文档对象
            PDDocument document = parser.getPDDocument();
            int page = document.getNumberOfPages();
            List<String> pages = Lists.newArrayList();
            for (int i = 0; i < page; i++) {
                PDFTextStripper reader = new PDFTextStripper();
                reader.setStartPage(i);
                reader.setEndPage(i);
                pages.add(reader.getText(document));
            }
            return pages;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
