package ru.craftysoft.platform.gateway.interceptor;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcClientInterceptor implements ClientInterceptor {

    private final Logger requestLogger;
    private final Logger responseLogger;

    public GrpcClientInterceptor(Class<?> clientClass) {
        this.requestLogger = LoggerFactory.getLogger(clientClass.getName() + ".request");
        this.responseLogger = LoggerFactory.getLogger(clientClass.getName() + ".response");
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        var newCall = next.newCall(method, callOptions);
        return new LoggingClientCall<>(newCall, method);
    }

    private class LoggingClientCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

        private final MethodDescriptor<ReqT, RespT> method;
        private final JsonFormat.Printer printer;

        protected LoggingClientCall(ClientCall<ReqT, RespT> delegate, MethodDescriptor<ReqT, RespT> method) {
            super(delegate);
            this.method = method;
            this.printer = JsonFormat.printer();
        }

        @Override
        public void sendMessage(ReqT message) {
            if (requestLogger.isDebugEnabled() && message instanceof MessageOrBuilder messageOrBuilder) {
                try {
                    var json = printer.print(messageOrBuilder);
                    requestLogger.debug("method={}\nrequest={}", method.getBareMethodName(), json);
                } catch (InvalidProtocolBufferException e) {
                    requestLogger.error("ошибка при преобразовании сообщения в JSON", e);
                } catch (Exception e) {
                    responseLogger.error("ошибка", e);
                }
            }
            super.sendMessage(message);
        }

        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
            var listener = new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                @Override
                public void onMessage(RespT message) {
                    if (responseLogger.isDebugEnabled() && message instanceof MessageOrBuilder messageOrBuilder) {
                        try {
                            var json = printer.print(messageOrBuilder);
                            responseLogger.debug("method={}\nresponse={}", method.getBareMethodName(), json);
                        } catch (InvalidProtocolBufferException e) {
                            responseLogger.error("ошибка при преобразовании сообщения в JSON", e);
                        } catch (Exception e) {
                            responseLogger.error("ошибка", e);
                        }
                    }
                    super.onMessage(message);
                }
            };
            super.start(listener, headers);
        }
    }
}
