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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.support.AbstractRegistry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceRepository;

import java.util.Collection;

@Cmd(name = "online", summary = "online dubbo", example = {
        "online dubbo",
        "online xx.xx.xxx.service"
})
public class Online implements BaseCommand {
    private Logger logger = LoggerFactory.getLogger(Online.class);
    private RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
    private ServiceRepository serviceRepository = ServiceRepository.getLoadedInstance();

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        logger.info("receive online command");
        String servicePattern = ".*";
        if (ArrayUtils.isNotEmpty(args)) {
            servicePattern = args[0];
        }

        boolean hasService = false;

        Collection<ProviderModel> providerModelList = serviceRepository.getExportedServices();
        for (ProviderModel providerModel : providerModelList) {
            if (providerModel.getServiceName().matches(servicePattern)) {
                hasService = true;
                Collection<Registry> registries = AbstractRegistryFactory.getRegistries();
                registries.forEach(registry -> {
                    // TODO, consider abstract the method to interface to avoid type cast
                    AbstractRegistry abstractRegistry = (AbstractRegistry) registry;
                    abstractRegistry.getRegisterStatedUrls().values().forEach(registerStatedURL -> {
                        if (!registerStatedURL.isRegistered()) {
                            abstractRegistry.register(registerStatedURL.getProviderUrl());
                        }
                    });
                });
            }
        }

        if (hasService) {
            return "OK";
        } else {
            return "service not found";
        }
    }
}
