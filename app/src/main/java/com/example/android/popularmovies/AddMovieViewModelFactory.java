package com.example.android.popularmovies;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

public class AddMovieViewModelFactory extends ViewModelProvider.NewInstanceFactory {

    private final AppDatabase movieDatabase;
    private final int movieId;

    public AddMovieViewModelFactory(AppDatabase database, int id) {
        movieDatabase = database;
        movieId = id;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new AddMovieViewModel(movieDatabase, movieId);
    }
}