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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.List;

/**
 * StaticDirectory
 *
 * 静态目录，该目录中维护的Invoker集合是不变的
 */
public class StaticDirectory<T> extends AbstractDirectory<T> {
    private static final Logger logger = LoggerFactory.getLogger(StaticDirectory.class);

    public StaticDirectory(List<Invoker<T>> invokers) {
        this(null, invokers, null);
    }

    public StaticDirectory(List<Invoker<T>> invokers, RouterChain<T> routerChain) {
        this(null, invokers, routerChain);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers) {
        this(url, invokers, null);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers, RouterChain<T> routerChain) {
        super(url == null && CollectionUtils.isNotEmpty(invokers) ? invokers.get(0).getUrl() : url, routerChain, false);
        if (CollectionUtils.isEmpty(invokers)) {
            throw new IllegalArgumentException("invokers == null");
        }
        this.setInvokers(new BitList<>(invokers));
    }

    @Override
    public Class<T> getInterface() {
        return getInvokers().get(0).getInterface();
    }

    @Override
    public List<Invoker<T>> getAllInvokers() {
        return getInvokers();
    }

    @Override
    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        for (Invoker<T> invoker : getValidInvokers()) {
            if (invoker.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        if (isDestroyed()) {
            return;
        }
        for (Invoker<T> invoker : getInvokers()) {
            invoker.destroy();
        }
        super.destroy();
    }

    public void buildRouterChain() {
        RouterChain<T> routerChain = RouterChain.buildChain(getInterface(), getUrl());
        routerChain.setInvokers(getInvokers());
        this.setRouterChain(routerChain);
    }

    public void notify(List<Invoker<T>> invokers) {
        this.setInvokers(new BitList<>(invokers));
        if (routerChain != null) {
            routerChain.setInvokers(this.getInvokers());
        }
    }

    /**
     * 根据Invocation来过滤合适的Invoker
     * @param invokers
     * @param invocation
     * @return
     * @throws RpcException
     */
    @Override
    protected List<Invoker<T>> doList(BitList<Invoker<T>> invokers, Invocation invocation) throws RpcException {
        if (routerChain != null) {
            try {
                // 通过路由链过滤合适的Invoker集合
                List<Invoker<T>> finalInvokers = routerChain.route(getConsumerUrl(), invokers, invocation);
                return finalInvokers == null ? BitList.emptyList() : finalInvokers;
            } catch (Throwable t) {
                logger.error("Failed to execute router: " + getUrl() + ", cause: " + t.getMessage(), t);
                return BitList.emptyList();
            }
        }
        return invokers;
    }

}
