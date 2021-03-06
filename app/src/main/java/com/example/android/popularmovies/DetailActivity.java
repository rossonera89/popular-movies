package com.example.android.popularmovies;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.popularmovies.adapters.ReviewAdapter;
import com.example.android.popularmovies.adapters.TrailerAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    /**
     * CurrentMovie Movie object
     */
    private Movie currentMovie;

    /**
     * database AppDatabase object
     */
    private AppDatabase database;

    /**
     * Boolean used for checking if the movie is added to the Favorites list
     */
    private boolean isFavorite;

    /**
     * ImageButton for adding the movie to the Favorites list
     */
    private ImageButton addToFavoritesButton;

    /**
     * TextView that is displayed when the trailer list is empty
     */
    private TextView emptyTrailerTextView;

    /**
     * TextView that is displayed when the review list is empty
     */
    private TextView emptyReviewTextView;

    /**
     * Review list
     */
    private List<Review> reviews;

    /**
     * Trailer list
     */
    private List<Trailer> trailers;

    /**
     * viewModel AddMovieViewModel object
     */
    private AddMovieViewModel viewModel;

    /**
     * Key of the current movie
     */
    private static final String CURRENT_MOVIE = "currentMovie";

    /**
     * Key of the scroll position X
     */
    private static final String SCROLL_POSITION_X = "scrollPositionX";

    /**
     * Key of the scroll position Y
     */
    private static final String SCROLL_POSITION_Y = "scrollPositionY";

    /**
     * Scroll position X
     */
    private int scrollX;

    /**
     * Scroll position Y
     */
    private int scrollY;

    /**
     * ScrollView of the DetailActivity
     */
    private ScrollView scrollView;

    /**
     * Key of the list of trailers
     */
    private static final String TRAILERS = "trailers";

    /**
     * Key of the list of reviews
     */
    private static final String REVIEWS = "reviews";

    /**
     * Key of the boolean isFavorite
     */
    private static final String IS_FAVORITE = "isFavorite";

    /**
     * RecyclerView for reviews
     */
    private RecyclerView reviewRecyclerView;

    /**
     * RecyclerView for trailers
     */
    private RecyclerView trailerRecyclerView;

    /**
     * Adapter for reviews
     */
    private ReviewAdapter reviewAdapter;

    /**
     * Adapter for trailers
     */
    private TrailerAdapter trailerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        /* Find references to all the UI components */
        addToFavoritesButton = findViewById(R.id.favorites_button);
        reviewRecyclerView = findViewById(R.id.reviews_recycler_view);
        trailerRecyclerView = findViewById(R.id.trailers_recycler_view);
        scrollView = findViewById(R.id.scroll_view);
        emptyReviewTextView = findViewById(R.id.empty_review_view);
        emptyTrailerTextView = findViewById(R.id.empty_trailer_view);

        /* Create a new TrailerAdapter and ReviewAdapter */
        trailerAdapter = new TrailerAdapter(trailers);
        reviewAdapter = new ReviewAdapter(reviews);

        /* Set a new LinearLayoutManager to the trailerRecyclerView and reviewRecyclerView*/
        reviewRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        trailerRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        /* Set the adapters to the RecyclerViews */
        reviewRecyclerView.setAdapter(reviewAdapter);
        trailerRecyclerView.setAdapter(trailerAdapter);

        /* Get instance of the AppDatabase using the app context */
        database = AppDatabase.getInstance(getApplicationContext());

        /* Check if the savedInstanceState exists, and contains the key CURRENT_MOVIE.
         * If so, get the parcelable under that key value from the savedInstanceState,
         * if not, get the parcelable from the intent. */
        if (savedInstanceState == null || !savedInstanceState.containsKey(CURRENT_MOVIE)) {
            currentMovie = getIntent().getParcelableExtra(CURRENT_MOVIE);
            checkIfFavorite();
        } else {
            currentMovie = savedInstanceState.getParcelable(CURRENT_MOVIE);
            isFavorite = savedInstanceState.getBoolean(IS_FAVORITE);
            trailers = savedInstanceState.getParcelableArrayList(TRAILERS);
            reviews = savedInstanceState.getParcelableArrayList(REVIEWS);
            scrollX = savedInstanceState.getInt(SCROLL_POSITION_X);
            scrollY = savedInstanceState.getInt(SCROLL_POSITION_Y);
        }
        generateUI();
    }

    /**
     * Store the variables under their keys to the savedInstanceState bundle
     */
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(CURRENT_MOVIE, currentMovie);
        savedInstanceState.putBoolean(IS_FAVORITE, isFavorite);
        savedInstanceState.putParcelableArrayList(TRAILERS, (ArrayList<? extends Parcelable>) trailers);
        savedInstanceState.putParcelableArrayList(REVIEWS, (ArrayList<? extends Parcelable>) reviews);
        scrollX = scrollView.getScrollX();
        scrollY = scrollView.getScrollY();
        savedInstanceState.putInt(SCROLL_POSITION_X, scrollX);
        savedInstanceState.putInt(SCROLL_POSITION_Y, scrollY);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * OnBackPressed return to the MainActivity
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(DetailActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Checks if the movie is saved in the Favorites list
     */
    private void checkIfFavorite() {
        /* Get the id of the current movie */
        final int id = currentMovie.getMovieId();

        /* Load the movie by the id of the current movie from the database */
        database.movieDao().loadMovieById(id);

        /* Declare the AddTaskViewModelFactory using the database and id parameters */
        AddMovieViewModelFactory factory = new AddMovieViewModelFactory(database, id);
            /* Initialize the viewModel variable by calling ViewModelProviders.of
            /* for that use the factory created */
        viewModel = ViewModelProviders.of(this, factory).get(AddMovieViewModel.class);

        /* Observe the LiveData object in the ViewModel. Use it also when
         * removing the observer */
        viewModel.getMovie().observe(this, new Observer<Movie>() {
            @Override
            public void onChanged(@Nullable Movie movieInDatabase) {
                viewModel.getMovie().removeObserver(this);
                /* If the movieInDatabase isn't null, set the isFavorite boolean to true,
                 * and set the image resource of the button the ic_star_rate */
                if (movieInDatabase != null) {
                    isFavorite = true;
                    addToFavoritesButton.setImageResource(R.drawable.ic_star_rate);
                    /* If the movieInDatabase is null, set the isFavorite boolean to false,
                     * and set the image resource of the button the ic_star_empty */
                } else {
                    isFavorite = false;
                    addToFavoritesButton.setImageResource(R.drawable.ic_star_empty);
                }
            }
        });
    }

    /**
     * ReviewAsyncTask class that uses the movie ID to create the reviewUrl String of that
     * movie, makes the HTTP request and parses the JSON String in order to create a new Review object.
     * Returns a list of reviews.
     */
    private class ReviewAsyncTask extends AsyncTask<String, Void, List<Review>> {

        String reviewUrl = null;

        @Override
        protected List<Review> doInBackground(String... strings) {

            int id = currentMovie.getMovieId();
            reviews = new ArrayList<>();

            try {
                URL url = QueryUtils.createReviewTrailerUrl(String.valueOf(id), QueryUtils.REVIEW_QUERY);
                String reviewJson = QueryUtils.makeHttpRequest(url);

                /* Create a JSONObject from the JSON response string */
                JSONObject baseJsonResponse = new JSONObject(reviewJson);

                /* Extract the JSONArray with the key "results" **/
                JSONArray reviewArray = baseJsonResponse.getJSONArray("results");

                if (reviewArray.length() == 0) {
                    return reviews;
                }

                /* For each review in the reviewArray, create a Review object */
                for (int i = 0; i < reviewArray.length(); i++) {

                    /* Get a single review at position i within the list of reviews */
                    JSONObject reviewObject = reviewArray.getJSONObject(i);

                    /* Extract the value for the required keys */
                    String reviewAuthor = reviewObject.getString("author");
                    String reviewText = reviewObject.getString("content");
                    reviewUrl = reviewObject.getString("url");

                    /* Create a new Review object with the given values */
                    Review currentReview = new Review(reviewAuthor, reviewText, reviewUrl);

                    currentReview.setReviewAuthor(reviewAuthor);
                    currentReview.setReviewText(reviewText);
                    currentReview.setReviewUrl(reviewUrl);
                    reviews.add(i, currentReview);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the movie JSON results.", e);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the movie JSON response results", e);
            }
            /* Return a list of reviews */
            return reviews;
        }

        /**
         * Use the review values to populate the UI
         */
        @Override
        protected void onPostExecute(List<Review> reviews) {
            /* If there are no reviews, hide the reviewRecyclerView and inform the user there
             * are no reviews found */
            if (reviews.size() == 0) {
                reviewRecyclerView.setVisibility(View.GONE);
                emptyReviewTextView.setText(getString(R.string.no_reviews_found));
                /* If there are review values, populate the UI with them */
            } else {
                populateReviews();
            }
        }
    }

    /**
     * Populate the reviews of the current movie
     */
    private void populateReviews() {
        reviewAdapter = new ReviewAdapter(reviews);
        reviewRecyclerView.setAdapter(reviewAdapter);
        reviewRecyclerView.setVisibility(View.VISIBLE);
        emptyReviewTextView.setVisibility(View.GONE);
        reviewRecyclerView.setLayoutManager(new LinearLayoutManager(DetailActivity.this));
    }

    /**
     * Populate the trailers of the current movie
     */
    private void populateTrailers() {
        trailerAdapter = new TrailerAdapter(trailers);
        trailerRecyclerView.setVisibility(View.VISIBLE);
        trailerRecyclerView.setAdapter(trailerAdapter);
        emptyTrailerTextView.setVisibility(View.GONE);
        trailerRecyclerView.setLayoutManager(new LinearLayoutManager(DetailActivity.this));
    }

    /**
     * TrailerAsyncTask class that uses the movie ID to create the trailerUrlPath String of that
     * movie, makes the HTTP request and parses the JSON String in order to create a cnew Trailer object.
     * Returns a list of trailers.
     */
    private class TrailerAsyncTask extends AsyncTask<String, Void, List<Trailer>> {

        String trailerUrlPath = null;

        @Override
        protected List<Trailer> doInBackground(String... strings) {

            int id = currentMovie.getMovieId();
            trailers = new ArrayList<>();

            try {
                URL url = QueryUtils.createReviewTrailerUrl(String.valueOf(id), QueryUtils.TRAILER_QUERY);
                String movieString = QueryUtils.makeHttpRequest(url);

                /* Create a JSONObject from the JSON response string */
                JSONObject baseJsonResponse = new JSONObject(movieString);

                /* Extract the JSONArray with the key "results" */
                JSONArray movieTrailerArray = baseJsonResponse.getJSONArray("results");

                if (movieTrailerArray.length() == 0) {
                    return trailers;
                }
                for (int i = 0; i < movieTrailerArray.length(); i++) {
                    /* Create a trailerObject for the first trailer in the movieTrailerArray, */
                    JSONObject trailerObject = movieTrailerArray.getJSONObject(i);

                    /* Extract the values for the required keys */
                    trailerUrlPath = trailerObject.getString("key");
                    String trailerName = trailerObject.getString("name");

                    /* Create a new Trailer object with the given values */
                    Trailer currentTrailer = new Trailer(trailerUrlPath, trailerName);

                    currentTrailer.setTrailerUrlPath(trailerUrlPath);
                    currentTrailer.setTrailerName(trailerName);
                    trailers.add(i, currentTrailer);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the movie JSON results.", e);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the movie JSON response results", e);
            }
            /* Return a list of trailers */
            return trailers;
        }

        /**
         * Use the trailer values to populate the UI
         */
        @Override
        protected void onPostExecute(List<Trailer> trailers) {
            /* If there are no trailers, hide the trailerRecyclerView and inform the user there
             * are no trailers found */
            if (trailers.size() == 0) {
                trailerRecyclerView.setVisibility(View.GONE);
                emptyTrailerTextView.setText(getString(R.string.no_trailers_found));
            } else {
                populateTrailers();
            }
        }
    }

    /**
     * Populates the UI with details of the selected movie
     */
    private void generateUI() {
        /* Get BaseContext and store it a Context variable */
        final Context context = getBaseContext();

        /* Scroll to the position saved in the onSaveInstanceState */
        scrollView.scrollTo(scrollX, scrollY);

        /* Find the TextViews of the title, plot_synopsis, user_rating and release_date,
         * as well as the ImageView of the movie_poster_thumbnail. */
        TextView titleTextView = findViewById(R.id.title_text_view);
        ImageView moviePosterImageView = findViewById(R.id.movie_poster_thumbnail_image_view);
        TextView plotSynopsisTextView = findViewById(R.id.plot_synopsis_text_view);
        TextView userRatingTextView = findViewById(R.id.user_rating_text_view);
        TextView releaseDateTextView = findViewById(R.id.release_date_text_view);

        /* Get the Intent and check if it is null. */
        Intent intent = getIntent();
        /* If the intent exists, get the currentMovie object from the parcelableExtra */
        if (intent != null) {
            /* Get the id of the current movie */
            final int id = currentMovie.getMovieId();

            /* Get the title of the current movie and set it to the titleTextView */
            final String title = currentMovie.getMovieTitle();
            titleTextView.setText(title);

            /* Get the release date of the current movie and set it to the releaseDateTextView */
            final String releaseDate = currentMovie.getMovieReleaseDate();
            releaseDateTextView.setText(releaseDate);

            /* Get the user rating of the current movie and set it to the userRatingTextView */
            final String userRating = currentMovie.getMovieUserRating();
            userRatingTextView.setText(userRating);

            /* Get the plot synopsis of the current movie and set it to the plotSynopsisTextView */
            final String plotSynopsis = currentMovie.getMoviePlotSynopsis();
            plotSynopsisTextView.setText(plotSynopsis);

            /* Get the full poster path Uri of the current movie and using the Picasso library
              load it into the moviePosterImageView */
            final String posterPath = currentMovie.getMovieUrlPoster();

            Uri fullPosterPathUri = MovieAdapter.formatPosterPath(currentMovie);
            com.squareup.picasso.Picasso
                    .get()
                    .load(fullPosterPathUri)
                    .into(moviePosterImageView);

            /* If there are no trailers, execute the TrailerAsyncTask using the trailer
            query String. If not, populate the trailers. */
            if (trailers == null) {
                new TrailerAsyncTask().execute(String.valueOf(id), QueryUtils.TRAILER_QUERY);
            } else if (trailers.size() == 0) {
                emptyTrailerTextView.setText(getString(R.string.no_trailers_found));
            } else {
                populateTrailers();
            }


            /* If there are no reviews, execute the ReviewAsyncTask using the review
            query String. If not, populate the reviews. */
            if (reviews == null) {
                new ReviewAsyncTask().execute(String.valueOf(id), String.valueOf(QueryUtils.REVIEW_QUERY));
            } else if (reviews.size() == 0) {
                emptyReviewTextView.setText(getString(R.string.no_reviews_found));
            } else {
                populateReviews();
            }

            /* Set the onClickListener to tha addToFavorites button */
            addToFavoritesButton.setOnClickListener(new View.OnClickListener() {
                                                        /* Using the onClick method, create a new movie with the values provided and check
                                                         * if the movie is added to the Favorites list or not.  */
                                                        @Override
                                                        public void onClick(View v) {
                                                            final Movie movie = new Movie(id, title, releaseDate, posterPath, userRating, plotSynopsis);
                                                            AppExecutors.getExecutors().diskIO().execute(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    /* If the movie is not the Favorites list, insert the movie and change the isFavorite boolean value to true.*/
                                                                    if (!isFavorite) {
                                                                        database.movieDao().insertMovie(movie);
                                                                        isFavorite = true;
                                                                        /* If the movie is in the Favorites list, delete the movie and change the isFavorite boolean value to false.*/
                                                                    } else {
                                                                        database.movieDao().deleteMovie(movie);
                                                                        isFavorite = false;
                                                                    }
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            /* If the movie is not the Favorites list, change the image resource and create a Toast to inform the user
                                                                            that the movie has been added to the Favorites list.*/
                                                                            if (isFavorite) {
                                                                                addToFavoritesButton.setImageResource(R.drawable.ic_star_rate);
                                                                                Toast.makeText(context, getString(R.string.toast_added_to_favorites), Toast.LENGTH_SHORT).show();
                                                                                                   /* If the movie is in the Favorites list, change the image resource and create a Toast to inform the user
                                                                            that the movie has been deleted from the Favorites list.*/
                                                                            } else {
                                                                                addToFavoritesButton.setImageResource(R.drawable.ic_star_empty);
                                                                                Toast.makeText(getApplicationContext(), getString(R.string.toast_deleted_from_favorites), Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            });
                                                        }
                                                    }
            );
        }
    }
}