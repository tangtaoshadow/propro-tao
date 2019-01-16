package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.algorithm.FragmentFactory;
import com.westlake.air.pecs.decoy.generator.ShuffleGenerator;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.peptide.FragmentResult;
import com.westlake.air.pecs.domain.bean.analyse.MzResult;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.PeptideDO;
import com.westlake.air.pecs.domain.query.PeptideQuery;
import com.westlake.air.pecs.service.PeptideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.westlake.air.pecs.constants.Constants.MAX_INSERT_RECORD_FOR_PEPTIDE;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-19 16:03
 */
@Controller
@RequestMapping("decoy")
public class DecoyController extends BaseController {

    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    ShuffleGenerator shuffleGenerator;
    @Autowired
    PeptideService peptideService;

    @RequestMapping(value = "/overview/{id}")
    String overview(Model model, @PathVariable("id") String id) {
        FragmentResult result = fragmentFactory.decoyOverview(id);

        model.addAttribute(SUCCESS_MSG, result.getMsgInfo());
        model.addAttribute("overlapList", result.getOverlapList());
        model.addAttribute("decoyList", result.getDecoyList());
        model.addAttribute("targetList", result.getTargetList());
        return "decoy/overview";
    }

    @RequestMapping(value = "/check")
    String check(Model model,
                 @RequestParam(value = "id", required = true) String id,
                 @RequestParam(value = "isDecoy", required = false) boolean isDecoy) {
        List<MzResult> result = fragmentFactory.check(id, 0.1, isDecoy);
        model.addAttribute("resultList", result.size() > 100 ? result.subList(0, 100) : result);
        return "decoy/check";
    }

    @RequestMapping(value = "/manager")
    String manager(Model model) {
        model.addAttribute("libraries", getLibraryList(LibraryDO.TYPE_STANDARD));
        return "decoy/manager";
    }

    @RequestMapping(value = "/delete")
    String delete(Model model,
                        @RequestParam(value = "id", required = true) String id) {
        peptideService.deleteAllDecoyByLibraryId(id);
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        LibraryDO library = resultDO.getModel();
        libraryService.countAndUpdateForLibrary(library);
        return "redirect:/library/detail/" + id;
    }

    @RequestMapping(value = "/generate")
    String generate(Model model,
                    @RequestParam(value = "id", required = true) String id) {

        logger.info("正在删除原有伪肽段");
        //删除原有的伪肽段
        peptideService.deleteAllDecoyByLibraryId(id);
        logger.info("原有伪肽段删除完毕");
        //计算原始肽段数目
        PeptideQuery query = new PeptideQuery();
        query.setIsDecoy(false);
        query.setLibraryId(id);
        long totalCount = peptideService.count(query);
        int totalPage = (int) (totalCount / MAX_INSERT_RECORD_FOR_PEPTIDE) + 1;
        query.setPageSize(MAX_INSERT_RECORD_FOR_PEPTIDE);
        int countForInsert = 0;
        for (int i = 1; i <= totalPage; i++) {
            query.setPageNo(i);
            ResultDO<List<PeptideDO>> resultDO = peptideService.getList(query);
            List<PeptideDO> list = shuffleGenerator.generate(resultDO.getModel());
            ResultDO resultTmp = peptideService.insertAll(list, false);
            if (resultTmp.isSuccess()) {
                countForInsert += list.size();
                logger.info("插入新生成伪肽段" + countForInsert + "条");
            }
        }

        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        LibraryDO library = resultDO.getModel();
        libraryService.countAndUpdateForLibrary(library);

        return "redirect:/library/detail/" + id;
    }
}
