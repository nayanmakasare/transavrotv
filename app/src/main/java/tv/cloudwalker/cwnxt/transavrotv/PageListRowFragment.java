package tv.cloudwalker.cwnxt.transavrotv;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.BrowseSupportFragment;
import android.support.v17.leanback.app.RowsSupportFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.FocusHighlight;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PageRow;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeIntents;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import SchedularService.SchedularServiceOuterClass;
import TileService.TileServiceGrpc;
import TileService.TileServiceOuterClass;
import VendorService.VendorServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import presenter.CustomCard;
import tv.cloudwalker.cwnxt.transavrotv.receviers.NetworkChangeReceiver;

public class PageListRowFragment extends BrowseSupportFragment implements NetworkChangeReceiver.NetworkConnectivityInterface
{
    private ArrayObjectAdapter mRowsAdapter;
    private static final String TAG = "PageListRowFragment";
    private static ManagedChannel tileServiceChannel;
    private static ManagedChannel recommendationChannel;
    private Thread subscribeThread;
    private NetworkChangeReceiver networkChangeReceiver;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkChangeReceiver = new NetworkChangeReceiver();
        tileServiceChannel = ManagedChannelBuilder.forAddress("192.168.1.143", 50051).usePlaintext().build();
        recommendationChannel = ManagedChannelBuilder.forAddress("192.168.1.143", 50054).usePlaintext().build();
        getActivity().registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        networkChangeReceiver.addListener(this);
        setupUi();
        loadData();
    }

    private void subscribe() {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Connection connection = ((CloudwalkerApplication)getActivity().getApplication()).factory.newConnection();
                        Channel channel = connection.createChannel();
                        AMQP.Queue.DeclareOk q = channel.queueDeclare("DemoQueue", false, false, false, null);
                        channel.queueBind(q.getQueue(), "amq.topic", "cvte.shinko");
                        QueueingConsumer consumer = new QueueingConsumer(channel);
                        channel.basicConsume(q.getQueue(), true, consumer);
                        while (true) {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                            String message = new String(delivery.getBody());
                            handleRabbitMessage(message);
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, "run: interut ",e);
                        break;
                    } catch (Exception e1) {
                        Log.e(TAG, "run: ",e1 );
                        Log.d(TAG, "Connection broken: " + e1.getClass().getName());
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        });
        subscribeThread.start();
    }

    private void handleRabbitMessage(String message){
        final String finalMessage = message;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), finalMessage, Toast.LENGTH_SHORT).show();
            }
        });

        message = message.toLowerCase();
        switch (message){
            case "refresh":{

            }
            break;
            case "recommendation":{

            }
            break;
            case "refreshSchedule":{
                Log.i(TAG, "handleRabbitMessage: "+message);
//                SchedularServiceOuterClass.UserScheduleResponse value = ((CloudwalkerApplication)getActivity().getApplication()).getSchedule();
//                if(value != null){
//                    makePages(value);
//                }
            }
            break;
        }
    }

    private void loadData() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
        createRows();
//        startEntranceTransition();
    }

    private void setupUi() {
        File brandSpecFile = new File(getActivity().getFilesDir().getAbsoluteFile() + "/brandSpec.proto");
        if(brandSpecFile.exists())
        {
            Log.i(TAG, "setupUi: local brandSpec found ");
            try {
                InputStream inputStream = new FileInputStream(brandSpecFile);
                VendorServiceOuterClass.VendorBrandSpecification vbs = VendorServiceOuterClass.VendorBrandSpecification.parseFrom(inputStream);
                if(vbs != null){
                    Log.i(TAG, "setupUi: settings server brand sepc");
                    serverUiSetup(vbs);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            setHeadersState(HEADERS_ENABLED);
            setBrandColor(getResources().getColor(R.color.fastlane_background));
        }
        setHeadersTransitionOnBackEnabled(true);
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), "Not yet implemented", Toast.LENGTH_SHORT).show();
            }
        });
//        prepareEntranceTransition();
    }

    private void serverUiSetup(VendorServiceOuterClass.VendorBrandSpecification vbs){
        Log.i(TAG, "serverUiSetup: 1");
        if(vbs.getHasFastlane()){
            setHeadersState(HEADERS_ENABLED);
        }else {
            Log.i(TAG, "serverUiSetup: 2");
            setHeadersState(HEADERS_DISABLED);
        }
        if(vbs.getFastlaneColor() != null && !vbs.getFastlaneColor().isEmpty()){
            Log.i(TAG, "serverUiSetup: 3");
            setBrandColor(Color.parseColor(vbs.getFastlaneColor()));
        }
        if(vbs.getSearchColor() != null && !vbs.getSearchColor().isEmpty()){
            Log.i(TAG, "serverUiSetup: 4");
            setSearchAffordanceColor(Color.parseColor(vbs.getSearchColor()));
        }
        if(vbs.getBrandLogoResource() != null && !vbs.getBrandLogoResource().isEmpty()){
            Log.i(TAG, "serverUiSetup: 5");
            String resourceFileName = ((CloudwalkerApplication)getActivity().getApplication()).ifResourceAvaliableFileName(vbs.getAboutUsResource());
            if(resourceFileName != null){
                Log.i(TAG, "serverUiSetup: 6");
                setBadgeDrawable(Drawable.createFromPath(resourceFileName));
            }
        }
    }

    private void createRows() {
        File scheduleFile = new File(getActivity().getFilesDir().getAbsoluteFile() + "/schedule.proto");
        if (scheduleFile.exists()) {
            Log.i(TAG, "createRows: 1 "+scheduleFile.getAbsolutePath());
            try {
                InputStream inputStream = new FileInputStream(scheduleFile);
                SchedularServiceOuterClass.UserScheduleResponse value = SchedularServiceOuterClass
                        .UserScheduleResponse
                        .parseFrom(inputStream);
                Log.i(TAG, "createRows: 2");
                if(value != null){
                    Log.i(TAG, "createRows: 3 "+value.getLauncherPageCount());
                    makePages(value);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.i(TAG, "createRows: 4");
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "createRows: 5");
            }
        }else {
            SchedularServiceOuterClass.UserScheduleResponse value = ((CloudwalkerApplication)getActivity().getApplication()).getSchedule();
            Log.i(TAG, "createRows: hit schedule ");
           if(value != null){
               makePages(value);
           }
        }
    }

    private void makePages(final SchedularServiceOuterClass.UserScheduleResponse schedule){
        // settings registary
        getMainFragmentRegistry().registerFragment(PageRow.class, new PageRowFragmentFactory(schedule));
        Log.i(TAG, "makePages: ");

        //adding Pages
        for(int i = 0 ; i < schedule.getLauncherPageList().size() ; i++){
            Log.i(TAG, "makePages: "+1);
            HeaderItem headerItem1 = new HeaderItem(i, schedule.getLauncherPageList().get(i).getPageName());
            PageRow pageRow = new PageRow(headerItem1);
            mRowsAdapter.add(pageRow);
        }
    }

    @Override
    public void networkConnected() {
        Log.i(TAG, "networkConnected: ");
        subscribe();
    }

    @Override
    public void networkDisconnected() {
        if(subscribeThread.isAlive() && !subscribeThread.isInterrupted()){
            Log.i(TAG, "networkDisconnected: ");
            subscribeThread.interrupt();
        }
    }

    private class PageRowFragmentFactory extends BrowseSupportFragment.FragmentFactory {
        private final SchedularServiceOuterClass.UserScheduleResponse schedule;

        PageRowFragmentFactory(SchedularServiceOuterClass.UserScheduleResponse schedule) {
            this.schedule = schedule;
        }

        @Override
        public Fragment createFragment(Object rowObj) {
            Row row = (Row)rowObj;
            DataRowFragment dataRowFragment = new DataRowFragment();
            Bundle bundle = new Bundle();
            bundle.putByteArray("launcherPage",schedule.getLauncherPage((int) row.getHeaderItem().getId()).toByteArray());
            dataRowFragment.setArguments(bundle);
            return dataRowFragment;
        }
    }

    public static class DataRowFragment extends RowsSupportFragment {

        private final ArrayObjectAdapter mRowsAdapter;
        private PackageManager packageManager = null;
        public static final io.grpc.Context.Key<String> TV_EMAC = io.grpc.Context.key("tvEmac");


        public DataRowFragment() {
            mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM));
            setAdapter(mRowsAdapter);
            setOnItemViewClickedListener(new OnItemViewClickedListener() {
                @Override
                public void onItemClicked(
                        Presenter.ViewHolder itemViewHolder,
                        Object item,
                        RowPresenter.ViewHolder rowViewHolder,
                        Row row) {
                    if(item instanceof TileServiceOuterClass.MovieTile)
                    {
                        Log.i(TAG, "onItemClicked: "+((TileServiceOuterClass.MovieTile) item).getMetadata().getTitle());
                        if(packageManager == null){
                            packageManager = itemViewHolder.view.getContext().getPackageManager();
                        }
                        handleTileClicked((TileServiceOuterClass.MovieTile) item, packageManager, itemViewHolder.view.getContext());
                    }
                }
            });


            setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
                @Override
                public void onItemSelected(Presenter.ViewHolder viewHolder, Object o, RowPresenter.ViewHolder viewHolder1, Row row) {
                    if(o instanceof TileServiceOuterClass.MovieTile)
                    Log.i(TAG, "onItemSelected: "+((TileServiceOuterClass.MovieTile) o).getMetadata().getTitle());
                }
            });
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.i(TAG, "onCreate: ");
            createRows();
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }

        private void createRows() {
            try {
                SchedularServiceOuterClass.LauncherPage launcherPage = SchedularServiceOuterClass
                        .LauncherPage
                        .parseFrom(getArguments().getByteArray("launcherPage"));

                for(final SchedularServiceOuterClass.LauncherRows launcherRows : launcherPage.getLauncherRowsList()) {
                    TileServiceGrpc.TileServiceStub tileServiceStub = TileServiceGrpc.newStub(tileServiceChannel);

                    tileServiceStub.getMovieTiles(TileServiceOuterClass
                            .RowId
                            .newBuilder()
                            .setRowId(launcherRows.getRowId())
                            .setGetFullData(false)
                            .build(), new StreamObserver<TileServiceOuterClass.MovieTile>() {
                        ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(new CustomCard());
                        @Override
                        public void onNext(TileServiceOuterClass.MovieTile value) {
                            rowAdapter.add(value);
                        }

                        @Override
                        public void onError(Throwable t) {
                            Log.e(TAG, "onError: Tiles ", t);
                        }

                        @Override
                        public void onCompleted() {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ListRow listRow = new ListRow(new HeaderItem(launcherRows.getRowName()), rowAdapter);
                                    mRowsAdapter.add(listRow);
                                }
                            });
                        }
                    });
                }

            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }

        private void handleTileClicked(@NonNull TileServiceOuterClass.MovieTile movieTile, PackageManager packageManager, Context context){
            Log.i(TAG, "handleTileClicked: 1 "+movieTile.getContent().getPackage());

            String tilePackageName = "";
            //packageName Manupilation
            if(movieTile.getContent().getPackage().contains("youtube")){
                tilePackageName = "com.google.android.youtube.tv";
            }else {
                tilePackageName = movieTile.getContent().getPackage();
            }

            // check if package installed or not
            if(!isPackageInstalled(tilePackageName,packageManager)){
                Log.i(TAG, "handleTileClicked: 2");
                //TODO download the App
            }
            else if(tilePackageName.contains("youtube")) {
                Log.i(TAG, "handleTileClicked: 3");
                startYoutube(movieTile.getContent().getType(), context, movieTile.getContent().getTarget(0));
            }
            else if(movieTile.getContent().getType().equals("START"))
            {
                Log.i(TAG, "handleTileClicked: 4");
                Intent intent = packageManager.getLaunchIntentForPackage(tilePackageName);
                boolean isPackageFound = false;
                if (intent == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        intent = packageManager.getLeanbackLaunchIntentForPackage(tilePackageName);
                        if (intent == null) {
                            isPackageFound = false;
                        }else {
                            isPackageFound = true;
                        }
                    }
                }else{
                    isPackageFound = true;
                }
                Log.i(TAG, "handleTileClicked: 5 "+isPackageFound);
                if(!isPackageFound){
                    return;
                }else {
                    Log.i(TAG, "handleTileClicked: 6");
                    intent.setData(Uri.parse(movieTile.getContent().getTarget(0).replace("http://www.hotstar.com", "hotstar://content")));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        }

        private void startYoutube(@NonNull String type, Context context, String target) {
            if (type.compareToIgnoreCase("PLAY_VIDEO") == 0 || type.compareToIgnoreCase("CWYT_VIDEO") == 0) {
                Intent intent = YouTubeIntents.createPlayVideoIntentWithOptions(context, target, true, true);
                intent.setPackage("com.google.android.youtube.tv");
                context.startActivity(intent);
            } else if (type.compareToIgnoreCase("OPEN_PLAYLIST") == 0) {
                Intent intent = YouTubeIntents.createOpenPlaylistIntent(context, target);
                intent.setPackage("com.google.android.youtube.tv");
                intent.putExtra("finish_on_ended", true);
                context.startActivity(intent);
            } else if (type.compareToIgnoreCase("PLAY_PLAYLIST") == 0 || type.compareToIgnoreCase("CWYT_PLAYLIST") == 0) {
                Intent intent = YouTubeIntents.createPlayPlaylistIntent(context, target);
                intent.setPackage("com.google.android.youtube.tv");
                intent.putExtra("finish_on_ended", true);
                context.startActivity(intent);
            } else if (type.compareToIgnoreCase("OPEN_CHANNEL") == 0) {
                Intent intent = YouTubeIntents.createChannelIntent(context, target);
                intent.setPackage("com.google.android.youtube.tv");
                intent.putExtra("finish_on_ended", true);
                context.startActivity(intent);
            } else if (type.compareToIgnoreCase("OPEN_USER") == 0) {
                Intent intent = YouTubeIntents.createUserIntent(context, target);
                context.startActivity(intent);
            } else if (type.compareToIgnoreCase("OPEN_SEARCH") == 0) {
                Intent intent = YouTubeIntents.createSearchIntent(context, target);
                context.startActivity(intent);
            }
        }

    }

    private static boolean isPackageInstalled(String packagename, @NonNull PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packagename, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(networkChangeReceiver);
    }
}
