package com.patterncat;

import static org.junit.Assert.*;

import com.patterncat.bean.ReportBean;
import com.patterncat.service.DemoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RedisdemoApplication.class)
public class RedisdemoApplicationTests {

	@Autowired
	private StringRedisTemplate template;

	@Autowired
	private DemoService demoService;

	@Test
	public void set(){
		String key = "test-add";
		String value = "hello";
		template.opsForValue().set(key,value);
		assertEquals(value,template.opsForValue().get(key));
	}

	@Test
	public void incr(){
		String key = "test-incr";
		template.opsForValue().increment(key, 1);
		assertEquals(template.opsForValue().get(key),"1");
	}

	@Test
	public void should_not_cached(){
		demoService.getMessage("hello");
	}

	@Test
	public void should_cached(){
		demoService.getMessage("patterncat");
	}

	@Test
	public void should_cache_bean(){
		ReportBean b1 = demoService.getReport(1L, "2016-01-30", "hello", "world");
		ReportBean b2 = demoService.getReport(1L, "2016-01-30", "hello", "world");
	}

}
