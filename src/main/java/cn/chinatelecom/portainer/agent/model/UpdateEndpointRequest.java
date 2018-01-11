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

package cn.chinatelecom.portainer.agent.model;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 创建或更新Endpoint模型
 *
 * @author WalleZhang
 */
public class UpdateEndpointRequest {
    @JSONField(name = "Name")
    private String name;
    @JSONField(name = "URL")
    private String url;
    @JSONField(name = "PublicURL")
    private String publicUrl;
    @JSONField(name = "TLS")
    private boolean tls = false;
    @JSONField(name = "TLSSkipVerify")
    private boolean tlsSkipVerify = false;
    @JSONField(name = "TLSSkipClientVerify")
    private boolean tlsSkipClientVerify = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public boolean isTls() {
        return tls;
    }

    public void setTls(boolean tls) {
        this.tls = tls;
    }

    public boolean isTlsSkipVerify() {
        return tlsSkipVerify;
    }

    public void setTlsSkipVerify(boolean tlsSkipVerify) {
        this.tlsSkipVerify = tlsSkipVerify;
    }

    public boolean isTlsSkipClientVerify() {
        return tlsSkipClientVerify;
    }

    public void setTlsSkipClientVerify(boolean tlsSkipClientVerify) {
        this.tlsSkipClientVerify = tlsSkipClientVerify;
    }
}
