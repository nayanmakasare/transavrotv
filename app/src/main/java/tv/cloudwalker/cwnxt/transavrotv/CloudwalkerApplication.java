package tv.cloudwalker.cwnxt.transavrotv;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Instrumentation;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.rabbitmq.client.ConnectionFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import SchedularService.SchedularServiceGrpc;
import SchedularService.SchedularServiceOuterClass;
import VendorService.VendorServiceGrpc;
import VendorService.VendorServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import tv.cloudwalker.cwnxt.transavrotv.receviers.RemoteIRBdReceiver;

public class CloudwalkerApplication extends Application
{
    private SharedPreferences sharedPreferences = null;
    private static final String TAG = "CloudwalkerApplication";
    private RemoteIRBdReceiver rcv;
    private ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    public ConnectionFactory factory = new ConnectionFactory();


    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        getSharedPreferences();
        checkScheduleValidation();
//        settingReceiver();
        getAsyncVendorBrandSepc();
        setupConnectionFactory();
    }


    private void setupConnectionFactory() {
        try {
            factory.setAutomaticRecoveryEnabled(false);
            factory.setUri("amqp://blwmcjci:vUyxgSpStc5IgHuSi4WFdVPVnxQ8qR57@barnacle.rmq.cloudamqp.com/blwmcjci");
        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
            e1.printStackTrace();
        }
    }


    public SharedPreferences getSharedPreferences() {
        if(sharedPreferences == null){
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        }
        return sharedPreferences;
    }

    private void settingReceiver() {
        rcv = new RemoteIRBdReceiver(((ActivityManager) getSystemService(ACTIVITY_SERVICE)), new Instrumentation(), threadPoolExecutor);
        String CLOUDWALKER_REMOTE_BROADCAST = "tv.cloudwalker.ir.REMOTE";
        IntentFilter mIntentFilter = new IntentFilter(CLOUDWALKER_REMOTE_BROADCAST);
        registerReceiver(rcv, mIntentFilter);
    }

    private void checkScheduleValidation(){
        Calendar rightNow = Calendar.getInstance();
        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
        String scheduleHour = sharedPreferences.getString("schedule", "0-0");
        Log.i(TAG, "checkScheduleValidation: "+scheduleHour);
        if(scheduleHour.equals("0-0")) {
            Log.i(TAG, "checkScheduleValidation: settings schedule for the first time");
            getSchedule();
        }else{
            String[] scheduleLimit = scheduleHour.split("-");
            int lowerLimit = Integer.parseInt(scheduleLimit[0]);
            int upperLimit = Integer.parseInt(scheduleLimit[1]);
            //check the current hour is with in the schedule
            Log.i(TAG, "checkScheduleValidation: checking "+lowerLimit+" "+upperLimit+" "+currentHour);
            if(currentHour >= lowerLimit && currentHour < upperLimit){
                Log.d(TAG, "checkScheduleValidation: with in schedule ");
            }else {
                Log.i(TAG, "checkScheduleValidation: not in schedule "+currentHour);
                getSchedule();
            }
        }
    }

    public SchedularServiceOuterClass.UserScheduleResponse getSchedule() {
        //blocking call
        Log.i(TAG, "getSchedule: 1");
        ManagedChannel managedChannel  = ManagedChannelBuilder.forAddress("192.168.1.143", 50055).usePlaintext().build();
        SchedularServiceGrpc.SchedularServiceBlockingStub schedularServiceBlockingStub = SchedularServiceGrpc.newBlockingStub(managedChannel);
        SchedularServiceOuterClass.UserScheduleResponse value = schedularServiceBlockingStub.getSchedule(SchedularServiceOuterClass
                .RequestSchedule
                .newBuilder()
                .setVendor("cvte")
                .setBrand("shinko")
                .build());

        Log.i(TAG, "getSchedule: 2");
        if(value != null){
            try {
                Log.i(TAG, "getSchedule: 3");
                File scheduleFile = new File(getFilesDir().getAbsoluteFile() + "/schedule.proto");
                if(scheduleFile.exists()){
                    scheduleFile.delete();
                }
                OutputStream output = new FileOutputStream(scheduleFile);
                value.writeTo(output);
                output.flush();
                output.close();
                setScheduleToPref("schedule");
                Log.i(TAG, "getSchedule: 4");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            managedChannel.shutdown();
            return value;
        }
        else {
            managedChannel.shutdown();
            return null;
        }
    }

    public void getAsyncVendorBrandSepc(){
        Log.i(TAG, "getAsyncVendorBrandSepc: ");
        final ManagedChannel managedChannel  = ManagedChannelBuilder.forAddress("192.168.1.143", 50053).usePlaintext().build();
        VendorServiceGrpc.VendorServiceStub vendorServiceStub = VendorServiceGrpc.newStub(managedChannel);
        vendorServiceStub.getVendorSpecification(VendorServiceOuterClass.VendorRequestSpecification
                .newBuilder()
                .setVendor("cvte")
                .setBrand("shinko").build(), new StreamObserver<VendorServiceOuterClass.VendorBrandSpecification>() {
            @Override
            public void onNext(VendorServiceOuterClass.VendorBrandSpecification value) {
                Log.i(TAG, "onNext: vendorSpec ");
                if(value != null){
                    try {
                        File brandSpecification = new File(getFilesDir().getAbsoluteFile() + "/brandSpec.proto");
                        if(brandSpecification.exists()){
                            brandSpecification.delete();
                        }
                        OutputStream output = new FileOutputStream(brandSpecification);
                        value.writeTo(output);
                        output.flush();
                        output.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.i(TAG, "onError: vendorSpec "+t.getMessage());
                if(!managedChannel.isShutdown())
                    managedChannel.shutdownNow();
            }

            @Override
            public void onCompleted() {
                Log.i(TAG, "onCompleted: ");
                if(!managedChannel.isShutdown())
                    managedChannel.shutdownNow();
            }
        });
    }

    public String ifResourceAvaliableFileName(String url){
        String[] segments = url.split("/");
        String resourceFileName = segments[segments.length - 1];
        File resourceFile = new File(getFilesDir()+resourceFileName);
        segments = null;
        resourceFileName = null;
        if(resourceFile.exists()){
            return resourceFile.getAbsolutePath();
        }else {
            return null;
        }
    }

    private void setScheduleToPref(String key){
        if(sharedPreferences == null){
            getSharedPreferences();
        }
        Calendar rightNow = Calendar.getInstance();
        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if(currentHour >= 6 && currentHour < 9){
            editor.putString(key, "6-9");
        }else if(currentHour >= 9 && currentHour < 12){
            editor.putString(key, "9-12");
        }else if(currentHour >= 12 && currentHour < 15){
            editor.putString(key, "12-15");
        }else if(currentHour >= 15 && currentHour < 18){
            editor.putString(key, "15-18");
        }else if(currentHour >= 18 && currentHour < 21){
            editor.putString(key, "18-21");
        }else if(currentHour >= 21 && currentHour < 24){
            editor.putString(key, "21-24");
        }else if(currentHour >= 24){
            editor.putString(key, "1-6");
        }
        editor.apply();
        editor.commit();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(rcv);
    }
}
