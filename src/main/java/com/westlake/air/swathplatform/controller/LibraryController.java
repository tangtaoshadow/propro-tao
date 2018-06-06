package com.westlake.air.swathplatform.controller;

import com.westlake.air.swathplatform.constants.SuccessMsg;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.traml.*;
import com.westlake.air.swathplatform.service.LibraryService;
import com.westlake.air.swathplatform.service.impl.TraMLServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.xml.transform.Result;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
@Controller
@RequestMapping("library")
public class LibraryController extends BaseController {

    @Autowired
    TraMLServiceImpl traMLServiceImpl;

    @Autowired
    LibraryService libraryService;

    TraML traML;

    @RequestMapping(value = {"/", "/list"}, method = RequestMethod.GET)
    String list(Model model) {
        return "library/list";
    }

    @RequestMapping(value = {"/listJson"})
    @ResponseBody
    List<LibraryDO> listJson() {
        List<LibraryDO> libraries = libraryService.findAll();
        return libraries;
    }

    @RequestMapping(value = "/create")
    String create(Model model) {
        return "library/create";
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    String add(Model model,
                      @RequestParam(value = "name", required = true) String name,
                      @RequestParam(value = "instrument", required = false) String instrument,
                      @RequestParam(value = "description", required = false) String description,
                      @RequestParam(value = "file") MultipartFile file,
                      RedirectAttributes redirectAttributes) {
        LibraryDO library = new LibraryDO();
        library.setName(name);
        library.setInstrument(instrument);
        library.setDescription(description);
        ResultDO resultDO = libraryService.save(library);
        if(resultDO.isSuccess()){
            redirectAttributes.addFlashAttribute(SUCCESS_MSG,SuccessMsg.CREATE_LIBRARY_SUCCESS);
            return "redirect:/library/list";
        }else{
            logger.warn(resultDO.getMsgInfo());
            redirectAttributes.addFlashAttribute(ERROR_MSG,resultDO.getMsgInfo());
            redirectAttributes.addFlashAttribute("library",library);
            return "redirect:/library/create";
        }
    }

    @RequestMapping(value = "/edit/{id}")
    String edit(Model model,@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        if (resultDO.isFailured()){
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/list";
        }else{
            model.addAttribute("library", resultDO.getModel());
            return "/library/edit";
        }
    }

    @RequestMapping(value = "/detail/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        if (resultDO.isSuccess()){
            model.addAttribute("library", resultDO.getModel());
            return "/library/detail";
        }else{
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/list";
        }
    }

    @RequestMapping(value = "/update",method = RequestMethod.POST)
    String update(Model model,
                  @RequestParam(value = "id", required = true) String id,
                  @RequestParam(value = "name") String name,
                  @RequestParam(value = "instrument") String instrument,
                  @RequestParam(value = "description") String description,
                  @RequestParam(value = "file") MultipartFile file,
                  RedirectAttributes redirectAttributes) {
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        if(resultDO.isSuccess()){
            LibraryDO libraryDO = resultDO.getModel();
            libraryDO.setDescription(description);
            libraryDO.setInstrument(instrument);
            ResultDO saveResult = libraryService.save(libraryDO);
            if(saveResult.isSuccess()){
                redirectAttributes.addFlashAttribute(SUCCESS_MSG,SuccessMsg.CREATE_LIBRARY_SUCCESS);
                return "redirect:/library/detail/"+libraryDO.getId();
            }else{
                redirectAttributes.addFlashAttribute(ERROR_MSG, saveResult.getMsgInfo());
                return "redirect:/library/list";
            }
        }else{
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/list";
        }
    }

    @RequestMapping("/load2memory")
    String transTraML(Model model) {
        File file = new File(LibraryController.class.getClassLoader().getResource("data/BreastCancer_s69_osw.TraML").getPath());
        traML = traMLServiceImpl.parse(file);
        model.addAttribute("version", traML.getVersion());
        return "library/list";
    }

    @RequestMapping("test")
    String testTraML() {
        StringBuilder result = new StringBuilder();
        //最长的蛋白质id长度
        String longestProteinId = "";
        //最长的肽段id长度
        String longestPeptideId = "";
        boolean isAllTheIdEqualToCvParam = true;
        boolean isAllTheSequenceEmpty = true;
        //扫描所有的蛋白质
        List<Protein> proteins = traML.getProteinList();
        for (Protein p : proteins) {
            List<CvParam> cvParams = p.getCvParams();
            //测试文件中所有的蛋白质的id和cvParams中对应的id是否完全一致
            for (CvParam cvParam : cvParams) {
                if (cvParam.getName().equals("protein accession")) {
                    if (!p.getId().equals(cvParam.getValue())) {
                        result.append("Protein的Id和CvParam中的Value居然有不相等的!\r\n\r\n");
                        isAllTheIdEqualToCvParam = false;
                    }
                }
            }
            //测试所有蛋白质的Sequence是不是都是空的
            if (!p.getSequence().isEmpty()) {
                isAllTheSequenceEmpty = false;
                result.append("Protein 的Sequence居然有不是空的").append(p.getId()).append(":").append(p.getSequence()).append("\r\n\r\n");
            }
            //拿到Id最长的蛋白质id
            if (p.getId().length() > longestProteinId.length()) {
                longestProteinId = p.getId();
            }
        }
        result.append("最长的蛋白质ID长度为").append(longestProteinId.length()).append("--").append(longestProteinId);
        if (isAllTheIdEqualToCvParam) {
            result.append("所有蛋白质的Id和CvParams中Value的值均相等\r\n\r\n");
        }
        if (isAllTheSequenceEmpty) {
            result.append("所有蛋白质的Sequence均为空\r\n\r\n");
        }
        //扫描所有肽段
        List<Peptide> peptides = traML.getCompoundList().getPeptideList();
        List<String> peptideGroupLabel = new ArrayList<>();
        List<String> peptideChargeState = new ArrayList<>();

        boolean isAllTheIdEqualToUserParam = true;
        for (Peptide peptide : peptides) {
            if (peptide.getId().length() > longestPeptideId.length()) {
                longestPeptideId = peptide.getId();
            }
            List<UserParam> userParams = peptide.getUserParams();
            for (UserParam userParam : userParams) {
                if (!userParam.getName().equals("full_peptide_name")) {
                    isAllTheIdEqualToUserParam = false;
                    result.append("肽段的Sequence和对应的UserParams中的full peptide name的值不相同\r\n\r\n");
                }
            }
            List<CvParam> cvParams = peptide.getCvParams();
            for (CvParam cvParam : cvParams) {
                if (cvParam.getName().equals("peptide group label")) {
                    if (!peptideGroupLabel.contains(cvParam.getValue())) {
                        peptideGroupLabel.add(cvParam.getValue());
                    }
                }
                if (cvParam.getName().equals("charge state")) {
                    if (!peptideChargeState.contains(cvParam.getValue())) {
                        peptideChargeState.add(cvParam.getValue());
                    }
                }
            }
        }
        result.append("最长的肽段ID长度为").append(longestPeptideId.length()).append("--").append(longestPeptideId).append("\r\n\r\n");
        if (isAllTheIdEqualToUserParam) {
            result.append("所有肽段的Sequence和UserParams中Full Peptide Name的值均相等").append("\r\n\r\n");
        }
        result.append("GroupLabel:");
        for (String s : peptideGroupLabel) {
            result.append(s).append(";");
        }
        result.append("\r\n");

        result.append("ChargeState:");
        for (String s : peptideChargeState) {
            result.append(s).append(";");
        }
        result.append("\r\n");

        List<Transition> transitions = traML.getTransitionList();

        result.append("总计有蛋白质").append(proteins.size()).append("个\r\n");
        result.append("总计有肽段").append(peptides.size()).append("个\r\n");
        result.append("总计有Transition").append(transitions.size()).append("个\r\n");

        return result.toString();
    }
}
