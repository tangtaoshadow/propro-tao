package com.westlake.air.swathplatform.dao;

import com.alibaba.fastjson.JSONObject;
import com.westlake.air.swathplatform.parser.model.chemistry.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-11 09:40
 */
@Service
public class ElementsDAO {

    public final Logger logger = LoggerFactory.getLogger(ElementsDAO.class);

    String elementsStr = "";
    List<Element> elementList = new ArrayList<>();
    HashMap<String, Element> symbolElementsMap = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            File file = new File(getClass().getClassLoader().getResource("dbfile/elementsWithWeight.json").getPath());
            FileInputStream fis = new FileInputStream(file);
            int fileLength = fis.available();
            byte[] bytes = new byte[fileLength];
            fis.read(bytes);
            elementsStr = new String(bytes, 0, fileLength);
            elementList = JSONObject.parseArray(elementsStr, Element.class);
            for (Element element : elementList) {
                countWeight(element);
                symbolElementsMap.put(element.getSymbol(), element);
            }
            System.out.println("Init Element Database file success!!!");
        } catch (IOException e) {
            System.out.println("Init Element Database file failed!!!");
            e.printStackTrace();
        }
    }

    public Element getElementBySymbol(String elementSymbol) {
        return getElementsMap().get(elementSymbol);
    }

    public HashMap<String, Element> getElementsMap() {
        return symbolElementsMap;
    }

    public List<Element> getElementList() {
        return elementList;
    }

    public String getJson() {
        return elementsStr;
    }

    /**
     * 计算平均质量和Mono质量,根据OpenMS源代码中的逻辑,平均质量为"求和(分布率*相对原子质量)",Mono质量为取相对原子质量最小的一个
     *
     * @param element
     */
    private void countWeight(Element element) {
        Double smallestWeight = null;
        Double biggestAbundance = null;
        Double biggestAbundanceWeight = 0d;
        double averageWeight = 0;
        for (String iso : element.getIsotopes()) {
            String[] isoArray = iso.split(":");
            double abundance = Double.parseDouble(isoArray[0]);
            double weight = Double.parseDouble(isoArray[1]);

            if (smallestWeight == null) {
                smallestWeight = weight;
            } else if (smallestWeight > weight) {
                smallestWeight = weight;
            }

            if (biggestAbundance == null){
                biggestAbundance = abundance;
                biggestAbundanceWeight = weight;
            }else if(abundance > biggestAbundance){
                biggestAbundance = abundance;
                biggestAbundanceWeight = weight;
            }

            averageWeight += abundance * weight / 100;
        }

        element.setMaxAbundanceWeight(biggestAbundanceWeight);
        element.setMonoWeight(smallestWeight);
        element.setAverageWeight(averageWeight);

    }

    /**
     * check if the database file is right
     */
    private void check() {
        //check if all the isotopes' abundance summary is 100
        for (Element element : elementList) {
            List<String> isotopes = element.getIsotopes();
            double count = 0;
            for (String isotope : isotopes) {
                String[] isotopeArray = isotope.split(":");
                count += Double.parseDouble(isotopeArray[0]);
            }
            if (count != 100) {
                System.out.println("同位素分布不完整,得出的分布总和为:" + count + ";元素名称为:" + element.getName());
            }
        }
    }
}
