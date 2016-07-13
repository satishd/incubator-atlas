/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.sample;

import com.google.common.base.Preconditions;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.atlas.AtlasException;
import org.apache.atlas.RepositoryMetadataModule;
import org.apache.atlas.RequestContext;
import org.apache.atlas.services.MetadataService;
import org.apache.atlas.typesystem.Referenceable;
import org.apache.atlas.typesystem.json.InstanceSerialization;
import org.apache.atlas.typesystem.json.TypesSerialization;
import org.apache.atlas.typesystem.types.AttributeDefinition;
import org.apache.atlas.typesystem.types.ClassType;
import org.apache.atlas.typesystem.types.DataTypes;
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition;
import org.apache.atlas.typesystem.types.IDataType;
import org.apache.atlas.typesystem.types.Multiplicity;
import org.apache.atlas.typesystem.types.utils.TypesUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 *
 */
public class AtlasMetadataServiceTest {

    private static MetadataService metadataService;

    @BeforeTest
    public static void setup() {
        Injector metadataModuleInjector = Guice.createInjector(new RepositoryMetadataModule());
        metadataService = metadataModuleInjector.getInstance(MetadataService.class);
    }

    @Test
    public void testDeviceInfoType() throws Exception {
        registerType(createDeviceInfoType());

        DeviceInfo deviceInfo = createDeviceInfo();

        String instanceId = createEntity(new Referenceable(DeviceInfo.NAME_SPACE, deviceInfo.toMap()));
        Referenceable entity = getEntity(instanceId);
        DeviceInfo storedDeviceInfo = fromReferenceable(entity);
        Assert.assertEquals(deviceInfo, storedDeviceInfo);

    }

    public Referenceable getEntity(String entityGuid) throws AtlasException {
        String entityDefinition = metadataService.getEntityDefinition(entityGuid);
        return entityDefinition != null ? InstanceSerialization.fromJsonReferenceable(entityDefinition, true) : null;
    }

    public String createEntity(Referenceable referenceable) throws AtlasException {
        String entityJSON = InstanceSerialization.toJson(referenceable, true);

        List<String> guids = _createEntities(entityJSON);

        if(guids == null || guids.isEmpty()) {
            return null;
        }

        // return the Id for created instance with guids
        return guids.get(0);
    }

    private List<String> _createEntities(String entityJSON) throws AtlasException {
        try {
            return metadataService.createEntities(new JSONArray(Arrays.asList(entityJSON)).toString());
        } finally {
            RequestContext.clear();
        }
    }


    public void registerType(HierarchicalTypeDefinition<?> typeDef) throws Exception {

        if (metadataService.getTypeNamesList().contains(typeDef.typeName)) {
            return;
        }

        final String typeDefJson = TypesSerialization.toJson(typeDef, false);
        final JSONObject type = metadataService.createType(typeDefJson);
        final List<String> typeNamesList = metadataService.getTypeNamesList();

        System.out.println("typeNamesList = " + typeNamesList);
    }

    public static HierarchicalTypeDefinition<ClassType> createDeviceInfoType() {
        return TypesUtil.createClassTypeDef(
                DeviceInfo.NAME_SPACE, null,
                TypesUtil.createUniqueRequiredAttrDef(DeviceInfo.NAME, DataTypes.STRING_TYPE),
                TypesUtil.createUniqueRequiredAttrDef(DeviceInfo.XID, DataTypes.STRING_TYPE),
                attrDef(DeviceInfo.TIMESTAMP, DataTypes.LONG_TYPE),
                attrDef(DeviceInfo.VERSION, DataTypes.STRING_TYPE)
        );
    }

    private static AttributeDefinition attrDef(String name, IDataType dT) {
        return attrDef(name, dT, Multiplicity.OPTIONAL, false, null);
    }

    private static AttributeDefinition attrDef(String name, IDataType dT, Multiplicity m, boolean isComposite,
                                               String reverseAttributeName) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(dT);
        return new AttributeDefinition(name, dT.getName(), m, isComposite, false, false, reverseAttributeName);
    }

    private DeviceInfo fromReferenceable(Referenceable referenceable) {
        DeviceInfo deviceInfo = new DeviceInfo();

        deviceInfo.setXid(referenceable.get(DeviceInfo.XID).toString());
        deviceInfo.setName(referenceable.get(DeviceInfo.NAME).toString());
        deviceInfo.setTimestamp((Long) referenceable.get(DeviceInfo.TIMESTAMP));
        deviceInfo.setVersion(referenceable.get(DeviceInfo.VERSION).toString());

        return deviceInfo;
    }

    protected static DeviceInfo createDeviceInfo() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setName("device-" + System.currentTimeMillis());
        deviceInfo.setXid(UUID.randomUUID().toString());
        deviceInfo.setVersion(""+new Random().nextInt() % 10L);
        deviceInfo.setTimestamp(System.currentTimeMillis());

        return deviceInfo;
    }

    public static void main(String[] args) throws Exception {
        AtlasMetadataServiceTest atlasMetadataServiceTest = new AtlasMetadataServiceTest();
        atlasMetadataServiceTest.testDeviceInfoType();
    }

}
