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
package org.apache.dubbo.metadata.report;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.MetadataReportConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_DIRECTORY;
import static org.apache.dubbo.metadata.report.support.Constants.METADATA_REPORT_KEY;

/**
 * 2019-08-09
 */
public class MetadataReportInstance {

    private static AtomicBoolean init = new AtomicBoolean(false);

    private static final List<MetadataReport> metadataReports = new ArrayList<>();

    public static void init(MetadataReportConfig config) {
        if (init.get()) {
            return;
        }
        MetadataReportFactory metadataReportFactory = ExtensionLoader.getExtensionLoader(MetadataReportFactory.class).getAdaptiveExtension();
        URL url = config.toUrl();
        if (METADATA_REPORT_KEY.equals(url.getProtocol())) {
            String protocol = url.getParameter(METADATA_REPORT_KEY, DEFAULT_DIRECTORY);
            url = URLBuilder.from(url)
                    .setProtocol(protocol)
                    .removeParameter(METADATA_REPORT_KEY)
                    .build();
        }
        metadataReports.add(metadataReportFactory.getMetadataReport(url));
        init.set(true);
    }

    public static List<MetadataReport> getMetadataReports() {
        return getMetadataReports(false);
    }

    public static List<MetadataReport> getMetadataReports(boolean checked) {
        if (checked) {
            checkInit();
        }
        return metadataReports;
    }

    private static void checkInit() {
        if (!init.get()) {
            throw new IllegalStateException("the metadata report was not inited.");
        }
    }
}
