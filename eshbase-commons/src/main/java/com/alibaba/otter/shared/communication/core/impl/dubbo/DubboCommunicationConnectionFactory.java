/*
 * Copyright (C) 2010-2101 Alibaba Group Holding Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.otter.shared.communication.core.impl.dubbo;

import java.text.MessageFormat;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;
import com.alibaba.otter.shared.communication.core.CommunicationEndpoint;
import com.alibaba.otter.shared.communication.core.impl.connection.CommunicationConnection;
import com.alibaba.otter.shared.communication.core.impl.connection.CommunicationConnectionFactory;
import com.alibaba.otter.shared.communication.core.model.CommunicationParam;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * dubbo rpc服务链接的factory
 * 
 * @author jianghang 2011-11-29 上午11:13:31
 * @version 4.0.0
 */
public class DubboCommunicationConnectionFactory implements CommunicationConnectionFactory {

	private final String DUBBO_SERVICE_URL = "dubbo://{0}:{1}/endpoint?client=netty&codec=dubbo&serialization=java&lazy=true&iothreads=4&threads=50&connections=30&acceptEvent.timeout=50000";

	private DubboProtocol protocol = DubboProtocol.getDubboProtocol();
	private ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class)
			.getExtension("javassist");

	private LoadingCache<String, CommunicationEndpoint> connections = null;

	public DubboCommunicationConnectionFactory() {
		connections = CacheBuilder.newBuilder().maximumSize(1000) // 最多可以缓存1000个key
				.build(new CacheLoader<String, CommunicationEndpoint>() {

					@Override
					public CommunicationEndpoint load(String serviceUrl) throws Exception {
						return proxyFactory
								.getProxy(protocol.refer(CommunicationEndpoint.class, URL.valueOf(serviceUrl)));
					}
				});
	}

	public CommunicationConnection createConnection(CommunicationParam params) {
		if (params == null) {
			throw new IllegalArgumentException("param is null!");
		}
		try {
			// 构造对应的url
			String serviceUrl = MessageFormat.format(DUBBO_SERVICE_URL, params.getIp(),
					String.valueOf(params.getPort()));
			CommunicationEndpoint endpoint = connections.get(serviceUrl);
			return new DubboCommunicationConnection(params, endpoint);
		} catch (Exception ex) {
			return null;
		}

	}

	public void releaseConnection(CommunicationConnection connection) {
		// do nothing
	}

}
