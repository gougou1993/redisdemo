package com.patterncat;

import static org.junit.Assert.*;

import com.patterncat.bean.ReportBean;
import com.patterncat.service.DemoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RedisdemoApplication.class)
public class RedisdemoApplicationTests {

    @Autowired
    private StringRedisTemplate template;

    @Autowired
    private DemoService demoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void set() {
        String key = "test-add";
        String value = "hello";
        template.opsForValue().set(key, value);
        assertEquals(value, template.opsForValue().get(key));
    }

    @Test
    public void incrInt(){
        String key = "test-incr3";
        redisTemplate.opsForValue().increment(key, 1);
        assertTrue((Integer) redisTemplate.opsForValue().get(key) == 2);
    }

    @Test
      public void cas() throws InterruptedException, ExecutionException {
        String key = "test-cas-1";
        ValueOperations<String, String> strOps = redisTemplate.opsForValue();
        strOps.set(key, "hello");
        ExecutorService pool  = Executors.newCachedThreadPool();
        List<Callable<Object>> tasks = new ArrayList<>();
        for(int i=0;i<5;i++){
            final int idx = i;
            tasks.add(new Callable() {
                @Override
                public Object call() throws Exception {
                    return redisTemplate.execute(new SessionCallback() {
                        @Override
                        public Object execute(RedisOperations operations) throws DataAccessException {
                            operations.watch(key);
                            String origin = (String) operations.opsForValue().get(key);
                            operations.multi();
                            operations.opsForValue().set(key, origin + idx);
                            Object rs = operations.exec();
                            System.out.println("set:"+origin+idx+" rs:"+rs);
                            return rs;
                        }
                    });
                }
            });
        }
        List<Future<Object>> futures = pool.invokeAll(tasks);
        for(Future<Object> f:futures){
            System.out.println(f.get());
        }
        pool.shutdown();
        pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void wrong_cas_should_execute_in_session_callback() throws InterruptedException, ExecutionException {
        String key = "test-cas-wrong";
        ValueOperations<String, String> strOps = redisTemplate.opsForValue();
        strOps.set(key, "hello");
        ExecutorService pool  = Executors.newCachedThreadPool();
        List<Callable<Object>> tasks = new ArrayList<>();
        for(int i=0;i<5;i++){
            final int idx = i;
            tasks.add(new Callable() {
                @Override
                public Object call() throws Exception {
                    redisTemplate.watch(key);
                    String origin = (String) redisTemplate.opsForValue().get(key);
                    redisTemplate.multi();
                    redisTemplate.opsForValue().set(key, origin + idx);
                    Object rs = redisTemplate.exec();
                    System.out.println("set:"+origin+idx+" rs:"+rs);
                    return rs;
                }
            });
        }
        List<Future<Object>> futures = pool.invokeAll(tasks);
        for(Future<Object> f:futures){
            System.out.println(f.get());
        }
        pool.shutdown();
        pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 处理非string的情况
     * 更加灵活
     */
    @Test
    public void fixType(){
//        ValueOperations<String, Integer> intOps = redisTemplate.opsForValue();
//        intOps.increment("hello",20);
//        assertTrue(intOps.get("hello") == 20);

        HashOperations<String, String, Integer> hashOps = redisTemplate.opsForHash();
        hashOps.putIfAbsent("test-hash-user-score","patterncat",100);
        hashOps.putIfAbsent("test-hash-user-score","xixicat",90);
        assertTrue(hashOps.get("test-hash-user-score", "patterncat") == 100);
        assertTrue(hashOps.get("test-hash-user-score", "xixicat") == 90);

        ListOperations<String, Integer> listOps = redisTemplate.opsForList();
        SetOperations<String, Integer> setOps = redisTemplate.opsForSet();
        ZSetOperations<String, Integer> zsetOps = redisTemplate.opsForZSet();

    }

    @Test
    public void incr() {
        String key = "test-incr1";
        template.opsForValue().increment(key, 1);
        assertEquals(template.opsForValue().get(key), "1");
    }

    @Test
    public void hset() {
        String key = "userHit";
        int uid = 243546657;
        template.boundHashOps(key).putIfAbsent(uid, 1);
        System.out.println(template.boundHashOps(key).get(uid));
    }

    @Test
    public void should_not_cached() {
        demoService.getMessage("hello");
    }

    @Test
    public void should_cached() {
        demoService.getMessage("patterncat");
    }

    @Test
    public void should_cache_bean() {
        ReportBean b1 = demoService.getReport(1L, "2016-01-30", "hello", "world");
        ReportBean b2 = demoService.getReport(1L, "2016-01-30", "hello", "world");
    }

    @Test
    public void should_in_transaction() {
        //execute a transaction
        List<Object> txResults = template.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                operations.opsForSet().add("k1", "value1");
                operations.opsForSet().add("k2", "value2");
                operations.discard();
                // This will contain the results of all ops in the transaction
                return operations.exec();
            }
        });
        System.out.println("Number of items added to set: " + Arrays.deepToString(txResults.toArray()));
    }

}
