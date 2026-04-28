package com.example.myapplication.testing;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 测试工具：把异步 LiveData 转成同步等待结果。
 * Instrumentation 测试里经常需要“等到某个 LiveData 发出值再断言”。
 */
public final class LiveDataTestUtil {
    // 条件接口用于等待 LiveData 发出的值满足某个业务条件。
    public interface Condition<T> {
        boolean test(T value);
    }

    private LiveDataTestUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T getOrAwaitValue(@NonNull LiveData<T> liveData) throws Exception {
        // CountDownLatch 用来阻塞测试线程，直到 LiveData 回调 onChanged。
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);

        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T t) {
                data[0] = t;
                // 收到第一个值后立即解除等待，并移除 observer，避免测试泄漏。
                latch.countDown();
                liveData.removeObserver(this);
            }
        };

        liveData.observeForever(observer);

        if (!latch.await(3, TimeUnit.SECONDS)) {
            // 超时后必须移除 observer，否则后续测试可能受到影响。
            liveData.removeObserver(observer);
            throw new TimeoutException("LiveData value was never set.");
        }

        return (T) data[0];
    }

    @SuppressWarnings("unchecked")
    public static <T> T getOrAwaitValue(
            @NonNull LiveData<T> liveData,
            @NonNull Condition<T> condition,
            long timeoutSeconds
    ) throws Exception {
        // 带条件版本适合等待“收藏状态变为 true/false”这类异步结果。
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);

        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T t) {
                data[0] = t;
                if (condition.test(t)) {
                    latch.countDown();
                    liveData.removeObserver(this);
                }
            }
        };

        liveData.observeForever(observer);

        if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            liveData.removeObserver(observer);
            throw new TimeoutException("LiveData value did not satisfy condition.");
        }

        return (T) data[0];
    }
}
