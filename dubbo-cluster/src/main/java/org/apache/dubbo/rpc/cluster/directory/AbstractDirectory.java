/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.cluster.directory;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterChain;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

/**
 * Abstract implementation of Directory: Invoker list returned from this Directory's list method have been filtered by Routers
 *
 */
public abstract class AbstractDirectory<T> implements Directory<T> {

    // logger
    private static final Logger logger = LoggerFactory.getLogger(AbstractDirectory.class);

    private final URL url;

    private volatile boolean destroyed = false;

    protected volatile URL consumerUrl;

    protected RouterChain<T> routerChain;

    protected final Map<String, String> queryMap;

    public AbstractDirectory(URL url) {
        this(url, null);
    }

    public AbstractDirectory(URL url, RouterChain<T> routerChain) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }

        this.url = url.removeAttribute(REFER_KEY).removeParameter(MONITOR_KEY);

        Object referParams = url.getAttribute(REFER_KEY);
        if (referParams != null) {
            this.queryMap = (Map<String, String>) referParams;
            this.consumerUrl = turnRegistryUrlToConsumerUrl(url, queryMap);
        } else {
            this.queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(REFER_KEY));
            this.consumerUrl = url.addParameters(queryMap);
        }

        setRouterChain(routerChain);
    }

    private URL turnRegistryUrlToConsumerUrl(URL url, Map<String, String> queryMap) {
        return URLBuilder.from(url)
                .setHost(queryMap.get("register.ip"))
                .setPort(0)
                .setProtocol(queryMap.get(PROTOCOL_KEY) == null ? DUBBO : queryMap.get(PROTOCOL_KEY))
                .setPath(queryMap.get(PATH_KEY) != null ? queryMap.get(PATH_KEY) : queryMap.get(INTERFACE_KEY))
                .clearParameters()
                .addParameters(queryMap)
                .removeParameter(MONITOR_KEY)
                .build();
    }

    @Override
    public List<Invoker<T>> list(Invocation invocation) throws RpcException {
        if (destroyed) {
            throw new RpcException("Directory already destroyed .url: " + getUrl());
        }

        return doList(invocation);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    public RouterChain<T> getRouterChain() {
        return routerChain;
    }

    public void setRouterChain(RouterChain<T> routerChain) {
        this.routerChain = routerChain;
    }

    protected void addRouters(List<Router> routers) {
        routers = routers == null ? Collections.emptyList() : routers;
        routerChain.addRouters(routers);
    }

    public URL getConsumerUrl() {
        return consumerUrl;
    }

    public void setConsumerUrl(URL consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void destroy() {
        destroyed = true;
    }

    @Override
    public void discordAddresses() {
        // do nothing by default
    }

    protected abstract List<Invoker<T>> doList(Invocation invocation) throws RpcException;

}
