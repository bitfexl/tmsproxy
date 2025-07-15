package com.github.bitfexl.tmsproxy.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;

public class DuplicatingWriteStream<T> implements WriteStream<T> {
    private final WriteStream<T> a;
    private volatile WriteStream<T> b;

    private volatile boolean aDrainHandlerCalled, bDrainHandlerCalled;

    public DuplicatingWriteStream(WriteStream<T> a, WriteStream<T> b) {
        this.a = a;
        this.b = b;
    }

    public void removeB() {
        b = null;
    }

    @Override
    public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
        a.exceptionHandler(handler);
        if (b != null) {
        b.exceptionHandler(handler);
        }
        return this;
    }

    @Override
    public Future<Void> write(T data) {
        if (b != null) {
            return Future.all(a.write(data), b.write(data)).map((Void) null);
        }
        return a.write(data);
    }

    @Override
    public void write(T data, Handler<AsyncResult<Void>> handler) {
        write(data).andThen(handler);
    }

    @Override
    public Future<Void> end() {
        if (b != null) {
            return Future.all(a.end(), b.end()).map((Void) null);
        }
        return a.end();
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
        end().andThen(handler);
    }

    @Override
    public WriteStream<T> setWriteQueueMaxSize(int maxSize) {
        a.setWriteQueueMaxSize(maxSize);
        if (b != null) {
            b.setWriteQueueMaxSize(maxSize);
        }
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return a.writeQueueFull() || (b != null && b.writeQueueFull());
    }

    @Override
    public WriteStream<T> drainHandler(Handler<Void> handler) {
        aDrainHandlerCalled = false;
        bDrainHandlerCalled = false;

        a.drainHandler(__ -> {
            aDrainHandlerCalled = true;
            if (bDrainHandlerCalled || b == null) {
                handler.handle(null);
            }
        });

        if (b != null) {
            b.drainHandler(__ -> {
                bDrainHandlerCalled = true;
                if (aDrainHandlerCalled) {
                    handler.handle(null);
                }
            });
        }

        return this;
    }
}
