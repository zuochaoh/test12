package com.ruoyi.web.controller.system;


import org.apache.commons.lang3.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
@Component
public class test1 {

    @Value("${ruoyi.profile}")
    private  String tempath;
    @PostConstruct
    public  void testTestWord() throws FileNotFoundException {
        WordUtils wordUtils = new WordUtils();
        //String templatePath = test1.class.getResource("").getPath()+"\\123456.doc";
        //String templatePath = "F:\\xm\\qihuo\\ruoyi-admin\\tem\\123456.doc";
        System.out.println(tempath);
        String templatePath = tempath+"\\123456.doc";
        Map<String, Object> map_data = new HashMap<>();
        map_data.put("project_id", "2019.21");
        map_data.put("project_name", "ZJIC");
        map_data.put("depth", "10.22");
        map_data.put("hole_id", "ZKS12");
        map_data.put("hole_altitude", "100");
        map_data.put("hole_mileage", "23.21");
        map_data.put("endhole_depth", "43");
        ArrayList<Map<String, String>> list_data = new ArrayList<>();
        Map<String, String> temp = new HashMap<>();
        for(int i=0;i<10;i++){
            temp = new HashMap<>();
            temp.put("layer_id", i+"");
            temp.put("start_depth", "start_depth");
            temp.put("end_depth", "end_depth");
            temp.put("geotechnical_name", "geotechnical_name");
            temp.put("geotechnical_description", "geotechnical_description");
            temp.put("sample_id", "sample_id");
            temp.put("sample_depth", "sample_depth");
            list_data.add(temp);
        }
        File file = new File("d:\\test.doc");
        FileOutputStream out = new FileOutputStream(file);
        Daochu.Export2GeotechnicalLayeringTable(map_data, list_data, templatePath, out);
    }

    public void main(String[] args) throws FileNotFoundException {
        //String filePath = test1.class.getResource("").getPath();
        this.testTestWord();
    }
}
