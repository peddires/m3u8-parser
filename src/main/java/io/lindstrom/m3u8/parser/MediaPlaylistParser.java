package io.lindstrom.m3u8.parser;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.model.PlaylistType;

import java.time.OffsetDateTime;
import java.util.Iterator;

import static io.lindstrom.m3u8.parser.Tags.*;

/**
 * MediaPlaylistParser can read and write Media Playlists according to RFC 8216 (HTTP Live Streaming).
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * MediaPlaylistParser parser = new MediaPlaylistParser();
 *
 * // Parse playlist
 * MediaPlaylist playlist = parser.readPlaylist(Paths.get("path/to/media-playlist.m3u8"));
 *
 * // Update playlist version
 * MediaPlaylist updated = MediaPlaylist.builder()
 *                                      .from(playlist)
 *                                      .version(2)
 *                                      .build();
 *
 * // Write playlist to standard out
 * System.out.println(parser.writePlaylistAsString(updated));
 * }
 * </pre>
 *
 * This implementation is reusable and thread safe.
 */
public class MediaPlaylistParser extends AbstractPlaylistParser<MediaPlaylist, MediaPlaylistParser.Builder> {
    private final ByteRangeParser byteRangeParser = new ByteRangeParser();
    private final SegmentMapParser segmentMapParser = new SegmentMapParser(byteRangeParser);
    private final SegmentKeyParser segmentKeyParser = new SegmentKeyParser();
    private final CueOutParser cueOutParser = new CueOutParser();

    @Override
    Builder newBuilder() {
        return new Builder();
    }

    @Override
    void onTag(Builder builderWrapper, String prefix, String attributes, Iterator<String> lineIterator) throws PlaylistParserException {
        MediaPlaylist.Builder builder = builderWrapper.playlistBuilder;
        MediaSegment.Builder mediaSegmentBuilder = builderWrapper.segmentBuilder;

        switch (prefix) {
            case EXT_X_VERSION:
                builder.version(Integer.parseInt(attributes));
                break;

            case EXT_X_INDEPENDENT_SEGMENTS:
                builder.independentSegments(true);
                break;

            case EXT_X_PLAYLIST_TYPE:
                builder.playlistType(PlaylistType.valueOf(attributes));
                break;

            case EXT_X_I_FRAMES_ONLY:
                builder.iFramesOnly(true);
                break;

            case EXT_X_BYTERANGE:
                mediaSegmentBuilder.byteRange(byteRangeParser.parse(attributes));
                break;

            case EXT_X_TARGETDURATION:
                builder.targetDuration(Integer.parseInt(attributes));
                break;

            case EXTINF:
                String[] values = attributes.split(",", 2);
                mediaSegmentBuilder.duration(Double.parseDouble(values[0]));
                if (values.length == 2 && !values[1].isEmpty()) {
                    mediaSegmentBuilder.title(values[1]);
                }
                break;

            case EXT_X_PROGRAM_DATE_TIME:
                mediaSegmentBuilder.programDateTime(OffsetDateTime.parse(attributes));
                break;

            case EXT_X_MAP:
                mediaSegmentBuilder.segmentMap(segmentMapParser.parse(attributes));
                break;

            case EXT_X_ENDLIST:
                builder.ongoing(false);
                break;

            case EXT_X_MEDIA_SEQUENCE:
                builder.mediaSequence(Integer.parseInt(attributes));
                break;

            case EXT_X_KEY:
                mediaSegmentBuilder.segmentKey(segmentKeyParser.parse(attributes));
                break;

            case EXT_X_DISCONTINUITY:
                mediaSegmentBuilder.discontinuity(true);
                break;

            case EXT_X_CUE_OUT:
                mediaSegmentBuilder.cueOut(cueOutParser.parse(attributes));
                break;

            case EXT_X_DISCONTINUITY_SEQUENCE:
            case EXT_X_DATERANGE:
            case EXT_X_START:
            default:
                throw new PlaylistParserException("Tag not implemented: " + prefix);
        }
    }

    @Override
    void onURI(Builder builderWrapper, String uri) {
        builderWrapper.segmentBuilder.uri(uri);
        builderWrapper.playlistBuilder.addMediaSegments(builderWrapper.segmentBuilder.build());
        builderWrapper.segmentBuilder = MediaSegment.builder();
    }

    @Override
    MediaPlaylist build(Builder builderWrapper) {
        return builderWrapper.playlistBuilder.build();
    }

    @Override
    void write(MediaPlaylist playlist, StringBuilder stringBuilder) {
        if (playlist.iFramesOnly()) {
            stringBuilder.append(EXT_X_I_FRAMES_ONLY).append("\n");
        }

        playlist.playlistType().ifPresent(value ->
                stringBuilder.append(EXT_X_PLAYLIST_TYPE).append(":")
                        .append(value.toString()).append('\n'));

        stringBuilder.append(EXT_X_TARGETDURATION).append(":").append(playlist.targetDuration()).append("\n");
        stringBuilder.append(EXT_X_MEDIA_SEQUENCE).append(":").append(playlist.mediaSequence()).append("\n");

        playlist.mediaSegments().forEach(mediaSegment ->
                writeMediaSegment(mediaSegment, stringBuilder));

        if (!playlist.ongoing()) {
            stringBuilder.append(EXT_X_ENDLIST).append('\n');
        }
    }

    private void writeMediaSegment(MediaSegment mediaSegment, StringBuilder stringBuilder) {

        // EXT-X-DISCONTINUITY
        if (mediaSegment.discontinuity()) {
            stringBuilder.append(EXT_X_DISCONTINUITY).append('\n');
        }

        // EXT-X-PROGRAM-DATE-TIME
        mediaSegment.programDateTime().ifPresent(value -> stringBuilder
                .append(EXT_X_PROGRAM_DATE_TIME).append(':')
                .append(value)
                .append('\n'));

        // EXT-X-MAP
        mediaSegment.segmentMap().ifPresent(map -> segmentMapParser.write(map, stringBuilder));

        // EXTINF
        stringBuilder.append(EXTINF).append(":").append(mediaSegment.duration()).append(",");
        mediaSegment.title().ifPresent(stringBuilder::append);
        stringBuilder.append('\n');

        // EXT-X-BYTERANGE
        mediaSegment.byteRange().ifPresent(byteRange -> byteRangeParser.write(byteRange, stringBuilder));

        // EXT-X-KEY
        mediaSegment.segmentKey().ifPresent(key -> segmentKeyParser.write(key, stringBuilder));

        // <URI>
        stringBuilder.append(mediaSegment.uri()).append('\n');
    }

    /**
     * Wrapper class for playlist and segment builders
     */
    static class Builder {
        private final MediaPlaylist.Builder playlistBuilder = MediaPlaylist.builder();
        private MediaSegment.Builder segmentBuilder = MediaSegment.builder();
    }
}
