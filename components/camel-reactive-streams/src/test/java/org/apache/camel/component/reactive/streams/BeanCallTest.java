/**
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
package org.apache.camel.component.reactive.streams;

import io.reactivex.Flowable;

import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.reactive.streams.util.UnwrappingStreamProcessor;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.reactivestreams.Publisher;

/**
 *
 */
public class BeanCallTest extends CamelTestSupport {

    @Test
    public void beanCallTest() throws Exception {
        new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                onException(Throwable.class).to("direct:handle").handled(true);

                from("direct:num")
                        .bean(BeanCallTest.this, "processBody")
                        .process(new UnwrappingStreamProcessor()).split().body() // Can be removed?
                        .to("mock:endpoint");

                from("direct:handle")
                        .setBody().constant("ERR")
                        .to("mock:endpoint");

            }
        }.addRoutesToCamelContext(context);

        MockEndpoint mock = getMockEndpoint("mock:endpoint");
        mock.expectedMessageCount(1);

        context.start();

        template.sendBody("direct:num", 1);
        mock.assertIsSatisfied();

        Exchange exchange = mock.getExchanges().get(0);
        assertEquals("HelloBody 1", exchange.getIn().getBody());
    }

    @Test
    public void beanCallWithErrorTest() throws Exception {
        new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                onException(Throwable.class).to("direct:handle").handled(true);

                from("direct:num")
                        .bean(BeanCallTest.this, "processBodyWrongType")
                        .process(new UnwrappingStreamProcessor()).split().body() // Can be removed?
                        .to("mock:endpoint");

                from("direct:handle")
                        .setBody().constant("ERR")
                        .to("mock:endpoint");
            }
        }.addRoutesToCamelContext(context);

        MockEndpoint mock = getMockEndpoint("mock:endpoint");
        mock.expectedMessageCount(1);

        context.start();

        template.sendBody("direct:num", 1);
        mock.assertIsSatisfied();

        Exchange exchange = mock.getExchanges().get(0);
        assertEquals("ERR", exchange.getIn().getBody());
    }

    @Test
    public void beanCallHeaderMappingTest() throws Exception {
        new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                onException(Throwable.class).to("direct:handle").handled(true);

                from("direct:num")
                        .bean(BeanCallTest.this, "processHeader")
                        .process(new UnwrappingStreamProcessor()).split().body() // Can be removed?
                        .to("mock:endpoint");

                from("direct:handle")
                        .setBody().constant("ERR")
                        .to("mock:endpoint");
            }
        }.addRoutesToCamelContext(context);

        MockEndpoint mock = getMockEndpoint("mock:endpoint");
        mock.expectedMessageCount(1);

        context.start();

        template.sendBodyAndHeader("direct:num", 1, "myheader", 2);
        mock.assertIsSatisfied();

        Exchange exchange = mock.getExchanges().get(0);
        assertEquals("HelloHeader 2", exchange.getIn().getBody());
    }

    public Publisher<String> processBody(Publisher<Integer> data) {
        return Flowable.fromPublisher(data)
                .map(l -> "HelloBody " + l);
    }

    public Publisher<String> processBodyWrongType(Publisher<BeanCallTest> data) {
        return Flowable.fromPublisher(data)
                .map(l -> "HelloBody " + l);
    }

    public Publisher<String> processHeader(@Header("myheader") Publisher<Integer> data) {
        return Flowable.fromPublisher(data)
                .map(l -> "HelloHeader " + l);
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }
}
