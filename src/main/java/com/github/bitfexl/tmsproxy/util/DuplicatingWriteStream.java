package com.github.bitfexl.tmsproxy.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;

public class DuplicatingWriteStream<T> implements WriteStream<T> {
    private final WriteStream<T> a, b;

    private volatile boolean aDrainHandlerCalled, bDrainHandlerCalled;

    public DuplicatingWriteStream(WriteStream<T> a, WriteStream<T> b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
        a.exceptionHandler(handler);
        b.exceptionHandler(handler);
        return this;
    }

    @Override
    public Future<Void> write(T data) {
        return Future.all(a.write(data), b.write(data)).map((Void)null);
    }

    @Override
    public void write(T data, Handler<AsyncResult<Void>> handler) {
        write(data).andThen(handler);
    }

    @Override
    public Future<Void> end() {
        return Future.all(a.end(), b.end()).map((Void)null);
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
        end().andThen(handler);
    }

    @Override
    public WriteStream<T> setWriteQueueMaxSize(int maxSize) {
        a.setWriteQueueMaxSize(maxSize);
        b.setWriteQueueMaxSize(maxSize);
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return a.writeQueueFull() || b.writeQueueFull();
    }

    @Override
    public WriteStream<T> drainHandler(Handler<Void> handler) {
        aDrainHandlerCalled = false;
        bDrainHandlerCalled = false;

        a.drainHandler(__ -> {
            aDrainHandlerCalled = true;
            if (bDrainHandlerCalled) {
                handler.handle(null);
            }
        });

        b.drainHandler(__ -> {
            bDrainHandlerCalled = true;
            if (aDrainHandlerCalled) {
                handler.handle(null);
            }
        });

        return this;
    }
}
