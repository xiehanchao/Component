package com.example.common.okhttp.ok;


import android.content.Context;
import android.util.Log;


import com.example.common.okhttp.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**********************************************************************
 *
 *
 * @类名 OkHttpBuilder
 * @包名 com.hotwheels.ok
 * @author 谢晗超
 * @创建日期 2018/5/13
 ***********************************************************************/
public class OkHttpBuilder {
    /**
     * DEBUG标记
     */
    public static final String TAG = OkHttpBuilder.class.getSimpleName();
    /**
     * 默认请求连接超时时间（秒）
     */
    private static final long DEFAULT_CONN_TIMEOUT = 30L;

    /**
     * 默认请求读取超时时间（秒）
     */
    private static final long DEFAULT_READ_TIMEOUT = 30L;

    /**
     * 默认请求写入超时时间（秒）
     */
    private static final long DEFAULT_WRITE_TIMEOUT = 30L;
    /**
     * 连接超时时间（秒）
     */
    private long mConnectTimeout = DEFAULT_CONN_TIMEOUT;

    /**
     * 读取超时时间（秒）
     */
    private long mReadTimeOut = DEFAULT_READ_TIMEOUT;

    /**
     * 读取超时时间（秒）
     */
    private long mWriteTimeOut = DEFAULT_WRITE_TIMEOUT;

    /**
     * 拦截器列表
     */
    private List<Interceptor> mInterceptors = new ArrayList<>();

    private List<Interceptor> mCache = new ArrayList<>();

    public  static Context context;

    public OkHttpBuilder() {
        this.mInterceptors.add(this.getLogInterceptor());
        //this.mCache.add(new CacheInterceptor());
    }

    class CacheInterceptor implements Interceptor{

        @Override
        public Response intercept(Chain chain) throws IOException {

            Response originResponse = chain.proceed(chain.request());

            //设置缓存时间为60秒，并移除了pragma消息头，移除它的原因是因为pragma也是控制缓存的一个消息头属性
            return originResponse.newBuilder().removeHeader("pragma")
                    .header("Cache-Control","max-age=60").build();
        }
    }

    /**
     * 取得日志拦截器
     *
     * @return 日志拦截器
     */
    private Interceptor getLogInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Log.e("XXX", "222");

                Request request = chain.request();

                // 构建请求地址
                HttpUrl url = request.url();

                // 构建请求头
                Headers headers = new Headers.Builder()
                        .add("Content-Type", "application/x-www-form-urlencoded")
                        .add("Accept", "application/json")
                        .add("Accept-Charset", "utf-8")
                        .add("ContentType", "utf-8")
                        .build();
                StringBuilder headerBuilder = new StringBuilder();
                for (int i = 0; i < headers.size(); i++) {
                    if (i == 0) {
                        headerBuilder.append(headers.name(i) + ": " + headers.value(i));
                    } else {
                        headerBuilder.append(" | " + headers.name(i) + ": " + headers.value(i));
                    }
                }

                // 构建请求体
                RequestBody requestBody = null;
                String requestContent = "";
                if (request.body() != null) {
                    MediaType mediaType = request.body().contentType();
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);
                    requestContent = IOUtils.read(buffer.inputStream());
                    requestBody = RequestBody.create(mediaType, requestContent);
                }


                // 构建新请求
                Request.Builder requestBuilder = request.newBuilder();
                requestBuilder.url(url).headers(headers);
                if (requestBody != null && !request.method().equals("GET")) {
                    requestBuilder.method(request.method(),requestBody);
                }
                CacheControl FORCE_CACHE = new CacheControl.Builder()
                        .onlyIfCached()
                        .maxStale(5, TimeUnit.SECONDS)
                        .build();
                //requestBuilder.cacheControl(CacheControl.FORCE_CACHE);
                Request newRequest = requestBuilder.build();

                // 打印请求数据
                StringBuilder requestLogBuilder = new StringBuilder();
                requestLogBuilder.append("请求地址：" + newRequest.url().toString() + "\r\n");
                requestLogBuilder.append("请求方式：" + newRequest.method() + "\r\n");
                requestLogBuilder.append("请求头：" + headerBuilder.toString() + "\r\n");
                requestLogBuilder.append("请求体：" + requestContent + "\r\n");
                Log.i(TAG, requestLogBuilder.toString());

                // 网络请求，取得响应数据
                Response response = chain.proceed(newRequest);

                // 构建响应体
                String responseContent = response.body().string();
                MediaType mediaTypeResponse = response.body().contentType();
                ResponseBody responseBody = ResponseBody.create(mediaTypeResponse, responseContent);
                // 构建新响应
                Response newResponse = response.newBuilder()
                        .body(responseBody)
                        .build();

                // 打印响应数据
                StringBuilder responseLogBuilder = new StringBuilder();
                responseLogBuilder.append("响应码：" + newResponse.code() + "\r\n");
                responseLogBuilder.append("响应体：" + responseContent + "\r\n");
                Log.i(TAG, responseLogBuilder.toString());

                return newResponse;
            }
        };
    }



    /**
     * 构建OkHttpClient对象
     *
     * @return OkHttpClient对象
     */
    public OkHttpClient build() {

        //缓存文件夹
        File cacheFile = new File(context.getExternalCacheDir().toString(),"cache11");
        System.out.println("cacheFile = " + cacheFile);
        //缓存大小为10M
        int cacheSize = 10 * 1024 * 1024;
        //创建缓存对象
        final Cache cache = new Cache(cacheFile,cacheSize);
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(mConnectTimeout, TimeUnit.SECONDS)
                .readTimeout(mReadTimeOut, TimeUnit.SECONDS)
                .writeTimeout(mWriteTimeOut, TimeUnit.SECONDS);
                //.cache(cache);
        for (Interceptor interceptor : mInterceptors) {
            builder.addInterceptor(interceptor);
        }

        for (Interceptor interceptor : mCache) {
            builder.addNetworkInterceptor(interceptor);
        }
        return builder.build();
    }

}
