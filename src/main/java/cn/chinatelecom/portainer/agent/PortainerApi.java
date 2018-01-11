/*
 *  Copyright 2017 WalleZhang
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cn.chinatelecom.portainer.agent;

import cn.chinatelecom.portainer.agent.model.AuthRequest;
import cn.chinatelecom.portainer.agent.model.AuthResponse;
import cn.chinatelecom.portainer.agent.model.UpdateEndpointRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * 发起到portainer的自注册请求
 *
 * @author WalleZhang
 */
public class PortainerApi {
    private final static CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    private final static String AUTH_URL = "/api/auth";
    private final static String ENDPOINTS_URL = "/api/endpoints";
    private final static String BASE_URL = System.getProperty("PORTAINER_API_URL");
    private final static String USERNAME = System.getProperty("PORTAINER_USERNAME");
    private final static String PASSWORD = System.getProperty("PORTAINER_PASSWORD");
    private static String LOCAL_IP;

    static {
        try {
            LOCAL_IP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static String auth() {
        HttpPost httpPost = new HttpPost(BASE_URL + AUTH_URL);
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername(USERNAME);
        authRequest.setPassword(PASSWORD);

        StringEntity stringEntity = new StringEntity(JSON.toJSONString(authRequest), ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringEntity);
        CloseableHttpResponse response = null;

        try {
            response = HTTP_CLIENT.execute(httpPost);
            AuthResponse authResponse = JSON.parseObject(EntityUtils.toString(response.getEntity()), AuthResponse.class);
            return authResponse.getJwt();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static int getEndpointId(String authToken) throws Exception {
        HttpGet httpGet = new HttpGet(BASE_URL + ENDPOINTS_URL);
        httpGet.addHeader("Authorization", authToken);

        CloseableHttpResponse response = null;
        List<Map<String, Object>> endpoints = null;
        try {
            response = HTTP_CLIENT.execute(httpGet);
            endpoints = JSON.parseObject(EntityUtils.toString(response.getEntity()), new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (endpoints == null) {
            throw new Exception("获取Endpoints失败！");
        }

        for (Map<String, Object> endpoint : endpoints) {
            if (LOCAL_IP.equals(endpoint.get("Name"))) {
                return (int) endpoint.get("Id");
            }
        }

        return -1;
    }

    public static void registerEndpoint() throws Exception {
        String token = auth();
        UpdateEndpointRequest updateEndpointRequest = new UpdateEndpointRequest();
        updateEndpointRequest.setName(LOCAL_IP);
        updateEndpointRequest.setPublicUrl(LOCAL_IP + ":5000");
        updateEndpointRequest.setUrl(LOCAL_IP + ":5000");

        HttpEntityEnclosingRequestBase requestBase;

        int id = getEndpointId(token);

        if (id > 0) {
            // 若Endpoint已存在，则更新
            requestBase = new HttpPut(BASE_URL + ENDPOINTS_URL + "/" + id);
        } else {
            // 不存在，新建
            requestBase = new HttpPost(BASE_URL + ENDPOINTS_URL);
        }

        requestBase.addHeader("Authorization", token);
        requestBase.setEntity(new StringEntity(JSON.toJSONString(updateEndpointRequest)));

        HTTP_CLIENT.execute(requestBase);
    }
}
