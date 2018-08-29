package com.westlake.air.pecs.async;

import com.westlake.air.pecs.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-29 20:28
 */
public class BaseTask {

    @Autowired
    TaskService taskService;
}
