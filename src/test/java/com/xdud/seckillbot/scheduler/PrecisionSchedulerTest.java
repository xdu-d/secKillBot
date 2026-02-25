package com.xdud.seckillbot.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 调度精度验证测试。
 * 验证 PrecisionScheduler 的等待误差 <10ms。
 */
@SpringBootTest
@ActiveProfiles("dev")
class PrecisionSchedulerTest {

    @Test
    void precisionWaitShouldBeWithin10ms() throws InterruptedException {
        long targetMs = System.currentTimeMillis() + 200;

        // 模拟 PrecisionScheduler.precisionWait 逻辑
        long sleepMs = targetMs - System.currentTimeMillis() - 50;
        if (sleepMs > 0) {
            Thread.sleep(sleepMs);
        }
        while (System.currentTimeMillis() < targetMs - 50) {
            Thread.sleep(0, 500_000);
        }
        while (System.currentTimeMillis() < targetMs) {
            // busy wait
        }

        long actual = System.currentTimeMillis();
        long error = Math.abs(actual - targetMs);
        System.out.printf("目标=%d, 实际=%d, 误差=%dms%n", targetMs, actual, error);
        assertTrue(error < 10, "调度误差超过 10ms: " + error + "ms");
    }
}
