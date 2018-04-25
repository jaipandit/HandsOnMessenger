package j.com.handsonmessenger;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * This is how the intent service is implemented.
 */
public class MessengerService extends Service {

    public static String TAG = MessengerService.class.getSimpleName();
    public static String KEY_MSG = "key_msg";


    private ServiceThread workerThread;
    private Handler handler;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStart command");
        super.onStartCommand(intent, flags, startId);

        Message msg = new Message();
        msg.what = 1;
        msg.arg1 = startId;
        msg.setTarget(handler);
        msg.sendToTarget();
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "service created");
        workerThread = new ServiceThread();
        workerThread.start();

        handler = new Handler(workerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                // the msgs will be processes here on the worker theres.
                if (msg.what == 1) {
                    Log.i(TAG, "Handing msg on thread Id: " + Process.myTid());
                }
                stopSelf(msg.arg1);
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        workerThread.quit();
        Log.i(TAG, "service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * A nice implementation of Thread, which waits till the looper initializes.
     * This is how the HandlerThread is also implemented.
     */
    private class ServiceThread extends Thread {

        private Looper looper;
        private int threadId;

        /**
         * Blocking api.
         */
        @NonNull
        public Looper getLooper() {
            synchronized (this) {
                while (looper == null) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return looper;
        }

        public void run() {
            threadId = Process.myTid();
            Log.i(TAG, "Service Worker thread id: " + threadId);
            Looper.prepare();

            synchronized (this) {
                looper = Looper.myLooper();
                this.notifyAll();
            }
            // block the thread.
            Looper.loop();
        }

        public void quit() {
            Looper looper = getLooper();
            if (looper != null) {
                looper.quit();
            }
        }
    }
}
