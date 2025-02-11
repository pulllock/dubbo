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
package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.Node;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;

/**
 * Directory. (SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Directory_service">Directory Service</a>
 *
 * @see org.apache.dubbo.rpc.cluster.Cluster#join(Directory)
 *
 * 目录接口，表示多个Invoker的集合。
 */
public interface Directory<T> extends Node {

    /**
     * get service type.
     *
     * @return service type.
     *
     * 接口类型
     */
    Class<T> getInterface();

    /**
     * list invokers.
     * filtered by invocation
     *
     * @return invokers
     *
     * 根据Invocation来过滤合适的Invoker
     */
    List<Invoker<T>> list(Invocation invocation) throws RpcException;

    /**
     * list invokers
     * include all invokers from registry
     *
     * 获取当前目录管理的所有的Invoker结合
     */
    List<Invoker<T>> getAllInvokers();

    /**
     * 消费者的URL
     * @return
     */
    URL getConsumerUrl();

    /**
     * 目录是否被销毁
     * @return
     */
    boolean isDestroyed();

    default boolean isEmpty() {
        return CollectionUtils.isEmpty(getAllInvokers());
    }

    default boolean isServiceDiscovery() {
        return false;
    }

    void discordAddresses();

    /**
     * 获取路由链
     * @return
     */
    RouterChain<T> getRouterChain();

    /**
     * invalidate an invoker, add it into reconnect task, remove from list next time
     * will be recovered by address refresh notification or reconnect success notification
     *
     * @param invoker invoker to invalidate
     *
     * 作废Invoker
     */
    void addInvalidateInvoker(Invoker<T> invoker);

    /**
     * disable an invoker, remove from list next time
     * will be removed when invoker is removed by address refresh notification
     * using in service offline notification
     *
     * @param invoker invoker to invalidate
     *
     *  禁用Invoker
     */
    void addDisabledInvoker(Invoker<T> invoker);

    /**
     * recover a disabled invoker
     *
     * @param invoker invoker to invalidate
     *
     * 恢复一个禁用的Invoker
     */
    void recoverDisabledInvoker(Invoker<T> invoker);

    default boolean isNotificationReceived() {
        return false;
    }
}
