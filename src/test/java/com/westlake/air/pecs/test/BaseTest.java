package com.westlake.air.pecs.test;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.PecsPlatformApplication;
import com.westlake.air.pecs.domain.db.TransitionDO;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-08 19:32
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PecsPlatformApplication.class)
@WebAppConfiguration
public class BaseTest {

    public void init(){
        assert true;
    }


    public TransitionDO getJsonFromFileTest(){
        try {
            return getJsonFromFile("json/transition.json", TransitionDO.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public TransitionDO getJsonFromFileTest1(){
        try {
            return getJsonFromFile("json/transition1.json", TransitionDO.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public TransitionDO getJsonFromFileTest2(){
        try {
            return getJsonFromFile("json/transition2.json", TransitionDO.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public TransitionDO getJsonFromFileTest3(){
        try {
            return getJsonFromFile("json/transition3.json", TransitionDO.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public TransitionDO getJsonFromFileTest4(){
        try {
            return getJsonFromFile("json/transition4.json", TransitionDO.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> T getJsonFromFile(String filePath, Class<T> clazz) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(filePath).getPath());
        FileInputStream fis = new FileInputStream(file);
        int fileLength = fis.available();
        byte[] bytes = new byte[fileLength];
        fis.read(bytes);
        String jsonString = new String(bytes, 0, fileLength);

        return JSON.parseObject(jsonString, clazz);
    }

}
