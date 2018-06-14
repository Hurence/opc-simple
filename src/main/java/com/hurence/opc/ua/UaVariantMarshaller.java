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

import com.hurence.opc.exception.OpcException;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * OPC-UA Variant to Java primitives conversions
 *
 * @author amarziali
 */
public class UaVariantMarshaller {

    private static final Logger logger = LoggerFactory.getLogger(UaVariantMarshaller.class);


    /**
     * Find the java {@link Class} of data held by a opc-ua variable node.
     *
     * @param client the ua client.
     * @param node   the variable node id.
     * @return the java class (can default to {@link String} if type is not built in or cannot be determined).
     */
    public static Optional<Class<?>> findJavaClass(OpcUaClient client, NodeId node) {
        if (node == null || node.expanded() == null) {
            throw new OpcException("Impossible to guess type from empty node");
        }
        try {
            VariableNode vn = client.getAddressSpace().createVariableNode(node);
            Integer valueRank = vn.getValueRank().get();

            boolean isArray = ValueRanks.Scalar != valueRank;
            Class<?> cls = null;
            NodeId dataType = vn.getDataType().exceptionally(e -> null).get();
            if (dataType != null) {
                cls = TypeUtil.getBackingClass(dataType.expanded());
            }

            if (cls == null) {
                Object value = vn.getValue().exceptionally(e -> null).get();
                //try to convert to enumeration
                if (value instanceof Integer) {
                    //here we have an enumeration
                    cls = Integer.class;
                }
            }

            if (cls != null) {
                if (cls.equals(UInteger.class)) {
                    cls = Long.class;
                } else if (cls.equals(UByte.class)) {
                    cls = Short.class;
                } else if (cls.equals(UShort.class)) {
                    cls = Integer.class;
                } else if (cls.equals(ULong.class)) {
                    cls = BigInteger.class;
                } else if (cls.equals(DateTime.class)) {
                    cls = Long.class;
                } else if (cls.equals(UUID.class)) {
                    cls = String.class;
                } else if (cls.equals(Number.class)) {
                    cls = Long.class;
                } else if (cls.equals(ByteString.class)) {
                    cls = byte[].class;
                } else if (cls.equals(XmlElement.class)) {
                    cls = String.class;
                } else if (cls.equals(NodeId.class)) {
                    cls = String.class;
                } else if (cls.equals(ExpandedNodeId.class)) {
                    cls = String.class;
                } else if (cls.equals(StatusCode.class)) {
                    cls = Long.class;
                } else if (cls.equals(QualifiedName.class)) {
                    cls = String.class;
                } else if (cls.equals(LocalizedText.class)) {
                    cls = String.class;
                } else if (cls.equals(ExtensionObject.class)) {
                    cls = Object.class;
                } else if (cls.equals(DataValue.class)) {
                    cls = Object.class;
                } else if (cls.equals(Variant.class)) {
                    cls = Object.class;
                }
            }

            //default case
            if (cls != null) {
                return Optional.of(isArray ? Array.newInstance(cls, 0).getClass() : cls);
            }
        } catch (Exception e) {
            logger.warn("Unable to map opc-ua type to java.", e);

        }
        return Optional.empty();

    }

    private static Object convert(Object src, Function<Object, Object> function) {
        boolean isArray = src.getClass().isArray();
        if (isArray) {
            return Arrays.stream((Object[]) src).map(function).toArray();
        }
        return function.apply(src);
    }


    /**
     * Find the data java type held by a opc-ua variable node.
     * It translates the variant and the builtin opc-ua types.
     * The method tries to return primitive types if possible.
     *
     * @param value the variable value
     * @return the converted values.
     */
    public static Object toJavaType(Object value) {
        if (value == null) {
            return null;
        }
        Class<?> cls = value.getClass().isArray() ? value.getClass().getComponentType() : value.getClass();
        try {
            if (cls.equals(UInteger.class)) {
                return convert(value, a -> ((UInteger) a).longValue());
            } else if (cls.equals(UByte.class)) {
                return convert(value, a -> ((UByte) a).shortValue());
            } else if (cls.equals(UShort.class)) {
                return convert(value, a -> ((UShort) a).intValue());
            } else if (cls.equals(ULong.class)) {
                return convert(value, a -> ((ULong) a).toBigInteger());
            } else if (cls.equals(DateTime.class)) {
                return convert(value, a -> ((DateTime) a).getUtcTime());
            } else if (cls.equals(UUID.class)) {
                return convert(value, a -> ((UUID) a).toString());
            } else if (cls.equals(Number.class)) {
                return convert(value, a -> ((Number) a).longValue());
            } else if (cls.equals(ByteString.class)) {
                return convert(value, a -> ((ByteString) a).bytes());
            } else if (cls.equals(XmlElement.class)) {
                return convert(value, a -> ((XmlElement) a).toString());
            } else if (cls.equals(NodeId.class)) {
                return convert(value, a -> ((NodeId) a).toParseableString());
            } else if (cls.equals(ExpandedNodeId.class)) {
                return convert(value, a -> ((ExpandedNodeId) a).toParseableString());
            } else if (cls.equals(StatusCode.class)) {
                return convert(value, a -> ((StatusCode) a).getValue());
            } else if (cls.equals(QualifiedName.class)) {
                return convert(value, a -> ((QualifiedName) a).toParseableString());
            } else if (cls.equals(LocalizedText.class)) {
                return convert(value, a -> ((LocalizedText) a).getText());
            } else if (cls.equals(ExtensionObject.class)) {
                return convert(value, a -> ((ExtensionObject) a).decode());
            } else if (cls.equals(DataValue.class)) {
                return convert(value, a -> toJavaType(((DataValue) a).getValue()));
            } else if (cls.equals(Variant.class)) {
                return convert(value, a -> toJavaType(((Variant) a).getValue()));
            }
        } catch (Exception e) {
            logger.warn("Unable to map value " + value + " to java type", e);
        }
        return value;
    }


}


