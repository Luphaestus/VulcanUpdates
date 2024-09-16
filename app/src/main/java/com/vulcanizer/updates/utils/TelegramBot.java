package com.vulcanizer.updates.utils;


import static com.vulcanizer.updates.utils.SensitiveInfo.CHAT_ID;
import static com.vulcanizer.updates.utils.SensitiveInfo.CHAT_ID_PERSON;
import static com.vulcanizer.updates.utils.SensitiveInfo.TELEGRAM_BOT_TOKEN;

import android.util.Log;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelegramBot {


    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage";

    private final OkHttpClient client = new OkHttpClient();

    // Create a single-threaded executor for background work
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void sendMessage(String message) {
        // Run the network operation in the background thread
        executorService.execute(() -> {
            try {
                JSONObject jsonObject = new JSONObject();

                jsonObject.put("chat_id", CHAT_ID);
                jsonObject.put("text", message);

                RequestBody body = RequestBody.create(
                        jsonObject.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(TELEGRAM_API_URL)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();

            } catch (Exception e) {
                Log.d("sendcrashouter", e.toString());
            }
        });
    }

    public void newperson(String message) {
        // Run the network operation in the background thread
        // Run the network operation in the background thread


        executorService.execute(() -> {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("chat_id", CHAT_ID_PERSON);
                jsonObject.put("text", message);

                RequestBody body = RequestBody.create(
                        jsonObject.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(TELEGRAM_API_URL)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();

            } catch (Exception ignored) {
            }
        });
    }
}
