package com.lounah.gallery.data.repository;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.lounah.gallery.data.entity.Resource;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public abstract class NetworkBoundResource<T> {

    private final MediatorLiveData<Resource<T>> result = new MediatorLiveData<>();

    @MainThread
    NetworkBoundResource() {
        result.setValue(Resource.loading(null));
        LiveData<T> dbSource = loadFromDb();
        result.addSource(dbSource, data -> {
            result.removeSource(dbSource);
            if (shouldFetch(data)) {
                fetchFromNetwork(dbSource);
            } else {
                result.addSource(dbSource, newData -> result.setValue(Resource.success(newData)));
            }
        });
    }

    private void fetchFromNetwork(final LiveData<T> dbSource) {

        result.addSource(dbSource, newData -> result.setValue(Resource.loading(newData)));

        createCall()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    result.removeSource(dbSource);
                    saveResultAndReInit(response);
                }, t -> {
                    onFetchFailed();
                    result.removeSource(dbSource);
                    result.addSource(dbSource, newData -> result.setValue(Resource.error(newData)));
        });

    }

    @MainThread
    private void saveResultAndReInit(T response) {
        Completable.fromAction(() -> {
           saveCallResult(response);
        }).subscribeOn(Schedulers.io())
                .subscribe(() -> result.addSource(loadFromDb(), newData -> result.setValue(Resource.success(newData))));
    }

    @WorkerThread
    protected abstract void saveCallResult(@NonNull T data);

    @MainThread
    protected abstract boolean shouldFetch(@Nullable T data);

    @NonNull
    @MainThread
    protected abstract LiveData<T> loadFromDb();

    @NonNull
    @MainThread
    protected abstract Single<T> createCall();

    @MainThread
    protected abstract void onFetchFailed();

    final LiveData<Resource<T>> getAsLiveData() {
        return result;
    }
}