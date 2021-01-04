package com.android.sample.exoplayer;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.Subject;
import io.reactivex.subjects.UnicastSubject;

public final class RxBus {

    //this how to create our bus
    private static final Subject<Boolean> SUBJECT = UnicastSubject.create();

    public static Disposable subscribe(@NonNull Consumer<Boolean> action) {
        return SUBJECT.subscribe(action);
    }

    //use this method to send data
    public static void publish(@NonNull Boolean message) {
        SUBJECT.onNext(message);
    }
}
