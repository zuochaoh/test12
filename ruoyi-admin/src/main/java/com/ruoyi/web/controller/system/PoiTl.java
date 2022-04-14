package com.ruoyi.web.controller.system;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.data.PictureRenderData;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class PoiTl {

//https://www.cnblogs.com/mark-luo/p/11897840.html

    public static void main(String[] args) throws Exception {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("names", "祁贡策");// 姓名
        data.put("name", "祁贡策");// 性别
        data.put("sex", "女");// 头像 photoPath 为头像的地址
        data.put("photo", new PictureRenderData(127, 185, "C:\\Users\\xuxh\\Desktop\\311.png"));
// 其他属性代码都省略
// 写入word输出

        try {
            //ClassPathResource template = new ClassPathResource("muban/123.docx");
            //String filePath = template.getFile().getPath();
            XWPFTemplate xwpfTemplate = XWPFTemplate.compile("F:\\xm\\qihuo\\ruoyi-admin\\target\\classes\\muban\\123.docx").render(data);
            String docName ="d:\\"+System.currentTimeMillis() + ".docx";
            File targetFile = new File(docName);
            FileOutputStream out = new FileOutputStream(targetFile);
            xwpfTemplate.write(out);
            out.flush();
            out.close();
            xwpfTemplate.close();

//            File file = new File("d:\\testpl.docx");
//            FileOutputStream out = new FileOutputStream(file);
//            document.write(out);
//            out.close();

            // 下载输出到浏览器
            //downFile(request,response,docName,targetFile);
            deleteDir(targetFile.getPath());
        } catch (Exception e) {
            //log.info("文件生成失败："+e.getMessage());
            throw new Exception("文件生成失败："+e.getMessage());
        }
    }

    /**
     * 下载文件到浏览器
     * @param request
     * @param response
     * @param filename 要下载的文件名
     * @param file     需要下载的文件对象
     * @throws IOException
     */
    public static void downFile(HttpServletRequest request, HttpServletResponse response, String filename, File file) throws IOException {
        //  文件存在才下载
        if (file.exists()) {
            OutputStream out = null;
            FileInputStream in = null;
            try {
                // 1.读取要下载的内容
                in = new FileInputStream(file);

                // 2. 告诉浏览器下载的方式以及一些设置
                // 解决文件名乱码问题，获取浏览器类型，转换对应文件名编码格式，IE要求文件名必须是utf-8, firefo要求是iso-8859-1编码
                String agent = request.getHeader("user-agent");
                if (agent.contains("FireFox")) {
                    filename = new String(filename.getBytes("UTF-8"), "iso-8859-1");
                } else {
                    filename = URLEncoder.encode(filename, "UTF-8");
                }
                // 设置下载文件的mineType，告诉浏览器下载文件类型
                String mineType = request.getServletContext().getMimeType(filename);
                response.setContentType(mineType);
                // 设置一个响应头，无论是否被浏览器解析，都下载
                response.setHeader("Content-disposition", "attachment; filename=" + filename);
                // 将要下载的文件内容通过输出流写到浏览器
                out = response.getOutputStream();
                int len = 0;
                byte[] buffer = new byte[1024];
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     *
     * @param filePath 将要删除的文件目录路径
     * @return boolean Returns "true" if all deletions were successful.
     * If a deletion fails, the method stops attempting to
     * delete and returns "false".
     */
    public static boolean deleteDir(String filePath) {
        File dir = new File(filePath);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(filePath + File.separator + children[i]);
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }
}
