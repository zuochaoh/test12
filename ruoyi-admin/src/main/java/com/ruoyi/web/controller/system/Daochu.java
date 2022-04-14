package com.ruoyi.web.controller.system;

import com.spire.doc.fields.DocPicture;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;


public class Daochu {
    public static boolean Export2GeotechnicalLayeringTable(Map<String, Object> map_data, ArrayList<Map<String, String>> list_data, String templatePath, OutputStream out) {
        boolean result = false;
        FileInputStream in = null;
        HWPFDocument document = null;
        try {
            in = new FileInputStream(templatePath);
            document = new HWPFDocument(in);
            Range range = document.getRange();
            range.replaceText("${title}", "我是导出的");  //
            range.replaceText("${project_name}", map_data.get("project_name").toString());  //
            range.replaceText("${depth}", map_data.get("depth").toString());  //
            range.replaceText("${hole_id}", map_data.get("hole_id").toString());  //
            range.replaceText("${hole_altitude}", map_data.get("hole_altitude").toString());  //
            range.replaceText("${hole_mileage}", map_data.get("hole_mileage").toString());  //
            range.replaceText("${endhole_depth}", map_data.get("endhole_depth").toString());  //
            //写入表格数据
            //遍历range范围内的table。
            TableIterator tableIter = new TableIterator(range);
            Table table;
            TableRow row;
            while (tableIter.hasNext()) {
                table = tableIter.next();
                int rowNum = table.numRows();
                for (int i=0, j=2; i<list_data.size()&&j<rowNum; i++,j++) {
                    row = table.getRow(j);
                    row.getCell(0).insertBefore(list_data.get(i).get("layer_id"));

                    //添加图片到单元格，并自定义图片大小
                  /*  DocPicture picture = row.getCell(0).getParagraph(0)ParagraappendPicture("C:\\Users\\Administrator\\Desktop\\image1.png");//添加图片到单元格（0，0）
                    picture.setWidth(100f);//设置图片宽度
                    picture.setHeight(100f);//设置图片高度

                    //将图片按原始尺寸添加到单元格
                    File file = new File("C:\\Users\\Administrator\\Desktop\\image2.png");//加载图片
                    FileInputStream inputStream = new FileInputStream(file);
                    BufferedImage image = ImageIO.read(file);
                    int width= image.getWidth();//获取图片尺寸
                    int height = image.getHeight();
                    picture = table.getRows().get(1).getCells().get(1).addParagraph().appendPicture(inputStream);//添加图片到单元格（1，1）
                    picture.setWidth(width);//设置图片宽度为原始宽度
                    picture.setHeight(height);//设置图片高度为原始高度*/


                    row.getCell(1).insertBefore(list_data.get(i).get("start_depth"));
                    row.getCell(2).insertBefore(list_data.get(i).get("end_depth"));
                    row.getCell(3).insertBefore(list_data.get(i).get("geotechnical_name"));
                    /*row.getCell(4).insertBefore(list_data.get(i).get("geotechnical_description"));
                    row.getCell(5).insertBefore(list_data.get(i).get("sample_id"));
                    row.getCell(6).insertBefore(list_data.get(i).get("sample_depth"));*/
                }
            }
            document.write(out);
            out.close();
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

}
