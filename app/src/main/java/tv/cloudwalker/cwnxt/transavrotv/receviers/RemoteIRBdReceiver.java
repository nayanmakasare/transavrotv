package tv.cloudwalker.cwnxt.transavrotv.receviers;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import tv.cloudwalker.cwnxt.transavrotv.BuildConfig;

public class RemoteIRBdReceiver extends BroadcastReceiver {

    private int counter = 0;

    private ThreadPoolExecutor threadPoolExecutor = null;
    private KillBackGroundAppRunnable runnable = null;



    public RemoteIRBdReceiver(ActivityManager activityManager, Instrumentation instrumentation, ThreadPoolExecutor threadPoolExecutor){
//        this.threadPoolExecutor = threadPoolExecutor;
//        runnable = new KillBackGroundAppRunnable(activityManager, instrumentation);
    }

    private RemoteIRBdReceiver(){

    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
//        if (intent.getExtras().getString("buttonkeys") != null) {
//            counter ++;
//            int keyCode = Integer.parseInt(intent.getExtras().getString("buttonkeys"));
//            if(counter % 2 == 0 && (keyCode < 19 || keyCode > 22)) {
//                counter = 0;
//                if(keyCode == 4){
//                    threadPoolExecutor.execute(runnable);
//                }
//            }
//        }
    }


    private class KillBackGroundAppRunnable implements Runnable {
        private ActivityManager activityManager = null;
        private Instrumentation instrumentation = null;

        private KillBackGroundAppRunnable(ActivityManager activityManager, Instrumentation instrumentation){
            this.activityManager = activityManager;
            this.instrumentation = instrumentation;
        }

        @Override
        public void run() {
            try{
                String packageToKill = activityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
                if(packageToKill.equals(BuildConfig.APPLICATION_ID)){
                    Thread.interrupted();
                }else {
                    instrumentation.sendKeyDownUpSync(3);
                    // this delay is important
                    Thread.sleep(1500);
                    List<ActivityManager.RunningAppProcessInfo> listOfProcess=activityManager.getRunningAppProcesses();
                    for(ActivityManager.RunningAppProcessInfo process:listOfProcess) {
                        if(process.processName.equals(packageToKill)){
                            android.os.Process.killProcess(process.pid);
                            android.os.Process.sendSignal(process.pid, android.os.Process.SIGNAL_KILL);
                            activityManager.killBackgroundProcesses(process.processName);
                            break;
                        }
                    }
                    listOfProcess = null;
                    packageToKill = null;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
