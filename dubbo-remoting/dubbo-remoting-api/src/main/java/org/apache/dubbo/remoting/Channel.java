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
package org.apache.dubbo.remoting;

import java.net.InetSocketAddress;

/**
 * Channel. (API/SPI, Prototype, ThreadSafe)
 *
 * @see org.apache.dubbo.remoting.Client
 * @see RemotingServer#getChannels()
 * @see RemotingServer#getChannel(InetSocketAddress)
 *
 * Channel是对网络通道的抽象，通讯的两端通过通道进行数据交换，
 * 通道也可以理解为是一个端，所以这里Channel继承了Endpoint
 */
public interface Channel extends Endpoint {

    /**
     * get remote address.
     *
     * @return remote address.
     *
     * 获取远程地址
     */
    InetSocketAddress getRemoteAddress();

    /**
     * is connected.
     *
     * @return connected
     *
     * 通道是否已连接
     */
    boolean isConnected();

    /**
     * has attribute.
     *
     * @param key key.
     * @return has or has not.
     */
    boolean hasAttribute(String key);

    /**
     * get attribute.
     *
     * @param key key.
     * @return value.
     */
    Object getAttribute(String key);

    /**
     * set attribute.
     *
     * @param key   key.
     * @param value value.
     */
    void setAttribute(String key, Object value);

    /**
     * remove attribute.
     *
     * @param key key.
     */
    void removeAttribute(String key);
}
