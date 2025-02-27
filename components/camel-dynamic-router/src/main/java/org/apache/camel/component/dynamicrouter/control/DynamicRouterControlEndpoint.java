/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.dynamicrouter.control;

import java.util.Optional;
import java.util.function.Supplier;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.dynamicrouter.DynamicRouterFilterService;
import org.apache.camel.component.dynamicrouter.control.DynamicRouterControlProducer.DynamicRouterControlProducerFactory;
import org.apache.camel.component.dynamicrouter.routing.DynamicRouterComponent;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

import static org.apache.camel.component.dynamicrouter.control.DynamicRouterControlConstants.COMPONENT_SCHEME_CONTROL;
import static org.apache.camel.component.dynamicrouter.control.DynamicRouterControlConstants.CONTROL_ACTION_LIST;
import static org.apache.camel.component.dynamicrouter.control.DynamicRouterControlConstants.CONTROL_ACTION_SUBSCRIBE;
import static org.apache.camel.component.dynamicrouter.control.DynamicRouterControlConstants.CONTROL_ACTION_UNSUBSCRIBE;
import static org.apache.camel.component.dynamicrouter.control.DynamicRouterControlConstants.CONTROL_PRODUCER_FACTORY_SUPPLIER;
import static org.apache.camel.component.dynamicrouter.control.DynamicRouterControlConstants.FIRST_VERSION_CONTROL;
import static org.apache.camel.component.dynamicrouter.control.DynamicRouterControlConstants.SYNTAX_CONTROL;
import static org.apache.camel.component.dynamicrouter.control.DynamicRouterControlConstants.TITLE_CONTROL;
import static org.apache.camel.component.dynamicrouter.routing.DynamicRouterConstants.COMPONENT_SCHEME_ROUTING;

/**
 * The Dynamic Router control endpoint for operations that allow routing participants to subscribe or unsubscribe to
 * participate in dynamic message routing.
 */
@UriEndpoint(firstVersion = FIRST_VERSION_CONTROL,
             scheme = COMPONENT_SCHEME_CONTROL,
             title = TITLE_CONTROL,
             syntax = SYNTAX_CONTROL,
             producerOnly = true,
             headersClass = DynamicRouterControlConstants.class,
             category = { Category.MESSAGING })
public class DynamicRouterControlEndpoint extends DefaultEndpoint {

    @UriPath(description = "Control action",
             enums = CONTROL_ACTION_SUBSCRIBE + "," + CONTROL_ACTION_UNSUBSCRIBE + "," + CONTROL_ACTION_LIST)
    @Metadata(required = true)
    private final String controlAction;

    /**
     * The component/endpoint configuration.
     */
    @UriParam
    private final DynamicRouterControlConfiguration configuration;

    /**
     * Service that manages {@link org.apache.camel.component.dynamicrouter.PrioritizedFilter}s for the Dynamic Router
     * channels.
     */
    private volatile DynamicRouterFilterService filterService;

    /**
     * Creates a {@link DynamicRouterControlProducer} instance.
     */
    private final Supplier<DynamicRouterControlProducerFactory> controlProducerFactorySupplier;

    /**
     * Creates the instance.
     *
     * @param uri           the URI that was used to cause the endpoint creation
     * @param component     the routing component to handle management of subscriber information from routing
     *                      participants
     * @param configuration the component/endpoint configuration
     */
    public DynamicRouterControlEndpoint(String uri, DynamicRouterControlComponent component, String controlAction,
                                        DynamicRouterControlConfiguration configuration,
                                        Supplier<DynamicRouterControlProducerFactory> controlProducerFactorySupplier) {
        super(uri, component);
        this.controlAction = controlAction;
        this.configuration = configuration;
        this.configuration.setControlAction(controlAction);
        this.controlProducerFactorySupplier = controlProducerFactorySupplier;
    }

    /**
     * Creates the instance.
     *
     * @param uri           the URI that was used to cause the endpoint creation
     * @param component     the routing component to handle management of subscriber information from routing
     *                      participants
     * @param configuration the component/endpoint configuration
     */
    public DynamicRouterControlEndpoint(String uri, DynamicRouterControlComponent component, String controlAction,
                                        DynamicRouterControlConfiguration configuration) {
        super(uri, component);
        this.controlAction = controlAction;
        this.configuration = configuration;
        this.configuration.setControlAction(controlAction);
        this.controlProducerFactorySupplier = CONTROL_PRODUCER_FACTORY_SUPPLIER;
    }

    @Override
    public DynamicRouterControlComponent getComponent() {
        return (DynamicRouterControlComponent) super.getComponent();
    }

    /**
     * Creates the {@link DynamicRouterControlProducer}.
     *
     * @return the {@link DynamicRouterControlProducer}
     * @see    DefaultEndpoint for more information about the producer creation process
     */
    @Override
    public Producer createProducer() {
        return controlProducerFactorySupplier.get().getInstance(this, filterService, configuration);
    }

    /**
     * This is a producer-only component.
     *
     * @param  processor not applicable to producer-only component
     * @return           not applicable to producer-only component
     */
    @Override
    public Consumer createConsumer(final Processor processor) {
        throw new IllegalStateException("Dynamic Router is a producer-only component");
    }

    /**
     * Starts the endpoint.
     *
     * @throws Exception when the component could not be found
     * @see              DefaultEndpoint#doStart() for more information about the startup process
     */
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        this.filterService = Optional.ofNullable(
                getCamelContext().getComponent(COMPONENT_SCHEME_ROUTING, DynamicRouterComponent.class))
                .map(DynamicRouterComponent::getFilterService)
                .orElseThrow(() -> new IllegalStateException("DynamicRouter component could not be found"));
    }

    /**
     * Gets the control action for this endpoint. It will be either "subscribe" or "unsubscribe"
     *
     * @return the control action -- either "subscribe" or "unsubscribe"
     */
    public String getControlAction() {
        return this.controlAction;
    }

    /**
     * Gets the {@link DynamicRouterControlConfiguration}.
     *
     * @return the {@link DynamicRouterControlConfiguration}
     */
    public DynamicRouterControlConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Factory to create the {@link DynamicRouterControlEndpoint}.
     */
    public static class DynamicRouterControlEndpointFactory {

        /**
         * Gets an instance of a {@link DynamicRouterControlEndpoint}.
         *
         * @param  uri           the URI that was used to trigger the creation of the endpoint
         * @param  component     the {@link DynamicRouterControlComponent}
         * @param  configuration the {@link DynamicRouterControlConfiguration}
         * @return               the {@link DynamicRouterControlEndpoint}
         */
        public DynamicRouterControlEndpoint getInstance(
                final String uri,
                final DynamicRouterControlComponent component,
                final String controlAction,
                final DynamicRouterControlConfiguration configuration) {
            return new DynamicRouterControlEndpoint(uri, component, controlAction, configuration);
        }

        /**
         * Gets an instance of a {@link DynamicRouterControlEndpoint}.
         *
         * @param  uri                            the URI that was used to trigger the creation of the endpoint
         * @param  component                      the {@link DynamicRouterControlComponent}
         * @param  configuration                  the {@link DynamicRouterControlConfiguration}
         * @param  controlProducerFactorySupplier the {@link DynamicRouterControlProducerFactory}
         * @return                                the {@link DynamicRouterControlEndpoint}
         */
        public DynamicRouterControlEndpoint getInstance(
                final String uri,
                final DynamicRouterControlComponent component,
                final String controlAction,
                final DynamicRouterControlConfiguration configuration,
                Supplier<DynamicRouterControlProducerFactory> controlProducerFactorySupplier) {
            return new DynamicRouterControlEndpoint(
                    uri, component, controlAction, configuration,
                    controlProducerFactorySupplier);
        }
    }
}
