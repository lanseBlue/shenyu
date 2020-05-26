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

package org.dromara.soul.plugin.apache.dubbo.body;

import org.dromara.soul.common.constant.Constants;
import org.dromara.soul.common.enums.PluginEnum;
import org.dromara.soul.common.enums.RpcTypeEnum;
import org.dromara.soul.plugin.api.SoulPlugin;
import org.dromara.soul.plugin.api.SoulPluginChain;
import org.dromara.soul.plugin.api.context.SoulContext;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * The type Body param plugin.
 */
public class BodyParamPlugin implements SoulPlugin {
    
    private final List<HttpMessageReader<?>> messageReaders;
    
    /**
     * Instantiates a new Body param plugin.
     */
    public BodyParamPlugin() {
        this.messageReaders = HandlerStrategies.withDefaults().messageReaders();
    }
    
    @Override
    public Mono<Void> execute(final ServerWebExchange exchange, final SoulPluginChain chain) {
        final ServerHttpRequest request = exchange.getRequest();
        final SoulContext soulContext = exchange.getAttribute(Constants.REQUESTDTO);
        if (Objects.nonNull(soulContext) && RpcTypeEnum.DUBBO.getName().equals(soulContext.getRpcType())) {
            MediaType mediaType = request.getHeaders().getContentType();
            ServerRequest serverRequest = ServerRequest.create(exchange, messageReaders);
            return serverRequest.bodyToMono(String.class)
                    .switchIfEmpty(Mono.defer(() -> Mono.just("")))
                    .flatMap(body -> {
                        if (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
                            exchange.getAttributes().put(Constants.DUBBO_PARAMS, body);
                        }
                        return chain.execute(exchange);
                    });
        }
        return chain.execute(exchange);
    }
    
    @Override
    public int getOrder() {
        return PluginEnum.DUBBO.getCode() - 1;
    }
}
