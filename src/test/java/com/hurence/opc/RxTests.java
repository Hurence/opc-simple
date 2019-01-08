/*
 *  Copyright (C) 2019 Hurence (support@hurence.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hurence.opc;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Timed;
import io.reactivex.subscribers.TestSubscriber;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Some RX perf tests.
 */
@Ignore
public class RxTests {

    @Test()
    public void hotFlowableBackpressureUnhandledTest() throws InterruptedException {
        final int count = 10000;
        Flowable<Long> test = Flowable.intervalRange(1, count, 0, 1, TimeUnit.MILLISECONDS)
                .publish().autoConnect();
        //first one is slow
        TestSubscriber<Long> slowSubscriber = new TestSubscriber<>();
        TestSubscriber<Long> fastSubscriber = new TestSubscriber<>();
        test.filter(aLong -> aLong % 2 == 0)
                .flatMap(aLong -> Flowable.just(aLong).delay(1, TimeUnit.SECONDS))
                .doOnError(Throwable::printStackTrace)
                .subscribe(slowSubscriber);
        //second is fast
        test.filter(aLong -> aLong % 2 == 1)
                .doOnError(Throwable::printStackTrace)
                .subscribe(fastSubscriber);
        fastSubscriber.await();
        slowSubscriber.await();

        fastSubscriber.assertTerminated();
        fastSubscriber.assertError(MissingBackpressureException.class);
        slowSubscriber.assertTerminated();
        slowSubscriber.assertError(MissingBackpressureException.class);

    }

    @Test()
    public void hotFlowableBackpressureHandledTest() throws InterruptedException {
        final int count = 10000;
        Flowable<Long> test = Flowable.intervalRange(1, count, 0, 1, TimeUnit.NANOSECONDS)
                .publish().autoConnect(2);
        //first one is slow
        TestSubscriber<Long> slowSubscriber = new TestSubscriber<>();
        TestSubscriber<Long> fastSubscriber = new TestSubscriber<>();
        test
                .onBackpressureDrop(aLong -> System.err.println("Element " + aLong + " dropped from slow consumer"))
                .filter(aLong -> aLong % 2 == 0)
                .flatMap(aLong -> Flowable.just(aLong).delay(1, TimeUnit.SECONDS))
                .doOnError(Throwable::printStackTrace)
                .subscribe(slowSubscriber);
        //second is fast
        test
                .onBackpressureDrop(aLong -> System.err.println("Element " + aLong + " dropped from fast consumer"))
                .filter(aLong -> aLong % 2 == 1)
                .doOnError(Throwable::printStackTrace)
                .subscribe(fastSubscriber);
        fastSubscriber.await();
        slowSubscriber.await();

        fastSubscriber.assertTerminated();
        fastSubscriber.assertNoErrors();
        fastSubscriber.assertValueCount(count / 2);
        slowSubscriber.assertNoErrors();
        slowSubscriber.assertTerminated();

    }

    @Test
    public void testDecimate() throws InterruptedException {
        final int count = 1_000;
        Flowable<Long> test = Flowable.intervalRange(1, count, 0, 1, TimeUnit.MILLISECONDS)
                .publish().autoConnect(1);
        TestSubscriber<Long> subscriber = new TestSubscriber<>();

        test
                .onBackpressureDrop()
                .throttleLatest(100, TimeUnit.MILLISECONDS)
                //.doOnNext(System.out::println)
                .subscribe(subscriber);

        subscriber.await();
        subscriber.assertTerminated();
        subscriber.assertNoErrors();
        subscriber.assertValueCount(10);
    }

    @Test
    public void testStormOfResamples() throws Exception {

        final FlowableProcessor<Integer> rateCounter = PublishProcessor.create();

        rateCounter.window(1, TimeUnit.SECONDS)
                .flatMap(integerFlowable -> integerFlowable.scan((a, b) -> a + b))
                .subscribe(r -> System.out.println("Current rate: " + r + " messages/second"));

        final Callable<Double> runnable = () -> {
            final int count = 1_000;
            Flowable<Timed<Long>> test = Flowable.intervalRange(1, count, 0, 10, TimeUnit.MILLISECONDS)
                    .timestamp()
                    .publish().autoConnect(100);
            TestSubscriber<List<Timed<Long>>> subscriber = new TestSubscriber<>();
            BehaviorProcessor<Timed<Long>> processor = BehaviorProcessor.create();

            Flowable.interval(1, TimeUnit.MILLISECONDS)
                    .takeWhile(aLong -> !processor.hasComplete())
                    .withLatestFrom(processor, (aLong, longTimed) -> longTimed)
                    .window(1, TimeUnit.SECONDS)
                    .flatMap(timedFlowable -> timedFlowable.toList().toFlowable())
                    .doOnNext(timeds -> rateCounter.onNext(timeds.size()))
                    .subscribe(subscriber);

            for (int i = 0; i < 100; i++) {
                test.subscribe(processor);
            }

            try {
                subscriber.await();
            } catch (Exception e) {

            }
            subscriber.assertTerminated();
            subscriber.assertNoErrors();
            //System.out.println(subscriber.valueCount());
            return subscriber.values().stream().flatMapToLong(timeds -> timeds.stream().mapToLong(o -> o.value()))
                    .average().getAsDouble();
        };

        ExecutorService svc = Executors.newCachedThreadPool();
        List<Future<Double>> futures = IntStream.range(0, 1_000).mapToObj(i -> svc.submit(runnable))
                .collect(Collectors.toList());

        svc.shutdown();
        svc.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        for (Future<Double> f : futures) {
            Assert.assertTrue(f.get() >= 10.0);
        }
    }

    @Test
    public void testTonsOfPubSub() throws Exception {

        final int publishers = 10_000;
        final int subscribers = 100;
        final PublishProcessor<Integer> publisher = PublishProcessor.create();
        final PublishProcessor<Integer> receiver = PublishProcessor.create();


        final int msgPerPublisherPerSecond = 1_000_000;
        Flowable flowable1 = Flowable.interval(1, TimeUnit.SECONDS)
                .flatMap(ignored -> Flowable.range(1, msgPerPublisherPerSecond))
                .onBackpressureDrop()
                .publish()
                .autoConnect();
        for (int i = 0; i < publishers; i++) {
            flowable1.subscribe(publisher);
        }


        for (int i = 0; i < subscribers; i++) {
            publisher

                    //.doOnNext(System.err::println)
                    .subscribe(receiver);
        }


        final Disposable disposable = receiver
                .window(1, TimeUnit.SECONDS)
                .flatMap(longFlowable -> longFlowable.count().toFlowable())
                .take(20, TimeUnit.SECONDS)
                .doOnNext(System.err::println)
                .subscribe();


        while (!disposable.isDisposed()) {
            Thread.sleep(100);
        }

    }
}