/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mdn.parsing;

import java.util.Optional;

import org.apache.james.mdn.fields.AddressType;
import org.apache.james.mdn.fields.Field;
import org.apache.james.mdn.fields.Gateway;
import org.apache.james.mdn.fields.Text;

import com.google.common.base.Preconditions;

public class GatewayParser implements FieldsParser.FieldParser {
    private final TypedValue.Factory factory;

    public GatewayParser(boolean strict) {
        this.factory = new TypedValue.Factory(strict, AddressType.DNS);
    }

    @Override
    public Optional<Field> parse(String value) {
        Preconditions.checkNotNull(value);

        return Optional.of(factory.parse(value))
            .filter(this::isUsable)
            .map(typedValue -> new Gateway(typedValue.getType(), Text.fromRawText(typedValue.getValue())));
    }

    private boolean isUsable(TypedValue typedValue) {
        return  !typedValue.getValue().trim().isEmpty();
    }
}
