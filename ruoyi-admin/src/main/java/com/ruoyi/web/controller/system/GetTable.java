package com.ruoyi.web.controller.system;

import com.spire.doc.*;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.fields.DocPicture;
import com.spire.doc.interfaces.ITable;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetTable {
    public static void main(String[] args)throws IOException {
        //加载Word测试文档
        Document doc = new Document();
        doc.loadFromFile("C:\\Users\\xuxh\\Desktop\\123.docx");

        //获取第一节
        Section section = doc.getSections().get(0);

        String text = section.getParagraphs().get(0).getText();

        //获取第一个表格
        ITable table = section.getTables().get(0);

        //创建txt文件（用于写入表格中提取的文本）
        String output = "C:\\Users\\xuxh\\Desktop\\ReadTextFromTable.txt";
        File textfile = new File(output);
        if (textfile.exists())
        {
            textfile.delete();
        }
        textfile.createNewFile();
        FileWriter fw = new FileWriter(textfile, true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(text + "\t");//获取文本内容
        //创建List
        List images = new ArrayList();

        //遍历表格中的行
        for (int i = 0; i < table.getRows().getCount(); i++)
        {
            TableRow row = table.getRows().get(i);
            //遍历每行中的单元格
            for (int j = 0; j < row.getCells().getCount(); j++)
            {
                TableCell cell = row.getCells().get(j);
                //遍历单元格中的段落
                for (int k = 0; k < cell.getParagraphs().getCount(); k++)
                {
                    Paragraph paragraph = cell.getParagraphs().get(k);
                    bw.write(paragraph.getText() + "\t");//获取文本内容

                    //遍历段落中的所有子对象
                    for (int x = 0; x < paragraph.getChildObjects().getCount(); x++)
                    {
                        Object object = paragraph.getChildObjects().get(x);
                        //判定对象是否为图片
                        if (object instanceof DocPicture)
                        {
                            //获取图片
                            DocPicture picture = (DocPicture) object;
                            images.add(picture.getImage());
                        }
                    }
                }
            }
            bw.write("\r\n");//写入内容到txt文件
        }
        bw.flush();
        bw.close();
        fw.close();

        //将图片以PNG文件格式保存
        for (int z = 0; z < images.size(); z++)
        {
            File imagefile = new File(String.format("C:\\Users\\xuxh\\Desktop\\提取的表格图片-%d.png", z));
            ImageIO.write((RenderedImage) images.get(z), "PNG", imagefile);
        }
    }
}
