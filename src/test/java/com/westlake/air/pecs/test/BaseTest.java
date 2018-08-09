package com.westlake.air.pecs.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.pecs.PecsPlatformApplication;
import com.westlake.air.pecs.domain.db.TransitionDO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

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

    @Test
    public void getJsonFromFileTest(){
        try {
            TransitionDO transitionDO = getJsonFromFile("json/transition.json", TransitionDO.class);
            assert transitionDO.getPeptideRef().equals("TC(UniMod:4)VADESAENC(UniMod:4)DK_2");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
