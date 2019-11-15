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
package org.apache.dubbo.metadata.annotation.processing.model;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * {@link Collection} Type Model
 *
 * @since 2.7.5
 */
public class CollectionTypeModel {

    private Collection<String> strings; // The composite element is simple type

    private List<Color> colors;     // The composite element is Enum type

    private Queue<PrimitiveTypeModel> primitiveTypeModels;  // The composite element is POJO type

    private Deque<Model> models;  // The composite element is hierarchical POJO type

    private Set<Model[]> modelArrays; // The composite element is hierarchical POJO type

}
