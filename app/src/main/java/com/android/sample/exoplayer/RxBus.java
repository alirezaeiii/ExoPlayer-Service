package com.android.sample.exoplayer;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public final class RxBus {

    private static final Subject<Boolean> SUBJECT = PublishSubject.create();

    public static Disposable subscribe(Consumer<Boolean> action) {
        return SUBJECT.subscribe(action);
    }

    public static void publish(Boolean message) {
        SUBJECT.onNext(message);
    }
}
