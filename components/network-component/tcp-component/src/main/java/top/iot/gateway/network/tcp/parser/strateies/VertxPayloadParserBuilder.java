package top.iot.gateway.network.tcp.parser.strateies;

import top.iot.gateway.component.common.ValueObject;
import top.iot.gateway.network.tcp.parser.PayloadParser;
import top.iot.gateway.network.tcp.parser.PayloadParserBuilderStrategy;
import top.iot.gateway.network.tcp.parser.PayloadParserType;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class VertxPayloadParserBuilder implements PayloadParserBuilderStrategy {
    @Override
    public abstract PayloadParserType getType();

    protected abstract RecordParser createParser(ValueObject config);

    @Override
    public PayloadParser build(ValueObject config) {
        return new RecordPayloadParser(() -> createParser(config));
    }

    static class RecordPayloadParser implements PayloadParser {
        private final Supplier<RecordParser> recordParserSupplier;
        private final EmitterProcessor<Buffer> processor = EmitterProcessor.create(false);
        private final FluxSink<Buffer> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

        private RecordParser recordParser;

        public RecordPayloadParser(Supplier<RecordParser> recordParserSupplier) {
            this.recordParserSupplier = recordParserSupplier;
            reset();
        }

        @Override
        public void handle(Buffer buffer) {
            recordParser.handle(buffer);
        }

        @Override
        public Flux<Buffer> handlePayload() {
            return processor.map(Function.identity());
        }

        @Override
        public void close() {
            processor.onComplete();
        }

        @Override
        public void reset() {
            this.recordParser = recordParserSupplier.get();
            this.recordParser.handler(sink::next);
        }
    }

}
