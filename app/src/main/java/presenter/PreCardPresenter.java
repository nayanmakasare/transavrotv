package presenter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import RecommendationService.RecommendationServiceOuterClass;
import TileService.TileServiceOuterClass.MovieTile;
import tv.cloudwalker.cwnxt.transavrotv.R;

public class PreCardPresenter extends Presenter {

    private Drawable mDefaultCardImage;
    private int landscapeWidth, landscapeHeight ,squareWidth, squareHeight, portraitWidth, portraitHeight;
    private static final String TAG = "PreCardPresenter";


    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        mDefaultCardImage = ContextCompat.getDrawable(parent.getContext(), R.drawable.brand_logo);


        ImageCardView cardView = new ImageCardView(parent.getContext());

        landscapeWidth  = dpToPx(parent.getContext() , parent.getContext() .getResources().getInteger(R.integer.tileLandScapeWidth));
        landscapeHeight = dpToPx(parent.getContext()  , parent.getContext() .getResources().getInteger(R.integer.tileLandScapeHeight));

        squareWidth  = dpToPx(parent.getContext() , parent.getContext() .getResources().getInteger(R.integer.tileSquareWidth));
        squareHeight = dpToPx(parent.getContext()  , parent.getContext() .getResources().getInteger(R.integer.tileSquareHeight));

        portraitWidth  = dpToPx(parent.getContext() , parent.getContext() .getResources().getInteger(R.integer.tilePotraitWidth));
        portraitHeight = dpToPx(parent.getContext()  , parent.getContext() .getResources().getInteger(R.integer.tilePotraitHeight));

        cardView.setCardType(ImageCardView.CARD_TYPE_INFO_OVER);
        cardView.setMainImageScaleType(ImageView.ScaleType.FIT_CENTER);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.setMainImageDimensions(landscapeWidth, landscapeHeight);
        return new Presenter.ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        if(item instanceof MovieTile)
        {
            MovieTile movie = (MovieTile) item;
            if(movie.getPosters().getLandscapeList() != null && !movie.getPosters().getLandscapeList().isEmpty()){
                if(movie.getPosters().getLandscape(0) != null && !movie.getPosters().getLandscape(0).isEmpty()){
                    String imageUrl = movie.getPosters().getLandscape(0);
                    cardView.setTitleText(movie.getMetadata().getTitle());
                    int width = landscapeWidth;
                    int height = landscapeHeight;
                    imageUrl = "http://static.cloudwalker.tv/images/tiles/"+imageUrl;
                    cardView.setContentText(movie.getContent().getPackage());
                    cardView.setMainImageDimensions(width, height);
                    Glide.with(viewHolder.view.getContext())
                            .load(imageUrl)
                            .error(mDefaultCardImage)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .override(width, height)
                            .into(cardView.getMainImageView());
                }
            }
        }else if(item instanceof RecommendationServiceOuterClass.MovieTile){
            RecommendationServiceOuterClass.MovieTile movie = (RecommendationServiceOuterClass.MovieTile) item;
            if(movie.getPosters().getLandscapeList() != null && !movie.getPosters().getLandscapeList().isEmpty()){
                if(movie.getPosters().getLandscape(0) != null && !movie.getPosters().getLandscape(0).isEmpty()){
                    String imageUrl = movie.getPosters().getLandscape(0);
                    cardView.setTitleText(movie.getMetadata().getTitle());
                    int width = landscapeWidth;
                    int height = landscapeHeight;
                    imageUrl = "http://static.cloudwalker.tv/images/tiles/"+imageUrl;
                    cardView.setContentText(movie.getContent().getPackage());
                    cardView.setMainImageDimensions(width, height);
                    Glide.with(viewHolder.view.getContext())
                            .load(imageUrl)
                            .error(mDefaultCardImage)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .override(width, height)
                            .into(cardView.getMainImageView());
                }
            }
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
        cardView.setTitleText(null);
        cardView.setContentText(null);
    }

    private int dpToPx(Context ctx , int dp) {
        float density = ctx.getResources()
                .getDisplayMetrics()
                .density;
        return Math.round((float) dp * density);
    }
}
