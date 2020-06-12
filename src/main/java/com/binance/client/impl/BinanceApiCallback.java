package com.binance.client.impl;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;


public class BinanceApiCallback implements Callback {

    @Override
    public void onFailure(Call call, IOException e) {

    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {

    }

//    public void onResponse(Call<T> call, Response<T> response) {
//        if (response.isSuccessful()) {
//            callback.onResponse(response.body());
//        } else {
//            if (response.code() == 504) {
//                // HTTP 504 return code is used when the API successfully sent the message but not get a response within the timeout period.
//                // It is important to NOT treat this as a failure; the execution status is UNKNOWN and could have been a success.
//                return;
//            }
//            try {
//                BinanceApiError apiError = getBinanceApiError(response);
//                onFailure(call, new BinanceApiException(apiError));
//            } catch (IOException e) {
//                onFailure(call, new BinanceApiException(e));
//            }
//        }
//    }
//
//    @Override
//    public void onFailure(Call<T> call, Throwable throwable) {
//        System.out.println("BinanceApiCallback.onFailure()" + throwable);
//        if (throwable instanceof BinanceApiException) {
//            callback.onFailure(throwable);
//        } else {
//            callback.onFailure(new BinanceApiException(throwable));
//        }
//    }
}