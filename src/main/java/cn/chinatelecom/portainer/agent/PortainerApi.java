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
 * Send register request to portainer
 *
 * @author WalleZhang
 */
public class PortainerApi {
    private final static CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    private final static String AUTH_URL = "/api/auth";
    private final static String ENDPOINTS_URL = "/api/endpoints";
    private final static String BASE_URL = System.getenv("PORTAINER_API_URL");
    private final static String USERNAME = System.getenv("PORTAINER_USERNAME");
    private final static String PASSWORD = System.getenv("PORTAINER_PASSWORD");
    private final static String AGENT_PORT = System.getenv("PORTAINER_AGENT_PORT") == null ? "5000" : System.getenv("PORTAINER_AGENT_PORT");
    private static String AGENT_IP;

    static {
        try {
            AGENT_IP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            AGENT_IP = System.getenv("PORTAINER_AGENT_IP");
        }
    }

    /**
     * Get authentication token
     */
    private static String auth() {
        Log.info(String.format("Request URL is %s", BASE_URL + AUTH_URL));
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

    /**
     * Get endpoint id.
     *
     * @param authToken authentication header
     * @return endpoint id or -1 if endpoint is not exist.
     * @throws Exception get endpoint fail
     */
    private static int getEndpointId(String authToken) throws Exception {
        Log.info(String.format("Request URL is %s", BASE_URL + ENDPOINTS_URL));
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
            throw new Exception("Get Endpoints failed!");
        }

        for (Map<String, Object> endpoint : endpoints) {
            if (AGENT_IP.equals(endpoint.get("Name"))) {
                return (int) endpoint.get("Id");
            }
        }

        return -1;
    }

    /**
     * register endpoint automatically
     *
     * @throws Exception register failed
     */
    public static void registerEndpoint() throws Exception {
        String token = auth();
        UpdateEndpointRequest updateEndpointRequest = new UpdateEndpointRequest();
        updateEndpointRequest.setName(AGENT_IP);
        updateEndpointRequest.setPublicUrl(AGENT_IP);
        updateEndpointRequest.setUrl("tcp://" + AGENT_IP + ":" + AGENT_PORT);

        HttpEntityEnclosingRequestBase requestBase;

        int id = getEndpointId(token);

        Log.info(String.format("Get endpoint id is %s", id));

        if (id > 0) {
            // Update endpoint if it is exist
            requestBase = new HttpPut(BASE_URL + ENDPOINTS_URL + "/" + id);
        } else {
            // Or create a new one
            requestBase = new HttpPost(BASE_URL + ENDPOINTS_URL);
        }

        requestBase.addHeader("Authorization", token);
        requestBase.setEntity(new StringEntity(JSON.toJSONString(updateEndpointRequest)));

        HTTP_CLIENT.execute(requestBase);
    }
}
