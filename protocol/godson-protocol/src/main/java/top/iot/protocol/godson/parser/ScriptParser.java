package top.iot.protocol.godson.parser;//package org.godsonIot.core.protocol.parser;
//
//import io.vertx.core.buffer.Buffer;
//import io.vertx.core.parsetools.RecordParser;
//import lombok.extern.slf4j.Slf4j;
//import reactor.core.publisher.EmitterProcessor;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.FluxSink;
//
//import java.util.List;
//import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.function.Consumer;
//import java.util.function.Function;
//
///**
// * <pre>
// * PayloadParser parser = new PayloadParser();
// * parser.fixed(4)
// *       .handler(buffer -> {
// *            int len = BytesUtils.highBytes2Int(buffer.getBytes());
// *            parser.fixed(len);
// *         })
// *       .handler(buffer -> parser.result(buffer.toString("UTF-8")).complete());
// * </pre>
// */
//@Slf4j
//public class ScriptParser {
//
//    private final EmitterProcessor<Buffer> processor = EmitterProcessor.create(true);
//
//    private final FluxSink<Buffer> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
//
//    private final List<Consumer<Buffer>> pipe = new CopyOnWriteArrayList<>();
//
//    private final List<Buffer> result = new CopyOnWriteArrayList<>();
//
//    private volatile RecordParser recordParser;
//
//    private Function<Buffer, Buffer> directMapper;
//
//    private Consumer<RecordParser> firstInit;
//
//    private final AtomicInteger currentPipe = new AtomicInteger();
//
//    public ScriptParser result(String buffer) {
//        return result(Buffer.buffer(buffer));
//    }
//
//    public ScriptParser result(byte[] buffer) {
//        return result(Buffer.buffer(buffer));
//    }
//
//    public ScriptParser handler(Consumer<Buffer> handler) {
//        pipe.add(handler);
//        return this;
//    }
//
//    public ScriptParser delimited(String delimited) {
//        if (recordParser == null) {
//            setParser(RecordParser.newDelimited(delimited));
//            firstInit = (parser -> parser.delimitedMode(delimited));
//            return this;
//        }
//        recordParser.delimitedMode(delimited);
//        return this;
//    }
//
//    public ScriptParser fixed(int size) {
//        if (size == 0) {
//            complete();
//            return this;
//        }
//        if (recordParser == null) {
//            setParser(RecordParser.newFixed(size));
//            firstInit = (parser -> parser.fixedSizeMode(size));
//            return this;
//        }
//        recordParser.fixedSizeMode(size);
//        return this;
//    }
//
//    public ScriptParser direct(Function<Buffer, Buffer> mapper) {
//        this.directMapper = mapper;
//        return this;
//    }
//
//    private Consumer<Buffer> getNextHandler() {
//        int i = currentPipe.getAndIncrement();
//        if (i < pipe.size()) {
//            return pipe.get(i);
//        }
//        currentPipe.set(0);
//        return pipe.get(0);
//    }
//
//    private void setParser(RecordParser parser) {
//        this.recordParser = parser;
//        this.recordParser.handler(buffer -> getNextHandler().accept(buffer));
//    }
//
//    public ScriptParser complete() {
//        currentPipe.set(0);
//        if (recordParser != null) {
//            firstInit.accept(recordParser);
//        }
//        if (!this.result.isEmpty()) {
//            Buffer buffer = Buffer.buffer();
//            for (Buffer buf : this.result) {
//                buffer.appendBuffer(buf);
//            }
//            this.result.clear();
//            sink.next(buffer);
//        }
//        return this;
//
//    }
//
//    public ScriptParser result(Buffer buffer) {
//        this.result.add(buffer);
//        return this;
//    }
//
//    public synchronized void handle(Buffer buffer) {
//        if (recordParser == null && directMapper == null) {
//            log.error("record parser not init");
//            return;
//        }
//        if (recordParser != null) {
//            recordParser.handle(buffer);
//            return;
//        }
//        Buffer buf = directMapper.apply(buffer);
//        if (null != buf) {
//            sink.next(buf);
//        }
//    }
//
//    public Flux<Buffer> handlePayload() {
//        return processor.map(Function.identity());
//    }
//
//    public void reset() {
//        this.result.clear();
//        complete();
//    }
//
//    public void close() {
//        processor.onComplete();
//        currentPipe.set(0);
//        this.result.clear();
//    }
//
//}
