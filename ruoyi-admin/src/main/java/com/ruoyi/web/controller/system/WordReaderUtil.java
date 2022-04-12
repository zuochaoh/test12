package com.ruoyi.web.controller.system;

import com.spire.doc.fields.DocPicture;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 读取word文档中表格数据，支持doc、docx */
public class WordReaderUtil {

    public static void main(String[] args) {
        String url = "C:\\Users\\Desktop\\word\\120501.docx";
        //String url1 = "C:\\Users\\xuxh\\Desktop\\现场描述.doc";
        String url1 = "C:\\Users\\xuxh\\Desktop\\123.doc";
        List<String> list = tableInWord(url1, 1);
        System.out.println(list);
    }

    /**
     * 读取文档中表格
     * @param filePath 文档路径
     * @param orderNum 设置需要读取的第几个表格
     */
    public static List<String> tableInWord(String filePath,Integer orderNum){
        try{
            FileInputStream in = new FileInputStream(filePath);//载入文档
            // 处理docx格式 即office2007以后版本
            if(filePath.toLowerCase().endsWith("docx")){
                //word 2007 图片不会被读取， 表格中的数据会被放在字符串的最后
                org.apache.poi.xwpf.usermodel.XWPFDocument xwpf = new org.apache.poi.xwpf.usermodel.XWPFDocument(in);//得到word文档的信息
                List<XWPFParagraph> paras = xwpf.getParagraphs();
                for (XWPFParagraph para : paras) {
                    //对齐方式 alignment 枚举值
                    ParagraphAlignment alignment = para.getAlignment();
                    //获取段落所有的文本对象
                    List<XWPFRun> runs = para.getRuns();
                    //文本的颜色 color
                    //String color = runs.get(0).getColor();
                    //文本 大小 fontSize
                    //int fontSize = runs.get(0).getFontSize();
                    //文本 类型 fontFamily
                    //String fontFamily = runs.get(0).getFontFamily();
                    //输出内容
                    System.out.println("-------"+para.getText());
                }
                Iterator<XWPFTable> itpre = xwpf.getTablesIterator();//得到word中的表格
                int total = 0;
                while (itpre.hasNext()) {
                    itpre.next();
                    total += 1;
                }
                Iterator<XWPFTable> it = xwpf.getTablesIterator();//得到word中的表格
                // 设置需要读取的表格  set是设置需要读取的第几个表格，total是文件中表格的总数
                int set = orderNum;
                int num = set;
                // 过滤前面不需要的表格
                for (int i = 0; i < set-1; i++) {
                    it.hasNext();
                    it.next();
                }
                List<String> tableList = new ArrayList<>();
                while(it.hasNext()){
                    XWPFTable table = it.next();
                    System.out.println("这是第" + num + "个表的数据");
                    List<XWPFTableRow> rows = table.getRows();
                    //读取每一行数据
                    for (int i = 0; i < rows.size(); i++) {
                        XWPFTableRow row = rows.get(i);
                        //读取每一列数据
                        List<XWPFTableCell> cells = row.getTableCells();
                        List<String> rowList = new ArrayList<>();
                        for (int j = 0; j < cells.size(); j++) {
                            XWPFTableCell cell = cells.get(j);
                            rowList.add(cell.getText());
                            //输出当前的单元格的数据
                            System.out.print(cell.getText()+"["+i+","+j+"]" + "\t");
                        }
                        tableList.addAll(rowList);
                        System.out.println();
                    }
                    // 过滤多余的表格
                    while (num < total) {
                        it.hasNext();
                        it.next();
                        num += 1;
                    }
                }
                return tableList;
            }else{
                // 处理doc格式 即office2003版本
                POIFSFileSystem pfs = new POIFSFileSystem(in);
                HWPFDocument hwpf = new HWPFDocument(pfs);
                Range range = hwpf.getRange();//得到文档的读取范围

                for (int i = 0; i < range.numParagraphs(); i++) {
                    Paragraph p = range.getParagraph(i);
                    // check if style index is greater than total number of styles
                    int numStyles = hwpf.getStyleSheet().numStyles();
                    int styleIndex = p.getStyleIndex();
                    String contexts = p.text();
                    System.out.println(i+"-------"+contexts); // 标题+内容
                }



                TableIterator itpre = new TableIterator(range);;//得到word中的表格
                int total = 0;
                while (itpre.hasNext()) {
                    itpre.next();
                    total += 1;
                }
                TableIterator it = new TableIterator(range);
                // 迭代文档中的表格
                // 如果有多个表格只读取需要的一个 set是设置需要读取的第几个表格，total是文件中表格的总数
                int set = orderNum;
                int num = set;
                for (int i = 0; i < set-1; i++) {
                    it.hasNext();
                    it.next();
                }
                List<String> tableList = new ArrayList<>();
                //创建List
                List images = new ArrayList();

                while (it.hasNext()) {
                    Table tb = (Table) it.next();
                    System.out.println("这是第" + num + "个表的数据");
                    //迭代行，默认从0开始,可以依据需要设置i的值,改变起始行数，也可设置读取到那行，只需修改循环的判断条件即可
                    for (int i = 0; i < tb.numRows(); i++) {
                        List<String> rowList = new ArrayList<>();
                        TableRow tr = tb.getRow(i);
                        //迭代列，默认从0开始
                        for (int j = 0; j < tr.numCells(); j++) {
                            TableCell td = tr.getCell(j);//取得单元格
                            //取得单元格的内容
                            for(int k = 0; k < td.numParagraphs(); k++){
                                Paragraph para = td.getParagraph(k);
                                String s = para.text();
                                //去除后面的特殊符号
                                if(null != s && !"".equals(s)){
                                    s = s.substring(0, s.length()-1);
                                }
                                rowList.add(s);
                                System.out.print(s+"["+i+","+j+"]" + "\t");
                            }
                        }
                        tableList.addAll(rowList);
                        System.out.println();
                    }
                    // 过滤多余的表格
                    while (num < total) {
                        it.hasNext();
                        it.next();
                        num += 1;
                    }
                }

                return tableList;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
}