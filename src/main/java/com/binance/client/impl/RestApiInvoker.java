package com.binance.client.impl;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.binance.client.exception.BinanceApiException;
import com.binance.client.impl.utils.JsonWrapper;

import java.io.IOException;

abstract class RestApiInvoker {

    private static final Logger log = LoggerFactory.getLogger(RestApiInvoker.class);
    private static final OkHttpClient client = new OkHttpClient();

    static void checkResponse(JsonWrapper json) {
        try {
            if (json.containKey("success")) {
                boolean success = json.getBoolean("success");
                if (!success) {
                    String err_code = json.getStringOrDefault("code", "");
                    String err_msg = json.getStringOrDefault("msg", "");
                    if ("".equals(err_code)) {
                        throw new BinanceApiException(BinanceApiException.EXEC_ERROR, "[Executing] " + err_msg);
                    } else {
                        throw new BinanceApiException(BinanceApiException.EXEC_ERROR,
                                "[Executing] " + err_code + ": " + err_msg);
                    }
                }
            } else if (json.containKey("code")) {

                int code = json.getInteger("code");
                if (code != 200) {
                    String message = json.getStringOrDefault("msg", "");
                    throw new BinanceApiException(BinanceApiException.EXEC_ERROR,
                            "[Executing] " + code + ": " + message);
                }
            }
        } catch (BinanceApiException e) {
            throw e;
        } catch (Exception e) {
            throw new BinanceApiException(BinanceApiException.RUNTIME_ERROR,
                    "[Invoking] Unexpected error: " + e.getMessage());
        }
    }

    static <T> T callSync(RestApiRequest<T> request) {
        try {
            String str;
            log.debug("Request URL " + request.request.url());
            Response response = client.newCall(request.request).execute();
            // System.out.println(response.body().string());
            if (response != null && response.body() != null) {
                str = response.body().string();
                Headers headers = response.headers();
                for (String hName : headers.names()) {
                    if (hName.startsWith("x-mbx-used-weight")) {
                        log.debug("Header: " + hName + " : " + headers.values(hName));
                    }
                }

                if (response.code() == 429) { // HTTP 429 return code is used when breaking a request rate limit.
                    log.error("[Invoking] IP is breaking request rate limit. http error code 429");
                    headers = response.headers();
                    for (String hName : headers.names()) {
                        log.debug("Header: " + hName + " : " + headers.values(hName));
                    }
                    Thread.sleep(61_000L);
                } else if (response.code() == 418) { // HTTP 418 return code is used when an IP has been auto-banned for continuing to send requests after receiving 429 codes.
                    log.error("[Invoking] IP is baned. http error code 418");
                    headers = response.headers();
                    for (String hName : headers.names()) {
                        log.debug("Header: " + hName + " : " + headers.values(hName));
                    }
                    throw new BinanceApiException("BANNED", "[Invoking] IP is banned");
                }

                response.close();
            } else {
                throw new BinanceApiException(BinanceApiException.ENV_ERROR,
                        "[Invoking] Cannot get the response from server");
            }
            //log.debug("Response =====> " + str); // FIXME
            JsonWrapper jsonWrapper = JsonWrapper.parseFromString(str);
            checkResponse(jsonWrapper);
            return request.jsonParser.parseJson(jsonWrapper);
        } catch (BinanceApiException e) {
            throw e;
        } catch (Exception e) {
            throw new BinanceApiException(BinanceApiException.ENV_ERROR,
                    "[Invoking] Unexpected error: " + e.getMessage());
        }
    }

//    static <T> T callASync(RestApiRequest<T> request, ) {
//        try {
//            String str;
//            log.debug("Request URL " + request.request.url());
//            Call call = client.newCall(request.request);
//            call.enqueue(new Callback() {
//                @Override
//                public void onFailure(Call call, IOException e) {
//
//                }
//
//                @Override
//                public void onResponse(Call call, Response response) throws IOException {
//
//                }
//            });
//
//
//            // System.out.println(response.body().string());
//            if (response != null && response.body() != null) {
//                str = response.body().string();
//                Headers headers = response.headers();
//                for (String hName : headers.names()) {
//                    if (hName.startsWith("x-mbx-used-weight")) {
//                        log.debug("Header: " + hName + " : " + headers.values(hName));
//                    }
//                }
//
//                if (response.code() == 429) { // HTTP 429 return code is used when breaking a request rate limit.
//                    log.error("[Invoking] IP is breaking request rate limit. http error code 429");
//                    headers = response.headers();
//                    for (String hName : headers.names()) {
//                        log.debug("Header: " + hName + " : " + headers.values(hName));
//                    }
//                    Thread.sleep(61_000L);
//                } else if (response.code() == 418) { // HTTP 418 return code is used when an IP has been auto-banned for continuing to send requests after receiving 429 codes.
//                    log.error("[Invoking] IP is baned. http error code 418");
//                    headers = response.headers();
//                    for (String hName : headers.names()) {
//                        log.debug("Header: " + hName + " : " + headers.values(hName));
//                    }
//                    throw new BinanceApiException("BANNED", "[Invoking] IP is banned");
//                }
//
//                response.close();
//            } else {
//                throw new BinanceApiException(BinanceApiException.ENV_ERROR,
//                        "[Invoking] Cannot get the response from server");
//            }
//            //log.debug("Response =====> " + str); // FIXME
//            JsonWrapper jsonWrapper = JsonWrapper.parseFromString(str);
//            checkResponse(jsonWrapper);
//            return request.jsonParser.parseJson(jsonWrapper);
//        } catch (BinanceApiException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new BinanceApiException(BinanceApiException.ENV_ERROR,
//                    "[Invoking] Unexpected error: " + e.getMessage());
//        }
//    }

    static WebSocket createWebSocket(Request request, WebSocketListener listener) {
        return client.newWebSocket(request, listener);
    }

}
