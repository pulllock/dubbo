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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_SERVICE_REFERENCE_PATH;
import static org.apache.dubbo.rpc.cluster.Constants.WEIGHT_KEY;

/**
 * This class select one provider from multiple providers randomly.
 * You can define weights for each provider:
 * If the weights are all the same then it will use random.nextInt(number of invokers).
 * If the weights are different then it will use random.nextInt(w1 + w2 + ... + wn)
 * Note that if the performance of the machine is better than others, you can set a larger weight.
 * If the performance is not so good, you can set a smaller weight.
 *
 * 带权重的随机的负载均衡策略
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "random";

    /**
     * Select one invoker between a list using a random criteria
     * @param invokers List of possible invokers
     * @param url URL
     * @param invocation Invocation
     * @param <T>
     * @return The selected invoker
     */
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // Number of invokers
        // Invoker的数量
        int length = invokers.size();

        // 如果不需要进行加权重的随机计算，则直接从Invoker列表中随机选择一个
        if (!needWeightLoadBalance(invokers,invocation)){
            return invokers.get(ThreadLocalRandom.current().nextInt(length));
        }

        // Every invoker has the same weight?
        // 是否所有的Invoker的权重都一样
        boolean sameWeight = true;

        // the maxWeight of every invokers, the minWeight = 0 or the maxWeight of the last invoker
        // weights用来存储每个Invoker对应的权重
        int[] weights = new int[length];

        // The sum of weights
        // 记录总权重
        int totalWeight = 0;
        for (int i = 0; i < length; i++) {
            // 获取每个Invoker的权重
            int weight = getWeight(invokers.get(i), invocation);

            // Sum
            // 每个Invoker权重加一起合成总权重
            totalWeight += weight;
            // save for later use
            weights[i] = totalWeight;
            // 检测是不是每个Invoker的权重都相同
            if (sameWeight && totalWeight != weight * (i + 1)) {
                sameWeight = false;
            }
        }

        // 如果不是所有的Invoker权重都相同，则计算Invoker的权重区间
        if (totalWeight > 0 && !sameWeight) {
            // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeight.
            // 获取一个0到总权重之间的随机数
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            // Return a invoker based on the random value.
            for (int i = 0; i < length; i++) {
                // 随机数落在了Invoker的权重范围内，则返回该Invoker
                if (offset < weights[i]) {
                    return invokers.get(i);
                }
            }
        }
        // If all invokers have the same weight value or totalWeight=0, return evenly.
        // 如果每个Invoker的权重都相同，则随机选择一个Invoker
        return invokers.get(ThreadLocalRandom.current().nextInt(length));
    }

    private <T> boolean needWeightLoadBalance(List<Invoker<T>> invokers, Invocation invocation) {

        Invoker invoker = invokers.get(0);
        URL invokerUrl = invoker.getUrl();
        // Multiple registry scenario, load balance among multiple registries.
        if (REGISTRY_SERVICE_REFERENCE_PATH.equals(invokerUrl.getServiceInterface())) {
            String weight = invokerUrl.getParameter(REGISTRY_KEY + "." + WEIGHT_KEY);
            if (StringUtils.isNotEmpty(weight)) {
                return true;
            }
        } else {
            String weight = invokerUrl.getMethodParameter(invocation.getMethodName(), WEIGHT_KEY);
            if (StringUtils.isNotEmpty(weight)) {
                return true;
            }else {
                String timeStamp = invoker.getUrl().getParameter(TIMESTAMP_KEY);
                if (StringUtils.isNotEmpty(timeStamp)) {
                    return true;
                }
            }
        }
        return false;
    }


}
