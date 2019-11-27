package tv.cloudwalker.cwnxt.transavrotv;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BrowseSupportFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.DiffCallback;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import RecommendationService.RecommendationServiceGrpc;
import RecommendationService.RecommendationServiceOuterClass;
import SchedularService.SchedularServiceGrpc;
import SchedularService.SchedularServiceOuterClass;
import TileService.TileServiceGrpc;
import TileService.TileServiceOuterClass;
import VendorService.VendorServiceGrpc;
import VendorService.VendorServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import presenter.PreCardPresenter;

public class MainFragment extends BrowseSupportFragment implements OnItemViewClickedListener
{
        private static final String TAG = "MainFragment";
        private static final String UserId = "vaibhav123";
        public ArrayObjectAdapter mRowsAdapter;
        private ManagedChannel managedChannel;
        private RecommendationServiceGrpc.RecommendationServiceStub recommendationServiceStub;
        private int counter = 0;
        private ArrayObjectAdapter contentBasedAdapter, colabFilAdapter;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setupUiConfig();
            getSchedule();
//            fetchRowsAndTiles();
            preparingTileClickCall();
            setOnItemViewClickedListener(this);
        }



        private void preparingTileClickCall(){
            managedChannel  = ManagedChannelBuilder.forAddress("192.168.1.143", 50054).usePlaintext().build();
            recommendationServiceStub = RecommendationServiceGrpc.newStub(managedChannel);
        }


        private void setupUiConfig(){
            managedChannel  = ManagedChannelBuilder.forAddress("192.168.1.143", 50053).usePlaintext().build();
            VendorServiceGrpc.VendorServiceStub vendorServiceStub = VendorServiceGrpc.newStub(managedChannel);
            vendorServiceStub
                    .getVendorSpecification(VendorServiceOuterClass.VendorRequestSpecification.newBuilder().setVendor("cvte").setBrand("shinko").build(), new StreamObserver<VendorServiceOuterClass.VendorBrandSpecification>() {
                        @Override
                        public void onNext(final VendorServiceOuterClass.VendorBrandSpecification value) {
                            Log.i(TAG, "onNext: ");
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setBrandColor(Color.parseColor(value.getFastlaneColor()));
                                    setSearchAffordanceColor(Color.parseColor(value.getSearchColor()));
                                }
                            });
                        }

                        @Override
                        public void onError(Throwable t) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setBrandColor(getActivity().getResources().getColor(R.color.fastlane_background));
                                    setBadgeDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.brand_logo));
                                    setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
                                }
                            });
                            Log.e(TAG, "onError: ",t );
                        }

                        @Override
                        public void onCompleted() {
                            Log.i(TAG, "onCompleted: ");
                        }
                    });
            mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
            setAdapter(mRowsAdapter);
            setOnSearchClickedListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "onClick: ");

                }
            });
        }

        private void getSchedule() {
            managedChannel  = ManagedChannelBuilder.forAddress("192.168.1.143", 50055).usePlaintext().build();
            SchedularServiceGrpc.SchedularServiceStub schedularServiceStub = SchedularServiceGrpc.newStub(managedChannel);
            schedularServiceStub.getSchedule(SchedularServiceOuterClass
                    .RequestSchedule
                    .newBuilder()
                    .setVendor("cvte")
                    .setBrand("shinko")
                    .build(), new StreamObserver<SchedularServiceOuterClass.UserScheduleResponse>() {
                @Override
                public void onNext(SchedularServiceOuterClass.UserScheduleResponse value) {
                    Log.i(TAG, "onNext: **********************");
                    for(SchedularServiceOuterClass.LauncherPage launcherPage : value.getLauncherPageList()){
                        Log.i(TAG, "onNext: PageName "+launcherPage.getPageName());
                        for(SchedularServiceOuterClass.Carousel carousel : launcherPage.getCarouselList()){
                            Log.i(TAG, "onNext: carosuel "+carousel.getImageUrl());
                        }
                        for(SchedularServiceOuterClass.LauncherRows launcherRows : launcherPage.getLauncherRowsList()){
                            Log.i(TAG, "onNext: Rowid "+launcherRows.getRowId());
                            fetchTiles(launcherRows);
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "onError: schdule ",t );
                }

                @Override
                public void onCompleted() {

                }
            });
        }

    private void fetchTiles(final SchedularServiceOuterClass.LauncherRows launcherRows) {
        managedChannel  = ManagedChannelBuilder.forAddress("192.168.1.143", 50051).usePlaintext().build();
        TileServiceGrpc.TileServiceStub tileServiceStub = TileServiceGrpc.newStub(managedChannel);

        tileServiceStub.getMovieTiles(TileServiceOuterClass
                .RowId
                .newBuilder()
                .setRowId(launcherRows.getRowId())
                .build(), new StreamObserver<TileServiceOuterClass.MovieTile>() {
            @Override
            public void onNext(TileServiceOuterClass.MovieTile value) {
                Log.i(TAG, "onNext: MovieTiles ---- "+launcherRows.getRowName()+"------"+value.getMetadata().getTitle());
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "onError: Tiles ", t);
            }

            @Override
            public void onCompleted() {
                Log.i(TAG, "onCompleted: MOvieTiles --------------------"+launcherRows.getRowName());
            }
        });
    }

    @Override
        public void onDestroyView() {
            super.onDestroyView();
            try {
                managedChannel.shutdown().awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void onItemClicked(Presenter.ViewHolder viewHolder, Object o, RowPresenter.ViewHolder viewHolder1, Row row) {
            if(recommendationServiceStub == null){
                preparingTileClickCall();
            }
            if(o instanceof TileServiceOuterClass.MovieTile){
                Log.i(TAG, "onItemClicked: ");
                recommendationServiceStub.tileClicked(RecommendationServiceOuterClass
                        .TileClickedRequest
                        .newBuilder()
                        .setTileId(((TileServiceOuterClass.MovieTile) o).getRefId())
                        .setTileScore(1.0)
                        .setUserId(UserId)
                        .build(), new StreamObserver<RecommendationServiceOuterClass.TileClickedResponse>() {
                    @Override
                    public void onNext(RecommendationServiceOuterClass.TileClickedResponse value) {
                        Log.i(TAG, "onNext: ");
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.i(TAG, "onError: ");
                    }

                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "onCompleted: ");
                    }
                });
            }else if(o instanceof RecommendationServiceOuterClass.MovieTile){
                Log.i(TAG, "onItemClicked: Rec ");
                recommendationServiceStub.tileClicked(RecommendationServiceOuterClass
                        .TileClickedRequest
                        .newBuilder()
                        .setTileId(((RecommendationServiceOuterClass.MovieTile) o).getRefId())
                        .setTileScore(1.0)
                        .setUserId(UserId)
                        .build(), new StreamObserver<RecommendationServiceOuterClass.TileClickedResponse>() {
                    @Override
                    public void onNext(RecommendationServiceOuterClass.TileClickedResponse value) {
                        Log.i(TAG, "onNext: ");
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.i(TAG, "onError: ");
                    }

                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "onCompleted: ");
                    }
                });
            }
            counter++;
            if(counter == 10){
                counter = 0;
                Log.i(TAG, "onItemClicked: in ");
                recommendationServiceStub
                        .getCollabrativeFilteringData(RecommendationServiceOuterClass
                                .GetRecommendationRequest
                                .newBuilder()
                                .setUserId(UserId)
                                .build(), new StreamObserver<RecommendationServiceOuterClass.MovieTile>() {
                            List<RecommendationServiceOuterClass.MovieTile> movieTileList = new ArrayList<>();
                            @Override
                            public void onNext(RecommendationServiceOuterClass.MovieTile value) {
                                Log.d(TAG, "onNext: "+value.getMetadata().getTitle());
                                movieTileList.add(value);
                            }

                            @Override
                            public void onError(Throwable t) {
                                Log.e(TAG, "onError: ",t);
                            }

                            @Override
                            public void onCompleted() {
                                Log.i(TAG, "onCompleted: ");
                                if(movieTileList.size() > 0){
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(colabFilAdapter != null){
                                                colabFilAdapter.setItems(movieTileList, new DiffCallback() {
                                                    @Override
                                                    public boolean areItemsTheSame(@NonNull Object o, @NonNull Object value1) {
                                                        return !((RecommendationServiceOuterClass.MovieTile) o).getRefId().equals(((RecommendationServiceOuterClass.MovieTile) value1).getRefId());
                                                    }

                                                    @Override
                                                    public boolean areContentsTheSame(@NonNull Object o, @NonNull Object value1) {
                                                        return !((RecommendationServiceOuterClass.MovieTile) o).getRefId().equals(((RecommendationServiceOuterClass.MovieTile) value1).getRefId());
                                                    }
                                                });
                                            }else {
                                                colabFilAdapter = new ArrayObjectAdapter(new PreCardPresenter());
                                                colabFilAdapter.setItems(movieTileList, new DiffCallback() {
                                                    @Override
                                                    public boolean areItemsTheSame(@NonNull Object o, @NonNull Object value1) {
                                                        return !((RecommendationServiceOuterClass.MovieTile) o).getRefId().equals(((RecommendationServiceOuterClass.MovieTile) value1).getRefId());
                                                    }

                                                    @Override
                                                    public boolean areContentsTheSame(@NonNull Object o, @NonNull Object value1) {
                                                        return !((RecommendationServiceOuterClass.MovieTile) o).getRefId().equals(((RecommendationServiceOuterClass.MovieTile) value1).getRefId());
                                                    }
                                                });
                                                ListRow listRow = new ListRow(new HeaderItem("Dummy1"), colabFilAdapter);
                                                mRowsAdapter.add(1, listRow);
                                            }
                                        }
                                    });
                                }
                            }
                        });


                recommendationServiceStub
                        .getContentbasedData(RecommendationServiceOuterClass
                                .GetRecommendationRequest
                                .newBuilder()
                                .setUserId(UserId)
                                .build(),  new StreamObserver<RecommendationServiceOuterClass.MovieTile>() {
                            List<RecommendationServiceOuterClass.MovieTile> movieTileList = new ArrayList<>();
                            @Override
                            public void onNext(RecommendationServiceOuterClass.MovieTile value) {
                                Log.d(TAG, "onNext: "+value.getMetadata().getTitle());
                                movieTileList.add(value);
                            }

                            @Override
                            public void onError(Throwable t) {
                                Log.e(TAG, "onError: ",t);
                            }

                            @Override
                            public void onCompleted() {
                                Log.i(TAG, "onCompleted: ");
                                if(movieTileList.size() > 0 ){
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(contentBasedAdapter != null){
                                                contentBasedAdapter.setItems(movieTileList, new DiffCallback() {
                                                    @Override
                                                    public boolean areItemsTheSame(@NonNull Object o, @NonNull Object value1) {
                                                        return !((RecommendationServiceOuterClass.MovieTile) o).getRefId().equals(((RecommendationServiceOuterClass.MovieTile) value1).getRefId());
                                                    }

                                                    @Override
                                                    public boolean areContentsTheSame(@NonNull Object o, @NonNull Object value1) {
                                                        return !((RecommendationServiceOuterClass.MovieTile) o).getRefId().equals(((RecommendationServiceOuterClass.MovieTile) value1).getRefId());
                                                    }
                                                });
                                            }else {
                                                contentBasedAdapter = new ArrayObjectAdapter(new PreCardPresenter());
                                                contentBasedAdapter.setItems(movieTileList, new DiffCallback() {
                                                    @Override
                                                    public boolean areItemsTheSame(@NonNull Object o, @NonNull Object value1) {
                                                        return !((RecommendationServiceOuterClass.MovieTile) o).getRefId().equals(((RecommendationServiceOuterClass.MovieTile) value1).getRefId());
                                                    }

                                                    @Override
                                                    public boolean areContentsTheSame(@NonNull Object o, @NonNull Object value1) {
                                                        return !((RecommendationServiceOuterClass.MovieTile) o).getRefId().equals(((RecommendationServiceOuterClass.MovieTile) value1).getRefId());
                                                    }
                                                });
                                                ListRow listRow = new ListRow(new HeaderItem("Dummy2"), contentBasedAdapter);
                                                mRowsAdapter.add(0, listRow);
                                            }
                                        }
                                    });
                                }
                            }
                        });
            }
        }
}














//    func (h *RecommendationServiceHandler) TileClickedProcessing(userId, tileId string, tileScore float64)  {
//        h.RedisConnection.ZIncrBy(fmt.Sprintf("user:%s:items", userId), tileScore, tileId)
//        h.RedisConnection.ZIncrBy("cloudwalkersTilesScores", tileScore, tileId)
//        h.RedisConnection.ZIncrBy(fmt.Sprintf("item:%s:scores", tileId),tileScore, userId)
//        h.RedisConnection.SAdd("users", userId)
//        }