package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.propro.domain.db.FragmentInfo;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.PeptideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Created by Nico Wang
 * Time: 2019-07-08 16:42
 */
@Controller
@RequestMapping("patch")
public class PatchController {

    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;

    @RequestMapping("addToLib")
    @ResponseBody
    String addToLibrary(){
        String fromLibId = "5d22f3bce0073c12f081d5bca";
        String toLibId = "5d2220d9e0073c44c41b10cda";
        String[] selectedPeps = new String[]{"INNEEIVSDPIYIEVQGLPHFTK_3","IQIEAIPLALQGR_3","DYAVSTVPVADGLHLK_2","GLTEVTQSLK_2","DMYSFLEDMGLK_3","SPEQPPEGSSTPAEPEPSGGGPSAEAAPDTTADPAIAASDPATK_4",
                "ADVTPADFSEWSK_2","GVFTKPIDSSSQPQQQFPK_2","GREEINFVEIK_2","MFEIVFEDPKIPGEK_2","NYTQNIDTLEQVAGIQR_2","DGLDAASYYAPVR_2","LTLLHYDPVVK_2","LAAAGPPGPFR_2","TPVITGAPYEYR_2",
                "QEIIDWPGTEGR_2","TLGDQLSLLLGAR_3","TPEDAQAVINAYTEINK_2","VEQVLSLEPQHELK_2","GTFIIDPAAVIR_2","AADFIDQALAQK_3","TPVISGGPYEYR_2","YILAGVENSK_2","THLMSESEWR_3","LKPQYLEELPGQLK_2",
                "LFLQFGAQGSPFLK_2","GAGSSEPVTGLDAK_2","VADPDHDHTGFLTEYVATR_3","VEATFGVDESNAK_2","DIILTVK_2","HLYFWGYSEAAK_2","GTFIIDPGGVIR_2","LGGNEQVTR_2"};

        LibraryDO toLibraryDO = libraryService.getById(toLibId);

        List<String> remainPep = new ArrayList<>();
        for (String pep: selectedPeps){
            PeptideDO peptideDO = peptideService.getByLibraryIdAndPeptideRefAndIsDecoy(fromLibId, pep, false);
            if (peptideDO != null){
                peptideDO.setLibraryId(toLibId);
                peptideDO.setLibraryName(toLibraryDO.getName());
                peptideDO.setId(null);
                peptideService.insert(peptideDO);
            }else {
                remainPep.add(pep);
            }
        }

        libraryService.countAndUpdateForLibrary(toLibraryDO);
        System.out.println("Still remain: " + JSON.toJSON(remainPep));
        return null;
    }

    @RequestMapping("top")
    @ResponseBody
    String addNewTopLibrary(){
        int top = 6;
        String libraryId = "5d255033e0073c51c82c2d9a0";
        LibraryDO oldLibrary = libraryService.getById(libraryId);
        LibraryDO newLibrary = new LibraryDO();
        newLibrary.setName(oldLibrary.getName() + "_top" + top);
        newLibrary.setType(oldLibrary.getType());
        newLibrary.setCreator(oldLibrary.getCreator());
        libraryService.insert(newLibrary);
        newLibrary = libraryService.getByName(newLibrary.getName());
        List<PeptideDO> peptideDOList = peptideService.getAllByLibraryId(libraryId);
        for (PeptideDO peptideDO: peptideDOList){
            HashMap<String, FragmentInfo> fragInfoMap = peptideDO.getFragmentMap();
            List<Map.Entry<String,FragmentInfo>> fragInfoList = new ArrayList<>(fragInfoMap.entrySet());
            fragInfoList.sort(new Comparator<Map.Entry<String, FragmentInfo>>() {
                @Override
                public int compare(Map.Entry<String, FragmentInfo> o1, Map.Entry<String, FragmentInfo> o2) {
                    return o2.getValue().getIntensity().compareTo(o1.getValue().getIntensity());
                }
            });
            HashMap<String, FragmentInfo> topFragMap = new HashMap<>();
            for (int i = 0; i < top; i++){
                topFragMap.put(fragInfoList.get(i).getKey(), fragInfoList.get(i).getValue());
            }
            peptideDO.setId(null);
            peptideDO.setLibraryName(newLibrary.getName());
            peptideDO.setLibraryId(newLibrary.getId());
            peptideDO.setFragmentMap(topFragMap);
        }
        peptideService.insertAll(peptideDOList, false);
        libraryService.countAndUpdateForLibrary(newLibrary);
        return null;
    }

//    @RequestMapping("deleteNull")
//    @ResponseBody
//    String deleteNullInDB(){
//        peptideService.deleteAllByLibraryId(null);
//        return null;
//    }


}
