package com.android.sample.exoplayer;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class RxSubject<T> {

    private final Subject<T> subject;

    public RxSubject() {
        subject = PublishSubject.create();
    }

    public Disposable subscribe(Consumer<T> action) {
        return subject.subscribe(action);
    }

    public void publish(T message) {
        subject.onNext(message);
    }

    public static void unsubscribe(Disposable... disposables) {
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
    }
}
