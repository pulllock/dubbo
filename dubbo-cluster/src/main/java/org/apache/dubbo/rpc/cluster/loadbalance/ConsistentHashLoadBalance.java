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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;

/**
 * ConsistentHashLoadBalance
 *
 * 基于一致性哈希的负载均衡策略
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "consistenthash";
    /**
     * Hash nodes name
     */
    public static final String HASH_NODES = "hash.nodes";
    /**
     * Hash arguments name
     */
    public static final String HASH_ARGUMENTS = "hash.arguments";
    private final ConcurrentMap<String, ConsistentHashSelector<?>> selectors = new ConcurrentHashMap<String, ConsistentHashSelector<?>>();
    @SuppressWarnings("unchecked")
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // 调用的方法名
        String methodName = RpcUtils.getMethodName(invocation);

        // 服务接口.方法名作为可以、
        String key = invokers.get(0).getUrl().getServiceKey() + "." + methodName;

        // using the hashcode of list to compute the hash only pay attention to the elements in the list
        // invoker列表发生变化的时候会重新生成ConsistentHashSelector
        int invokersHashCode = getCorrespondingHashCode(invokers);

        // 根据key从缓存中获取ConsistentHashSelector
        ConsistentHashSelector<T> selector = (ConsistentHashSelector<T>) selectors.get(key);
        if (selector == null || selector.identityHashCode != invokersHashCode) {
            selectors.put(key, new ConsistentHashSelector<T>(invokers, methodName, invokersHashCode));
            selector = (ConsistentHashSelector<T>) selectors.get(key);
        }

        // 一致性哈希算法来进行Invoker的选择
        return selector.select(invocation);
    }

    /**
     * get hash code of invokers
     * Make this method to public in order to use this method in test case
     * @param invokers
     * @return
     */
    public <T> int getCorrespondingHashCode(List<Invoker<T>> invokers){
        return invokers.hashCode();
    }

    private static final class ConsistentHashSelector<T> {

        /**
         * 记录虚拟Invoker对象的哈希环
         */
        private final TreeMap<Long, Invoker<T>> virtualInvokers;

        /**
         * 虚拟Invoker个数
         */
        private final int replicaNumber;

        /**
         * Invoker集合的哈希值，可用来判断Invoker列表是否发生了变化
         */
        private final int identityHashCode;

        /**
         * 需要参与哈希计算的参数索引
         */
        private final int[] argumentIndex;

        /**
         * key: server(invoker) address
         * value: count of requests accept by certain server
         */
        private Map<String, AtomicLong> serverRequestCountMap = new ConcurrentHashMap<>();

        /**
         * count of total requests accept by all servers
         */
        private AtomicLong totalRequestCount;

        /**
         * count of current servers(invokers)
         */
        private int serverCount;

        /**
         * the ratio which allow count of requests accept by each server
         * overrate average (totalRequestCount/serverCount).
         * 1.5 is recommended, in the future we can make this param configurable
         */
        private static final double OVERLOAD_RATIO_THREAD = 1.5F;

        ConsistentHashSelector(List<Invoker<T>> invokers, String methodName, int identityHashCode) {

            // 构建哈希槽
            this.virtualInvokers = new TreeMap<Long, Invoker<T>>();

            // 记录Invoker集合的哈希值
            this.identityHashCode = identityHashCode;
            URL url = invokers.get(0).getUrl();

            // 从hash.nodes中获取虚拟节点的个数，默认是160个
            this.replicaNumber = url.getMethodParameter(methodName, HASH_NODES, 160);

            // 获取参与哈希计算的参数下标，默认是第一个参数
            String[] index = COMMA_SPLIT_PATTERN.split(url.getMethodParameter(methodName, HASH_ARGUMENTS, "0"));
            argumentIndex = new int[index.length];
            for (int i = 0; i < index.length; i++) {
                argumentIndex[i] = Integer.parseInt(index[i]);
            }

            // 构建虚拟的哈希槽
            for (Invoker<T> invoker : invokers) {
                String address = invoker.getUrl().getAddress();
                // replicaNumber默认160
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = Bytes.getMD5(address + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualInvokers.put(m, invoker);
                    }
                }
            }

            totalRequestCount = new AtomicLong(0);
            serverCount = invokers.size();
            serverRequestCountMap.clear();
        }

        /**
         * 选择要调用的Invoker对象
         * @param invocation
         * @return
         */
        public Invoker<T> select(Invocation invocation) {
            // 参与哈希计算的参数拼接到一起
            String key = toKey(invocation.getArguments());
            byte[] digest = Bytes.getMD5(key);

            // 将key进行哈希后选择Invoker对象
            return selectForKey(hash(digest, 0));
        }
        private String toKey(Object[] args) {
            StringBuilder buf = new StringBuilder();
            for (int i : argumentIndex) {
                if (i >= 0 && i < args.length) {
                    buf.append(args[i]);
                }
            }
            return buf.toString();
        }
        private Invoker<T> selectForKey(long hash) {

            // 从虚拟Invoker对象哈希环中找到第一个节点值大于等于指定哈希值的Invoker对象
            Map.Entry<Long, Invoker<T>> entry = virtualInvokers.ceilingEntry(hash);
            if (entry == null) {
                // 如果哈希值大于环中所有Invoker，则返回哈希环的第一个Invoker对象
                entry = virtualInvokers.firstEntry();
            }

            String serverAddress = entry.getValue().getUrl().getAddress();

            /**
             * The following part of codes aims to select suitable invoker.
             * This part is not complete thread safety.
             * However, in the scene of consumer-side load balance,
             * thread race for this part of codes
             * (execution time cost for this part of codes without any IO or
             * network operation is very low) will rarely occur. And even in
             * extreme case, a few requests are assigned to an invoker which
             * is above OVERLOAD_RATIO_THREAD will not make a significant impact
             * on the effect of this new algorithm.
             * And make this part of codes synchronized will reduce efficiency of
             * every request. In my opinion, this is not worth. So it is not a
             * problem for this part is not complete thread safety.
             */
            double overloadThread = ((double) totalRequestCount.get() / (double) serverCount) * OVERLOAD_RATIO_THREAD;
            /**
             * Find a valid server node:
             * 1. Not have accept request yet
             * or
             * 2. Not have overloaded (request count already accept < thread (average request count * overloadRatioAllowed ))
             */
            while (serverRequestCountMap.containsKey(serverAddress)
                && serverRequestCountMap.get(serverAddress).get() >= overloadThread) {
                /**
                 * If server node is not valid, get next node
                 */
                entry = getNextInvokerNode(virtualInvokers, entry);
                serverAddress = entry.getValue().getUrl().getAddress();
            }
            if (!serverRequestCountMap.containsKey(serverAddress)) {
                serverRequestCountMap.put(serverAddress, new AtomicLong(1));
            } else {
                serverRequestCountMap.get(serverAddress).incrementAndGet();
            }
            totalRequestCount.incrementAndGet();

            return entry.getValue();
        }

        private Map.Entry<Long, Invoker<T>> getNextInvokerNode(TreeMap<Long, Invoker<T>> virtualInvokers, Map.Entry<Long, Invoker<T>> entry){
            Map.Entry<Long, Invoker<T>> nextEntry = virtualInvokers.higherEntry(entry.getKey());
            if(nextEntry == null){
                return virtualInvokers.firstEntry();
            }
            return nextEntry;
        }

        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }
    }

}
