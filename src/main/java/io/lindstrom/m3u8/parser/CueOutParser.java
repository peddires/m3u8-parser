package io.lindstrom.m3u8.parser;

import io.lindstrom.m3u8.model.CueOut;

import java.util.Map;
import java.util.concurrent.atomic.DoubleAccumulator;

import static io.lindstrom.m3u8.parser.Tags.EXT_X_CUE_OUT;

public class CueOutParser extends AbstractLineParser<CueOut>{
    CueOutParser() {
        super(EXT_X_CUE_OUT);
    }

    @Override
    CueOut parseAttributes(Map<String, String> attributes) throws PlaylistParserException {
        CueOut.Builder builder = CueOut.builder();


        builder.duration(Double.valueOf(attributes.get("DURATION")));
        builder.id(Double.valueOf(attributes.get("ID")));
        builder.cue(attributes.get("CUE"));
//        builder.method(KeyMethod.parse(attributes.get(METHOD)));
//
//        if (attributes.containsKey(URI)) {
//            builder.uri(attributes.get(URI));
//        }
//        if (attributes.containsKey(IV)) {
//            builder.iv(attributes.get(IV));
//        }
//        if (attributes.containsKey(KEYFORMAT)) {
//            builder.keyFormat(attributes.get(KEYFORMAT));
//        }
//        if (attributes.containsKey(KEYFORMATVERSIONS)) {
//            builder.keyFormatVersions(attributes.get(KEYFORMATVERSIONS));
//        }
//
        return builder.build();
//    }

}

    @Override
    String writeAttributes(CueOut value) {
        return null;
    }
}
