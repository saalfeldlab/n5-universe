def isCompressionV1: has("compressionType") and ( .compressionType | type == "string" );

def makeRawV2: { "compression" : { "type": "raw" } };

def makeBzipV2( $blockSize ): { "compression": {
    "type": "bzip2",
    "blockSize": $blockSize }};

def makeGzipV2( $useZlib; $level ): { "compression": {
    "type": "gzip",
    "useZlib": $useZlib,
    "level": $level }};

def makeLzV2( $blockSize ): { "compression": {
    "type": "lz4",
    "blockSize": $blockSize }};

def makeXzV2( $preset ): { "compression": {
    "type": "xz",
    "preset": $preset }};

def compressionV1toV2: .compressionType as $type |
    if $type == "raw" then . + makeRawV2 | del(.compressionType)
    elif $type == "gzip" then . + makeGzipV2( false; -1 ) | del(.compressionType)
    elif $type == "bzip2" then . + makeBzipV2( 9 ) | del(.compressionType)
    elif $type == "lz4" then . + makeLzV2( 65536 ) | del(.compressionType)
    elif $type == "xz" then . + makeXzV2( 6 ) | del(.compressionType)
    else . end;

def convertCompression: if isCompressionV1 then compressionV1toV2 else . end;
