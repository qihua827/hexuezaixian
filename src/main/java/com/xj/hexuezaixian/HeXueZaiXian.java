package com.xj.hexuezaixian;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
@Component
public class HeXueZaiXian {
    String accessToken;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Async
    @PostConstruct
    public void openshua() throws IOException, InterruptedException {
        int maxRetry = 5; // 最多重试 5 次
        int retryCount = 0;

        while (true) {
            accessToken  =redisTemplate.opsForValue().get("shua");


            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, "{\r\n    \"isPass\": 0,\r\n    \"order\": \"\",\r\n    \"orderField\": \"\",\r\n    \"pageNum\": 1,\r\n    \"pageSize\": 1000\r\n}");
            Request request = new Request.Builder()
                    .url("http://api.jxjyzx.qdu.edu.cn/LearningSpace/list")
                    .post(body)
                    .addHeader("Access-Token", accessToken)
                    .addHeader("User-Agent", "Apifox/1.0.0")
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = client.newCall(request).execute();
            String string = response.body().string();
            response.close();

            JSONObject entries = JSONUtil.parseObj(string);
            String dataStr = entries.getStr("data", "");

            if (dataStr.isEmpty()) {
                System.err.println("[WARN] API 返回 data 为空，可能是 Token 失效或服务器问题，等待 10 秒重试...");
                retryCount++;

                if (retryCount >= maxRetry) {
                    System.err.println("[ERROR] 连续 " + maxRetry + " 次 API 响应异常，可能需要手动检查！停止调用");
                    break;

                }

                Thread.sleep(10_000);
                continue;
            }

            retryCount = 0; // 如果成功获取数据，重置重试计数
            JSONObject dataObj = JSONUtil.parseObj(dataStr);
            JSONArray listObj = JSONUtil.parseArray(dataObj.getStr("list", "[]"));

            for (Object listOb : listObj) {
                JSONObject entries1 = JSONUtil.parseObj(listOb);
                String interactiveScore = entries1.getStr("interactiveScore", "0");
                if (!"100.0".equals(interactiveScore)) {
                    String classId = entries1.getStr("id", "");
                    openShuaKe(classId, accessToken);
                    break;
                }
            }
        }
    }

    private static void openShuaKe(String classId,String  accessToken) throws IOException, InterruptedException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
//        MediaType mediaType = MediaType.parse("text/plain");
//        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url("http://api.jxjyzx.qdu.edu.cn/studyLearn/courseDirectoryProcess?courseOpenId="+classId)
                .method("GET",null)
                .addHeader("Access-Token", accessToken)
                .addHeader("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
                .addHeader("Accept", "*/*")
                .addHeader("Host", "api.jxjyzx.qdu.edu.cn")
                .addHeader("Connection", "keep-alive")
                .build();
                Response response = client.newCall(request).execute();
                String string = response.body().string();
                JSONObject entries = JSONUtil.parseObj(string);
                String dataStr = entries.get("data").toString();
                JSONObject dataObj = JSONUtil.parseObj(dataStr);
                JSONArray objects = JSONUtil.parseArray(dataObj.get("moduleList").toString());
                response.close();

        for (Object object : objects) {
            JSONObject entries1 = JSONUtil.parseObj(object);
            String percent = entries1.get("percent").toString();

            if (!percent.equals("100")){
                rellShua(accessToken, entries1);
                return;
            }

        }

                System.out.println(objects);
    }

    private static void rellShua(String accessToken, JSONObject entries1) throws IOException, InterruptedException {
        String topics = entries1.get("topics").toString();
        JSONArray topicsArr = JSONUtil.parseArray(topics);
        for (Object o : topicsArr) {
            JSONObject topic = JSONUtil.parseObj(o);
            Object cells = topic.get("cells");
            String className = topic.get("name").toString();
            JSONArray cellsArr = JSONUtil.parseArray(cells);
            for (Object cell : cellsArr) {
                JSONObject cellObj = JSONUtil.parseObj(cell);
                if (!cellObj.get("process").toString().equals("100")){
                    System.out.println("开始刷"+ className);
                    rellRellShua(cellObj,accessToken);
                    return;
                }
            }
        }



    }

    private static void rellRellShua(JSONObject cellObj, String accessToken) throws IOException, InterruptedException {
        String id = cellObj.get("id").toString();


        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url("http://api.jxjyzx.qdu.edu.cn/studyLearn/cellDetail?cellId="+id)
                .method("GET", null)
                .addHeader("Accept", " application/json, text/plain, */*")
                .addHeader("Accept-Language", " zh-CN,zh;q=0.9,en;q=0.8")
                .addHeader("Connection", " keep-alive")
                .addHeader("Host", " api.jxjyzx.qdu.edu.cn")
                .addHeader("Origin", " http://student.jxjyzx.qdu.edu.cn")
                .addHeader("Referer", " http://student.jxjyzx.qdu.edu.cn/")
                .addHeader("User-Agent", " Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .addHeader("access-token",accessToken)
                .build();
                 Response response = client.newCall(request).execute();

                 String string = response.body().string();
                JSONObject entries = JSONUtil.parseObj(string);
                 Object data = entries.get("data");
                 JSONObject data1 = JSONUtil.parseObj(data);
                 String cellLogId = data1.get("cellLogId").toString();
                 response.close();

                 rellOpenShua(cellLogId,accessToken);

    }

    private static void rellOpenShua(String cellLogsId, String accessToken) throws IOException, InterruptedException {
        for (int i = 0; i <=3; i++) {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, "{\"id\":\""+cellLogsId+"\",\"stopSeconds\":0,\"videoEndTime\":600}");
            Request request = new Request.Builder()
                    .url("http://api.jxjyzx.qdu.edu.cn/studyLearn/leaveCellLog")
                    .method("POST", body)
                    .addHeader("Accept", " application/json, text/plain, */*")
                    .addHeader("Accept-Language", " zh-CN,zh;q=0.9,en;q=0.8")
                    .addHeader("Connection", " keep-alive")
                    .addHeader("Host", " api.jxjyzx.qdu.edu.cn")
                    .addHeader("Origin", " http://student.jxjyzx.qdu.edu.cn")
                    .addHeader("Referer", " http://student.jxjyzx.qdu.edu.cn/")
                    .addHeader("User-Agent", " Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    .addHeader("access-token", accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = client.newCall(request).execute();
            response.close();


            Thread.sleep(40000);

            System.out.println("调用API记录学习时间"+ LocalDateTimeUtil.now());

        }

    }
}
