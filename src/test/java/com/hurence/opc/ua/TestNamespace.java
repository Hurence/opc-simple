/*
 *  Copyright (C) 2018 Hurence (support@hurence.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hurence.opc.ua;


import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.*;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.FolderNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.AnalogItemNode;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.Range;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TestNamespace implements Namespace {


    public static final String URI = "urn:test:namespace";

    private final UShort index;

    private final ServerNodeMap nodeMap;
    private final SubscriptionModel subscriptionModel;

    public TestNamespace(final UShort index, final OpcUaServer server) {
        this.index = index;
        this.nodeMap = server.getNodeMap();
        this.subscriptionModel = new SubscriptionModel(server, this);

        registerItems();
    }

    private void registerItems() {

        // create a folder

        final UaFolderNode folder = new UaFolderNode(
                this.nodeMap,
                new NodeId(this.index, 1),
                new QualifiedName(this.index, "TestFolder"),
                LocalizedText.english("Test folder"));

        // add our folder to the objects folder

        this.nodeMap.getNode(Identifiers.ObjectsFolder).ifPresent(node -> {
            ((FolderNode) node).addComponent(folder);
        });

        // add single variable

        {
            final AnalogItemNode variable = new AnalogItemNode(
                    this.nodeMap,
                    new NodeId(this.index, "sint"),
                    new QualifiedName(this.index, "SinT"),
                    LocalizedText.english("Sin (t)"),
                    LocalizedText.english("Sinus of (t)"),
                    UInteger.valueOf(AccessLevel.getMask(AccessLevel.READ_WRITE)),
                    UInteger.valueOf(AccessLevel.getMask(AccessLevel.READ_WRITE))) {

                @Override
                public DataValue getValue() {
                    return new DataValue(new Variant(Math.sin(System.currentTimeMillis() / 1000)));
                }
            };

            variable.setInstrumentRange(new Range(-1.0, +1.0));
            variable.setDataType(Identifiers.Double);
            variable.setDescription(LocalizedText.english("Sinusoid signal"));
            variable.addReference(new Reference(variable.getNodeId(), Identifiers.HasTypeDefinition,
                    Identifiers.AnalogItemType.expanded(),
                    NodeClass.VariableType, true));
            folder.addOrganizes(variable);
        }


    }

    @Override
    public void read(ReadContext context, Double maxAge, TimestampsToReturn timestamps, List<ReadValueId> readValueIds) {
        final List<DataValue> results = new ArrayList<>(readValueIds.size());
        for (final ReadValueId id : readValueIds) {
            final ServerNode node = this.nodeMap.get(id.getNodeId());

            final DataValue value = node != null
                    ? node.readAttribute(new AttributeContext(context), id.getAttributeId())
                    : new DataValue(StatusCodes.Bad_NodeIdUnknown);
            results.add(value);
        }
        // report back with result
        context.complete(results);
    }


    @Override
    public void write(
            final WriteContext context,
            final List<WriteValue> writeValues) {

        final List<StatusCode> results = writeValues.stream()
                .map(value -> {
                    if (this.nodeMap.containsKey(value.getNodeId())) {
                        return new StatusCode(StatusCodes.Bad_NotWritable);
                    } else {
                        return new StatusCode(StatusCodes.Bad_NodeIdUnknown);
                    }
                })
                .collect(Collectors.toList());

        // report back with result

        context.complete(results);
    }

    @Override
    public CompletableFuture<List<Reference>> browse(final AccessContext context, final NodeId nodeId) {
        final ServerNode node = this.nodeMap.get(nodeId);

        if (node != null) {
            return CompletableFuture.completedFuture(node.getReferences());
        } else {
            final CompletableFuture<List<Reference>> f = new CompletableFuture<>();
            f.completeExceptionally(new UaException(StatusCodes.Bad_NodeIdUnknown));
            return f;
        }
    }

    @Override
    public Optional<MethodInvocationHandler> getInvocationHandler(final NodeId methodId) {
        return Optional
                .ofNullable(this.nodeMap.get(methodId))
                .filter(n -> n instanceof UaMethodNode)
                .map(n -> {
                    final UaMethodNode m = (UaMethodNode) n;
                    return m.getInvocationHandler()
                            .orElse(new MethodInvocationHandler.NotImplementedHandler());
                });
    }

    @Override
    public void onDataItemsCreated(final List<DataItem> dataItems) {
        this.subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(final List<DataItem> dataItems) {
        this.subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(final List<DataItem> dataItems) {
        this.subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(final List<MonitoredItem> monitoredItems) {
        this.subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    @Override
    public UShort getNamespaceIndex() {
        return this.index;
    }

    @Override
    public String getNamespaceUri() {
        return URI;
    }

}