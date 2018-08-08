package com.westlake.air.pecs.test;

import com.westlake.air.pecs.PecsPlatformApplication;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-08 19:32
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PecsPlatformApplication.class)
@WebAppConfiguration
public class BaseTest {

}
