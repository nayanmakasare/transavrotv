package presenter;

import android.support.v17.leanback.widget.Presenter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import TileService.TileServiceOuterClass;
import tv.cloudwalker.cwnxt.transavrotv.R;

public class CustomCard extends Presenter
{

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.custom_presenter, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object o) {
            if(o instanceof TileServiceOuterClass.MovieTile){
                if(((TileServiceOuterClass.MovieTile) o).getPosters().getLandscapeList().size() > 0 && !((TileServiceOuterClass.MovieTile) o).getPosters().getLandscape(0).isEmpty()){
                    String imageUrl = "http://static.cloudwalker.tv/images/tiles/"+((TileServiceOuterClass.MovieTile) o).getPosters().getLandscape(0);
                    Glide.with(viewHolder.view.getContext())
                            .load(imageUrl)
                            .into((ImageView) viewHolder.view.findViewById(R.id.moviePoster));
                }
            }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {

    }
}
