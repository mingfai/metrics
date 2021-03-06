package com.codahale.metrics.servlets.experiments;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jetty8.InstrumentedHandler;
import com.codahale.metrics.jetty8.InstrumentedQueuedThreadPool;
import com.codahale.metrics.jetty8.InstrumentedSelectChannelConnector;
import com.codahale.metrics.servlets.AdminServlet;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ThreadPool;

import static com.codahale.metrics.MetricRegistry.name;

public class ExampleServer {
    private static final MetricRegistry REGISTRY = new MetricRegistry();
    private static final Counter COUNTER_1 = REGISTRY.counter(name(ExampleServer.class,
                                                                   "wah",
                                                                   "doody"));
    private static final Counter COUNTER_2 = REGISTRY.counter(name(ExampleServer.class, "woo"));
    static {
        REGISTRY.register(name(ExampleServer.class, "boo"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                throw new RuntimeException("asplode!");
            }
        });
    }

    public static void main(String[] args) throws Exception {
        COUNTER_1.inc();
        COUNTER_2.inc();

        final Server server = new Server();

        final Connector connector = new InstrumentedSelectChannelConnector(REGISTRY, 8080, Clock.defaultClock());
        server.addConnector(connector);

        final ThreadPool threadPool = new InstrumentedQueuedThreadPool(REGISTRY);
        server.setThreadPool(threadPool);

        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/initial");
        context.setAttribute(MetricsServlet.METRICS_REGISTRY, REGISTRY);
        context.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, new HealthCheckRegistry());

        final ServletHolder holder = new ServletHolder(new AdminServlet());
        context.addServlet(holder, "/dingo/*");
        
        server.setHandler(new InstrumentedHandler(REGISTRY, context));
        
        server.start();
        server.join();
    }
}
